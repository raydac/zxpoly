#!/bin/bash

ZXPOLY_HOME="$(dirname ${BASH_SOURCE[0]})"
LOG_FILE=$ZXPOLY_HOME/console.log

# uncomment the line below if graphics works slowly
# JAVA_EXTRA_GFX_FLAGS="-Dsun.java2d.opengl=true"

JAVA_FLAGS="-client -XX:+IgnoreUnrecognizedVMOptions --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED -Dsun.java2d.dpiaware=true -Dswing.aatext=true -Dawt.useSystemAAFontSettings=on"

if [ -z $JAVA_HOME ]; then
    echo \$JAVA_HOME is undefined &>$LOG_FILE
    JAVA_RUN=java
else
    echo Detected \$JAVA_HOME : $JAVA_HOME &>$LOG_FILE
    JAVA_RUN=$JAVA_HOME/bin/java
fi

if [ -f $ZXPOLY_HOME/.pid ];
then
    SAVED_PID=$(cat $ZXPOLY_HOME/.pid)
    if [ -f /proc/$SAVED_PID/exe ];
    then
        echo Emulator already started! if it is wrong, just delete the .pid file in the editor folder root!
	exit 1
    fi
fi    

echo \$JAVA_RUN=$JAVA_RUN &>>$LOG_FILE

echo ------JAVA_VERSION------ &>>$LOG_FILE

$JAVA_RUN -version &>>$LOG_FILE

echo ------------------------ &>>$LOG_FILE

$JAVA_RUN $JAVA_FLAGS $JAVA_EXTRA_GFX_FLAGS -jar $ZXPOLY_HOME/zxpoly-emul.jar $@ &>> $ZXPOLY_HOME/console.log&
THE_PID=$!
echo $THE_PID>$ZXPOLY_HOME/.pid
wait $THE_PID
rm $ZXPOLY_HOME/.pid
exit 0

