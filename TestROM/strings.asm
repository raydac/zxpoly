STR_HEADER:
			DEFB 21
			DEFM "-= TEST ROM v 1.01 =-"

STR_COPY:
			DEFB 32
			DEFB 127
			DEFM "2007 Raydac Research Group Ltd."

ST_ZXPOLY:
			DEFB 20
			DEFM "Check memory ZX-Poly"

ST_ZX128:
			DEFB 12
			DEFM "Check ZX-128"

ST_RAM0ROM:
			DEFB 15
			DEFM "Check RAM0->ROM"

ST_MEM128:
			DEFB 16
			DEFM "Check memory 128"

ST_CHKCPU0:
			DEFB 10
			DEFM "Check CPU0"

ST_CHKCPU1:
			DEFB 10
			DEFM "Check CPU1"

ST_CHKCPU2:
			DEFB 10
			DEFM "Check CPU2"

ST_CHKCPU3:
			DEFB 10
			DEFM "Check CPU3"

STR_OK:
			DEFB 2
			DEFM "OK"

STR_NONE:
			DEFB 3
			DEFM "NON"

STR_BAD:
			DEFB 3
			DEFM "BAD"

STR_EMPTY:
			DEFB 32
			DEFM "                               "

STR_PANYK:
		 	DEFB 27
			DEFM "PRESS ANY KEY TO VIDEO TEST"
