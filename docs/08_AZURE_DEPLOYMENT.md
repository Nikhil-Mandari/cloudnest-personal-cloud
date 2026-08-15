# ☁️ CloudNest — Azure Deployment Guide

This document describes the **recommended Azure deployment model** for CloudNest and the
exact environment/secret configuration required. It deliberately keeps the existing
architecture (microservices + Eureka + Config Server + MySQL + MinIO) intact — the goal is
the simplest reliable production-ish deployment for a student/demo budget, not an
architecture rewrite.

---

## 1. Recommended model: **Azure VM running Docker Compose (Option B)**

### Why this model

CloudNest is a **12-container Docker Compose stack** with:

- **10 Java/Spring Boot services** (Eureka, Config Server, API Gateway, auth, user, file,
  folder, share, notification, billing) that discover each other via **Eureka** over a
  private compose network
- **MySQL 8.4** (persistent volume) and **MinIO** (persistent volume) for storage
- A **React frontend** (Vite) served separately

Compared against the alternatives for this specific project:

| Option | Fit | Reason |
|---|---|---|
| **A. Azure Container Apps** | ⚠️ Possible, higher effort | ACA gives per-service scaling and HTTPS, but Eureka-based `lb://` service discovery is LAN-oriented. You would need to register public/ACA URLs, manage 10 ingress bindings, and rework config-server/eureka wiring. Realistic only with a dedicated effort. |
| **B. Azure VM + Docker Compose** | ✅ **Recommended** | One VM runs the *exact* stack you test locally (`docker compose up -d`). Zero architecture change, full Eureka/config behavior preserved, lowest cost, easiest to debug. |
| **C. ACA for stateless + managed MySQL/Blob** | ❌ Highest effort | Splitting storage between MinIO and Azure Blob and services between ACA and VMs maximizes complexity for a demo deployment and risks breaking the working file-service path. |

**Decision: Option B — a single Azure VM (B2s or B2ms) running Docker Compose, fronted by
a TLS-capable reverse proxy, with MySQL + MinIO kept as named volumes inside the VM.**

This matches the project's constraints:

- **Budget** — B2s (2 vCPU / 4 GB) is the cheapest viable tier for ~10 JVMs; B2ms is
  comfortable. (The full local stack runs in roughly 4–8 GB of container memory.)
- **Persistence** — MySQL and MinIO data live on the VM's managed disk (`/var/lib/docker/volumes`),
  with snapshots as backup (see §19).
- **HTTPS** — a reverse proxy (Caddy or Nginx) terminates TLS and proxies to the gateway
  and frontend (see §15).
- **No architecture change** — the PR into `dev` stays exactly what is tested locally.

> If you later outgrow the VM, the natural next step is Option A (Azure Container Apps)
> with **Eureka replaced by direct config-server properties or service DNS**, and MinIO
> swapped for **Azure Blob Storage** via a new `ObjectStorage` adapter (§16). Do not do
> that now — it is a separate workstream.

---

## 2. Architecture at a glance

```
Internet
   │  HTTPS (443)
   ▼
Caddy / Nginx reverse proxy (VM)
   ├── / ............ frontend (http://localhost:5174 or static build)
   └── /api/** ...... API Gateway (http://localhost:8080)
                        ├── auth-service ......... :8081
                        ├── user-service ......... :8082
                        ├── file-service ......... :8083
                        ├── folder-service ....... :8084
                        ├── share-service ........ :8085
                        ├── notification-service . :8086
                        ├── billing-service ...... :8087
                        └── (Eureka :8761 / Config :8888 — internal, not exposed)
   MySQL 8.4 (volume) · MinIO (volume) · Eureka · Config Server (internal network)
```

The Eureka/config ports are **not** published externally; only the gateway (8080) and the
frontend are exposed. Microservices call each other over the private compose network
using Eureka service discovery, exactly as they do locally.

---

## 3. Prerequisites

