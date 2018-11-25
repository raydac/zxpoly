;Decompressor for v3.01
;HL-source address DE-target address
DELPZ:
  LD BC,4
  ADD HL,BC
  XOR A
  LD C,A
  LD B,A
xpD0:
  LD A,(HL)
  SRL A
  JR NC,xpD1
  CALL xpSUB ;short copy
  RRA
  RL B
  AND 7
xpM2:
  JR NZ,xpNex
  LD A,(HL)
  INC HL
xpNex:
  LD C,(HL)
  INC HL
  PUSH HL
  LD H,D
  LD L,E
  SBC HL,BC
  LD B,0
  LD C,A
xpM1:
  INC BC
  INC BC
  LDIR
  POP HL
  EX AF,AF
  JR Z,xpD0
  JR NZ,xpDRR
xpD1:
  RRA
  JR C,xpZ1
  RRA
  JR C,xpZ2
  JR Z,xpDEND
  INC HL
xpDRR:
  LD B,A      ;nocompr
xpDL0:
  LD A,(HL)
  INC HL
  XOR (HL)
  LD (DE),A
  INC DE
  DJNZ xpDL0
  JR xpD0
xpZ2:
  SRL A      ;repeat
  JR C,xpZ2L
  LD C,A
  XOR A
  EX AF,AF
xpZ22:
  INC HL
  PUSH HL
  LD H,D
  LD L,E
  DEC HL
  JR xpM1
xpZ2L:
  CALL xpSUB
  RRA
  RL B
  LD C,(HL)
  JR xpZ22
xpZ1:
  SRL A
  JR NC,xpTWO
  LD C,A ;long copy
  INC HL
  LD A,(HL)
  AND $1F
  LD B,A
  LD A,C
  CALL xpSUB
  OR A
  JR xpM2
xpTWO:
  INC A  ;two bytes
  LD C,A
  INC HL
  PUSH HL
  LD H,D
  LD L,E
  SBC HL,BC
  LD C,2
  LDIR
  POP HL
  JR xpD0
xpDEND:
  RET
xpSUB:  EX AF,AF
  LD A,(HL)
  RLCA
  RLCA
  RLCA
  AND 7
  EX AF,AF
  INC HL
  RET
