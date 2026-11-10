@echo off
powershell.exe -NoProfile -STA -ExecutionPolicy Bypass -File "%~dp0configure_mariadb.ps1" %*
exit /b %errorlevel%
