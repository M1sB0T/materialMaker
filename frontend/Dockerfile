# ---- 依赖阶段 ----
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ---- 构建阶段 ----
FROM node:20-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ---- 运行阶段（standalone 产物） ----
FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV API_ORIGIN=http://backend:8080
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
EXPOSE 3000
CMD ["node", "server.js"]
