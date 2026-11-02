@echo off
setlocal
if defined BENTO_JAVA_HOME (
    set "JAVA_HOME=%BENTO_JAVA_HOME%"
) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)
if not defined JAVA_HOME (
    echo Set JAVA_HOME or BENTO_JAVA_HOME to your JDK 17 installation.
    exit /b 1
)
call "%~dp0gradlew.bat" -p "%~dp0." %*
exit /b %ERRORLEVEL%

