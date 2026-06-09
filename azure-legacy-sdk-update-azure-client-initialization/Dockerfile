# ──────────────────────────────────────────────────────────────────────────────
# Stage 1 – Build
# ──────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Cache dependencies before copying source
COPY pom.xml .
RUN mvn dependency:go-offline --batch-mode -q

# Build the executable fat JAR
COPY src ./src
RUN mvn package -DskipTests --batch-mode -q && \
    mv target/azure-client-initialization-*.jar target/app.jar

# ──────────────────────────────────────────────────────────────────────────────
# Stage 2 – Runtime
# Targets Azure Container Apps / AKS: non-root, minimal image, JVM container
# awareness, and DefaultAzureCredential / Workload Identity support.
# ──────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

# Security: create a dedicated non-root user (UID/GID 1001)
RUN groupadd --gid 1001 appuser && \
    useradd  --uid 1001 --gid 1001 --shell /bin/bash --create-home appuser

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=builder --chown=appuser:appuser /workspace/target/app.jar app.jar

USER appuser

# JVM container-awareness: respect cgroup CPU/memory limits and use 75 % of
# the container's RAM as the max heap.  JAVA_TOOL_OPTIONS is automatically
# read by the JVM without any wrapper script.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Azure identity environment variables.
# In ACA / AKS, leave these unset and configure a Managed Identity / Workload
# Identity instead so that DefaultAzureCredential resolves automatically.
ENV AZURE_SUBSCRIPTION_ID=""
ENV AZURE_TENANT_ID=""
ENV AZURE_CLIENT_ID=""
ENV AZURE_CLIENT_SECRET=""
ENV ENV_JOB_NAME=""

# Use exec form so the JVM receives OS signals (SIGTERM) directly — required
# for graceful shutdown in ACA / AKS.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
