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
java -jar target\backend-spring-0.0.1-SNAPSHOT.jar
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