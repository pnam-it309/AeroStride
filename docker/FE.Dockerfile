# Stage 1: Base/Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files and install dependencies
COPY FE/package*.json ./
RUN npm ci

# Copy source code
COPY FE/ .

# Development stage (for hot-reloading)
FROM build AS development
# Note: Vite needs to listen on 0.0.0.0 to be accessible outside the container
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]

# Stage 2: Runtime stage (Production)
FROM build AS builder
RUN npm run build

FROM nginxinc/nginx-unprivileged:alpine AS production
WORKDIR /usr/share/nginx/html

# Copy the built assets from the build stage
COPY --from=builder /app/dist .

# Copy custom nginx configuration for SPA routing
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

# Expose the unprivileged port
EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
