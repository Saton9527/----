# ACM Training 部署（Ubuntu 手动版）

## 1. 服务器环境
- Ubuntu 20.04/22.04
- JDK 17
- MySQL 8
- Nginx

## 2. 上传项目
把整个项目上传到服务器，例如：`/opt/acm-train`

## 3. 数据库初始化（只做一次）
注意：以下命令会使用 schema.sql 的 DROP 语句，首次初始化时使用，线上有数据请勿执行。

```bash
sudo mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS acm_train DEFAULT CHARSET utf8mb4;"
sudo mysql -u root -p acm_train < /opt/acm-train/backend-spring/src/main/resources/schema.sql
sudo mysql -u root -p acm_train < /opt/acm-train/backend-spring/src/main/resources/data.sql
```

建议创建独立用户：
```bash
sudo mysql -u root -p -e "CREATE USER IF NOT EXISTS 'acm_user'@'localhost' IDENTIFIED BY 'acm_pass';"
sudo mysql -u root -p -e "GRANT ALL PRIVILEGES ON acm_train.* TO 'acm_user'@'localhost'; FLUSH PRIVILEGES;"
```

## 4. 后端打包与启动
```bash
cd /opt/acm-train/backend-spring
./mvnw -DskipTests package
```

启动（生产配置）：
```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=acm_train
export DB_USER=acm_user
export DB_PASS=acm_pass
export MAIL_HOST=smtp.example.com
export MAIL_PORT=587
export MAIL_USERNAME=bot@example.com
export MAIL_PASSWORD=change-me
export MAIL_FROM=bot@example.com
export ATC_SUBMISSIONS_ENABLED=true
export ATC_SUBMISSIONS_API_BASE_URL=https://kenkoooo.com/atcoder/atcoder-api/v3
export ATC_PROBLEM_API_BASE_URL=https://kenkoooo.com/atcoder/resources
export ATC_REQUEST_INTERVAL_MS=1200
export CONTEST_SYNC_ENABLED=true
export CONTEST_REMINDER_ENABLED=true
export CONTEST_MAIL_TO=coach1@example.com,coach2@example.com
export ALERT_MAIL_ENABLED=true
export ALERT_MAIL_TO=coach1@example.com,coach2@example.com

java -jar target/backend-spring-0.0.1-SNAPSHOT.jar
```

如果你有多个可用的 AtCoder 镜像源，可以把下面两个变量改成逗号分隔的有序列表：

```bash
export ATC_SUBMISSIONS_API_BASE_URL=https://mirror-a.example.com/atcoder-api/v3,https://mirror-b.example.com/atcoder-api/v3
export ATC_PROBLEM_API_BASE_URL=https://mirror-a.example.com/resources,https://mirror-b.example.com/resources
```

如果你需要调整比赛同步和邮件提醒频率，可继续设置：

```bash
export CONTEST_SYNC_CRON="0 0 */4 * * *"
export CONTEST_REMINDER_CRON="0 */10 * * * *"
export ALERT_MAIL_CRON="0 */10 * * * *"
export CF_REMINDER_MINUTES=120
export ATC_REMINDER_MINUTES=120
```

## 5. 前端打包
```bash
cd /opt/acm-train/frontend
npm install
npm run build
```

把 `dist/` 拷贝到 Nginx 根目录：
```bash
sudo rm -rf /var/www/acm-frontend
sudo cp -r dist /var/www/acm-frontend
```

## 6. Nginx 配置
把 `deploy/nginx-acm.conf` 放到 `/etc/nginx/sites-available/acm.conf`：
```bash
sudo cp /opt/acm-train/deploy/nginx-acm.conf /etc/nginx/sites-available/acm.conf
sudo ln -s /etc/nginx/sites-available/acm.conf /etc/nginx/sites-enabled/acm.conf
sudo nginx -t
sudo systemctl restart nginx
```

## 7. 访问
- 前端：http://你的服务器IP
- 后端：http://你的服务器IP/api/...

## 8. 账号
- 教练：coach01 / 123456
- 学生：student01 / 123456

## 9. 注意事项
- 生产环境一定不要再用 `spring.sql.init.mode=always`，已经在 `application-prod.yml` 里关闭。
- 上线后建议改密码、使用真实用户管理。
