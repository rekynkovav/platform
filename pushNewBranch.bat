@echo off
chcp 65001 >nul 2>&1
title Git Snapshot Push
color 0A

echo ============================================
echo   GIT SNAPSHOT - CREATE AND PUSH BRANCH
echo ============================================
echo.

where git >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Git not found in PATH.
    pause
    exit /b 1
)

git rev-parse --is-inside-work-tree >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Not a Git repository.
    pause
    exit /b 1
)

git diff-index --quiet HEAD -- && git diff-files --quiet -- && (
    set UNTRACKED=0
    for /f "delims=" %%i in ('git ls-files --others --exclude-standard 2^>nul') do set UNTRACKED=1
    call :check_clean
)
goto :do_work

:check_clean
if %UNTRACKED%==0 (
    echo [INFO] Working tree clean. Nothing to commit.
    pause
    exit /b 0
)
goto :eof

:do_work
for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'"') do set TS=%%i
set BRANCH=snapshot/%TS%

echo [STEP 1/4] Staging all changes...
git add -A

echo [STEP 2/4] Committing...
git commit -m "Snapshot %TS%"

echo [STEP 3/4] Creating branch: %BRANCH%
git checkout -b %BRANCH%

echo [STEP 4/4] Pushing to origin...
git push -u origin %BRANCH%
if errorlevel 1 (
    echo [ERROR] Push failed. Check network and remote.
    pause
    exit /b 1
)

echo.
echo ============================================
echo   SUCCESS
echo ============================================
echo Branch: %BRANCH%
echo Remote: origin/%BRANCH%
echo.
pause
exit /b 0