1. An Azure subscription with a **Free trial / student** credit (Azure for Students).
2. A resource group, e.g. `cloudnest-rg` (region: e.g. `Central India` or `West Europe`).
3. A VM (Ubuntu 22.04 LTS / 24.04 LTS), size **B2s** minimum, **B2ms** recommended.
4. SSH key for the VM.
5. A DNS name (or Azure DNS zone) for the production domain, e.g. `cloudnest.example.com`.
6. Docker Engine + Compose v2 on the VM (`apt install docker.io docker-compose-v2`).
7. A GitHub account/token to pull the repo (or upload the repo tarball).
8. The external accounts already used by the app: Gmail SMTP (App Password),
   Razorpay TEST (or LIVE) keys, and optionally Google/GitHub OAuth apps.

---

## 4. Provisioning the VM (quick path)

```bash
# Azure CLI (local machine)
az group create --name cloudnest-rg --location centralindia
az vm create \
  --resource-group cloudnest-rg \
  --name cloudnest-vm \
  --image UbuntuLTS \
  --size Standard_B2ms \
  --admin-username azureuser \
  --generate-ssh-keys \
  --nsg-rule SSH

# open only 80/443 (and 22 for admin)
az vm open-port --resource-group cloudnest-rg --name cloudnest-vm --port 80
az vm open-port --resource-group cloudnest-rg --name cloudnest-vm --port 443
```

> ⚠️ Do **not** open 8080/5174/8761 to the internet. The reverse proxy is the only entry
> point. The ports stay bound to `localhost` inside the VM.

---

## 5. Environment variables (full checklist)

All of these are supplied at runtime via the VM's environment or a gitignored `.env`
file. **Never commit real values.** The tracked `backend/.env.example` is a safe template.

| Variable | Required | Purpose |
|---|---|---|
| `JWT_SECRET` | ✅ | Strong 32+ byte secret for token signing (app fails fast if weak). |
| `MYSQL_ROOT_PASSWORD` | ✅ | MySQL root password (and compose override). |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | ✅ | MinIO credentials. |
| `MINIO_BUCKET_NAME` | ✅ | Default bucket (e.g. `cloudnest`). |
| `MAIL_ENABLED` | ✅ | `true` for real SMTP (OTP delivery). |
| `MAIL_HOST` / `MAIL_PORT` | ✅ | `smtp.gmail.com` / `587`. |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | ✅ | Gmail account + App Password. |
| `MAIL_FROM` | ✅ | Sender address. |
| `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` | ✅ | `true` / `true`. |
| `RAZORPAY_ENABLED` | ✅ | `true` when keys are configured. |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | ✅ | Razorpay TEST or LIVE keys. |
| `RAZORPAY_WEBHOOK_SECRET` | ✅ | Webhook signature secret. |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` / `GOOGLE_REDIRECT_URI` | ◻️ | Google OAuth login. |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` / `GITHUB_REDIRECT_URI` | ◻️ | GitHub OAuth login. |
| `OAUTH_FRONTEND_BASE_URL` | ◻️ | Frontend origin for OAuth redirects. |

Compose already reads every one of these (`${VAR:-default}`), so provisioning is:
`cp backend/.env.example .env` → fill values → `docker compose up -d`.

---

## 6. Build & images

The compose file references local images (`cloudnest/<service>:latest`). On the VM:

```bash
git clone https://github.com/Nikhil-Mandari/cloudnest-personal-cloud.git
cd cloudnest-personal-cloud
# (optional) build all images
docker compose build
# or pull pre-built images if you publish them to a registry
docker compose up -d
```

For a repeatable pipeline, build once and push to **Azure Container Registry** (or Docker
Hub), then change `image:` lines to the registry URI and run `docker compose pull`.
This is recommended but not required for a demo.

---

## 7. Database (MySQL)

- Runs in the compose network as `cloudnest-mysql`, port 3306 internal only.
- Data persists in the named volume `mysql-data`.
- **Backup:** `docker exec cloudnest-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --all-databases' > backup.sql` on a schedule (cron), plus a VM disk snapshot.
- Restore: pipe the dump back into the container and restart services.

---

## 8. Object storage (MinIO)

- Runs as `cloudnest-minio` with the named volume `minio-data`.
- The file-service writes objects through `MinioService` (see §16 for the Azure Blob path).
- **Backup:** copy the volume (e.g. `docker run --rm -v minio-data:/data -v $(pwd):/backup alpine tar czf /backup/minio-data.tgz -C /data .`) or enable MinIO bucket replication. VM disk snapshots are the simplest safety net.

