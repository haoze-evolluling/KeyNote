@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

if /I "%1"=="clean" (
    echo Cleaning build cache...
    call gradlew clean
    echo.
)

echo Building Release...
call gradlew assembleRelease
echo.
echo Build exit code: %ERRORLEVEL%
