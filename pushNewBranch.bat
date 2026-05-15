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

:: Ensure .gitignore exists — CREATE IF MISSING
if not exist .gitignore (
    echo Creating .gitignore...
    (
        echo node_modules/
        echo .cache/
        echo dist/
        echo build/
        echo .next/
        echo .env
        echo *.log
    ) > .gitignore
    echo .gitignore created.
)

:: Get timestamp
for /f "delims=" %%i in ('powershell -Command "Get-Date -Format 'dd-MM-yyyy_HH-mm-ss'"') do set BRANCH_NAME=%%i

echo Creating new branch: %BRANCH_NAME%
echo.

:: Add changes — RESPECT .gitignore
echo Adding changes (respecting .gitignore)...
git add .

:: Commit
echo Committing changes...
git commit -m "Auto-commit: snapshot %BRANCH_NAME%" --allow-empty

:: Create and switch to new branch
echo Creating new branch...
git checkout -b %BRANCH_NAME%

:: Update README.md
echo Updating README.md...
call :updateReadme

:: Push new branch
echo Pushing to origin...
git add README.md
git commit -m "Update README.md: %BRANCH_NAME%"
git push origin %BRANCH_NAME%

echo.
echo ============================================
echo DONE!
echo ============================================
echo New branch: %BRANCH_NAME%
echo.

pause
goto :eof

:: Function to update README.md
:updateReadme
set "timestamp=%BRANCH_NAME%"
set "date_part=%timestamp:~0,10%"
set "time_part=%timestamp:~11,8%"

echo. >> README.md
echo ## New Features (%date_part% %time_part%) >> README.md
echo. >> README.md
echo - Auto-generated update from branch: %BRANCH_NAME% >> README.md
echo - Commit timestamp: %timestamp% >> README.md
echo. >> README.md
echo --- >> README.md
echo. >> README.md

exit /b 0