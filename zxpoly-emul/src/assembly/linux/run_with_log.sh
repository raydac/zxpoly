#!/bin/bash

ZXPOLY_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="$ZXPOLY_HOME/console.log"
JAVA_HOME="$ZXPOLY_HOME/jre"

#JAVA_EXTRA_GFX_FLAGS="-Dcom.sun.management.jmxremote=true -Dsun.java2d.opengl=true"
#JAVA_EXTRA_GFX_FLAGS="-Dsun.java2d.opengl=true"

JAVA_FLAGS="-XX:+UseZGC -XX:+TieredCompilation -XX:MaxMetaspaceSize=128m -Dsun.rmi.transport.tcp.maxConnectionThreads=0 -XX:-DontCompileHugeMethods -XX:+DisableAttachMechanism -Dswing.bufferPerWindow=false -Xms512m -Xmx1024m --add-opens=java.base/java.util=ALL-UNNAMED --enable-native-access=ALL-UNNAMED"

JAVA_RUN="$JAVA_HOME/bin/java"

export vblank_mode="${vblank_mode:-0}"
export __GL_SYNC_TO_VBLANK="${__GL_SYNC_TO_VBLANK:-0}"

if [ -f "$ZXPOLY_HOME/.pid" ]; then
    SAVED_PID="$(cat "$ZXPOLY_HOME/.pid")"
    if kill -0 "$SAVED_PID" 2>/dev/null; then
        echo "Emulator already started! If it is wrong, just delete the .pid file in the emulator folder root!"
        exit 1
    fi
fi

echo "\$JAVA_RUN=$JAVA_RUN" > "$LOG_FILE"

echo "------JAVA_VERSION------" >> "$LOG_FILE"

"$JAVA_RUN" -version >> "$LOG_FILE" 2>&1

echo "------------------------" >> "$LOG_FILE"

"$JAVA_RUN" $JAVA_FLAGS $JAVA_EXTRA_GFX_FLAGS -jar "$ZXPOLY_HOME/zxpoly-emul.jar" "$@" >> "$LOG_FILE" 2>&1 &
THE_PID=$!
renice -n -5 "$THE_PID" >/dev/null 2>&1 || true
echo "$THE_PID" > "$ZXPOLY_HOME/.pid"
wait "$THE_PID"
rm -f "$ZXPOLY_HOME/.pid"
exit 0
