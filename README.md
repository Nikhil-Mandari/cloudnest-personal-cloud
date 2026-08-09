# ☁️ CloudNest

<p align="center">
  <b>Your Personal Cloud. Your Files. Anywhere.</b><br/>
  A Full Stack Personal Cloud Storage & File Management Platform
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Status-Under%20Development-orange?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Backend-Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Frontend-React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB" />
  <img src="https://img.shields.io/badge/Database-MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Storage-Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white" />
</p>

---

## 📌 Overview

CloudNest is a **Full Stack Personal Cloud Storage & File Management System** built with **Java Spring Boot, React, TypeScript, MySQL, and Supabase Storage**.

It enables users to securely upload, organize, preview, search, and manage personal files from anywhere through a modern web interface.

---

## ✨ Core Features (Planned)

- 🔐 JWT-based Authentication & Authorization
- 📁 Folder & File Management
- ☁️ Cloud File Upload (Supabase Storage)
- 🔍 File Search & Filters
- ⭐ Favorites & Quick Access
- 🗑️ Trash & Restore
- 👤 Profile Management
- 🛡️ Role-based Admin Controls
- 📊 Activity Logs & Dashboard Insights

---

## 🛠️ Tech Stack

### 🎨 Frontend
- React
- TypeScript
- Vite
- Tailwind CSS
- React Router
- Axios
- Framer Motion
- React Hook Form

### ⚙️ Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- JWT Authentication
- Bean Validation
- Swagger (OpenAPI)

### 🗄️ Database & Storage
- MySQL
- Supabase Storage

### 🚀 Deployment
- Frontend: Vercel
- Backend: Render
- Database: MySQL
- Storage: Supabase

---

## 📂 Project Structure

```text
cloudnest/
├── docs/
├── frontend/
├── backend/
├── database/
├── docker/
├── postman/
├── .github/
└── README.md
```

---

## 📖 Documentation

- ✅ `docs/01_PROJECT_CONTEXT.md` — Project context and vision
- ⏳ `docs/02_REQUIREMENTS.md` — Functional and non-functional requirements
- ⏳ `docs/03_ARCHITECTURE.md` — System architecture
- ⏳ `docs/04_DATABASE_DESIGN.md` — Database schema
- ⏳ `docs/05_API_CONTRACT.md` — API design and endpoints

---

## 🧭 Development Roadmap

- [x] Project initialization
- [x] Project context documentation
- [ ] Authentication module
- [ ] User module
- [ ] File upload module
- [ ] Folder management module
- [ ] Search module
- [ ] Sharing module
- [ ] Admin dashboard
- [ ] Deployment & production hardening

---

## 🧪 Local Setup (Planned Standard Workflow)

```bash
# 1) Clone repository
git clone https://github.com/Nikhil-Mandari/cloudnest.git

# 2) Move into project
cd cloudnest

# 3) Start frontend and backend (instructions will be added module-wise)
```

---

## 🐳 Docker Setup (Backend)

The entire backend is containerized with production-ready multi-stage Dockerfiles
(**Eclipse Temurin JDK 25** build stage → slim **JRE 25** runtime, non-root user,
HEALTHCHECK, `curl` for health probes) and a single `docker-compose.yml` at the
project root.

**Prerequisites:** Docker Engine + Docker Compose v2 (BuildKit is the default).

> ⚠️ **Memory:** the full stack runs ~9 Spring Boot JVMs inside the Docker VM —
> **16 GB RAM recommended**. On an ~8 GB machine, run infrastructure + selected
> services only (see [`docker/README.md`](docker/README.md) → System Requirements).

### Quick start

```bash
# Build all images and start the full stack
docker compose up -d --build

# Container status / health
docker compose ps

# Follow all logs
docker compose logs -f
```

| Service              | Container name                  | Port | Endpoint                    |
|----------------------|---------------------------------|------|-----------------------------|
| API Gateway          | `cloudnest-api-gateway`         | 8080 | http://localhost:8080       |
| Auth Service         | `cloudnest-auth-service`        | 8081 | http://localhost:8081       |
| User Service         | `cloudnest-user-service`        | 8082 | http://localhost:8082       |
| File Service         | `cloudnest-file-service`        | 8083 | http://localhost:8083       |
| Folder Service       | `cloudnest-folder-service`      | 8084 | http://localhost:8084       |
| Notification Service | `cloudnest-notification-service`| 8085 | http://localhost:8085       |
| Share Service        | `cloudnest-share-service`       | 8086 | http://localhost:8086       |
| Eureka Server        | `cloudnest-eureka-server`       | 8761 | http://localhost:8761       |
| Config Server        | `cloudnest-config-server`       | 8888 | http://localhost:8888       |
| MySQL 8.4 (LTS)      | `cloudnest-mysql`               | 3307 | localhost:3307              |

### Useful commands

```bash
docker compose build              # build (or rebuild) all images
docker compose up -d              # start the stack in the background
docker compose up -d --build      # build + start
docker compose down               # stop & remove containers (keeps the MySQL volume)
docker compose down -v            # ⚠️ also delete the MySQL data volume (destructive)
docker compose logs -f <service>  # tail one service's logs (e.g. auth-service)
docker compose ps                 # status + health of every container
docker compose exec <service> sh  # open a shell inside a running container
docker exec -it cloudnest-mysql mysql -uroot -p   # MySQL CLI
docker image ls                   # list built images
docker system prune -a            # ⚠️ remove unused images / build cache
```

### Configuration

All knobs are environment variables (fully documented in
[`docker/README.md`](docker/README.md)). Create a `.env` file in the project root
to override defaults:

```bash
MYSQL_ROOT_PASSWORD=root
JWT_SECRET=<your-base64-encoded-secret>
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
SUPABASE_STORAGE_BUCKET=cloudnest-files
```

Local development is **unaffected**: every service keeps its `localhost` defaults
and only uses the Docker service names (`config-server`, `eureka-server`, `mysql`)
when the environment variables are provided by Docker Compose.

### Verify the stack

- **Eureka dashboard:** http://localhost:8761 → all services appear as `UP`
- **Config Server:** http://localhost:8888/auth-service/default → returns JSON config
- **Health:** http://localhost:8080/actuator/health (gateway) and every service port
- **JWT flow:** register/login through the gateway, then call protected routes with the token
- **Feign:** share-service resolves `user-service` / `folder-service` / `file-service` via Eureka

> 📖 Full walkthrough (build stages, networking, env vars, troubleshooting):
> [`docker/README.md`](docker/README.md)

---

## 🔒 Security Highlights

- BCrypt password hashing
- JWT token-based authentication
- Role-Based Access Control (RBAC)
- Input validation
- File ownership checks
- Global exception handling

---

## 📈 Project Status

🚧 **Under Active Development**  
CloudNest is currently being built module-by-module following clean architecture and production-ready engineering practices.

---

## 🤝 Contribution

Contributions, suggestions, and feedback are welcome.  
Once contribution guidelines are finalized, a dedicated `CONTRIBUTING.md` will be added.

---

## 📄 License

License information will be added soon.

---

<p align="center">
  Built with ❤️ by <b>Nikhil Mandari</b>
</p>
