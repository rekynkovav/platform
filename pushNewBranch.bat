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

:: Get timestamp in format: DD-MM-YYYY_HH-MM-SS
for /f "delims=" %%i in ('powershell -Command "Get-Date -Format 'dd-MM-yyyy_HH-mm-ss'"') do set BRANCH_NAME=%%i

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

:: Update README.md with new features
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

echo.
echo # New Features (%date_part% %time_part%) >> README.md
echo.
echo - Auto-generated update from branch: %BRANCH_NAME%
echo - Commit timestamp: %timestamp%
echo.
echo ---
echo.

exit /b 0
