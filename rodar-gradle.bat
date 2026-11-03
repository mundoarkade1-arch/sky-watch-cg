@echo off
cd /d %~dp0android-app
call gradlew.bat %* --console=plain > ..\build-logs\gradle-ultimo.log 2>&1
set GRADLE_EXIT=%ERRORLEVEL%
echo FIM-CODIGO-%GRADLE_EXIT% >> ..\build-logs\gradle-ultimo.log
exit /b %GRADLE_EXIT%