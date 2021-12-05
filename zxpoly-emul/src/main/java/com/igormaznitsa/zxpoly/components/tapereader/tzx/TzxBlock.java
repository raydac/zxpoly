package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

import java.io.IOException;
import java.util.Locale;

public enum TzxBlock {
  STANDARD_SPEED_DATA_BLOCK(0x10, TzxBlockStandardSpeedData::new),
  TURBO_SPEED_DATA_BLOCK(0x11, TzxBlockTurboSpeedData::new),
  PURE_TONE(0x12, TzxBlockPureTone::new),
  VAR_SEQUENCE_PULSES(0x13, TzxBlockVarSequencePulses::new),
  PURE_DATA_BLOCK(0x14, TzxBlockPureData::new),
  DIRECT_RECORDING_BLOCK(0x15, TzxBlockDirectRecording::new),
  CSW_RECORDING_BLOCK(0x18, TzxBlockCSWRecording::new),
  GENERALIZED_DATA_BLOCK(0x19, TzxBlockGeneralizedData::new),
  PAUSE_OR_STOP(0x20, TzxBlockPauseOrStop::new),
  GROUP_START(0x21, TzxBlockGroupStart::new),
  GROUP_END(0x22, TzxBlockGroupEnd::new),
  JUMP_TO_BLOCK(0x23, TzxBlockJumpTo::new),
  LOOP_START(0x24, TzxBlockLoopStart::new),
  LOOP_END(0x25, TzxBlockLoopEnd::new),
  CALL_SEQUENCE(0x26, TzxBlockCallSequence::new),
  RETURN_FROM_SEQUENCE(0x27, TzxBlockSequenceReturn::new),
  SELECT_BLOCK(0x28, TzxBlockSelect::new),
  STOP_TAPE_IF_48K(0x2A, TzxBlockStopTapeIf48k::new),
  SET_SIGNAL_LEVEL(0x2B, TzxBlockSetSignalLevel::new),
  TEXT_DESCRIPTION(0x30, TzxBlockTextDescription::new),
  MESSAGE_BLOCK(0x31, TzxBlockMessage::new),
  ARCHIVE_INFO(0x32, TzxBlockArchiveInfo::new),
  HARDWARE_TYPE(0x33, TzxBlockHardwareType::new),
  EMULATION_INFO(0x34, TzxBlockEmulationinfo::new),
  CUSTOM_INFO(0x35, TzxBlockCustomInfo::new),
  SNAPSHOT(0x40, TzxBlockSnapshot::new),
  KANSAS_CITY_STANDARD(0x4B, TzxBlockKansasCityStandard::new),
  C64ROM(0x16, TzxBlockC64Rom::new),
  C64TURBO(0x17, TzxBlockC64Turbo::new),
  GLUE(0x5A, TzxBlockGlue::new),
  UNKNOWN(-1, in -> {
    throw new IOException("UNKNOWN BLOCK");
  });

  private final int id;
  private final TzxReadFunction readFunction;

  TzxBlock(final int id, final TzxReadFunction readFunction) {
    this.id = id;
    this.readFunction = readFunction;
  }

  public static AbstractTzxBlock readNextBlock(final JBBPBitInputStream inputStream) throws IOException {
    final int id = inputStream.readByte();
    TzxBlock block = UNKNOWN;
    for (final TzxBlock b : values()) {
      if (b.id == id) {
        block = b;
        break;
      }
    }
    if (block == UNKNOWN) {
      throw new IOException("Unsupported block id: 0x" + Integer.toHexString(id).toUpperCase(Locale.ENGLISH));
    }
    return block.getReadFunction().read(inputStream);
  }

  public int getId() {
    return this.id;
  }

  public TzxReadFunction getReadFunction() {
    return this.readFunction;
  }

  @FunctionalInterface
  public interface TzxReadFunction {
    AbstractTzxBlock read(JBBPBitInputStream inputStream) throws IOException;
  }
}
