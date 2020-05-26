package com.igormaznitsa.zxpoly.streamer;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class TcpWriter extends AbstractTcpSingleThreadServer {
  public TcpWriter(
      String id,
      int bufferSize,
      InetAddress address,
      int port
  ) {
    super(id, bufferSize, address, port);
  }

  public void write(final byte [] data) {
    this.buffer.put(data);
  }

  @Override
  protected void doBusiness(Socket socket) throws Exception {
    final OutputStream outStream = socket.getOutputStream();
    while (!this.isStopped() && !Thread.currentThread().isInterrupted()) {
      final byte[] next = this.buffer.next();
      if (next != null) {
        outStream.write(next);
        outStream.flush();
      }
    }
  }
}
