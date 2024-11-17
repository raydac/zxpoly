package com.igormaznitsa.z80.fuse;

import static com.igormaznitsa.z80.Pair.pairOf;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.igormaznitsa.z80.Pair;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.z80.Z80CPUBus;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.BeforeClass;
import org.junit.Test;

public class Z80FuseTest {

  private static List<Pair<InfoIn, InfoExpected>> testList;

  @BeforeClass
  public static void setUp() throws Exception {
    final Map<String, InfoIn> mapInfoIn = readInTests();
    final Map<String, InfoExpected> mapInfoExpected = readExpectedTests();
    assertEquals(mapInfoIn.size(), mapInfoExpected.size());

    final List<Pair<InfoIn, InfoExpected>> tests = new ArrayList<>();

    mapInfoIn.keySet().stream().sorted().forEach(key -> {
      final InfoIn in = mapInfoIn.get(key);
      final InfoExpected expected = mapInfoExpected.get(key);
      assertNotNull("Key: " + key, in);
      assertNotNull("Key: " + key, expected);
      tests.add(pairOf(in, expected));
    });

    testList = unmodifiableList(tests);
  }

  private static Map<String, InfoIn> readInTests() throws IOException {
    final Map<String, InfoIn> result = new HashMap<>();

    try (final BufferedReader reader = new BufferedReader(
            new InputStreamReader(requireNonNull(getSystemResourceAsStream("fuse-test/tests.in")),
                    UTF_8))) {

      try {
        while (!Thread.currentThread().isInterrupted()) {
          final InfoIn next = new InfoIn(reader);
          result.put(next.name, next);
        }
      } catch (EOFException ex) {
        // DO NOTHING
      }

    }

    return result;
  }

  private static Map<String, InfoExpected> readExpectedTests() throws IOException {
    final Map<String, InfoExpected> result = new HashMap<>();

    try (final BufferedReader reader = new BufferedReader(
            new InputStreamReader(requireNonNull(getSystemResourceAsStream("fuse-test/tests.expected")),
                    UTF_8))) {

      try {
        while (!Thread.currentThread().isInterrupted()) {
          final InfoExpected next = new InfoExpected(reader);
          result.put(next.name, next);
        }
      } catch (EOFException ex) {
        // DO NOTHING
      }
    }

    return result;
  }

  @Test
  public void doAllTests() {
    final AtomicInteger counterOk = new AtomicInteger(0);

    final List<Pair<InfoIn, InfoExpected>> failedTests = new ArrayList<>();

    testList
            // .filter(x -> x.getLeft().name.equals("eda3"))
            .forEach(test -> {
              printTestHeader(test);
              final boolean ok = this.doTest(test);
              if (ok) {
                System.out.println("OK");
                counterOk.incrementAndGet();
              } else {
                failedTests.add(test);
                System.out.println("FAIL");
              }
            });

    System.out.println("-----------------------------------------------------");
    System.out.printf("Total %d tests, passed %d tests, failed %d tests%n",
            (counterOk.get() + failedTests.size()),
            counterOk.get(), failedTests.size());

    if (!failedTests.isEmpty()) {

      System.err.println("Failed tests");
      System.err.println("----------------------");
      failedTests.forEach(x -> {
        System.err.println("Name: " + x.getLeft().name);
        System.err.println(x.getLeft().makeDescription());
        System.err.println();
      });

      fail();
    }
  }

  private void printTestHeader(final Pair<InfoIn, InfoExpected> test) {
    final StringBuilder buffer = new StringBuilder();
    buffer.append(test.getLeft().name);
    final int dots = 64 - test.getLeft().name.length();
    buffer.append(".".repeat(Math.max(0, dots)));
    System.out.print(buffer);
  }

  private int executeForTstates(final Z80 cpu, int tstates) {
    while (tstates > 0) {
      tstates -= cpu.nextInstruction(0, false, false, false);
    }
    return tstates;
  }

