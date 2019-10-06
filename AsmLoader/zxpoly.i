; Macroses for ZX-Poly platform
;
; Format: Sjasmplus
; Version: 1.01
; Author: Igor Maznitsa


  IFNDEF ZXPOLYMACROSES

  DISPLAY "ACTIVATE ZXPOLY BLOCK"

  DEFINE ZXPOLYMACROSES 1

; Constants describe processors
CPU0 EQU 0
CPU1 EQU 1
CPU2 EQU 2
CPU3 EQU 3

; Constants describe video mode
VMODE_CPU0 EQU 0	; Standard ZX-Spectrum mode, video in CPU0 address area
VMODE_CPU1 EQU 1	; Standard ZX-Spectrum mode, video in CPU1 address area
VMODE_CPU2 EQU 2	; Standard ZX-Spectrum mode, video in CPU2 address area
VMODE_CPU3 EQU 3	; Standard ZX-Spectrum mode, video in CPU3 address area
VMODE_POLY EQU 4	; ZX-Poly 16 colors, video data will be taken from CPU0-CPU3 address area
VMODE_HRES EQU 5	; ZX-Poly 512x384, video data will be taken from CPU0-CPU3 address area


ZXPOLY_MAIN EQU $3D00	; The main ZX-Poly configuration port

FLAG_MASK_NWAIT13 EQU 1 	; This flag describes bit0 of the main ZX-Poly port, the reset bit will send WAIT signal to CPU1-CPU3
FLAG_MASK_RESET EQU 2	; This flag describes bit1 of the main ZX-Poly port, the set bit will send RESET (soft, it means that the signal will be send only to processors) signal to CPU0-CPU3
FLAG_MASK_VIDEO EQU $1C	; This flag describes bit2-4 of the main ZX-Poly port, these bits describe current video mode
FLAG_MASK_CPUIO EQU $60	; This flag describes bit5-6 of the main ZX-Poly port, these bits describe current index of a CPUIO for CPU0
FLAG_MASK_BLOCK EQU $80	; This flag describes bit7 of the main ZX-Poly port, if the bit set then all ZX-Poly ports will be locked until hard RESET signal

; Inside registers of CPU modules
CPU0_R0 EQU $00FF
CPU0_R1 EQU $01FF
CPU0_R2 EQU $02FF
CPU0_R3 EQU $03FF

CPU1_R0 EQU $10FF
CPU1_R1 EQU $11FF
CPU1_R2 EQU $12FF
CPU1_R3 EQU $13FF

CPU2_R0 EQU $20FF
CPU2_R1 EQU $21FF
CPU2_R2 EQU $22FF
CPU2_R3 EQU $23FF

CPU3_R0 EQU $30FF
CPU3_R1 EQU $31FF
CPU3_R2 EQU $32FF
CPU3_R3 EQU $33FF

; Macrocommand to check if a CPU in HALT mode, if the CPU module has been halted then CY contains 1 else CY has 0
; Using: A, Flags(CY)
; cpu - index of CPU module
	MACRO	IS_CPU_HALT	cpu
		ld a,(CPU0_R0 | (cpu<<12))>>>8
		in a,((CPU0_R0 | (cpu<<12)) & $FF)
		rra
	ENDM

; Macrocommand to check if a CPU in WAIT mode, if the CPU module has been waited then CY contains 1 else CY has 0
; Using: A, Flags(CY)
; cpu - index of CPU module
	MACRO	IS_CPU_WAIT	cpu
		ld a,(CPU0_R0 | (cpu<<12))>>>8
		in a,((CPU0_R0 | (cpu<<12)) & $FF)
		rra
		rra
	ENDM

; Macrocommand to get last command packed address (A15,A14,A12,A8,A2,A1)
; Using: A, Flags(CY)
; cpu - index of CPU module
	MACRO	GET_CPU_ADDR cpu
		ld a,(CPU0_R0 | (cpu<<12))>>>8
		in a,((CPU0_R0 | (cpu<<12)) & $FF)
		srl a
		srl a
	ENDM

