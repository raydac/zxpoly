; ZX-Poly TEST ROM
;
; version: 1.00 (06 jan 2007)
; author: Igor Maznitsa
;
; Can be compiled by ZASM  http://k1.spdns.de/Develop/Projects/zasm-4.0/Distributions/

; I keep the copy of current state $3D00 in E' and current state $7FFD in D'

            .z80

            .org $0000
            .include "./zxpolytest.def"

			di
            xor a
            exx
            ld e,a
            ld d,a
            exx

			ld bc,$3D00
			in a,(c)
			and $3
			jr z,ITSCPU0
			halt

ITSCPU0:
			ld sp,STACKSTART

            msetzx128 0

			ld a,0
			call CLRSCR_L
			xor a
			call CLRATTR_L

			msetattr 5
			msetxy 5,0
			mprint STR_HEADER

			msetattr 6
			msetxy 0,23
			mprint STR_COPY


; TEST "Check ZX-128"
			msetxy 0,2
			ld hl,ST_ZX128
			call _PRTEST

			xor a
			mset128ram
			ld a,$11
			ld ($C001),A
			ld a,1
			mset128ram
			ld a,$22
			LD ($C001),A
			ld a,2
			mset128ram
			ld a,$33
			ld ($C001),a
			xor A
			mset128ram
			ld d,$11
			ld a,($C001)
			cp d
			jr nz,_nozx128
			ld a,1
			mset128ram
			ld d,$22
			ld a,($C001)
			cp d
			jr nz,_nozx128
			ld a,2
			mset128ram
			ld d,$33
			ld a,($C001)
			cp d
			jr nz,_nozx128
			msetattr 4|128
			mprint STR_OK
			jr _test2
_nozx128:
			msetattr 2|128
			mprint STR_BAD
		 	jp _endmain

_test2:
; TEST "Check memory 128", testing of all pages
			msetxy 0,3
			ld hl,ST_MEM128
			call _PRTEST

			ld b,7

_begn000:
			ld a,5
			cp b
			jr _tstpage5
            ld hl,$C000
			jr _sttst
_tstpage5:
		    ld hl,$DC03
_sttst:
			ld c,b
			ld a,b
			mset128ram

_lpl000:
			ld (hl),c
			inc c

			inc hl
			ld a,l
			or h
			jr nz,_lpl000

			ld a,b
			or a
			jr z,_fillend
			dec b
			jr 	_begn000

_fillend:
			ld b,7

_begn111:
			ld a,5
			cp b
			jr _tstpge51
            ld hl,$C000
			jr _sttst2
_tstpge51:
			ld hl,$DC03
_sttst2:
			ld c,b
			ld a,b
			mset128ram

_lpl101:
			ld a,(hl)
			cp c
			jr nz,_test2bad

			inc c
			inc hl
			ld a,l
			or h
			jr nz,_lpl101

			ld a,b
			or a
			jr z,_test2ok
			dec b
			jr 	_begn111

_test2bad:
			msetattr 2|128
			mprint STR_BAD
		 	jp _endmain
_test2ok:
			msetattr 4|128
			mprint STR_OK

; TEST "Check ZX-POLY" allowed
; fill all memory of other processors and check results

_test3:
			msetxy 0,4
			ld hl,ST_ZXPOLY
			call _PRTEST

			ld bc,CPU0R1
			ld a,$20
			out (c),a

			ld d,4 ; fill all memory of all other CPU (1-3)

_loop000:
			dec d
			ld a,d

			or a
			jr z,_checkfill

			call SET_IOCPU

			ld e,d

			ld hl,$BFFF
			ld bc,$4000
_loop111:
			ld a,l
			or h
			jr z,_loop000
			out  (c),e
			inc e
			dec hl
			inc bc

			jr _loop111

_checkfill:
			ld d,4	; check values at all CPU IO memories

_loop0120:
			dec d
			ld a,d

			or a
			jr z,_endchgood

			call SET_IOCPU
			ld e,d

			ld hl,$BFFF
			ld bc,$4000
_loop222:
			ld a,l
			or h
			jr z,_loop0120
			in a,(c)
			cp e
			jr nz,_endchbad
			inc e
			dec hl
			inc bc

			jr _loop222

_endchbad:
			xor a
			ld bc,CPU0R1
			out (c),a
			call SET_IOCPU
			msetattr 2|128
			mprint STR_BAD
		 	jp _endmain

_endchgood:
			xor a
			ld bc,CPU0R1
			out (c),a
			call SET_IOCPU
			msetattr 4|128
			mprint STR_OK

