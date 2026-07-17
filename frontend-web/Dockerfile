# Vue 管理端：构建静态资源后由 Nginx 提供，并反向代理 /api 到后端容器。
ARG DOCKER_MIRROR=docker.m.daocloud.io/library
ARG NPM_REGISTRY=https://registry.npmmirror.com

FROM ${DOCKER_MIRROR}/node:22-alpine AS builder
ARG NPM_REGISTRY
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm config set registry "${NPM_REGISTRY}" \
    && npm config set replace-registry-host always \
    && npm ci --fetch-retries=5 --fetch-retry-mintimeout=10000 --fetch-retry-maxtimeout=120000

COPY . ./
RUN npm run build

FROM ${DOCKER_MIRROR}/nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
