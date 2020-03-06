#!/bin/bash

ZXPOLY_HOME="$(dirname ${BASH_SOURCE[0]})"
LOG_FILE=$ZXPOLY_HOME/console.log
JAVA_HOME=$ZXPOLY_HOME/jre

#JAVA_EXTRA_GFX_FLAGS="-Dcom.sun.management.jmxremote=true -Dsun.java2d.opengl=true"
#JAVA_EXTRA_GFX_FLAGS="-Dsun.java2d.opengl=true"

JAVA_FLAGS="-server -Xverify:none -Xms512m -Xmx1024m --add-opens=java.base/java.util=ALL-UNNAMED"

JAVA_RUN=$JAVA_HOME/bin/java

if [ -f $ZXPOLY_HOME/.pid ];
then
    SAVED_PID=$(cat $ZXPOLY_HOME/.pid)
    if [ -f /proc/$SAVED_PID/exe ];
    then
        echo Emulator already started! if it is wrong, just delete the .pid file in the editor folder root!
        exit 1
    fi
fi

echo \$JAVA_RUN=$JAVA_RUN &>$LOG_FILE

echo ------JAVA_VERSION------ &>>$LOG_FILE

$JAVA_RUN -version &>>$LOG_FILE

echo ------------------------ &>>$LOG_FILE

$JAVA_RUN $JAVA_FLAGS $JAVA_EXTRA_GFX_FLAGS -Djava.library.path="$ZXPOLY_HOME" -jar "$ZXPOLY_HOME"/zxpoly-emul.jar $@ &>>$LOG_FILE&
THE_PID=$!
echo $THE_PID>$ZXPOLY_HOME/.pid
wait $THE_PID
rm $ZXPOLY_HOME/.pid
exit 0
