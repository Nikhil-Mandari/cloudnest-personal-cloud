# 🐳 CloudNest — Docker & Docker Compose (Backend)

Production-ready containerization for the entire CloudNest microservices backend:
**MySQL 8.4**, **Config Server**, **Eureka Server**, **API Gateway** and the six
business services (auth, user, file, folder, share, notification).

---

## 📦 What's Included

| Artifact | Location |
|---|---|
| 9 multi-stage Dockerfiles | `backend/<service>/Dockerfile` |
| 9 `.dockerignore` files | `backend/<service>/.dockerignore` |
| 1 Compose stack | `docker-compose.yml` (project root) |
| Env-driven configs | `backend/config-repo/*.yml`, `backend/**/application.yml` |

### Dockerfile design (every service)

- **Build stage:** `maven:3.9.11-eclipse-temurin-25` — Maven + **JDK 25**
- **Runtime stage:** `eclipse-temurin:25-jre` — slim JRE 25, keeps images small
- Multi-stage build: build tools & sources never land in the final image
- **Non-root user** (`appuser`, UID 1000) — no root process in containers
- `curl` installed for **HEALTHCHECK** probes against `/actuator/health`
- BuildKit cache mount on `/root/.m2` → fast incremental rebuilds
- Tests are skipped during image builds (`-DskipTests`); run them in CI instead
- Layer caching: dependency resolution runs before copying sources

---

## 🚀 Quick Start

```bash
# From the project root (where docker-compose.yml lives)
docker compose up -d --build
```

Wait for all containers to become healthy:

```bash
docker compose ps
# NAME                        STATUS
# cloudnest-eureka-server     Up (healthy)
# cloudnest-config-server     Up (healthy)
# cloudnest-api-gateway       Up (healthy)
# ...                         Up (healthy)
```

---

## 🗺️ Architecture & Networking

All containers join the `cloudnest-net` **bridge** network and reach each other by
**Docker service name** (DNS), not `localhost`:

```text
                    Client
                      │  :8080
                      ▼
               api-gateway ──► Eureka (lb://) discovery
                      │
    ┌─────────┬───────┼────────┬──────────┬───────────┐
    ▼         ▼       ▼        ▼          ▼           ▼
auth-service user  file-service folder  share  notification
(8081)     (8082)   (8083)    (8084)   (8086)   (8085)
    │         │       │        │        │  │
    └─────────┴───┬───┴────────┴────────┘  └── Feign (via Eureka)
                 ▼
               mysql:3306
    (config-server:8888 serves config to every service,
     eureka-server:8761 registers every service)
```

### Service names (in-container DNS)

| Compose service   | Docker DNS name     | Port |
|-------------------|---------------------|------|
| mysql             | `mysql`             | 3306 (host 3307) |
| eureka-server     | `eureka-server`     | 8761 |
| config-server     | `config-server`     | 8888 |
| auth-service      | `auth-service`      | 8081 |
| user-service      | `user-service`      | 8082 |
| file-service      | `file-service`      | 8083 |
| folder-service    | `folder-service`    | 8084 |
| notification-service | `notification-service` | 8085 |
| share-service     | `share-service`     | 8086 |
| api-gateway       | `api-gateway`       | 8080 |

---

## ⚙️ Configuration — Local vs Docker

Nothing was hard-coded to a Docker host. Every connection is now an environment
variable with a **localhost default**, so the stack runs identically:

- **Locally** (IDE / `mvn spring-boot:run`) → no env vars needed, defaults to
  `localhost` (unchanged behaviour).
- **In Docker** → Compose sets the vars to Docker service names.

| Env var | Default | Docker value | Used by |
|---|---|---|---|
| `CONFIG_SERVER_URL` | `http://localhost:8888` | `http://config-server:8888` | all services (config import) |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | `http://eureka-server:8761/eureka/` | all services + config-server |
| `CONFIG_REPO_LOCATION` | `../config-repo` | `/app/config-repo` | config-server (native search-locations) |
| `MYSQL_ROOT_PASSWORD` | `root` | from `.env` | mysql + all DB users |
| `AUTH_DB_URL` / `USER_DB_URL` / `FILE_DB_URL` / `FOLDER_DB_URL` / `SHARE_DB_URL` / `NOTIFICATION_DB_URL` | `jdbc:mysql://localhost:3306/<db>…` | `jdbc:mysql://mysql:3306/<db>…` | each service |
| `AUTH_DB_USERNAME` … `NOTIFICATION_DB_USERNAME` | `root` | `root` | each service |
| `AUTH_DB_PASSWORD` … `NOTIFICATION_DB_PASSWORD` | `root` | `root` | each service |
| `JWT_SECRET` | built-in dev secret | from `.env` | auth-service, api-gateway |
| `SUPABASE_URL` / `SUPABASE_ANON_KEY` / `SUPABASE_SERVICE_ROLE_KEY` / `SUPABASE_STORAGE_BUCKET` | placeholders | from `.env` | file-service |

Databases (`auth_db`, `user_db`, `file_db`, `folder_db`, `share_db`,
`notification_db`) are created automatically on first connect
(`createDatabaseIfNotExist=true`).

> **Production note:** point the Config Server at a Git backend
> (`spring.cloud.config.server.git.uri`) and set `JWT_SECRET`,
> `MYSQL_ROOT_PASSWORD`, and Supabase keys via a secret manager instead of `.env`.

---

## 🔁 Health Checks & Startup Order

- Every image declares a `HEALTHCHECK` (curl → `/actuator/health`); Compose
  re-declares it per service with tuned intervals.