; Checking the processors
;----------------------------

; CHECK CPU0
_testcpu0:
			msetzxpoly 0
			msetxy 0,5
			ld hl,ST_CHKCPU0
			call _PRTEST

			ld bc,CPU0R0
_addrcpu0:
			in a,(c)
			ld b,a
			ld hl,_addrcpu0
			call PACKADDR_
			sla a
			sla a
			cp b
			jr nz,_cpu0bad

			; check work of the cpu after reset
			ld bc,CPU0R2
			ld a,_cpucont & $FF
			out (c),a
			ld bc,CPU0R3
			ld a,_cpucont >> 8
			out (c),a
			ld bc,CPU0R1
			ld a,$C3
			out (c),a
			ld bc,CPU0R0
			ld a,$20 ; Send soft reset
			out (c),a
			jr _cpu0bad

_cpucont:
			ld sp,STACKSTART
			jr _cpu0ok

_cpu0bad:
			msetattr 2|128
			mprint STR_BAD
		 	jp _endmain

_cpu0ok:
			msetattr 4|128
			mprint STR_OK

; CHECK CPU1
_testcpu1:
			msetzxpoly 0
			msetxy 0,6
			ld hl,ST_CHKCPU1
			call _PRTEST

			ld a,1
			call SET_IOCPU
			call SENDPROC_
			xor a
			call SET_IOCPU

			msetzxpoly 1

			ld a,CPUTSTPROC & $FF
			ld bc,CPU1R2
			out (c),a
			ld a,CPUTSTPROC >> 8
			ld bc,CPU1R3
			out (c),a
			ld a,$C3
			ld bc,CPU1R1
			out (c),a

			ld a,$22
			ld bc,CPU1R0
			out (c),a

			nop
			nop
			nop


_cyclerdio:
			in a,(c)
			bit 0,a
			jr z, _cyclerdio

			msetzxpoly $20 ; stop processor and make it as IO CPU
			ld bc,CPUTESTRSLT
			in d,(c)
			msetzxpoly 0
			ld a,d
			or a
			jr z,_cpu1none
			cp CPUTESTOK
			jr z,_cpu1ok
			msetattr 2|128
			mprint STR_BAD
			jp _endmain
_cpu1none:
			msetattr 2|128
			mprint STR_NONE
			jp _endmain

_cpu1ok:
			msetattr 4|128
			mprint STR_OK

; CHECK CPU2
_testcpu2:
			msetzxpoly 0 ; wait all processors and disable CPU IO
			msetxy 0,7
			ld hl,ST_CHKCPU2
			call _PRTEST

			ld a,2
			call SET_IOCPU
			call SENDPROC_
			xor a
			call SET_IOCPU

			msetzxpoly 1

			ld a,CPUTSTPROC & $FF
			ld bc,CPU2R2
			out (c),a
			ld a,CPUTSTPROC >> 8
			ld bc,CPU2R3
			out (c),a
			ld a,$C3
			ld bc,CPU2R1
			out (c),a

			ld a,$24
			ld bc,CPU2R0
			out (c),a
			nop
			nop
			nop

_cyclerdio2:
			in a,(c)
			bit 0,a
			jr z, _cyclerdio2

			msetzxpoly $40 ; stop processor and make it as IO CPU
			ld bc,CPUTESTRSLT
			in d,(c)
			msetzxpoly 0
			ld a,d
			or a
			jr z,_cpu2none


			cp CPUTESTOK
			jr z,_cpu2ok
			msetattr 2|128
			mprint STR_BAD
		 	jp _endmain
_cpu2none:
			msetattr 2|128
			mprint STR_NONE
			jp _endmain
_cpu2ok:
		    msetattr 4|128
			mprint STR_OK


; CHECK CPU3
_testcpu3:
			msetzxpoly 0 ; wait all processors and disable CPU IO
			msetxy 0,8
			ld hl,ST_CHKCPU3
			call _PRTEST

			ld a,3
			call SET_IOCPU
			call SENDPROC_
			xor a
			call SET_IOCPU

			msetzxpoly 1

			ld a,CPUTSTPROC & $FF
			ld bc,CPU3R2
			out (c),a
			ld a,CPUTSTPROC >> 8
			ld bc,CPU3R3
			out (c),a

			ld a,$C3
			ld bc,CPU3R1
			out (c),a

			ld a,$26
			ld bc,CPU3R0
			out (c),a

			nop
			nop
			nop

