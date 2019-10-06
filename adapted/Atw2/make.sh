#!/bin/bash

rm -rf target
mkdir target

tools/sjasmplus --lst=target/lst.lst multiloader.asm


dosbox -c "cycles max" -c "MOUNT D $PWD" -c "d:" -c "tools\\ZCOP.EXE target\\atw2.trd others\\boot.\$B  planes\\a1C0.\$C planes\\a1C1.\$C planes\\a1C2.\$C planes\\a1C3.\$C" -c "exit"

