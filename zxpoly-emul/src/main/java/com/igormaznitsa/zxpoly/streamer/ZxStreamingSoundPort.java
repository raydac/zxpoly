package com.igormaznitsa.zxpoly.streamer;

import static com.igormaznitsa.zxpoly.components.sound.Beeper.AUDIO_FORMAT;

import com.igormaznitsa.zxpoly.components.sound.SourceSoundPort;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public final class ZxStreamingSoundPort extends SourceSoundPort implements SourceDataLine {

  private static final int SAMPLES_PER_INT =
          (Math.round(AUDIO_FORMAT.getSampleRate()) * 2 * 2) / 50;
  private final AudioFormat audioFormat;
  private final TcpWriter soundWriter;

  public ZxStreamingSoundPort(final TcpWriter soundWriter) {
    super(null, "zx-snd-grabber-port", null);
    this.audioFormat = AUDIO_FORMAT;
    this.soundWriter = soundWriter;
  }

  @Override
  public boolean doesSupport(final AudioFormat format) {
    return this.audioFormat.matches(format);
  }

  @Override
  public SourceDataLine asSourceDataLine() {
    return this;
  }

  @Override
  public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
    this.open(format);
  }

  @Override
  public void open(AudioFormat format) throws LineUnavailableException {
    if (!this.audioFormat.matches(format)) {
      throw new LineUnavailableException("Line doesn't support audio format");
    }
  }

  @Override
  public int write(final byte[] b, final int off, final int len) {
    final byte[] copy = Arrays.copyOfRange(b, off, off + len);
    this.soundWriter.write(copy);
    return len;
  }

  @Override
  public void drain() {

  }

  @Override
  public void flush() {

  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }

  @Override
  public boolean isRunning() {
    return true;
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public AudioFormat getFormat() {
    return this.audioFormat;
  }

  @Override
  public int getBufferSize() {
    return 0;
  }

  @Override
  public int available() {
    return SAMPLES_PER_INT;
  }

  @Override
  public int getFramePosition() {
    return 0;
  }

  @Override
  public long getLongFramePosition() {
    return 0;
  }

  @Override
  public long getMicrosecondPosition() {
    return 0;
  }

  @Override
  public float getLevel() {
    return 0;
  }

  @Override
  public Line.Info getLineInfo() {
    return new Line.Info(ZxStreamingSoundPort.class);
  }

  @Override
  public void open() throws LineUnavailableException {
  }

  @Override
  public void close() {
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public Control[] getControls() {
    return new Control[0];
  }

  @Override
  public boolean isControlSupported(Control.Type control) {
    return false;
  }

  @Override
  public Control getControl(Control.Type control) {
    return null;
  }

  @Override
  public void addLineListener(LineListener listener) {

  }

  @Override
  public void removeLineListener(LineListener listener) {

  }
}
