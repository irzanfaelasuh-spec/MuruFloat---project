@echo off
setlocal
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo This project uses Gradle 8.13. Install Gradle 8.13 or use the GitHub Actions build workflow.
exit /b 1
