FROM node:22-alpine AS build

WORKDIR /web
COPY web-app/package.json web-app/package-lock.json ./
RUN npm ci
COPY web-app/ ./
RUN npm run build

FROM caddy:2.10-alpine
COPY infra/caddy/Caddyfile.local /etc/caddy/Caddyfile
COPY --from=build /web/dist /srv
EXPOSE 5173
