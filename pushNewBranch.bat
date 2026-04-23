@echo off
chcp 65001 >nul
title Platform Project - Push to New Branch
color 0A

echo ============================================
echo   PLATFORM PROJECT PUSH TO NEW BRANCH
echo ============================================
echo.

:: Check for Git
where git >nul 2>nul
if errorlevel 1 (
    echo ERROR: Git not found!
    pause
    exit /b 1
)

:: Get timestamp in format: YYYY-MM-DD_HH-MM-SS
for /f "delims=" %%i in ('powershell -Command "Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'"') do set BRANCH_NAME=%%i

echo Creating new branch: %BRANCH_NAME%
echo.

:: Add all changes
echo Adding all changes...
git add .

:: Commit
echo Committing changes...
git commit -m "Auto-commit: snapshot %BRANCH_NAME%" --allow-empty

:: Create and switch to new branch
echo Creating new branch...
git checkout -b %BRANCH_NAME%

:: Push new branch
echo Pushing to origin...
git push origin %BRANCH_NAME%

echo.
echo ============================================
echo DONE!
echo ============================================
echo New branch: %BRANCH_NAME%
echo.

pause
