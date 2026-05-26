@echo off
echo ========================================
echo Compiling Backend
echo ========================================
echo.

cd backend
echo Running Maven compile...
call mvnw clean compile

echo.
echo ========================================
echo Compilation completed
echo ========================================
pause
