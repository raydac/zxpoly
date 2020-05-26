package com.igormaznitsa.zxpoly.streamer;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class Tcp2HttpMediaStreamRetranslator {

  private final String mime;
  private final InetAddress tcpReaderAddress;
  private final InetAddress httpServerAddress;
  private final int tcpReaderPort;
  private final int httpServerPort;
  private final AtomicReference<HttpServer> httpServerRef = new AtomicReference<>();
  private final AtomicReference<TcpReader> tcpReaderRef = new AtomicReference<>();
  private volatile boolean stopped;

  public Tcp2HttpMediaStreamRetranslator(
      final String mime,
      final InetAddress addressIn,
      final int portIn,
      final InetAddress addressOut,
      final int portOut
  ) {
    this.mime = mime;
    this.tcpReaderAddress = addressIn;
    this.httpServerAddress = addressOut;
    this.tcpReaderPort = portIn;
    this.httpServerPort = portOut;
  }

  public void start() throws IOException {
    startTcpServer();
    startHttpServer();
  }

  private void startTcpServer() {
    final TcpReader newReader =
        new TcpReader("tcp-reader", 0x10000, 10, InetAddress.getLoopbackAddress(), 0);
    if (this.tcpReaderRef.compareAndSet(null, newReader)) {
      newReader.start();
    }
  }

  private void startHttpServer() throws IOException {
    final HttpServer server =
        HttpServer.create(new InetSocketAddress(this.httpServerAddress, this.httpServerPort), 3);
    if (this.httpServerRef.compareAndSet(null, server)) {
      server.createContext("/ts", this::handleHttpRequest);
      server.setExecutor(Executors.newSingleThreadExecutor());
      server.start();
    }
  }

  public String getHttpAddress() {
    final HttpServer server = this.httpServerRef.get();
    final InetSocketAddress address = server == null ? null : server.getAddress();
    return address == null ? "none" :
        address.getAddress().getHostAddress() + ":" + address.getPort();
  }

  private void handleHttpRequest(final HttpExchange exchange) throws IOException {
    Headers headers = exchange.getResponseHeaders();
    headers.add("Content-Type", this.mime);
    headers.add("Connection", "keep-alive");

    exchange.sendResponseHeaders(200, 0);

    OutputStream os = exchange.getResponseBody();
    try {
      final TcpReader reader = this.tcpReaderRef.get();
      if (reader != null) {
        while (!stopped && !Thread.currentThread().isInterrupted()) {
          final byte[] data = reader.read();
          if (data != null) {
            os.write(data);
            os.flush();
          }
        }
      }

    } finally {
      os.close();
    }
  }

  public void stop() {
    final HttpServer server = this.httpServerRef.getAndSet(null);
    if (server != null) {
      server.stop(0);
    }
  }

  public String getTcpAddress() {
    final TcpReader reader = this.tcpReaderRef.get();
    return reader == null ? "none" : reader.getServerAddress();
  }
}
