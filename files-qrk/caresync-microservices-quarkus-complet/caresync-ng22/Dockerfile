# Build multi-stage : Angular (production) -> nginx statique.
# environment.prod.ts pointe déjà vers une apiUrl absolue (https://api.caresync.be/api/v1),
# donc pas de reverse-proxy nécessaire dans cette image, uniquement le SPA fallback.

# ── Stage 1 : build Angular ──────────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ── Stage 2 : nginx ───────────────────────────────────────────────────────────
FROM nginx:1.27-alpine

RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/dist/caresync-ng22/browser/ /usr/share/nginx/html/
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# nginx:alpine tourne déjà en tant qu'utilisateur "nginx" non-root pour les
# workers ; le process master doit rester root pour bind :80, comportement
# par défaut de l'image officielle, conservé tel quel.
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s CMD wget -qO- http://localhost/healthz || exit 1
