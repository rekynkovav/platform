@echo off
echo Test script started
where git
echo Git exit code: %errorlevel%
echo.
echo Current directory: %cd%
echo.
git --version
echo Git version exit code: %errorlevel%
echo.
git status --porcelain
echo Git status exit code: %errorlevel%
echo.
pause
