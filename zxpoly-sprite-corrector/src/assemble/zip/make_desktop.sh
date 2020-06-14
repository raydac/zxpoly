#!/bin/bash

# Script just generates free desktop descriptor to start application

APP_HOME="$(realpath $(dirname ${BASH_SOURCE[0]}))"
TARGET=$APP_HOME/zx-poly-corrector.desktop

echo [Desktop Entry] > $TARGET
echo Version=1.1 >> $TARGET
echo Encoding=UTF-8 >> $TARGET
echo Type=Application >> $TARGET
echo Name=ZX-Poly corrector >> $TARGET
echo GenericName=ZX-Poly corrector >> $TARGET
echo Icon=$APP_HOME/icon.svg >> $TARGET
echo Exec=\"$APP_HOME/run.sh\" %f >> $TARGET
echo Comment=Sprite corrector for ZX-Poly platform >> $TARGET
echo "Categories=Game;Emulator;" >> $TARGET
echo "Keywords=zx;spectrum;poly;editor;game;" >> $TARGET
echo Terminal=false >> $TARGET
echo StartupWMClass=zx-poly-corrector >> $TARGET

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

