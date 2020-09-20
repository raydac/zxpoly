package com.igormaznitsa.z80.fuse;

import static org.junit.Assert.assertEquals;


import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
  final int memptr;
  final int i;
  final int im;
  final int r;
  final boolean iff1;
  final boolean iff2;
  final boolean halted;
  final int tstates;

  final List<int[]> lines;

  InfoIn(final BufferedReader reader) throws IOException {
    String line;
    while (true) {
      line = reader.readLine();
      if (line == null) {
        throw new EOFException("End of file");
      }
      if (!line.trim().isEmpty()) {
        break;
      }
    }

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
    this.memptr = registers[12];

    line = reader.readLine();
    if (line == null) {
      throw new IOException("Can't find special register line");
    }

    final String[] splitted = line.split("\\s+");
    assertEquals(line + " : " + Arrays.toString(splitted), 7, splitted.length);

    this.i = Integer.parseInt(splitted[0].trim(), 16);
    this.r = Integer.parseInt(splitted[1].trim(), 16);
    this.iff1 = !"0".equals((splitted[2].trim()));
    this.iff2 = !"0".equals((splitted[3].trim()));
    this.im = Integer.parseInt(splitted[4].trim());
    this.halted = !"0".equals((splitted[5].trim()));
    this.tstates = Integer.parseInt(splitted[6].trim());

    final List<int[]> lines = new ArrayList<>();
    while (!Thread.currentThread().isInterrupted()) {
      line = reader.readLine();
      if (line == null) {
        throw new IOException("Unexpected end of file");
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
          .mapToInt(x -> Integer.parseInt(x, 16))
          .toArray();

      lines.add(parsed);
    }

    this.lines = Collections.unmodifiableList(lines);
  }

}