  private boolean doTest(final Pair<InfoIn, InfoExpected> test) {
    final byte[] areaRam = new byte[0xFFFF];
    final byte[] areaIoRd = new byte[0xFFFF];
    final byte[] areaIoWr = new byte[0xFFFF];

    test.getRight().actions.stream()
            .filter(x -> x.type == InfoExpected.ActionType.PR)
            .forEach(x -> areaIoRd[x.address] = (byte) x.data);

    final Z80 cpu = new Z80(new Z80CPUBus() {
      @Override
      public byte readMemory(Z80 cpu, int ctx, int address, boolean m1, boolean cmdOrPrefix) {
        return areaRam[address];
      }

      @Override
      public void writeMemory(Z80 cpu, int ctx, int address, byte data) {
        areaRam[address] = data;
      }

      @Override
      public int readPtr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
      }

      @Override
      public int readSpecRegValue(Z80 cpu, int ctx, int reg, int origValue) {
        return origValue;
      }

      @Override
      public int readSpecRegPairValue(Z80 cpu, int ctx, int regPair, int origValue) {
        return origValue;
      }

      @Override
      public int readRegPortAddr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
      }

      @Override
      public byte readPort(Z80 cpu, int ctx, int port) {
        return areaIoRd[port];
      }

      @Override
      public void writePort(Z80 cpu, int ctx, int port, byte data) {
        areaIoWr[port] = data;
      }

      @Override
      public byte onCPURequestDataLines(Z80 cpu, int ctx) {
        return 0;
      }

      @Override
      public void onRETI(Z80 cpu, int ctx) {

      }

      @Override
      public void onInterrupt(Z80 cpu, int ctx, boolean nmi) {

      }

      @Override
      public int postProcessXor(Z80 cpu, int ctx, int regIndex, int valueA, int value, int result) {
        return result;
      }

      @Override
      public int postProcessAnd(Z80 cpu, int ctx, int regIndex, int valueA, int value, int result) {
        return result;
      }

      @Override
      public int postProcessOr(Z80 cpu, int ctx, int regIndex, int valueA, int value, int result) {
        return result;
      }
    });
    cpu.doReset();

    fillCpu(cpu, test.getLeft());

    final AtomicBoolean ok = new AtomicBoolean(true);

    final int resultTstates = executeForTstates(cpu, test.getRight().tstates);
    if (resultTstates < 0) {
      System.out.printf("%nDetected negative CPU tstates for %s: %d%n", test.getLeft().name, resultTstates);
      ok.set(false);
    }

    test.getRight().actions.stream()
            .filter(x -> x.type == InfoExpected.ActionType.PW)
            .forEach(x -> {
              if (x.data != (areaIoWr[x.address] & 0xFF)) {
                System.out.printf("%nDetected non-correct value 0x%X in port 0x%X, expected 0x%X%n",
                        (areaIoWr[x.address] & 0xFF), x.address, x.data);
                ok.set(false);
              }
            });

    test.getRight().actions.stream()
            .filter(x -> x.type == InfoExpected.ActionType.MW)
            .forEach(x -> {
              if (x.data != (areaRam[x.address] & 0xFF)) {
                System.out.printf("%nDetected non-correct value 0x%X in RAM 0x%X, expected 0x%X%n",
                        (areaRam[x.address] & 0xFF), x.address, x.data);
                ok.set(false);
              }
            });

    if (!checkCpuState(cpu, test.getRight(), true, true)) {
      ok.set(false);
    }

    return ok.get();
  }

  private boolean checkCpuState(
          final Z80 cpu,
          final InfoExpected expected,
          final boolean checkR,
          final boolean logError
  ) {
    boolean result = true;

    final int afMask = 0xFFFF;

    if (cpu.getRegister(Z80.REG_PC) != expected.pc) {
      if (logError) {
        System.out
                .printf("%nPC expected 0x%X <> 0x%X%n", expected.pc, cpu.getRegister(Z80.REG_PC));
      }
      result = false;
    }
    if (cpu.getRegister(Z80.REG_SP) != expected.sp) {
      if (logError) {
        System.out
                .printf("%nSP expected 0x%X <> 0x%X%n", expected.sp, cpu.getRegister(Z80.REG_SP));
      }
      result = false;
    }
    if (cpu.getIM() != expected.im) {
      if (logError) {
        System.out.printf("%nIM expected 0x%X <> 0x%X%n", expected.im, cpu.getIM());
      }
      result = false;
    }
    if (cpu.getRegister(Z80.REG_I) != expected.i) {
      if (logError) {
        System.out
                .printf("%nI expected 0x%X <> 0x%X%n", expected.i, cpu.getRegister(Z80.REG_I));
      }
      result = false;
    }
    if (checkR && cpu.getRegister(Z80.REG_R) != expected.r) {
      if (logError) {
        System.out
                .printf("%nR expected 0x%X <> 0x%X%n", expected.r, cpu.getRegister(Z80.REG_R));
      }
      result = false;
    }
    if (cpu.getRegister(Z80.REG_IX) != expected.ix) {
      if (logError) {
        System.out
                .printf("%nIX expected 0x%X <> 0x%X%n", expected.ix, cpu.getRegister(Z80.REG_IX));
      }
      result = false;
    }
    if (cpu.getRegister(Z80.REG_IY) != expected.iy) {
      if (logError) {
        System.out
                .printf("%nIY expected 0x%X <> 0x%X%n", expected.ix, cpu.getRegister(Z80.REG_IY));
      }
      result = false;
    }
    if ((cpu.getRegisterPair(Z80.REGPAIR_AF, false) & afMask) != (expected.af & afMask)) {
      if (logError) {
        final int expectedF = (expected.af & afMask) & 0xFF;
        final int foundF = (cpu.getRegisterPair(Z80.REGPAIR_AF, false) & afMask) & 0xFF;
        final int xorF = expectedF ^ foundF;
        String badFlagsInF = "ok";
        if (xorF != 0) {
          badFlagsInF = String.valueOf((xorF & 128) == 0 ? '_' : 'S') +
                  ((xorF & 64) == 0 ? '_' : 'Z') +
                  ((xorF & 32) == 0 ? '_' : 'Y') +
                  ((xorF & 16) == 0 ? '_' : 'H') +
                  ((xorF & 8) == 0 ? '_' : 'X') +
                  ((xorF & 4) == 0 ? '_' : 'P') +
                  ((xorF & 2) == 0 ? '_' : 'N') +
                  ((xorF & 1) == 0 ? '_' : 'C');
        }

        System.out.printf("%nAF expected 0x%X <> 0x%X, F is %s%n", (expected.af & afMask),
                (cpu.getRegisterPair(Z80.REGPAIR_AF, false) & afMask), badFlagsInF);
      }
      result = false;
    }
    if (cpu.getRegisterPair(Z80.REGPAIR_BC, false) != expected.bc) {
      if (logError) {
        System.out.printf("%nBC expected 0x%X <> 0x%X%n", expected.bc,
                cpu.getRegisterPair(Z80.REGPAIR_BC, false));
      }
      result = false;
    }
    if (cpu.getRegisterPair(Z80.REGPAIR_DE, false) != expected.de) {
      if (logError) {
        System.out.printf("%nDE expected 0x%X <> 0x%X%n", expected.de,
                cpu.getRegisterPair(Z80.REGPAIR_DE, false));
      }
      result = false;
    }
    if (cpu.getRegisterPair(Z80.REGPAIR_HL, false) != expected.hl) {
      if (logError) {
        System.out.printf("%nHL expected 0x%X <> 0x%X%n", expected.hl,
                cpu.getRegisterPair(Z80.REGPAIR_HL, false));
      }
      result = false;
    }
    if ((cpu.getRegisterPair(Z80.REGPAIR_AF, true) & afMask) != (expected.altAf & afMask)) {
      if (logError) {
        System.out.printf("%nAF' expected 0x%X <> 0x%X%n", (expected.altAf & afMask),
                (cpu.getRegisterPair(Z80.REGPAIR_AF, true) & afMask));
      }
      result = false;
    }
    if (cpu.getRegisterPair(Z80.REGPAIR_BC, true) != expected.altBc) {
      if (logError) {
        System.out.printf("%nBC' expected 0x%X <> 0x%X%n", expected.altBc,
                cpu.getRegisterPair(Z80.REGPAIR_BC, true));
      }
      result = false;
    }
    if (cpu.getRegisterPair(Z80.REGPAIR_DE, true) != expected.altDe) {
      if (logError) {
        System.out.printf("%nDE' expected 0x%X <> 0x%X%n", expected.altDe,
                cpu.getRegisterPair(Z80.REGPAIR_DE, true));
      }
      result = false;
    }
    if (cpu.getRegisterPair(Z80.REGPAIR_HL, true) != expected.altHl) {
      if (logError) {
        System.out.printf("%nHL' expected 0x%X <> 0x%X%n", expected.altHl,
                cpu.getRegisterPair(Z80.REGPAIR_HL, true));
      }
      result = false;
    }
    if (cpu.getMemPtr() != expected.memptr) {
      if (logError) {
        System.out.printf("%nMEMPTR expected 0x%X <> 0x%X%n", expected.memptr, cpu.getMemPtr());
      }
      result = false;
    }

    return result;
  }

  private void fillCpu(final Z80 cpu, final InfoIn in) {
    cpu.setIFF(in.iff1, in.iff2);
    cpu.setRegisterPair(Z80.REGPAIR_AF, in.af, false);
    cpu.setRegisterPair(Z80.REGPAIR_BC, in.bc, false);
    cpu.setRegisterPair(Z80.REGPAIR_DE, in.de, false);
    cpu.setRegisterPair(Z80.REGPAIR_HL, in.hl, false);
    cpu.setRegisterPair(Z80.REGPAIR_AF, in.altAf, true);
    cpu.setRegisterPair(Z80.REGPAIR_BC, in.altBc, true);
    cpu.setRegisterPair(Z80.REGPAIR_DE, in.altDe, true);
    cpu.setRegisterPair(Z80.REGPAIR_HL, in.altHl, true);

    cpu.setRegister(Z80.REG_IX, in.ix);
    cpu.setRegister(Z80.REG_IY, in.iy);

    cpu.setRegister(Z80.REG_R, in.r);
    cpu.setRegister(Z80.REG_I, in.i);
    cpu.setIM(in.im);

    cpu.setRegister(Z80.REG_PC, in.pc);
    cpu.setRegister(Z80.REG_SP, in.sp);

    cpu.setMemPtr(in.memPtr);

    for (final int[] line : in.lines) {
      int pc = line[0];
      for (int i = 1; i < line.length; i++) {
        final int cellData = line[i];
        if ((cellData & ~0xFF) != 0) {
          fail(format("Unexpected RAM data in %s: %d", in.name, cellData));
        }
        cpu.getBus().writeMemory(null, 0, pc, (byte) cellData);
        pc++;
      }
    }

  }

}
