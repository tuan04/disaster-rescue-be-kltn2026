# syntax=docker/dockerfile:1
FROM node:24-bookworm-slim
WORKDIR /app

ENV NODE_ENV=production \
    PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    COLLECTOR_HEADLESS=true

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --omit=dev && \
    npx --no-install playwright install --with-deps chromium && \
    chmod -R a+rX /ms-playwright && \
    mkdir -p /app/.auth && chown node:node /app/.auth && \
    rm -rf /var/lib/apt/lists/*

COPY --chown=node:node src/ ./src/
COPY --chown=node:node config/ ./config/

USER node
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD node -e "fetch('http://127.0.0.1:3000/health', { signal: AbortSignal.timeout(4000) }).then(r => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))"
CMD ["node", "src/server.js"]
