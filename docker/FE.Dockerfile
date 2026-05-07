# Stage 1: Base/Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files first to leverage Docker layer caching
COPY FE/package*.json ./

# Use BuildKit cache mount for npm cache to speed up installations
# This avoids re-downloading packages even when package.json changes slightly
RUN --mount=type=cache,target=/root/.npm \
    npm install --legacy-peer-deps

# Copy source code
COPY FE/ .

# Development stage (for hot-reloading)
FROM build AS development

ARG FE_DEV_PORT
EXPOSE ${FE_DEV_PORT}

# Use polling for hot-reload on Windows hosts
ENV CHOKIDAR_USEPOLLING=true
ENV WATCHPACK_POLLING=true
CMD ["sh", "-c", "npm run dev -- --host 0.0.0.0 --port ${FE_DEV_PORT}"]

# Stage 2: Builder stage (Production)
FROM build AS builder
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
ENV NODE_OPTIONS=--max-old-space-size=4096
RUN npm run build

# Stage 3: Runtime stage (Production)
FROM nginxinc/nginx-unprivileged:alpine AS production
WORKDIR /usr/share/nginx/html

# Copy the built assets from the builder stage
COPY --from=builder /app/dist .

# Copy custom nginx configuration for SPA routing
COPY docker/nginx.conf /etc/nginx/templates/default.conf.template

# Port from env (no default)
ARG NGINX_PORT
EXPOSE ${NGINX_PORT}

# Healthcheck for Nginx (uses NGINX_PORT env var)
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:${NGINX_PORT}/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
