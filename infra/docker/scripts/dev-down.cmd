@echo off
REM dev-down.cmd - Stop the local development environment
REM Preserves named volumes (postgres-data, backend-m2-cache)
REM
REM Usage: dev-down.cmd

setlocal EnableDelayedExpansion

REM Resolve repository root (scripts/ -> docker/ -> infra/ -> repo-root)
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
for %%I in ("%SCRIPT_DIR%\..\..\..") do set "REPO_ROOT=%%~fI"
set "ENV_FILE=%REPO_ROOT%\.env"
set "COMPOSE_DIR=%SCRIPT_DIR%\.."

REM Run docker compose down (without -v to preserve named volumes)
cd /d "%COMPOSE_DIR%"
docker compose --env-file "%ENV_FILE%" down
