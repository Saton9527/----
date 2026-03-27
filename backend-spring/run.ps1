$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

powershell -ExecutionPolicy Bypass -File .\start-local-mysql.ps1

$env:DB_HOST='127.0.0.1'
$env:DB_PORT='3307'
$env:DB_NAME='acm_train'
$env:DB_USER='root'
$env:DB_PASS=''

$mvn = Resolve-Path "..\tools\apache-maven-3.9.9\bin\mvn.cmd"
& $mvn "-DskipTests" clean package

$jar = Resolve-Path ".\target\backend-spring-0.0.1-SNAPSHOT.jar"
java -jar $jar
