$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (!(Test-Path ".\out")) {
  New-Item -ItemType Directory -Path ".\out" | Out-Null
}

javac -encoding UTF-8 -d .\out .\src\App.java
java -cp .\out App
