; Loader to load several data blocks as images and start them.

LOAD_ADDR EQU 24500 ; the address from which all blocks will be loaded in memory

STARTBIN1 EQU 24500
STARTBIN2 EQU 48800

START	EQU		$4700

	.org START
	jp MAIN

	.include "zxpoly.def"
	.include "trdos.def"
	.include "other.def"

MAIN:
	di
	ld a,$30
	ld bc,$7FFD

	DOS_RESET ; reset TR-DOS
	DOS_DRIVE 0 ; select A drive

	DOS_DISK

	di

	DOS_LOAD DATA_BIN3,LOAD_ADDR
	di
	COPY2CPU CPU3,$4000,$FFFF-$4000

	DOS_LOAD DATA_BIN2,LOAD_ADDR
	di
	COPY2CPU CPU2,$4000,$FFFF-$4000

	DOS_LOAD DATA_BIN1,LOAD_ADDR
	di
	COPY2CPU CPU1,$4000,$FFFF-$4000

	DOS_LOAD DATA_BIN0,LOAD_ADDR
	di
	SAVEREGS regarea

	COPY2CPU CPU3,$4000,$1FB3
	COPY2CPU CPU2,$4000,$1FB3
	COPY2CPU CPU1,$4000,$1FB3

	; lock IO writing for CPU1-CPU3
	DISABLE_IO_WR CPU1,1
	DISABLE_IO_WR CPU2,1
	DISABLE_IO_WR CPU3,1

	SETRESCOMMAND CPU0,$C3,RUNCODE & $FF,RUNCODE>>>8
	SETRESCOMMAND CPU1,$C3,RUNCODE & $FF,RUNCODE>>>8
	SETRESCOMMAND CPU2,$C3,RUNCODE & $FF,RUNCODE>>>8
	SETRESCOMMAND CPU3,$C3,RUNCODE & $FF,RUNCODE>>>8

	SETPOLYMAIN #93 ; block+softreset+zxpolyvideo

; Here is the common start block for all CPU modules
RUNCODE:
	di
	ld a,$30
	ld bc,$7FFD
	out (c),a

	LOADREGS regarea
	BORDER 1
	CALL STARTBIN1
	BORDER 4
	CALL STARTBIN2

; buffer to save register values

	.block 40

regarea:

DATA_BIN0	.ascii "a1_____0C"
DATA_BIN1	.ascii "a1_____1C"
DATA_BIN2	.ascii "a1_____2C"
DATA_BIN3	.ascii "a1_____3C"

; Lines below works only with DJASM
;	EMPTYTRD "test.trd"
;	SAVETRD "test.trd","ploader.C",START,$-START