---

## 9. Service deployment order

`docker compose up -d` handles ordering via healthchecks:

1. **mysql**, **minio** (infrastructure)
2. **eureka-server**, **config-server** (discovery + config)
3. **api-gateway** (routes via Eureka)
4. **auth, user, file, folder, share, notification, billing** (business services)

Each service has a `healthcheck` with a generous `start_period` because the JVMs boot
slowly under constrained CPU. After a cold start, wait 3–5 minutes, then check
`docker compose ps` — all 12 should report `healthy`.

---

## 10. Eureka / Config Server requirements

- **Eureka** (`:8761`) and **Config Server** (`:8888`) stay **internal** (not published).
- Services register with their compose service names (`auth-service`, `user-service`, …)
  and the gateway resolves them via `lb://<service-name>`.
- Config Server serves `backend/config-repo/*.yml` — mount/copy that directory into the
  container image or a bind mount so the deployed config matches the repo.
- If you ever split services across machines, Eureka must be reachable by all of them and
  `eureka.client.serviceUrl.defaultZone` must point at the Eureka host; for a single VM
  the default compose networking already satisfies this.

---

## 11. API Gateway

- Published on `localhost:8080`; the reverse proxy forwards `/api/**` to it.
- Routes: `/api/auth/**`, `/api/users/**`, `/api/files/**`, `/api/folders/**`,
  `/api/shares/**`, `/api/notifications/**`, `/api/billing/**`, plus the file-service
  docs routes (`/v3/api-docs`, `/swagger-ui/**`).
- The gateway performs JWT authentication and forwards identity headers
  (`X-User-Id`, `X-User-Username`, `X-User-Email`, `X-User-Role`) to downstream services.

---

## 12. Frontend

Two options:

- **Option 1 (recommended for demo):** build once and serve statically behind the proxy:
  ```bash
  cd frontend && npm ci && npm run build
  # point the proxy's / at frontend/dist (e.g. Caddy file_server)
  ```
- **Option 2:** run the Vite dev server behind the proxy (not recommended for prod).

The frontend must know the **public** API base. Set `VITE_API_BASE_URL` (or the proxy
rewrite) so `http://localhost:8080/api` becomes `https://<domain>/api` at runtime.
If the frontend is served from the same origin as the gateway (same domain, `/api` path),
CORS is a non-issue; the existing CORS config already allows `localhost:5173/5174` for
local development.

---

## 13. SMTP (Gmail OTP)

- `MAIL_ENABLED=true` + Gmail App Password → real OTP emails to the exact address the
  user registered with. The plaintext OTP is never returned in API responses.
- If Gmail is unavailable in the deployment region or flagged, use any SMTP provider
  (e.g. Azure Communication Services Email) by changing `MAIL_HOST`/`MAIL_PORT` and
  keeping the same `MailSender` configuration in auth-service. No code change required.

---

## 14. Razorpay

- **TEST mode:** set `RAZORPAY_ENABLED=true` with TEST keys; order creation and checkout
  open with the official SDK; the server verifies the signature before upgrading quota.
- **LIVE mode:** switch to LIVE keys and register the webhook URL below in the Razorpay
  dashboard. The webhook is signature-verified server-side; the same code path upgrades
  the subscription exactly once (duplicate webhooks are idempotent).
- The final payment completion depends on the merchant account exposing a usable payment
  method (card/UPI) — this is external-provider configuration, not application code.

---

## 15. HTTPS / reverse proxy

