@echo off
REM dev-logs.cmd - View logs from the local development environment
REM Follows logs from all services, or a specific service if provided
REM
REM Usage: dev-logs.cmd [service]
REM Examples:
REM   dev-logs.cmd           - Follow logs from all services
REM   dev-logs.cmd postgres  - Follow logs from postgres service
REM   dev-logs.cmd backend   - Follow logs from backend service

setlocal EnableDelayedExpansion

REM Resolve repository root (scripts/ -> docker/ -> infra/ -> repo-root)
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
for %%I in ("%SCRIPT_DIR%\..\..\..") do set "REPO_ROOT=%%~fI"
set "ENV_FILE=%REPO_ROOT%\.env"
set "COMPOSE_DIR=%SCRIPT_DIR%\.."

REM Change to compose directory
cd /d "%COMPOSE_DIR%"

REM If a service name is provided, filter to that service
if "%~1"=="" (
    docker compose --env-file "%ENV_FILE%" logs -f
) else (
    docker compose --env-file "%ENV_FILE%" logs -f %*
)
