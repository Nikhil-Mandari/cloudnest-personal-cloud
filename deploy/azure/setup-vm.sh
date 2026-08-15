#!/usr/bin/env bash
# CloudNest — VM setup + deploy (run ON the Azure VM as the admin user, with sudo)
# Usage: bash deploy/azure/setup-vm.sh <staging-domain> [git-clone-url]
# Example: bash deploy/azure/setup-vm.sh cloudnest.example.com
# Example: bash deploy/azure/setup-vm.sh cloudnest.example.com https://github.com/Nikhil-Mandari/cloudnest-personal-cloud.git
#
# Safe: installs Docker/Compose/Caddy, clones the repo (default: main), scaffolds .env
# from .env.example (YOU fill secrets), writes the Caddyfile, and runs docker compose up -d.
# Never commits .env. Never runs docker compose down -v.
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: bash deploy/azure/setup-vm.sh <staging-domain> [git-clone-url]"
  exit 1
fi
DOMAIN="$1"
REPO_URL="${2:-https://github.com/Nikhil-Mandari/cloudnest-personal-cloud.git}"
APP_DIR="${APP_DIR:-$HOME/cloudnest-personal-cloud}"

echo "==> Updating apt..."
sudo apt-get update -qq

echo "==> Installing Docker + Compose plugin..."
if ! command -v docker >/dev/null; then
  sudo apt-get install -y -qq ca-certificates curl gnupg
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
  sudo apt-get update -qq
  sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  sudo usermod -aG docker "$USER"
fi

echo "==> Installing Git + Caddy..."
sudo apt-get install -y -qq git
if ! command -v caddy >/dev/null; then
  sudo apt-get install -y -qq debian-keyring debian-archive-keyring apt-transport-https curl
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list >/dev/null
  sudo apt-get update -qq
  sudo apt-get install -y -qq caddy
fi

echo "==> Versions:"
docker --version
docker compose version
git --version
caddy version

echo "==> Cloning repo (branch: main)..."
if [ ! -d "$APP_DIR/.git" ]; then
  git clone --branch main --single-branch "$REPO_URL" "$APP_DIR"
else
  echo "   repo exists — pulling main"
  git -C "$APP_DIR" fetch origin main
  git -C "$APP_DIR" checkout main
  git -C "$APP_DIR" pull --ff-only origin main
fi
cd "$APP_DIR"

echo "==> Scaffolding .env from .env.example (fill secrets now!)..."
if [ ! -f .env ]; then
  cp .env.example .env
  echo "   .env created from .env.example — EDIT IT NOW: nano $APP_DIR/.env"
  echo "   Required: JWT_SECRET, MYSQL_ROOT_PASSWORD, MINIO_ROOT_USER/PASSWORD,"
  echo "             MAIL_USERNAME/PASSWORD/FROM, RAZORPAY_KEY_ID/SECRET/WEBHOOK_SECRET"
  echo "   NEVER commit this file (already gitignored)."
else
  echo "   .env already exists — leaving as-is."
fi

echo "==> Writing Caddyfile for $DOMAIN..."
sudo tee /etc/caddy/Caddyfile >/dev/null <<CADDY
$DOMAIN {
    encode gzip
    @api path /api/* /v3/api-docs* /swagger-ui/*
    handle @api {
        reverse_proxy 127.0.0.1:8080
    }
    handle {
        root * /var/www/cloudnest
        try_files {path} /index.html
        file_server
    }
}
CADDY

echo "==> Building frontend static bundle..."
cd "$APP_DIR/frontend"
if [ ! -d node_modules ]; then npm ci --no-audit --no-fund; fi
VITE_API_BASE_URL="https://$DOMAIN/api" npm run build
sudo mkdir -p /var/www/cloudnest
sudo cp -r dist/* /var/www/cloudnest/

echo "==> Deploying stack (this builds 10 service images — may take several minutes)..."
cd "$APP_DIR"
docker compose config --quiet && echo "   compose config OK"
docker compose up -d --build

echo "==> Waiting for services to stabilize (up to 5 minutes)..."
for i in $(seq 1 60); do
  sleep 5
  HEALTHY=$(docker compose ps --format '{{.Status}}' | grep -c 'healthy' || true)
  TOTAL=$(docker compose ps --format '{{.Status}}' | wc -l)
  echo "   [$(($i*5))s] healthy $HEALTHY/$TOTAL"
  [ "$HEALTHY" -eq "$TOTAL" ] && break
done

echo "==> Starting Caddy..."
sudo systemctl enable caddy
sudo systemctl restart caddy

echo
echo "==> DONE. Verify:"
echo "   docker compose ps            (expect 12/12 healthy)"
echo "   curl https://$DOMAIN/api/users/me   (expect 401 without token = gateway alive)"
echo "   curl -k https://$DOMAIN/    (frontend)"
echo "   Eureka: ssh -L 8761:localhost:8761 $USER@<vm-ip> then http://localhost:8761"
echo
echo "==> Remember: fill $APP_DIR/.env secrets BEFORE first real OTP/payment test, then:"
echo "   docker compose up -d && docker compose restart auth-service billing-service"
echo
echo "==> CORS note: staging serves frontend + API on the SAME origin ($DOMAIN),"
echo "   so the gateway's localhost-only CORS allowlist is never triggered."
echo "   If you later split origins, add the staging origin to CorsConfig.java."