**Recommended: Caddy** (automatic Let's Encrypt, single config file):

```caddyfile
cloudnest.example.com {
    @api path /api/*
    handle @api {
        reverse_proxy 127.0.0.1:8080
    }
    handle {
        root * /var/www/cloudnest/dist
        file_server
        try_files {path} /index.html
    }
}
```

Nginx equivalent: terminate TLS, `location /api/ { proxy_pass http://127.0.0.1:8080; }`,
and serve the static frontend with `try_files ... /index.html`.

Keep `443/80` open; **do not** expose raw service ports.

---

## 16. Storage strategy (MinIO today, Azure Blob later)

- The file-service speaks to MinIO through the `MinioService` interface
  (`MinioServiceImpl`). There is **no** generic `ObjectStorage` adapter yet.
- **For the VM deployment, keep MinIO** — it is S3-compatible, already wired, and
  persisted on the VM disk. Do not migrate storage code for a demo deployment.
- **Production growth path (separate workstream):** introduce an
  `ObjectStorage` interface with two implementations — `MinioStorage` (existing) and
  `AzureBlobStorage` — selected by configuration (`app.storage.provider=minio|azure`).
  `MinioClient` and the Azure Blob SDK both expose get/put/delete/presign, so the
  file-service call sites stay unchanged. Only do this when a managed-storage
  requirement actually exists.

---

## 17. Production callback URLs

| Purpose | URL |
|---|---|
| Google OAuth callback | `https://<domain>/api/auth/oauth/google/callback` |
| GitHub OAuth callback | `https://<domain>/api/auth/oauth/github/callback` |
| Razorpay webhook | `https://<domain>/api/billing/webhook/razorpay` |
| Frontend origin | `https://<domain>` |

Set `GOOGLE_REDIRECT_URI` / `GITHUB_REDIRECT_URI` / `OAUTH_FRONTEND_BASE_URL` to these
values. Register the OAuth callback in the Google/GitHub developer consoles and the
webhook in the Razorpay dashboard.

---

## 18. Health checks

- Compose healthchecks gate startup ordering (`start_period` tuned for slow JVM boots).
- Each Spring service exposes `/actuator/health` (`Up` includes a Gmail SMTP probe in
  auth-service — expect it to take a few seconds).
- The `scripts/health-check` helper verifies TCP + actuator health for the whole stack:
  `scripts/health-check` (all) or `scripts/health-check --infra-only`.
- Azure VM: an `az monitor`/custom probe on `https://<domain>/api/...` (401 without a
  token still proves the gateway is alive) can drive an Azure load-balancer/health probe
  if one is ever added.

---

## 19. Rollback, logs, backups

- **Rollback:** every service image is tagged; keep the previous tag and
  `docker compose up -d` with the old tag. For config-only rollbacks, revert
  `backend/config-repo/*.yml` and restart config-server + dependent services.
- **Logs:** `docker compose logs -f <service>`; ship to Azure Log Analytics later if
  needed (`docker logs` → Fluent Bit → Log Analytics).
- **Backups:**
  - MySQL: nightly `mysqldump` (cron) to `/backups`, plus VM disk snapshots.
  - MinIO: nightly volume tarball (cron) to `/backups`, plus VM disk snapshots.
  - Test restores from a snapshot at least once before relying on it.
- **Persistence:** never `docker compose down -v` in production — that deletes both
  named volumes.

---

## 20. Data persistence summary

| Data | Where | Persistence | Backup |
|---|---|---|---|
| MySQL rows | `mysql-data` volume | ✅ VM disk | mysqldump + snapshots |
| MinIO objects | `minio-data` volume | ✅ VM disk | volume tar + snapshots |
| Config | `backend/config-repo` (git) | ✅ repo | git |
| Secrets | VM `.env` (gitignored) | ✅ VM only | secret store (optional) |
| Session/tokens | JWT (stateless) + refresh token table | ✅ MySQL | via MySQL backup |

---

## Cost estimate (indicative, India region)

| Resource | Tier | Est. monthly |
|---|---|---|
| VM B2ms (2 vCPU / 8 GB, 32 GB disk) | Linux | ~₹1,200–1,500 |
| Azure DNS (optional) | — | minimal |
| Azure Container Registry (optional) | Basic | ~₹20 |

Azure for Students / free credits typically cover this comfortably.

---

## Quick start checklist

1. Create VM (B2ms, Ubuntu), open 80/443/22.
2. `git clone` repo on the VM.
3. `cp backend/.env.example .env` and fill all secrets.
4. `docker compose build && docker compose up -d`.
5. Wait 3–5 min; confirm `docker compose ps` shows 12/12 healthy.
6. Install Caddy; configure `cloudnest.example.com` → gateway + static frontend.
7. Point DNS at the VM's public IP.
8. Register Google/GitHub OAuth callbacks and the Razorpay webhook (§17).
9. Verify: signup OTP email, login, upload, share, quota, and a TEST payment.
