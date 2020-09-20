package com.igormaznitsa.z80.fuse;

import static org.junit.Assert.assertEquals;


import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class InfoExpected {
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
  final List<ExpectedAction> actions;
  final List<int[]> lines;

  InfoExpected(final BufferedReader reader) throws IOException {
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

    List<ExpectedAction> actions = new ArrayList<>();

    while (true) {
      line = reader.readLine();
      if (line == null) {
        throw new IOException("Unexpected end of file");
      }
      if (line.trim().isEmpty()) {
        continue;
      }

      if (line.split("\\s+").length > 5) {
        break;
      }

      actions.add(new ExpectedAction(line));
    }
    this.actions = Collections.unmodifiableList(actions);

    final int[] registers = Arrays.stream(line.split("\\s+"))
        .filter(x -> !x.trim().isEmpty())
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
      if (line.isEmpty() || "-1".equals(line)) {
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

  enum ActionType {
    MR, MW, MC, PR, PW, PC
  }

  static final class ExpectedAction {
    final int time;
    final ActionType type;
    final int address;
    final int data;

    ExpectedAction(final String line) {
      final String[] parsed = line.trim().split("\\s+");
      this.time = Integer.parseInt(parsed[0].trim());
      this.type = ActionType.valueOf(parsed[1].trim().toUpperCase(Locale.ENGLISH));
      this.address = Integer.parseInt(parsed[2].trim(), 16);
      if (this.type == ActionType.PC || this.type == ActionType.MC) {
        this.data = -1;
      } else {
        this.data = Integer.parseInt(parsed[3].trim(), 16);
      }
    }
  }

}
