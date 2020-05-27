package com.igormaznitsa.zxpoly.streamer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class TcpReader extends AbstractTcpSingleThreadServer {

  private final int maxChunkSize;

  public TcpReader(
      final String id,
      final int maxChunkSize,
      final int bufferSize,
      final InetAddress address,
      final int port
  ) {
    super(id, bufferSize, address, port);
    this.maxChunkSize = maxChunkSize;
  }

  public void write(final byte[] data) {
    this.buffer.put(data);
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
        this.buffer.put(Arrays.copyOfRange(chunk, 0, read));
      }
    }
  }

  public byte[] read() {
    return this.buffer.next();
  }
}
