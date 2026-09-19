#!/bin/bash

# Script just generates free desktop descriptor to start application

APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="$APP_HOME/zx-poly-emulator.desktop"

{
    echo "[Desktop Entry]"
    echo "Version=1.1"
    echo "Encoding=UTF-8"
    echo "Type=Application"
    echo "Name=ZX-Poly emulator"
    echo "GenericName=ZX-Poly emulator"
    echo "Icon=$APP_HOME/logo.svg"
    echo "Exec=\"$APP_HOME/run.sh\" %f"
    echo "Comment=Emulator of ZX-Poly platform"
    echo "Categories=Game;Emulator;"
    echo "Keywords=zx;spectrum;poly;emulator;game;"
    echo "Terminal=false"
    echo "StartupWMClass=zx-poly-emulator"
} > "$TARGET"

echo "Desktop script has been generated: $TARGET"

if [ -d "$HOME/.gnome/apps" ]; then
    echo "copy to ~/.gnome/apps"
    cp -f "$TARGET" "$HOME/.gnome/apps"
fi

if [ -d "$HOME/.local/share/applications" ]; then
    echo "copy to ~/.local/share/applications"
    cp -f "$TARGET" "$HOME/.local/share/applications"
fi

if [ -d "$HOME/Desktop" ]; then
    echo "copy to ~/Desktop"
    cp -f "$TARGET" "$HOME/Desktop"
fi