_cyclerdio3:
			in a,(c)
			bit 0,a
			jr z, _cyclerdio3

			msetzxpoly $60 ; stop processor and make it as IO CPU
			ld bc,CPUTESTRSLT
			in d,(c)
			msetzxpoly 0
			ld a,d
	 		or a
	 		jr z,_cpu3none
			cp CPUTESTOK
			jr z,_cpu3ok
			msetattr 2|128
			mprint STR_BAD
			jp _endmain
_cpu3none:
			msetattr 2|128
			mprint STR_NONE
			jp _endmain

_cpu3ok:
			msetattr 4|128
			mprint STR_OK

;----
			msetxy 0,9 ; test RAM0 as ROM
			ld hl,ST_RAM0ROM
			call _PRTEST

			xor a
			msetzx128	0 ; swich on RAM 0 at $C000
			ld ($1234),a

			ld bc,CHKROMEND-CHKROM0PROC
			ld de,$C000
			ld hl,CHKROM0PROC
			ldir
			ld ($1234),A

			jp $C000

_rmtstbad:
			msetattr 2|128
			mprint STR_BAD
			jp _endmain
_rmtstok:
			exx
			xor a
			ld d,a
			exx
			msetattr 4|128
			mprint STR_OK


_end:
			msetxy	2,20
			msetattr 7|128
			mprint STR_PANYK
			call WAITKEY_

;---------VUIDEO TESTS------------------
			ld hl,TSTIMG_G+2
			ld de,$C000
			call DELPZ
			ld a,1
			call SET_IOCPU
			ld bc,$4000
			ld ix,$C000
			call SCR2PRT_

			ld hl,TSTIMG_B+2
			ld de,$C000
			call DELPZ
			ld a,2
			call SET_IOCPU
			ld bc,$4000
			ld ix,$C000
			call SCR2PRT_

			ld hl,TSTIMG_Y+2
			ld de,$C000
			call DELPZ
			ld a,3
			call SET_IOCPU
			ld bc,$4000
			ld ix,$C000
			call SCR2PRT_

			xor a
			call SET_IOCPU
			ld hl,TSTIMG_R+2
			ld de,$4000
			call DELPZ

			mvideomode 4

			call WAITKEY_

TEST512:
			ld hl,IMG512_C1+2
			ld de,$C000
			call DELPZ
			ld a,1
			call SET_IOCPU
			ld bc,$4000
			ld ix,$C000
			call SCR2PRT_

			ld hl,IMG512_C2+2
			ld de,$C000
			call DELPZ
			ld a,2
			call SET_IOCPU
			ld bc,$4000
			ld ix,$C000
			call SCR2PRT_

			ld hl,IMG512_C3+2
			ld de,$C000
			call DELPZ
			ld a,3
			call SET_IOCPU
			ld bc,$4000
			ld ix,$C000
			call SCR2PRT_

			xor a
			call CLRATTR_L
			xor a
			call SET_IOCPU
			ld hl,IMG512_C0+2
			ld de,$4000
			call DELPZ

			mvideomode 5

_endmain:
			jp _endtests

CHKROM0PROC:
			nop
			ld bc,$7FFD
			ld a,$40
			out (c),a

			ld bc,CHKROMEND-CHKROM0PROC
			ld hl,$C000
			ld ix,0

_romcyc:
			ld a,(ix)
			ld e,a
			ld a,(hl)
			cp e
			jr nz,_rombad
			inc hl
			inc ix
			dec bc
			ld a,b
			or c
			jr nz,_romcyc

			xor a
			ld bc,$7FFD
			out (c),a
			jp _rmtstok

_rombad:
			xor a
			ld bc,$7FFD
			out (c),a
			jp _rmtstbad
CHKROMEND:
            nop


CPUTSTPROC:
			di
			ld sp,STACKSTART
			xor a
            ld (CPUTESTRSLT),a ; write flag "working" in result byte
			ld bc,ZX_POLY ; analyze the index of the processor
			in a,(c)
			ld e,a ; we have the 3D00  value for the CPU in E now
			and $3 ; we have the cpu number in A

			; check offset RAM and the number of the CPU
			ld d,e
			srl d
			srl d
			srl d
			srl d
			srl d ; now in D the RAM page for the CPU

			rlca
			cp d
			jr nz,_cp200Bad

_cp200Ok:
			ld a,CPUTESTOK
			ld (CPUTESTRSLT),a
			halt

_cp200Bad:
			ld a,CPUTESTBAD
			ld (CPUTESTRSLT),a
			halt