; To set a 64kb offset for a CPU module in total 512kB memory area
; Using: A, BC
; cpu - CPU module index
; page - offset (0-7)
	MACRO SET_CPU_MEM_OFFSET cpu,page
		ld bc,CPU0_R0 | (cpu<<12) ; save in BC the port number
		ld a,(COPY_CPU0_R0 + cpu)
		and ~7
		or page
		out (c),a
		ld (COPY_CPU0_R0 + cpu),a
	ENDM

; To disable writing operations to memory for a CPU module
; Using: BC,A
; cpu - CPU module index
; flag - 0 enable, 1 disable
	MACRO DISABLE_MEM_WR cpu,flag
		ld bc,CPU0_R0 | (cpu<<12) ; save in BC the port number
		ld a,(COPY_CPU0_R0 + cpu)

		IF flag==0
			res 3,a
		ELSE
			set 3,a
		ENDIF

		OUT (c),a
		LD (COPY_CPU0_R0 + cpu),a
	ENDM

; To disable writing operations to IO for a CPU module
; Using BC,A
; cpu - CPU module index
; flag - 0 enable, 1 disable
	MACRO DISABLE_IO_WR cpu,flag
		ld bc,CPU0_R0 | (cpu<<12) ; save in BC the port number
		ld a,(COPY_CPU0_R0 + cpu)

		IF flag==0
			res 4,a
		ELSE
			set 4,a
		ENDIF

		out (c),a
		ld (COPY_CPU0_R0 + cpu),a
	ENDM

; To send a soft RESET to a CPU module
; Using BC,A
; cpu - CPU module index
	MACRO SOFTRESET_CPU cpu
		ld bc,CPU0_R0 | (cpu<<12) ; save in BC the port number
		ld a,(COPY_CPU0_R0+cpu)
		set 5,a
		out (c),a
	ENDM


; To send NMI signal to a CPU module
; Using BC,A
; cpu - CPU module index
	MACRO CPUNMI cpu
		LD BC,CPU0_R0 | (cpu<<12) ; save in BC the port number
		LD A,(COPY_CPU0_R0+cpu)
		SET 6,A
		OUT (C),A
	ENDM

; To send INT signal to a CPU module
; Using: BC,A
; cpu - CPU module index
	MACRO CPUINT cpu
		ld bc,CPU0_R0 | (cpu<<12) ; save in BC the port number
		ld a,(COPY_CPU0_R0+cpu)
	 	set 7,a
		out (c),a
	ENDM

; To save 3 bytes of a softRESET command into a CPU module registers
; Using: BC,A
; cpu - CPU module index
; b1 - the first byte of the command
; b2 - the second byte of the command
; b3 - the tirth byte of the command
	MACRO SETRESCOMMAND cpu,b1,b2,b3
		ld BC,CPU0_R1 | (cpu<<12) ; save in BC the port number
		ld A,b1
		out (C),A
		ld (COPY_CPU0_R1 + cpu),A

		inc B
		ld A,b2
		out (C),A
		ld (COPY_CPU0_R2 + cpu),A

		inc B
		ld A,b3
		out (C),A
		ld (COPY_CPU0_R3 + cpu),A
	ENDM

; To set a stop address for a CPU module
; Using: BC,A
; cpu - CPU module index
; address - 16 bit stop address
	MACRO SETSTOPADDR cpu,address
		LD BC,CPU0_R2 | (cpu<<12) ; save in BC the port number
		LD A,addr & $FF
		OUT (C),A
		LD (COPY_CPU0_R1 + cpu),A
		INC B
		LD A,addr>>>8
		OUT (C),A
		LD (COPY_CPU0_R2 + cpu),A
	ENDM

; Set a value to the main ZX-Poly port
; Using: BC,A
; value - a value to be written
	MACRO SETPOLYMAIN value
		LD A,value
		LD BC,ZXPOLY_MAIN
		OUT (C),A
		LD (COPY_3D00),A
	ENDM


