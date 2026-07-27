@echo off
REM dev-reset.cmd - Stop the local development environment and remove all volumes
REM WARNING: This destroys all data in postgres-data and backend-m2-cache volumes
REM
REM Usage: dev-reset.cmd

setlocal EnableDelayedExpansion

REM Resolve repository root (scripts/ -> docker/ -> infra/ -> repo-root)
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
for %%I in ("%SCRIPT_DIR%\..\..\..") do set "REPO_ROOT=%%~fI"
set "ENV_FILE=%REPO_ROOT%\.env"
set "COMPOSE_DIR=%SCRIPT_DIR%\.."

REM Prompt for explicit confirmation
echo WARNING: This will stop all services and DELETE ALL DATA:
echo   - postgres-data volume (database)
echo   - backend-m2-cache volume (Maven dependencies)
echo.
echo Type RESET and press Enter to confirm, or anything else to cancel:
set /p CONFIRM=

if /i not "%CONFIRM%"=="RESET" (
    echo Reset cancelled. No changes were made.
    exit /b 1
)

REM Run docker compose down with volume removal
cd /d "%COMPOSE_DIR%"
docker compose --env-file "%ENV_FILE%" down -v --remove-orphans

echo.
echo Reset complete. All volumes have been removed.
