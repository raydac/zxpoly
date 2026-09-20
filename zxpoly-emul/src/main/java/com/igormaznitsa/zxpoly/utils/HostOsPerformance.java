package com.igormaznitsa.zxpoly.utils;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;
import org.apache.commons.lang3.SystemUtils;

public final class HostOsPerformance {

  private static final Logger LOGGER = Logger.getLogger(HostOsPerformance.class.getName());

  private static final int ABOVE_NORMAL_PRIORITY_CLASS = 0x00008000;
  private static final int PROCESS_POWER_THROTTLING = 4;
  private static final int PROCESS_POWER_THROTTLING_VERSION = 1;
  private static final int PROCESS_POWER_THROTTLING_EXECUTION_SPEED = 0x1;
  private static final int PROCESS_POWER_THROTTLING_IGNORE_TIMER_RESOLUTION = 0x4;

  private HostOsPerformance() {
  }

  public static void apply() {
    try {
      if (SystemUtils.IS_OS_WINDOWS) {
        HostOsPerformance.applyWindows();
      } else if (SystemUtils.IS_OS_MAC) {
        HostOsPerformance.applyMac();
      }
    } catch (final Throwable ex) {
      LOGGER.warning("Can't pin host-OS performance: " + ex.getMessage());
    }
  }

  private static void applyWindows() throws Throwable {
    final Linker linker = Linker.nativeLinker();
    final Arena arena = Arena.global();
    final SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", arena);
    final SymbolLookup winmm = SymbolLookup.libraryLookup("winmm", arena);

    final MethodHandle timeBeginPeriod = linker.downcallHandle(
        winmm.find("timeBeginPeriod")
            .orElseThrow(() -> new IllegalStateException("missing timeBeginPeriod")),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
    timeBeginPeriod.invoke(1);

    final MethodHandle getCurrentProcess = linker.downcallHandle(
        kernel32.find("GetCurrentProcess")
            .orElseThrow(() -> new IllegalStateException("missing GetCurrentProcess")),
        FunctionDescriptor.of(ValueLayout.ADDRESS));
    final MemorySegment process = (MemorySegment) getCurrentProcess.invoke();

    final MethodHandle setPriorityClass = linker.downcallHandle(
        kernel32.find("SetPriorityClass")
            .orElseThrow(() -> new IllegalStateException("missing SetPriorityClass")),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
    setPriorityClass.invoke(process, ABOVE_NORMAL_PRIORITY_CLASS);

    final MethodHandle setProcessInformation = linker.downcallHandle(
        kernel32.find("SetProcessInformation")
            .orElseThrow(() -> new IllegalStateException("missing SetProcessInformation")),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT));

    try (Arena confined = Arena.ofConfined()) {
      final MemorySegment state = confined.allocate(12, 4);
      HostOsPerformance.writePowerThrottlingState(
          state,
          PROCESS_POWER_THROTTLING_EXECUTION_SPEED |
              PROCESS_POWER_THROTTLING_IGNORE_TIMER_RESOLUTION);

      if (!HostOsPerformance.invokeSetProcessInformation(setProcessInformation, process, state)) {
        HostOsPerformance.writePowerThrottlingState(state,
            PROCESS_POWER_THROTTLING_EXECUTION_SPEED);
        HostOsPerformance.invokeSetProcessInformation(setProcessInformation, process, state);
      }
    }
  }

  private static void writePowerThrottlingState(final MemorySegment state, final int controlMask) {
    state.set(ValueLayout.JAVA_INT, 0, PROCESS_POWER_THROTTLING_VERSION);
    state.set(ValueLayout.JAVA_INT, 4, controlMask);
    state.set(ValueLayout.JAVA_INT, 8, 0);
  }

  private static boolean invokeSetProcessInformation(
      final MethodHandle setProcessInformation,
      final MemorySegment process,
      final MemorySegment state) throws Throwable {
    final int ok = (int) setProcessInformation.invoke(process, PROCESS_POWER_THROTTLING, state, 12);
    return ok != 0;
  }

  private static void applyMac() throws Exception {
    new ProcessBuilder(
        "caffeinate",
        "-i",
        "-w",
        Long.toString(ProcessHandle.current().pid()))
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start();
  }
}