; Set a value to a CPU module register
; Using: BC,A
; reg - a register of a CPU module
; value - a value to be written
	MACRO SETPOLYREG reg,value
		LD BC,reg ; save in BC the port number
		LD A,value
		OUT (C),A
		LD (COPY_CPU0_R0 + ((reg>>>8) & $F)+((reg>>>12)<<2)),A
	ENDM

; Get a value from a CPU module register copy
; Using: A
; reg - the register of a CPU module
	MACRO GETPOLYREGC reg
		LD A,(COPY_CPU0_R0 + ((reg>>>8) & $F)+((reg>>>12)<<2))
	ENDM

; Get a value directly from a CPU module register
; Using: A
; reg - the register of a CPU module
	MACRO GETPOLYREG reg
		LD A,reg>>>8
		IN A,(reg & $FF)
	ENDM

; Set video mode
; Using: A,BC
; mode - new videomode value
	MACRO SETVIDEOMODE mode
		LD A,(COPY_3D00)
		AND ~FLAG_MASK_VIDEO
		OR mode<<2
		LD BC,ZXPOLY_MAIN
		OUT (C),A
		LD (COPY_3D00),A
	ENDM

; Soft RESET for all CPU modules
; Using: A,BC
	MACRO SOFTRESET_ALL
		LD A,(COPY_3D00)
		SET 1,A
		LD BC,ZXPOLY_MAIN
		OUT (C),A
	ENDM

; Set WAIT for CPU1-CPU3
; Using: A,BC
; value - 0 - reset wait for CPU1-CPU3, 1 - set wait for CPU1-CPU3
	MACRO SETWAIT13 value
		LD A,(COPY_3D00)

		IF value==0
			SET 1,A
		ELSE
			RES 1,A
		ENDIF

		LD BC,ZXPOLY_MAIN
		LD (COPY_3D00),A
		OUT (C),A
	ENDM

; Set IO CPU module for CPU0
; Using: BC,A
; cpu - ubdex of a CPU module
	MACRO SETIOCPU cpu
		LD A,(COPY_3D00)
		AND ~FLAG_MASK_CPUIO
		OR cpu<<5
		LD BC,ZXPOLY_MAIN
		LD (COPY_3D00),A
		OUT (C),A
	ENDM

; Lock all zx-poly registers until hardware RESET
; Using: A,BC
	MACRO LOCKPOLY
		LD A,(COPY_3D00)
		SET 7,A
		LD BC,ZXPOLY_MAIN
		LD (COPY_3D00),A
		OUT (C),A
	ENDM

; Check zx-poly for locking. If locked then Z=1 , else Z=0
; Using: A,Flags(Z)
	MACRO ISPOLYLOCKED
		LD A,(COPY_3D00)
		TST A,7
	ENDM

; Check, is IO writing operations disabled. If the operations are disabled then Z=0, else Z=1
; Using: A
	MACRO ISIODISABLED
		LD A,ZXPOLY_MAIN>>>8
		IN A,(ZXPOLY_MAIN & $FF)
		TST 2
	ENDM

; Check, is memory writing operations disabled. If the operations are disabled then Z=0, else Z=1
; Using: A
	MACRO ISMEMDISABLED
		LD A,ZXPOLY_MAIN>>>8
		IN A,(ZXPOLY_MAIN & $FF)
		TST 3
	ENDM

; Check, is the CPU module working as an IO module for CPU0. If it is an IO module then Z=0, else Z=1
; Using: A, Flags(Z)
	MACRO ISIOCPU
		LD A,ZXPOLY_MAIN>>>8
		IN A,(ZXPOLY_MAIN & $FF)
		TST 4
	ENDM

; Get current CPU module number. The number will be saved in A
; Using: A
	MACRO GETCPUINDEX
		LD A,ZXPOLY_MAIN>>>8
		IN A,(ZXPOLY_MAIN & $FF)
		AND 3
	ENDM

