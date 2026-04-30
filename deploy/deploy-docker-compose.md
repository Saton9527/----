# ACM Training 一键部署（Docker Compose）

## 1. 依赖
- Docker
- Docker Compose（Docker Desktop 默认自带）

## 2. 启动
在项目根目录执行：
```bash
docker compose -f deploy/docker-compose.yml up -d --build
```

后端默认已经暴露 AtCoder 同步、比赛提醒和异常邮件环境变量，位于 `deploy/docker-compose.yml` 的 `backend.environment`。
如果你有多个可用镜像源，可以把下面两个变量改成逗号分隔、按顺序回退的地址列表：

```yaml
ATC_SUBMISSIONS_API_BASE_URL: https://mirror-a.example.com/atcoder-api/v3,https://mirror-b.example.com/atcoder-api/v3
ATC_PROBLEM_API_BASE_URL: https://mirror-a.example.com/resources,https://mirror-b.example.com/resources
```

可选控制项：

```yaml
ATC_SUBMISSIONS_ENABLED: "true"
ATC_REQUEST_INTERVAL_MS: 1200
CONTEST_SYNC_ENABLED: "true"
CONTEST_REMINDER_ENABLED: "true"
ALERT_MAIL_ENABLED: "true"
```

邮件发送至少要改成你自己的 SMTP 参数：

```yaml
MAIL_HOST: smtp.example.com
MAIL_PORT: 587
MAIL_USERNAME: bot@example.com
MAIL_PASSWORD: change-me
MAIL_FROM: bot@example.com
CONTEST_MAIL_TO: coach1@example.com,coach2@example.com
ALERT_MAIL_TO: coach1@example.com,coach2@example.com
```

提醒频率和阈值也可以直接在 compose 里调：

```yaml
CONTEST_SYNC_CRON: "0 0 */4 * * *"
CONTEST_REMINDER_CRON: "0 */10 * * * *"
ALERT_MAIL_CRON: "0 */10 * * * *"
CF_REMINDER_MINUTES: 120
ATC_REMINDER_MINUTES: 120
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
