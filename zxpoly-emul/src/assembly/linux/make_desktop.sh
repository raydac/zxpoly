#!/bin/bash

# Script just generates free desktop descriptor to start application

APP_HOME="$(realpath $(dirname ${BASH_SOURCE[0]}))"
TARGET=$APP_HOME/zx-poly-emulator.desktop

echo [Desktop Entry] > $TARGET
echo Encoding=UTF-8 >> $TARGET
echo Version=2.0.6 >> $TARGET
echo Type=Application >> $TARGET
echo Name=ZX-Poly emulator >> $TARGET
echo GenericName=ZX-Poly emulator >> $TARGET
echo Icon="$APP_HOME/logo.svg" >> $TARGET
echo Exec="$APP_HOME/run.sh" %f >> $TARGET
echo Comment=Emulator of ZX-Poly platform >> $TARGET
echo Categories=Game;Emulator; >> $TARGET
echo Terminal=false >> $TARGET
echo StartupWMClass=zx-poly-emulator >> $TARGET
echo OnlyShowIn=Old; >> $TARGET
echo StartupNotify=true >> $TARGET

echo Desktop script has been generated: $TARGET

if [ -d ~/.gnome/apps ]; then
    echo copy to ~/.gnome/apps
    cp -f $TARGET ~/.gnome/apps
fi

if [ -d ~/.local/share/applications ]; then
    echo copy to ~/.local/share/applications
    cp -f $TARGET ~/.local/share/applications
fi

if [ -d ~/Desktop ]; then
    echo copy to ~/Desktop
    cp -f $TARGET ~/Desktop
fi

