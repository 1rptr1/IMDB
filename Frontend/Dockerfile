# ---- Build stage ----
FROM node:18-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json* yarn.lock* pnpm-lock.yaml* .npmrc* ./
RUN npm ci || yarn install --frozen-lockfile || pnpm i --frozen-lockfile || npm i
COPY . .
# VITE_API_URL is read at build time in src/lib/api.ts
ARG VITE_API_URL
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build || yarn build || pnpm build

# ---- Runtime stage ----
FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Basic nginx config tweaks (compression is already decent by default)
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