ENDTSTCPUP:
			nop


; To send the test procedure to current selected IO CPU
SENDPROC_:
			ld hl,ENDTSTCPUP-CPUTSTPROC
			ld bc,CPUTESTRSLT
			ld de,CPUTSTPROC
_snd6732:
			ld a,(de)
			out (c),a
			inc de
			inc bc
			dec hl
			ld a,l
			or h
			jr nz,_snd6732

			ret

; Wait pressing of any key
WAITKEY_:
			xor a
			in a,($FE)
			cpl
			and $1F

			jr nz,_endwaitkey
			jr WAITKEY_
_endwaitkey:
			ret

; This function packs a 16 bits address in 8 bits
; The address must be placed in HL, result in A
PACKADDR_:
            xor a
			bit 1,l
			jr z,_pckad0
			set 0,a
_pckad0:
            bit 2,l
			jr z,_pckad1
			set 1,a
_pckad1:
            bit 0,h
			jr z,_pckad2
			set 2,a
_pckad2:
            bit 4,h
			jr z,_pckad3
			set 3,a
_pckad3:
            bit 6,h
			jr z,_pckad4
			set 4,a
_pckad4:
            bit 7,h
			jr z,_pckad5
			set 5,a
_pckad5:
            ret

_endtests:
            nop

loop:
	       jr loop


; AUX. FUNCTIONS

;This function sets the ZX128 port and save its copy in D'
; the value at A
; this function changes BC'

SET_ZX128:
;----------------------------------
			exx
			ld d,a
			ld bc,ZX_128
			out (c),d
			exx
			ret



;This function sets the VIDEO page at ZX128
; the page index at A (0,1)
; this function changes BC'

SET_ZX128VIDEO:
;----------------------------------
			exx

			res 3,d

			sla a
			sla a

			or d
			ld d,a

			ld bc,ZX_128
			out (c),d

			exx
			ret

;This function sets the ZX-Poly port and save its copy in E'
; the code to save at A
; this function changes BC'

SET_ZXPOLY:
;----------------------------------
			exx
			ld e,a
			ld bc,ZX_POLY
			out (c),e
			exx
			ret


; This function sets a video mode for the tested system
; the code of new mode at A
; this function changes BC'

SET_VIDEO:
;----------------------------------
			exx
			res 2,e
			res 3,e
			res 4,e

			sla a
			sla a

			or e
			ld e,a

			ld bc,ZX_POLY
			out (c),e

			exx
			ret

; This function sets a CPU module for IO area for CPU0
; the index of CPU module at A (0-3)
; this function changes BC'

SET_IOCPU:
;----------------------------------
			exx
			res 5,e
			res 6,e

			sla a
			sla a
			sla a
			sla a
			sla a

			or e
			ld e,a

			ld bc,ZX_POLY
			out (c),e

			exx
			ret

; Clear a screen area at 1th CPU RAM page
; Fill the screen area with A value
; change HL', L', A

CLRSCR_L:
;-----------------------------------
			ld l,a
			ld bc,VIDEOLEN
			ld de,VIDEORAM_L

_cscrl:
			ld a,l
			ld (de),a

			dec bc
			inc de

			xor a
			ld a,c
			or b
			jr nz,_cscrl

			ret

; Clear an attribute area at 1th CPU RAM page
; Fill the screen area with A value
; change DE, L, A

CLRATTR_L:
;-----------------------------------
			ld l,a
			ld bc,ATTRLEN
			ld de,VIDEORAM_L+VIDEOLEN

_catrl:
			ld a,l
			ld (de),a

			dec bc
			inc de

			xor a
			ld a,c
			or b
			jr nz,_cscrl

			ret


; Clear a screen area at 3th CPU RAM page
; Fill the screen area with A value
; change DE, L, A

CLRSCR_H:
;-----------------------------------
			ld l,a
			ld bc,VIDEOLEN
			ld de,VIDEORAM_H

_cscrh:
			ld a,l
			ld (de),a

			dec bc
			inc de

			xor a
			ld a,c
			or b
			jr nz,_cscrh

			ret

; Clear an attribute area at 3th CPU RAM page
; Fill the screen area with A value
; change DE, L, A

CLRATTR_H:
;-----------------------------------
			ld l,a
			ld bc,ATTRLEN
			ld de,VIDEORAM_H+VIDEOLEN

_catrh:
			ld a,l
			ld (de),a

			dec bc
			inc de

			xor a
			ld a,c
			or b
			jr nz,_cscrl
			ret

