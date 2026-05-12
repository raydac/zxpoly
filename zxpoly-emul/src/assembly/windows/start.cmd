@echo off
setlocal EnableExtensions

set "ZXPOLY_HOME=%cd%"

rem Uncomment one line below if graphics is slow or you need JMX.
rem set "JAVA_EXTRA_GFX_FLAGS=-Dsun.java2d.opengl=true"
rem set "JAVA_EXTRA_GFX_FLAGS=-Dcom.sun.management.jmxremote=true -Dsun.java2d.opengl=true"

if not defined JAVA_EXTRA_GFX_FLAGS set "JAVA_EXTRA_GFX_FLAGS="

set "JAVA_FLAGS=-XX:+UseZGC -XX:MaxMetaspaceSize=128m -Dsun.rmi.transport.tcp.maxConnectionThreads=0 -XX:-DontCompileHugeMethods -XX:+DisableAttachMechanism -Xms512m -Xmx1024m --add-opens=java.base/java.util=ALL-UNNAMED"
set "JAVA_RUN=%ZXPOLY_HOME%\jre\bin\javaw.exe"

start "ZXPoly" /D "%ZXPOLY_HOME%" "%JAVA_RUN%" %JAVA_FLAGS% %JAVA_EXTRA_GFX_FLAGS% -jar "%ZXPOLY_HOME%\zxpoly-emul.jar" %*
