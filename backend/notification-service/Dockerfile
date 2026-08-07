# syntax=docker/dockerfile:1

# ═══════════════════════════════════════════════════════════════
# CloudNest Notification Service — Production Dockerfile
# ═══════════════════════════════════════════════════════════════
# Multi-stage build:
#   Stage 1 (build):   Maven + Eclipse Temurin JDK 25
#   Stage 2 (runtime): Eclipse Temurin JRE 25 (slim, non-root)
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

LABEL org.opencontainers.image.title="CloudNest Notification Service" \
      org.opencontainers.image.description="In-app notifications for share events, system alerts, and user activity" \
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
    SERVER_PORT=8085

# Copy the jar BEFORE dropping privileges (COPY runs as the current USER)
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8085

USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl --fail --silent --show-error http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