; Print a string with counter as the first byte. The string will be typed in the current out coordinates with current attribute
; HL contains the address of the string

_PRINTSTR:
;-------------------------------------
			ld a,(hl) ; read the counter
			inc hl

_stoutp:
			or a
  			jr z,_endoutp

  			ld e,a
  			ld a,(hl)

			call _PRINTCHAR

  			ld a,(CHAR_X)
  			inc a
  			ld (CHAR_X),a

  			inc hl
  			ld a,e
  			dec a

  			jr _stoutp

_endoutp:
			ret




; Print a symbol on the screen from A
_PRINTCHAR:
;-------------------------------------
            exx

            sub 32

            ld h,0 ; calculate offset of the char
            ld l,a
            sla l
            rl h
            sla l
            rl h
            sla l
            rl h
			ld bc,FONT
			add hl,bc	; we have the char data offset placed in HL at the point

			ld a,(CHAR_Y); calculate screen position
			ld c,a
			ld b,0
		 	sla c
		 	rl b

			ld ix,ATTROFFST
			add ix,bc

            ld c,(ix)
            ld b,(ix+1)
            ld ix,0
            add ix,bc

            ld b,0
			ld a,(CHAR_X)
			ld c,a
			add ix,bc ; We have "pure" screen address at the point

		 	bit 3,d
		 	jr	z,_l1
		 	ld bc,VIDEORAM_H
		 	jr _l2
_l1:
			ld bc,VIDEORAM_L

_l2:
			add ix,bc ; We have full screen address here in IX

			ld b,1 ; output char on the screen
			ld a,8
_l3:
			ld c,a
			ld a,(hl)
			ld (ix),a
			ld a,c
			ld c,0
			add ix,bc
			inc hl
			dec a
			jr nz,_l3

			LD A,(CHAR_Y) ; Draw atribute
			ld h,0
            ld l,a
            sla l
            rl h
            sla l
            rl h
            sla l
            rl h
            sla l
            rl h
            sla l
            rl h
			ld a,(CHAR_X)
            or l
            ld l,a

            bit 3,d
            jr  z,_l4
		 	ld bc,VIDEORAM_H+VIDEOLEN
            jr _l5
_l4:
		 	ld bc,VIDEORAM_L+VIDEOLEN
_l5:
            add hl,bc
			ld a,(CHAR_ATTR)
            ld (hl),a

            exx
            ret

; PRINT THE TEST NAME
_PRTEST:
			xor a
			ld (CHAR_ATTR),A
			ld (CHAR_X),a

			ld c,l
			ld b,h
			ld hl,STR_EMPTY
			call _PRINTSTR
			ld l,c
			ld h,b

			ld a,7
			ld (CHAR_ATTR),a
			xor a
			ld (CHAR_X),a

			call _PRINTSTR

			ld a,4
			ld (CHAR_ATTR),a

		    ld a,(CHAR_X)

_cyc000:
			cp 29
			jr z,_cyc000end
			ld a,"."
			call _PRINTCHAR
			ld a,(CHAR_X)
			inc a
			ld (CHAR_X),a
			jr _cyc000
_cyc000end:
			ret

; Copy screen data from addr to PORT
; BC - start port index
; IX - start memory index
SCR2PRT_:
			ld hl,$1B00
_cycFGH:
			ld a,(ix)
			out (c),a
            inc ix
			dec hl
			inc bc
			ld a,l
			or h
			jr nz,_cycFGH
			ret

    .include "./lib/xdelpz.asm"

; offset memory address for screen char Y position
ATTROFFST:
	.dw $0000,$0020,$0040,$0060,$0080,$00A0,$00C0,$00E0
	.dw $0800,$0820,$0840,$0860,$0880,$08A0,$08C0,$08E0
	.dw $1000,$1020,$1040,$1060,$1080,$10A0,$10C0,$10E0

STRINGS:
	.include "./strings.asm"

FONT:
	.incbin "./data/font.fnt"

IMG512_C0:
	.incbin "./data/imgc0.lpz"
IMG512_C1:
	.incbin "./data/imgc1.lpz"
IMG512_C2:
	.incbin "./data/imgc2.lpz"
IMG512_C3:
	.incbin "./data/imgc3.lpz"

TSTIMG_R:
	.incbin "./data/imgr.lpz"
TSTIMG_G:
	.incbin "./data/imgg.lpz"
TSTIMG_B:
	.incbin "./data/imgb.lpz"
TSTIMG_Y:
	.incbin "./data/imgy.lpz"
