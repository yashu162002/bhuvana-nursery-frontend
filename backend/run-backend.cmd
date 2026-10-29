@echo off
setlocal
rem Builds and runs the API from backend\run so Maven never hits a locked jar.
rem Extra arguments are forwarded to run-backend.ps1 (e.g. -NoBuild).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-backend.ps1" %*