; Get RAM page offset for current CPU. The number will be saved in A
; Using: A, Flags(CY)
	MACRO GETRAMOFFSET
		LD A,ZXPOLY_MAIN>>>8
		IN A,(ZXPOLY_MAIN & $FF)
		RLCA
		RLCA
		RLCA
		AND 7
	ENDM

; Copy a memory block from memory CPU0 to selected CPU module memory
; Using: IX,A,DE,BC,HL
; cpu - destination CPU index
; addr - the start address of the memorey block
; length - the length of the memory block
	MACRO COPY2CPU cpu, addr, length
		; set disNMI for the ZX-Poly R1 of target CPU
		LD IX,COPY_CPU0_R1
		LD A,(IX+0)
		OR $30
		LD BC,CPU0_R1
		OUT (C),A
		LD (IX+0),A

		LD BC,CPU0_R1+cpu
		OUT (C),A
		LD (IX+cpu),A

		LD A,(COPY_3D00)
		AND ~$60
		OR cpu<<5
		LD (COPY_3D00),A
		LD BC,ZXPOLY_MAIN
		OUT (C),A

		; all completed and we can copy memory array
		LD DE,length
		LD BC,addr
		LD HL,addr
.loop:
		LD A,(HL)
		OUT (C),A
		INC HL
		INC BC
		DEC DE
		LD A,E
		OR D
		JR NZ,.loop

		; block has been copied
		; we need to remove IO CPU and unblock NMI
		LD A,(COPY_3D00)
		AND ~$60
		LD (COPY_3D00),A
		LD BC,ZXPOLY_MAIN
		OUT (C),A

		LD A,(IX+0)
		RES 5,A
		RES 4,A
		LD (IX+0),A
		LD BC,CPU0_R1
		OUT (C),A
	ENDM

; To restore all CPU registers from a memory area
; because it uses the stack mechanism to save registers, address must contains the top of the area
; Using: ...m
; addr - address of the area top
	MACRO LOADREGS addr
	   LD SP,(addr-2)
	   POP IX
	   POP IY
	   POP HL
	   POP DE
	   POP BC
	   EXX
	   POP HL
	   POP DE
	   POP BC
	   EXX
	   EX AF,AF
	   POP AF
	   EX AF,AF
	   POP AF
	   LD (addr-4),HL
	   LD HL,(addr-6)
	   LD SP,HL
	   LD HL,(addr-4)
	ENDM

; To save all CPU registers into a memory area
; because it uses the stack mechanism to save registers, address must contains the top of the area
; Using: ...
; addr - address of the area top
	MACRO SAVEREGS addr
		LD (addr-6),SP
		LD SP,addr-6
		PUSH AF
	   	EX AF,AF
	   	PUSH AF
		EX AF,AF
		EXX
		PUSH BC
		PUSH DE
		PUSH HL
		EXX
		PUSH BC
		PUSH DE
		PUSH HL
		PUSH IY
		PUSH IX
		LD (addr-2),SP
		LD SP,(addr-6)
	ENDM

  MACRO DOS_RESET
			ld c,0
			call 15635
	ENDM

	MACRO DOS_DISK
			ld c,$18
			call $3D13 ; to volume a disk
	ENDM

	MACRO DOS_DRIVE drv
			ld a,drv
			ld c,1
			call 15635
	ENDM

	MACRO DOS_LOAD str,addr
			ld hl,str ; to create a descriptor
			ld c,$13
			call $3D13

			ld c,$0A; find file
			call $3D13
			ld a,c
		  inc c
			jr z,.nofile

			ld c,$08
			call $3D13

			xor a
			ld ($5CF9),a
			ld hl,addr
			ld a,$FF

			ld c,$0E ; read from disk into memory area for an address
			call $3D13
			jr .end
.nofile:
			ld bc,$0FFE
			ld a,2
			out (c),a
.end:
	ENDM

  MACRO BORDER color
		ld a,color
		out ($FE),a
	ENDM

  ENDIF
