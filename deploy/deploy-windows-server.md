# ACM Training 部署（Windows Server 简版）

## 1. 服务器环境
- Windows Server 2019/2022
- JDK 17
- MySQL 8
- Nginx for Windows 或 IIS 反向代理

## 2. 数据库初始化（只做一次）
在 MySQL 控制台执行：
```sql
CREATE DATABASE IF NOT EXISTS acm_train DEFAULT CHARSET utf8mb4;
```
然后导入：
```bat
mysql -u root -p acm_train < D:\acm-train\backend-spring\src\main\resources\schema.sql
mysql -u root -p acm_train < D:\acm-train\backend-spring\src\main\resources\data.sql
```

## 3. 后端打包与启动
```bat
cd D:\acm-train\backend-spring
mvn -DskipTests package
set SPRING_PROFILES_ACTIVE=prod
set DB_HOST=127.0.0.1
set DB_PORT=3306
set DB_NAME=acm_train
set DB_USER=acm_user
set DB_PASS=acm_pass
set MAIL_HOST=smtp.example.com
set MAIL_PORT=587
set MAIL_USERNAME=bot@example.com
set MAIL_PASSWORD=change-me
set MAIL_FROM=bot@example.com
set ATC_SUBMISSIONS_ENABLED=true
set ATC_SUBMISSIONS_API_BASE_URL=https://kenkoooo.com/atcoder/atcoder-api/v3
set ATC_PROBLEM_API_BASE_URL=https://kenkoooo.com/atcoder/resources
set ATC_REQUEST_INTERVAL_MS=1200
set CONTEST_SYNC_ENABLED=true
set CONTEST_REMINDER_ENABLED=true
set CONTEST_MAIL_TO=coach1@example.com,coach2@example.com
set ALERT_MAIL_ENABLED=true
set ALERT_MAIL_TO=coach1@example.com,coach2@example.com
java -jar target\backend-spring-0.0.1-SNAPSHOT.jar
```

如果你有多个可用的 AtCoder 镜像源，可以把下面两个变量改成逗号分隔的有序列表：

```bat
set ATC_SUBMISSIONS_API_BASE_URL=https://mirror-a.example.com/atcoder-api/v3,https://mirror-b.example.com/atcoder-api/v3
set ATC_PROBLEM_API_BASE_URL=https://mirror-a.example.com/resources,https://mirror-b.example.com/resources
```

如果你需要调整比赛同步和邮件提醒频率，可继续设置：

```bat
set CONTEST_SYNC_CRON=0 0 */4 * * *
set CONTEST_REMINDER_CRON=0 */10 * * * *
set ALERT_MAIL_CRON=0 */10 * * * *
set CF_REMINDER_MINUTES=120
set ATC_REMINDER_MINUTES=120
```

## 4. 前端打包
```bat
cd D:\acm-train\frontend
npm install
npm run build
```

把 `dist` 放到 Nginx 目录或 IIS 站点目录，例如：`C:\nginx\html`。

## 5. Nginx 反向代理示例
`nginx.conf` 中加入：
```
location /api/ {
  proxy_pass http://127.0.0.1:8080;
}
```

## 6. 账号
- 教练：coach01 / 123456
- 学生：student01 / 123456

## 7. 注意事项
- 生产环境使用 `application-prod.yml`，已关闭自动重置数据库。
