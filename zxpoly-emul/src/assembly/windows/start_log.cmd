@echo off
setlocal EnableExtensions

set "ZXPOLY_HOME=%cd%"
set "LOG_FILE=%ZXPOLY_HOME%\console.log"

rem Uncomment one line below if graphics is slow or you need JMX.
rem set "JAVA_EXTRA_GFX_FLAGS=-Dsun.java2d.opengl=true"
rem set "JAVA_EXTRA_GFX_FLAGS=-Dcom.sun.management.jmxremote=true -Dsun.java2d.opengl=true"

if not defined JAVA_EXTRA_GFX_FLAGS set "JAVA_EXTRA_GFX_FLAGS="

set "JAVA_FLAGS=-XX:-DontCompileHugeMethods --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED  --enable-native-access=ALL-UNNAMED"
set "JAVA_RUN=%ZXPOLY_HOME%\jre\bin\java.exe"

echo JAVA_RUN="%JAVA_RUN%" > "%LOG_FILE%"

echo ------JAVA_VERSION------ >> "%LOG_FILE%"

"%JAVA_RUN%" -version 2>> "%LOG_FILE%"

echo ------------------------ >> "%LOG_FILE%"

"%JAVA_RUN%" %JAVA_FLAGS% %JAVA_EXTRA_GFX_FLAGS% -jar "%ZXPOLY_HOME%\zxpoly-emul.jar" %* 2>> "%LOG_FILE%"
