# ===== 前端镜像：Node 构建 + Nginx 运行（多阶段） =====
# 构建阶段：安装完整依赖（含 devDependencies 如 vite）并产出 dist
FROM node:22-alpine AS builder
WORKDIR /app

COPY daily_work_todo_front/package.json daily_work_todo_front/package-lock.json* ./
RUN npm ci

COPY daily_work_todo_front/ ./
RUN npm run build

# 运行阶段：Nginx 托管静态资源
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
