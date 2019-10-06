; ZX-Poly loader for tape

	DEVICE ZXSPECTRUM128

LOADBIN EQU 24600
STARTBIN1 EQU 24600

START:	EQU		#4100

	ORG START

	JP MAIN

	INCLUDE zxpoly.i

MAIN:

	MACRO TAP_LOAD name,addr ; read named bin block to address

; load header

.loadhdr:
	ld ix, TAPHDRADDR
	ld de, 17
	xor a
	scf
	call 1366
	jr nc,.end

; check header
	ld b,10
	ld de,TAPHDRADDR+1
	ld hl,name

.chckname:
	ld a,(de)
	cp (hl)
	jr nz,.loadhdr
	inc HL
	inc DE
	djnz .chckname

; load block
	ld ix,addr
	ld de,(TAPHDRADDR+11)
	ld a,$FF
	scf
	call 1366

.end:
	ENDM

; read sequently all named data blocks

	TAP_LOAD DATA_BIN3, LOADBIN
	DI
	COPY2CPU CPU3,$4000,$FFFF-$4000

	TAP_LOAD DATA_BIN2, LOADBIN
	DI
	COPY2CPU CPU2, $4000, $FFFF-$4000

	TAP_LOAD DATA_BIN1, LOADBIN
	DI
	COPY2CPU CPU1, $4000, $FFFF-$4000

	TAP_LOAD DATA_BIN0, LOADBIN
	DI

; save master CPU registers into memory buffer
	SAVEREGS MASTERCPUREGSTATE

	COPY2CPU CPU3,$4000,$1FB3
	COPY2CPU CPU2,$4000,$1FB3
	COPY2CPU CPU1,$4000,$1FB3

; disable IO for slave CPUs
	DISABLE_IO_WR CPU1,1
	DISABLE_IO_WR CPU2,1
	DISABLE_IO_WR CPU3,1

; set start command sequence for soft reset
	SETRESCOMMAND CPU0,$C3,RUNCODE & $FF,RUNCODE>>>8
	SETRESCOMMAND CPU1,$C3,RUNCODE & $FF,RUNCODE>>>8
	SETRESCOMMAND CPU2,$C3,RUNCODE & $FF,RUNCODE>>>8
	SETRESCOMMAND CPU3,$C3,RUNCODE & $FF,RUNCODE>>>8

; soft reset and switch video mode (256x192x16colors)
	SETPOLYMAIN $93 ; block+softreset+zxpolyvideo


; Here is the common start block for all CPU modules
RUNCODE:
	di

; here we place value in the $7FFD (ZX-128) port to have the same its value for all cpu
	ld a,$30
	ld bc,$7FFD
	out (c),A

; restore all CPU states
	LOADREGS MASTERCPUREGSTATE

; jump to the start address, it can be either JP or CALL
	jp STARTBIN1


; area to keep master CPU register state
MASTERCPUREGSTATE: BLOCK 40
TAPHDRADDR: BLOCK 20

	INCLUDE zxpoly.m

; names of data blocks
DATA_BIN0	BYTE "a1_____0C"
DATA_BIN1	BYTE "a2_____0C"
DATA_BIN2	BYTE "a3_____0C"
DATA_BIN3	BYTE "a4_____0C"

; save as bin to load
	SAVETAP "./taploader.tap",START