- MySQL uses `mysqladmin ping` with a `start_period` (first boot initializes data).
- `depends_on: condition: service_healthy` guarantees startup order:

```text
mysql ──┐
eureka-server ──┐
                ├──► config-server ──► auth/user/file/folder/share/notification ──► api-gateway
```

- `restart: unless-stopped` brings any crashed container back automatically.

---

## 🧰 All Docker Commands

### Build

```bash
# Everything (10 images)
docker compose build

# One service (faster iteration)
docker compose build auth-service

# Individual image (from a service dir)
cd backend/auth-service && docker build -t cloudnest/auth-service .

# Force rebuild without cache
docker compose build --no-cache auth-service
```

### Run / Stop

```bash
docker compose up -d              # start stack (background)
docker compose up -d --build      # build + start
# Foreground + attach logs (one service):
docker compose up auth-service
docker compose down               # stop + remove containers (volume kept)
docker compose down -v            # ⚠️ also delete MySQL data volume
docker compose restart auth-service
```

### Inspect

```bash
docker compose ps                 # status + health
docker compose logs -f            # tail ALL logs
docker compose logs -f auth-service
docker compose top                # running processes per container
docker ps                         # raw container list
docker image ls                   # built images
```

### Exec / Debug

```bash
docker compose exec auth-service sh                 # shell inside a container
docker compose exec mysql mysql -uroot -p           # MySQL client
docker exec -it cloudnest-mysql mysql -uroot -p    # same via container name
docker inspect cloudnest-auth-service              # full container metadata
```

### Cleanup

```bash
docker compose down
docker system prune -a          # ⚠️ removes unused images, containers, build cache
docker volume rm cloudnest-mysql-data   # only when you really want to wipe data
```

---

## ✅ Verification Checklist

| # | Check | Command / URL |
|---|---|---|
| 1 | **Service registration** | http://localhost:8761 — all 7 services `UP` |
| 2 | **Config Server** | `curl http://localhost:8888/auth-service/default` returns JSON |
| 3 | **Gateway routing** | `curl -s http://localhost:8080/api/auth/login -X POST` (routes to auth-service) |
| 4 | **MySQL connectivity** | `docker compose exec mysql mysql -uroot -p -e "SHOW DATABASES;"` → 6 `*_db` schemas |
| 5 | **JWT authentication** | register → login → use `Authorization: Bearer <token>` on `/api/files/**` |
| 6 | **Feign communication** | create a share → share-service calls user/folder/file via Eureka |
| 7 | **Health endpoints** | `curl -s http://localhost:8080/actuator/health` → `"UP"` |

---

## 💻 System Requirements (Local Dev)

The full Compose stack runs **11 containers**: MySQL, MinIO, Eureka Server, Config
Server, the six business services (auth, user, file, folder, share, notification)
and the API Gateway — **9 Java/Spring Boot JVMs** plus infrastructure, all inside
the Docker Desktop WSL VM.

- **Recommended: 16 GB RAM or more** for comfortable full-stack local development.
- **8 GB:** suitable for **partial / staggered** development and testing only
  (start infra first, then one or two services — see "Local Development" below).

### Observed behaviour on an ~8 GB machine

- With the complete stack running, Docker Desktop/WSL hit memory pressure and the
  Docker engine became unstable: containers eventually became unavailable and
  `docker` CLI calls hung.
- The instability was caused by the Docker VM and multiple Spring Boot JVMs
  competing for memory (each JVM uses roughly 300–700 MB during startup).
- Stopping the stack (`docker compose stop`) restored memory and Docker
  responsiveness — no data loss.
- MySQL and MinIO data lives in **named volumes**, so it persists across
  stop/restart (`docker compose down` keeps volumes; only `down -v` deletes them).

> 8 GB can still run the stack in parts, and 16 GB is a recommendation, not a
> guarantee of performance. If the engine becomes unresponsive, stop the stack and
> restart Docker Desktop before continuing.

---

## 🩺 Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| Container in `unhealthy` or restart loop | `docker compose logs <svc>`; check config-server reachability (`CONFIG_SERVER_URL`) and MySQL credentials |
| `Connection refused` to config-server / eureka | Let infra start first: `docker compose up -d mysql eureka-server config-server` then the rest |
| `Communications link failure` to MySQL | MySQL still initializing (first boot) — wait for `cloudnest-mysql` healthy, or check `MYSQL_ROOT_PASSWORD` |
| Service not visible in Eureka | Verify `EUREKA_SERVER_URL` env and `eureka.client.register-with-eureka`; check `docker compose logs <svc>` |
| 401 from gateway on protected routes | JWT secret mismatch — set the same `JWT_SECRET` in `.env` for auth-service and api-gateway |
| Port already in use (8080, 3306…) | Change `ports` mapping, e.g. `"8081:8081"` → `"18081:8081"`. The compose file already publishes MySQL on host port **3307** (`3307:3306`) to avoid clashing with a local MySQL service — connect from the host via `localhost:3307`, containers keep using `mysql:3306` |
| Stale code in image | `docker compose build --no-cache <svc>` |

---

## 🔧 Local Development (unchanged)

Nothing here breaks running services from your IDE:

```bash
# Start infra independently (or run MySQL locally)
docker compose up -d mysql eureka-server config-server

# Then run any service from its own directory
cd backend/auth-service && mvn spring-boot:run
```

The `CONFIG_REPO_LOCATION` default (`../config-repo`) assumes the Config Server
is started from `backend/config-server`. If you start it from the repo root, set
`CONFIG_REPO_LOCATION=file:./backend/config-repo`.