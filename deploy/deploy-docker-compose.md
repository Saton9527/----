# ACM Training 一键部署（Docker Compose）

## 1. 依赖
- Docker
- Docker Compose（Docker Desktop 默认自带）

## 2. 启动
在项目根目录执行：
```bash
docker compose -f deploy/docker-compose.yml up -d --build
```

## 3. 访问
- 前端：http://服务器IP
- 后端：http://服务器IP/api/...

## 4. 默认账号
- 教练：coach01 / 123456
- 学生：student01 / 123456

## 5. 数据库说明
- 首次启动会自动导入 `schema.sql` 与 `data.sql`。
- 数据会保存在 `mysql_data` 卷中，容器重启不会丢失。
- 如果你不想导入示例数据，可删掉 compose 中 `data.sql` 的挂载。

## 6. 停止与清理
停止：
```bash
docker compose -f deploy/docker-compose.yml down
```
清空数据库（慎用）：
```bash
docker compose -f deploy/docker-compose.yml down -v
```