@echo off
echo ========================================
echo Running Backend Tests
echo ========================================
echo.

cd backend
echo Running Maven tests...
call mvnw clean test

echo.
echo ========================================
echo Test execution completed
echo Check target/surefire-reports/ for details
echo ========================================
pause
