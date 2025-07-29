set ZXPOLY_HOME="%cd%"

rem uncomment the line below if graphics works slowly
rem set JAVA_EXTRA_GFX_FLAGS="-Dsun.java2d.opengl=true"
rem set JAVA_EXTRA_GFX_FLAGS="-Dcom.sun.management.jmxremote=true -Dsun.java2d.opengl=true"

rem set JAVA_EXTRA_GFX_FLAGS="-Dcom.sun.management.jmxremote=true -Dsun.java2d.opengl=true"

set JAVA_FLAGS="-XX:-DontCompileHugeMethods --add-opens=java.base/java.lang=ALL-UNNAMED"
set JAVA_RUN="%ZXPOLY_HOME%\jre\bin\javaw.exe"

start "ZXPoly" "%JAVA_RUN%" %JAVA_FLAGS% %JAVA_EXTRA_GFX_FLAGS% "-Djava.library.path=%ZXPOLY_HOME%" -jar "%ZXPOLY_HOME%/zxpoly-emul.jar" %*
