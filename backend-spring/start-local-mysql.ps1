$ErrorActionPreference = "Stop"

$base='C:\Program Files\MySQL\MySQL Server 8.0'
$mysqld=Join-Path $base 'bin\mysqld.exe'
$root='C:\Users\16043\.codex\memories\mysql-local'
$data=Join-Path $root 'data'
$cfg=Join-Path $root 'my-local.ini'

New-Item -ItemType Directory -Force $data | Out-Null

if (-not (Test-Path (Join-Path $data 'mysql'))) {
  & $mysqld --initialize-insecure --basedir="$base" --datadir="$data" --console
}

@"
[mysqld]
basedir=C:/Program Files/MySQL/MySQL Server 8.0
datadir=C:/Users/16043/.codex/memories/mysql-local/data
port=3307
bind-address=127.0.0.1
mysqlx=0
log-error=C:/Users/16043/.codex/memories/mysql-local/data/local.err
"@ | Set-Content $cfg -Encoding ASCII

$running = netstat -ano | Select-String ':3307'
if (-not $running) {
  Start-Process -FilePath $mysqld -ArgumentList "--defaults-file=$cfg","--console" -WorkingDirectory $root
  Start-Sleep -Seconds 5
}

Write-Output "MySQL local ready at 127.0.0.1:3307"
