# ==============================================================================
# BiteStream Angular Web Dashboard - Multi-Stage Dockerfile
# Stage 1: Build Angular SPA with Node.js 24
# Stage 2: Serve optimized static assets with Nginx Alpine
# ==============================================================================

FROM node:24-alpine AS build
WORKDIR /app

# Install dependencies using package-lock.json
COPY package*.json ./
RUN npm ci

# Copy project source and compile production bundle
COPY . .
RUN npm run build

# ==============================================================================
# Stage 2: Production Web Server (Nginx Alpine)
# ==============================================================================
FROM nginx:1.27-alpine

# Copy custom Nginx configuration with reverse proxy for /api and /actuator
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled static assets from build stage
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Healthcheck
HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1:80/ || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
