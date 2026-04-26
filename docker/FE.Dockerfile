# Stage 1: Base/Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files first to leverage Docker layer caching
COPY FE/package*.json ./

# Use BuildKit cache mount for npm cache to speed up installations
# This avoids re-downloading packages even when package.json changes slightly
RUN --mount=type=cache,target=/root/.npm \
    npm install

# Copy source code
COPY FE/ .

# Development stage (for hot-reloading)
FROM build AS development
EXPOSE 5173
# Use polling for hot-reload on Windows hosts
ENV CHOKIDAR_USEPOLLING=true
ENV WATCHPACK_POLLING=true
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]

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

# Default port (can be overridden by environment variable NGINX_PORT)
EXPOSE 8080

# Healthcheck for Nginx
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
