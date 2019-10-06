#!/bin/bash

mkdir target

tools/sjasmplus --lst=target/lst.lst multiloader.asm

dosbox -c "cycles max" -c "MOUNT D $PWD" -c "d:" -c "tools\\ZCOP.EXE target\\zxword.trd others\\boot.\$B  planes\\tzxunC0.\$C planes\\tzxunC1.\$C planes\\tzxunC2.\$C planes\\tzxunC3.\$C others\\ZXW26mh.\$W" -c "exit"

