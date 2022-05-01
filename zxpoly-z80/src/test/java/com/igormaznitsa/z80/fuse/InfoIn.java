package com.igormaznitsa.z80.fuse;

import com.igormaznitsa.z80.MemoryAccessProvider;
import com.igormaznitsa.z80.Z80Instruction;
import com.igormaznitsa.z80.disasm.Z80Disasm;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.*;

import static java.lang.Integer.parseInt;
import static org.junit.Assert.assertEquals;

final class InfoIn {
  final String name;
  final int af;
  final int bc;
  final int de;
  final int hl;
  final int altAf;
  final int altBc;
  final int altDe;
  final int altHl;
  final int ix;
  final int iy;
  final int sp;
  final int pc;
  final int memPtr;
  final int i;
  final int im;
  final int r;
  final boolean iff1;
  final boolean iff2;
  final boolean halted;
  final int tStates;

  final List<int[]> lines;

  InfoIn(final BufferedReader reader) throws IOException {
    String line;
    do {
      line = reader.readLine();
      if (line == null) {
        throw new EOFException("End of file");
      }
    } while (line.trim().isEmpty());

    this.name = line.trim();

    line = reader.readLine();
    if (line == null) {
      throw new IOException("Can't find register line");
    }
    final int[] registers = Arrays.stream(line.split("\\s+"))
            .mapToInt(x -> Integer.parseUnsignedInt(x.trim(), 16))
            .toArray();
    assertEquals(13, registers.length);

    this.af = registers[0];
    this.bc = registers[1];
    this.de = registers[2];
    this.hl = registers[3];
    this.altAf = registers[4];
    this.altBc = registers[5];
    this.altDe = registers[6];
    this.altHl = registers[7];
    this.ix = registers[8];
    this.iy = registers[9];
    this.sp = registers[10];
    this.pc = registers[11];
    this.memPtr = registers[12];

    line = reader.readLine();
    if (line == null) {
      throw new IOException("Can't find special register line");
    }

    final String[] splitted = line.split("\\s+");
    assertEquals(line + " : " + Arrays.toString(splitted), 7, splitted.length);

    this.i = parseInt(splitted[0].trim(), 16);
    this.r = parseInt(splitted[1].trim(), 16);
    this.iff1 = !"0".equals((splitted[2].trim()));
    this.iff2 = !"0".equals((splitted[3].trim()));
    this.im = parseInt(splitted[4].trim());
    this.halted = !"0".equals((splitted[5].trim()));
    this.tStates = parseInt(splitted[6].trim());

    final List<int[]> lines = new ArrayList<>();
    while (!Thread.currentThread().isInterrupted()) {
      line = reader.readLine();
      if (line == null) {
        throw new EOFException("Unexpected end of file");
      }
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }
      if ("-1".equals(line)) {
        break;
      }

      final int[] parsed = Arrays.stream(line.split("\\s"))
              .map(String::trim)
              .filter(x -> !x.equals("-1"))
              .mapToInt(x -> parseInt(x, 16))
              .toArray();

      lines.add(parsed);
    }

    this.lines = Collections.unmodifiableList(lines);
  }

  private static String toAddr(final int addr) {
    String result = Integer.toHexString(addr).toUpperCase(Locale.ENGLISH);
    return '#' + "0000".substring(result.length()) + result;
  }

  public String makeDescription() {
    final StringBuilder builder = new StringBuilder();

    builder
            .append("I=").append(this.i)
            .append(",R=").append(this.r)
            .append(",IFF1=").append(this.iff1 ? "1" : "0")
            .append(",IFF2=").append(this.iff2 ? "1" : "0")
            .append(",IM=").append(this.im)
            .append(",HALT=").append(this.halted ? "1" : "0")
            .append(",MEMPTR=").append(this.memPtr)
            .append(",Tstat.=").append(this.tStates);

    final byte[] ram = new byte[0x10000];
    final MemoryAccessProvider memoryAccessProvider = address -> ram[address];

    this.lines.forEach(line -> {
      Arrays.fill(ram, (byte) 0);
      int pc = line[0];
      for (int i = 1; i < line.length; i++) {
        ram[pc++] = (byte) line[i];
      }
      int disasmPc = line[0];
      while (disasmPc < pc) {
        final Z80Instruction instruction = Z80Disasm.decodeInstruction(memoryAccessProvider, disasmPc);
        builder.append('\n')
                .append(toAddr(disasmPc))
                .append(" ")
                .append(instruction.decode(memoryAccessProvider, disasmPc, disasmPc));
        disasmPc += instruction.getLength();
      }
    });

    return builder.toString();
  }

}
