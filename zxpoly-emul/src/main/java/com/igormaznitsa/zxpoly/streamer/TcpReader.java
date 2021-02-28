package com.igormaznitsa.zxpoly.streamer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class TcpReader extends AbstractTcpSingleThreadServer {

  private final int maxChunkSize;
  private final TcpReaderDataProcessor[] processors;

  public TcpReader(
          final String id,
          final int maxChunkSize,
          final int bufferSize,
          final InetAddress address,
          final int port,
          final TcpReaderDataProcessor... processors
  ) {
    super(id, bufferSize, address, port);
    this.processors = processors.clone();
    this.maxChunkSize = maxChunkSize;
  }

  public void write(final byte[] data) {
    this.buffer.offer(data);
  }

  @Override
  protected void doBusiness(Socket socket) throws Exception {
    final byte[] chunk = new byte[this.maxChunkSize];

    final InputStream inputStream = socket.getInputStream();
    while (!this.isStopped() && !Thread.currentThread().isInterrupted()) {
      final int available = Math.max(256, Math.min(this.maxChunkSize, inputStream.available()));
      final int read = inputStream.read(chunk, 0, available);
      if (read < 0) {
        throw new IOException("input stream is closed");
      } else if (read > 0) {
        final byte[] data = Arrays.copyOfRange(chunk, 0, read);
        boolean placeIntoBuffer = true;
        for (final TcpReaderDataProcessor l : this.processors) {
          placeIntoBuffer &= l.onIncomingData(this, data);
        }
        if (placeIntoBuffer) {
          this.buffer.put(data);
        }
      }
    }
  }

  public byte[] read() {
    return this.buffer.poll();
  }

  public interface TcpReaderDataProcessor {
    /**
     * @param source source tcp reader, must not be null
     * @param data   incoming data chunk, must not be null
     * @return true if place the data into buffer, false otherwise
     */
    boolean onIncomingData(TcpReader source, byte[] data);
  }
}
