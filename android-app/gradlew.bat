@echo off
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_HOME=%DIRNAME%
if not "%JAVA_HOME%" == "" goto have_java_home
set JAVACMD=java
%JAVACMD% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto have_javacmd
echo ERROR: JAVA_HOME is not set and no java command could be found in your PATH.
exit /b 1
:have_java_home
set JAVACMD=%JAVA_HOME%\bin\java.exe
if not exist "%JAVACMD%" (
    echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
    exit /b 1
)
:have_javacmd
set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
    echo ERROR: Gradle wrapper jar not found at %WRAPPER_JAR%
    exit /b 1
)
set CLASSPATH=%WRAPPER_JAR%
"%JAVACMD%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
