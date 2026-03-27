# Backend Spring Boot + MySQL

## Start

```powershell
cd D:\Java\code\毕业设计\backend-spring
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

This command will:
1. Start local MySQL on `127.0.0.1:3307`
2. Build and start Spring Boot on `127.0.0.1:8080`

## Default Accounts

- Coach: `coach01 / 123456`
- Student: `student01 / 123456`

## APIs

- `POST /api/auth/login`
- `GET /api/tasks`
- `GET /api/tasks?status=DONE`
- `POST /api/tasks`
- `PATCH /api/tasks/{id}/status`
- `PATCH /api/tasks/{id}/progress`
- `GET /api/rankings/overall`
- `GET /api/points/me/logs`
- `GET /api/dashboard/me/trend`
- `GET /api/recommendations/me`
- `GET /api/alerts`
- `GET /api/students`
