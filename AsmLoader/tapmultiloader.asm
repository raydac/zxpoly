; Make multiloader for TAPE

LOADBIN .EQU 24600
STARTBIN1 EQU 24600

START:	EQU		#4100

	.org START
	jp main

	.include "zxpoly.def"
	.include "tap.def"
	.include "other.def"

main:
; here we load all our named data blocks from the tape

	TAP_LOAD DATA_BIN3,LOADBIN
	DI
	COPY2CPU CPU3,$4000,$FFFF-$4000

	TAP_LOAD DATA_BIN2,LOADBIN
	DI
	COPY2CPU CPU2,$4000,$FFFF-$4000

	TAP_LOAD DATA_BIN1,LOADBIN
	DI
	COPY2CPU CPU1,$4000,$FFFF-$4000

	TAP_LOAD DATA_BIN0,LOADBIN
	DI

; save current register state of our main cpu
	SAVEREGS regarea

	COPY2CPU CPU3,$4000,$1FB3
	COPY2CPU CPU2,$4000,$1FB3
	COPY2CPU CPU1,$4000,$1FB3

; lock IO writing for CPU1-CPU3
	DISABLE_IO_WR CPU1,1
	DISABLE_IO_WR CPU2,1
	DISABLE_IO_WR CPU3,1

; set the start command for a soft reset
	SETRESCOMMAND CPU0,$C3,RUNCODE & $FF,RUNCODE>>>8
	SETRESCOMMAND CPU1,$C3,RUNCODE & $FF,RUNCODE>>>8
	SETRESCOMMAND CPU2,$C3,RUNCODE & $FF,RUNCODE>>>8
	SETRESCOMMAND CPU3,$C3,RUNCODE & $FF,RUNCODE>>>8

; make soft reset and switch the platform in needed video mode
	SETPOLYMAIN $93 ; block+softreset+zxpolyvideo


; Here is the common start block for all CPU modules
RUNCODE:
	DI

; here we place value in the 7FFD (zx-128) port to have the same its value for all cpu
	LD A,$30
	LD BC,$7FFD
	OUT (C),A

; restore cpu state from the area where we saved it early
	LOADREGS regarea

; go to the start address, it can be JP or CALL
	CALL STARTBIN1

; area of registers saving
regarea:
	.block 40

; names of data blocks
DATA_BIN0	.ascii "dblock_2_0"
DATA_BIN1	.ascii "dblock_2_1"
DATA_BIN2	.ascii "dblock_2_2"
DATA_BIN3	.ascii "dblock_2_3"

; save as bin to load
;	SAVEBIN "taploader.bin",START,$-START



