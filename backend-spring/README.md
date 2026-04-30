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

## OJ Sync / Alert Mail

- 手动同步：学生个人中心点击“同步真实 OJ”，或教练在学生管理里点“同步 OJ”。
- AtCoder 做题明细导入：学生个人中心点击“导入 ATC 提交”，或教练在学生管理里给指定学生导入登录态导出的 JSON。
- 比赛提醒：比赛页支持同步 Codeforces / AtCoder 官方 upcoming 比赛；教练也可以手动补充 QOJ 训练赛提醒。
- 定时同步默认关闭：`acm.sync.scheduler-enabled=false`
- 告警邮件默认关闭：`acm.alert.mail.enabled=false`
- 官方比赛自动同步默认关闭：`acm.contest.sync-enabled=false`
- 比赛提醒邮件默认关闭：`acm.contest.reminder-enabled=false`
- 比赛提醒与异常邮件都支持直接用环境变量开启：
- `CONTEST_SYNC_ENABLED`
- `CONTEST_SYNC_CRON`
- `CONTEST_REMINDER_ENABLED`
- `CONTEST_REMINDER_CRON`
- `CF_REMINDER_MINUTES`
- `ATC_REMINDER_MINUTES`
- `ALERT_MAIL_ENABLED`
- `ALERT_MAIL_CRON`
- AtCoder 自动做题同步支持环境变量：
- `ATC_SUBMISSIONS_ENABLED`
- `ATC_SUBMISSIONS_API_BASE_URL`
- `ATC_PROBLEM_API_BASE_URL`
- `ATC_REQUEST_INTERVAL_MS`
- `ATC_SUBMISSIONS_API_BASE_URL` 和 `ATC_PROBLEM_API_BASE_URL` 都支持逗号分隔的有序多源回退。
- 即使 `ATC_PROBLEM_API_BASE_URL` 默认源不可用，系统也会自动回退到：
  - `raw.githubusercontent.com/kenkoooo/AtCoderProblems/.../problem-models.json`
  - `cdn.jsdelivr.net/gh/kenkoooo/AtCoderProblems@master/.../problem-models.json`
  用于继续加载 AtCoder 题目难度与积分所需的 metadata。

示例：

```powershell
$env:ATC_SUBMISSIONS_ENABLED='true'
$env:ATC_SUBMISSIONS_API_BASE_URL='https://mirror-a.example.com/atcoder-api/v3,https://mirror-b.example.com/atcoder-api/v3'
$env:ATC_PROBLEM_API_BASE_URL='https://mirror-a.example.com/resources,https://mirror-b.example.com/resources'
$env:ATC_REQUEST_INTERVAL_MS='1200'
```

### AtCoder 提交导入说明

- 只使用官方公开源时，AtCoder 目前只能稳定同步 `rating` 和比赛历史；做题明细需要用户自行导出登录态下的提交 JSON。
- 当 `kenkoooo.com` 的 `resources` 接口不可用时，系统仍会自动回退到 GitHub Raw / jsDelivr 加载 `problem-models.json`，所以 AtCoder 题目难度和相关积分不会一起失效。
- 导入后会覆盖该学生现有的 AtCoder 已做题记录，并重算对应的 OJ 题目积分；Codeforces 题目记录和比赛积分不会受影响。
- 绑定的 AtCoder 账号必须和 JSON 里的 `user_id` 一致；如果 JSON 不带用户字段，系统会按当前绑定账号直接导入。

支持的 JSON 结构：

- 根节点直接是数组
- 或对象里包含 `submissions`、`results`、`items` 任一数组字段

支持识别的常见字段：

- 通过状态：`result` / `status` / `verdict`
- 用户：`user_id` / `userId` / `user` / `username`
- 题号：`problem_id` / `task_id` / `problemId` / `taskId` / `problem_code`
- 比赛：`contest_id` / `contestId`
- 时间：`epoch_second` / `epochSecond` / `accepted_at` / `submitted_at` / `created_at`
- 标题：`title` / `problem_title` / `problemTitle` / `name`
- 难度：`rating` / `difficulty`

最小可用示例：

```json
{
  "submissions": [
    {
      "user_id": "tourist",
      "problem_id": "abc300_a",
      "contest_id": "abc300",
      "result": "AC",
      "epoch_second": 1711000000,
      "title": "N-choice question",
      "rating": 200
    }
  ]
}
```

### 官方比赛同步与提醒

- Codeforces 使用官方 API：`https://codeforces.com/api/contest.list?gym=false`
- AtCoder 使用官方比赛页：`https://atcoder.jp/contests/`
- 手动同步入口：教练在“比赛提醒”页点击“同步 CF / ATC”
- 自动同步：打开 `acm.contest.sync-enabled=true`
- 邮件提醒：打开 `acm.contest.reminder-enabled=true`

示例配置：

```yaml
acm:
  contest:
    sync-enabled: true
    reminder-enabled: true
    cf-reminder-minutes: 120
    atc-reminder-minutes: 120
```

比赛提醒邮件收件人优先使用系统内教练邮箱；如果没有配置教练邮箱，会回退到：

```powershell
$env:CONTEST_MAIL_TO='coach1@example.com,coach2@example.com'
```

### 积分规则

- OJ 题目积分改为一位小数，默认按 `rating / 1000` 计分，例如 `1600` 题记 `1.6` 分。
- 会结合隐藏分做补正：
  - 题目难度低于隐藏分 `400+`：`0.0` 分
  - 题目难度低于隐藏分 `200+`：折半计分
  - 其余题目：全额计分
- OJ 同步或 AtCoder 提交导入后，会重算该学生全部 `OJ_PROBLEM` 积分流水，避免旧规则残留。

启用邮件前至少配置这些环境变量：

```powershell
$env:MAIL_HOST='smtp.example.com'
$env:MAIL_PORT='587'
$env:MAIL_USERNAME='bot@example.com'
$env:MAIL_PASSWORD='your-password'
$env:MAIL_FROM='bot@example.com'
$env:ALERT_MAIL_TO='coach1@example.com,coach2@example.com'
```

然后把配置打开：

```yaml
acm:
  sync:
    scheduler-enabled: true
  contest:
    sync-enabled: true
    reminder-enabled: true
  alert:
    mail:
      enabled: true
```
