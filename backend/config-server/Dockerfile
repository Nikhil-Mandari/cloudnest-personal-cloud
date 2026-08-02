# syntax=docker/dockerfile:1

# ═══════════════════════════════════════════════════════════════
# CloudNest Config Server — Production Dockerfile
# ═══════════════════════════════════════════════════════════════
# Multi-stage build:
#   Stage 1 (build):   Maven + Eclipse Temurin JDK 25
#   Stage 2 (runtime): Eclipse Temurin JRE 25 (slim, non-root)
#
# The configuration repository (backend/config-repo) is NOT baked into
# the image. It is mounted read-only at /app/config-repo by docker-compose
# (see CONFIG_REPO_LOCATION). For a fully self-contained production build,
# COPY config-repo into the image and point CONFIG_REPO_LOCATION at it.
# ═══════════════════════════════════════════════════════════════

# ── Stage 1: Build ──────────────────────────────────────────────
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

# Resolve dependencies first so the layer is cached until pom.xml changes
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline || true

# Compile and package (tests run in CI, skipped here for faster builds)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# ── Stage 2: Runtime ────────────────────────────────────────────
FROM eclipse-temurin:25-jre

LABEL org.opencontainers.image.title="CloudNest Config Server" \
      org.opencontainers.image.description="Centralized configuration server for CloudNest microservices" \
      org.opencontainers.image.version="0.0.1-SNAPSHOT" \
      org.opencontainers.image.authors="CloudNest"

# Minimal runtime tooling: curl (for HEALTHCHECK) + non-root app user
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 appuser \
    && useradd --system --uid 10001 --gid 10001 --no-create-home --home-dir /app appuser

WORKDIR /app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport" \
    SERVER_PORT=8888

# Copy the jar BEFORE dropping privileges (COPY runs as the current USER)
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8888

USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl --fail --silent --show-error http://localhost:8888/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
