FROM node:22-alpine AS build

WORKDIR /web
COPY web-app/package.json web-app/package-lock.json ./
RUN npm ci
COPY web-app/ ./
RUN npm run build

FROM caddy:2.11-alpine
COPY infra/caddy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /web/dist /srv
EXPOSE 80 443
