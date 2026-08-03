# syntax=docker/dockerfile:1.7
# =============================================================================
# DEV STAGE  - Vite dev server con HMR (usado por docker-compose.local.yml)
# =============================================================================
FROM node:20.19.0-alpine AS dev

WORKDIR /app

ENV NODE_OPTIONS=--dns-result-order=ipv4first
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

ARG VITE_META_APP_ID=
ENV VITE_META_APP_ID=${VITE_META_APP_ID}
ARG VITE_META_EMBEDDED_SIGNUP_CONFIG_ID=
ENV VITE_META_EMBEDDED_SIGNUP_CONFIG_ID=${VITE_META_EMBEDDED_SIGNUP_CONFIG_ID}
ARG VITE_META_GRAPH_API_VERSION=v23.0
ENV VITE_META_GRAPH_API_VERSION=${VITE_META_GRAPH_API_VERSION}
ARG VITE_PUBLIC_WHATSAPP_NUMBER=
ENV VITE_PUBLIC_WHATSAPP_NUMBER=${VITE_PUBLIC_WHATSAPP_NUMBER}

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN corepack enable && corepack prepare pnpm@10.18.3 --activate && pnpm install --frozen-lockfile

COPY . .

EXPOSE 5173

CMD ["pnpm", "dev", "--host", "0.0.0.0"]

# =============================================================================
# BUILD STAGE - Compilacion para produccion
# =============================================================================
FROM node:20.19.0-alpine AS build

WORKDIR /app

ENV NODE_OPTIONS=--dns-result-order=ipv4first
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

ARG VITE_META_APP_ID=
ENV VITE_META_APP_ID=${VITE_META_APP_ID}
ARG VITE_META_EMBEDDED_SIGNUP_CONFIG_ID=
ENV VITE_META_EMBEDDED_SIGNUP_CONFIG_ID=${VITE_META_EMBEDDED_SIGNUP_CONFIG_ID}
ARG VITE_META_GRAPH_API_VERSION=v23.0
ENV VITE_META_GRAPH_API_VERSION=${VITE_META_GRAPH_API_VERSION}
ARG VITE_PUBLIC_WHATSAPP_NUMBER=
ENV VITE_PUBLIC_WHATSAPP_NUMBER=${VITE_PUBLIC_WHATSAPP_NUMBER}

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN corepack enable && corepack prepare pnpm@10.18.3 --activate && pnpm install --frozen-lockfile

COPY . .

RUN pnpm build

# =============================================================================
# PRODUCTION STAGE - nginx sirviendo el bundle estatico
# =============================================================================
FROM nginx:1.27.4-alpine AS production

RUN apk add --no-cache tzdata

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
