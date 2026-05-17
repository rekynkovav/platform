@echo off
setlocal

set ANTHROPIC_BASE_URL=http://localhost:20128/v1
set ANTHROPIC_AUTH_TOKEN=sk-2c013d47591dfcdf-4e29ad-2f46e719
set ANTHROPIC_MODEL=111
set ANTHROPIC_SMALL_FAST_MODEL=111
set CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC=1
set CLAUDE_CODE_DISABLE_UPDATE_CHECK=1

set SESSION_FILE=%TEMP%\claude_active_%RANDOM%.tmp
echo 1 > "%SESSION_FILE%"

tasklist | find /i "omniroute" >nul
if errorlevel 1 (
    powershell -Command "Start-Process 'C:\Users\Suveren\AppData\Roaming\npm\routiform.cmd' -ArgumentList '--no-open' -WindowStyle Hidden"
    
    set COUNT=0
    goto :wait_loop
) else (
    goto :ready
)

:wait_loop
curl -s http://localhost:20128/v1/models >nul 2>&1
if not errorlevel 1 goto :ready
set /a COUNT=COUNT+1
if %COUNT% GEQ 30 (
    del "%SESSION_FILE%"
    exit /b 1
)
timeout /t 1 >nul
goto :wait_loop

:ready
call claude %*

if exist "%SESSION_FILE%" del "%SESSION_FILE%"

dir /b "%TEMP%\claude_active_*.tmp" >nul 2>&1
if errorlevel 1 (
    taskkill /f /im omniroute.exe /t >nul 2>&1
    taskkill /f /im node.exe /fi "WINDOWTITLE eq next-server*" /t >nul 2>&1
)

endlocal