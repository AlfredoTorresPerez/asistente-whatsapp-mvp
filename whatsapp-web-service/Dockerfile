FROM node:20-slim

WORKDIR /app

ENV PUPPETEER_SKIP_DOWNLOAD=true
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
ENV WHATSAPP_WEB_CHROME_EXECUTABLE=/usr/bin/chromium
ENV NPM_CONFIG_DNS_RESULT_ORDER=ipv4first
ENV NODE_OPTIONS=--dns-result-order=ipv4first

RUN for attempt in 1 2 3; do \
      apt-get update && break; \
      if [ "$attempt" = "3" ]; then exit 1; fi; \
      echo "apt-get update failed. Retrying in 15 seconds..."; \
      sleep 15; \
    done \
    && apt-get install -y --no-install-recommends \
      ca-certificates \
      chromium \
      dumb-init \
      fluxbox \
      fonts-liberation \
      novnc \
      tzdata \
      websockify \
      x11vnc \
      xvfb \
      procps \
    && rm -rf /var/lib/apt/lists/*

COPY package.json ./
RUN corepack enable \
    && for attempt in 1 2 3; do \
      corepack prepare pnpm@10.18.3 --activate && break; \
      if [ "$attempt" = "3" ]; then exit 1; fi; \
      echo "pnpm activation failed. Retrying in 15 seconds..."; \
      sleep 15; \
    done \
    && for attempt in 1 2 3; do \
      pnpm install --prod --no-frozen-lockfile && break; \
      if [ "$attempt" = "3" ]; then exit 1; fi; \
      echo "pnpm install failed. Retrying in 15 seconds..."; \
      sleep 15; \
    done

COPY src ./src
COPY docker-entrypoint.sh ./docker-entrypoint.sh
COPY start-visual.sh ./start-visual.sh
RUN chmod +x ./docker-entrypoint.sh \
    && chmod +x ./start-visual.sh \
    && mkdir -p /app/.wwebjs_auth /app/.wwebjs_cache /tmp/whatsapp-web-profile \
    && chown -R node:node /app/.wwebjs_auth /app/.wwebjs_cache /tmp/whatsapp-web-profile

USER node

EXPOSE 3001 6080

ENTRYPOINT ["dumb-init", "--"]
CMD ["./docker-entrypoint.sh", "./start-visual.sh"]
