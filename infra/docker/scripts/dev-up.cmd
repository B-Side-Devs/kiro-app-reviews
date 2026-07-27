@echo off
REM =============================================================================
REM dev-up.cmd — Start the local development environment (Windows batch)
REM =============================================================================
REM This script starts all services defined in infra/docker/docker-compose.yml.
REM It ensures the .env file exists at the repository root before invoking
REM docker compose. If .env is missing, it copies from .env.example and exits
REM with instructions to edit POSTGRES_PASSWORD.
REM
REM Usage:
REM   infra\docker\scripts\dev-up.cmd [additional docker compose up args...]
REM
REM Requirements: 1.1, 1.2, 1.4, 1.5, 8.4, 10.1, 10.2, 10.3, 16.1, 16.3
REM =============================================================================

setlocal EnableDelayedExpansion

REM -----------------------------------------------------------------------------
REM Resolve repository root (directory containing this script's parent's parent)
REM -----------------------------------------------------------------------------
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
for %%I in ("%SCRIPT_DIR%\..\..") do set "REPO_ROOT=%%~fI"
set "DOCKER_DIR=%REPO_ROOT%\infra\docker"

set "ENV_FILE=%REPO_ROOT%\.env"
set "ENV_EXAMPLE=%DOCKER_DIR%\.env.example"

REM -----------------------------------------------------------------------------
REM Verify .env exists; if missing, copy from template and exit with instruction
REM -----------------------------------------------------------------------------
if not exist "%ENV_FILE%" (
    echo ERROR: .env file not found at %ENV_FILE%
    echo.
    echo Creating .env from template...

    if not exist "%ENV_EXAMPLE%" (
        echo ERROR: Template file not found at %ENV_EXAMPLE%
        echo Please ensure infra\docker\.env.example exists.
        exit /b 1
    )

    copy "%ENV_EXAMPLE%" "%ENV_FILE%" > nul
    echo Created: %ENV_FILE%
    echo.
    echo ==========================================
    echo ACTION REQUIRED
    echo ==========================================
    echo 1. Edit %ENV_FILE%
    echo 2. Replace 'CHANGE_ME' with a real password for POSTGRES_PASSWORD
    echo 3. Re-run this script
    echo ==========================================
    exit /b 1
)

REM -----------------------------------------------------------------------------
REM Verify POSTGRES_PASSWORD is set (not empty or CHANGE_ME)
REM -----------------------------------------------------------------------------
REM Load .env file variables
for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    set "%%A=%%B"
)

if not defined POSTGRES_PASSWORD (
    echo ERROR: POSTGRES_PASSWORD is not defined.
    echo.
    echo Please edit %ENV_FILE% and set POSTGRES_PASSWORD to a real value.
    exit /b 1
)

if "%POSTGRES_PASSWORD%"=="CHANGE_ME" (
    echo ERROR: POSTGRES_PASSWORD is not properly configured.
    echo.
    echo Please edit %ENV_FILE% and set POSTGRES_PASSWORD to a real value.
    echo The password does not need to be strong — it is for local development only.
    exit /b 1
)

REM -----------------------------------------------------------------------------
REM Verify Docker is running
REM -----------------------------------------------------------------------------
docker info > nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker Engine is not running.
    echo.
    echo Please start Docker and re-run this script.
    exit /b 1
)

REM -----------------------------------------------------------------------------
REM Invoke docker compose from the docker directory
REM -----------------------------------------------------------------------------
pushd "%DOCKER_DIR%"

echo Starting development environment...
echo.

REM Pass any additional arguments to docker compose up (e.g., --build)
docker compose --env-file "%ENV_FILE%" up -d %*

echo.
echo Service status:
docker compose --env-file "%ENV_FILE%" ps

popd
endlocal
