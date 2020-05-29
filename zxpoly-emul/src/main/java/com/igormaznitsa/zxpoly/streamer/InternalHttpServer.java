package com.igormaznitsa.zxpoly.streamer;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class InternalHttpServer {

  private static final Logger LOGGER = Logger.getLogger("VideoStreamer.InternalHttpServer");

  private static final String STREAM_RESOURCE = "stream.ts";

  private final String mime;
  private final InetAddress tcpReaderAddress;
  private final InetAddress httpServerAddress;
  private final int tcpReaderPort;
  private final int httpServerPort;
  private final AtomicReference<HttpServer> httpServerRef = new AtomicReference<>();
  private final AtomicReference<TcpReader> tcpReaderRef = new AtomicReference<>();
  private final Consumer<InternalHttpServer> stopConsumer;
  private volatile boolean stopped;

  public InternalHttpServer(
      final String mime,
      final InetAddress addressIn,
      final int portIn,
      final InetAddress addressOut,
      final int portOut,
      final Consumer<InternalHttpServer> stopConsumer
  ) {
    this.mime = mime;
    this.tcpReaderAddress = addressIn;
    this.httpServerAddress = addressOut;
    this.tcpReaderPort = portIn;
    this.httpServerPort = portOut;
    this.stopConsumer = stopConsumer;
  }

  public void start() throws IOException {
    startTcpServer();
    startHttpServer();
  }

  private void startTcpServer() {
    final TcpReader newReader =
        new TcpReader("tcp-reader", 0x10000, 10, InetAddress.getLoopbackAddress(), 0);
    if (this.tcpReaderRef.compareAndSet(null, newReader)) {
      newReader.addListener(new AbstractTcpSingleThreadServer.TcpServerListener() {
        @Override
        public void onConnectionDone(final AbstractTcpSingleThreadServer source,
                                     final Socket socket) {
          if (source == newReader) {
            LOGGER.info("TCP reader connection lost");
            stop();
          }
        }
      });
      newReader.start();
    }
  }

  private void startHttpServer() throws IOException {
    final HttpServer server =
        HttpServer.create(new InetSocketAddress(this.httpServerAddress, this.httpServerPort), 3);
    if (this.httpServerRef.compareAndSet(null, server)) {
      server.createContext("/" + STREAM_RESOURCE, this::handleStreamData);
      server.createContext("/", this::handleMainPage);
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

  private void handleMainPage(final HttpExchange exchange) throws IOException {
    final String linkToVideoStream = "http://" + this.getHttpAddress() + "/" + STREAM_RESOURCE;

    final String page = "<!DOCTYPE html>"
        + "<html>"
        + "<body>"
        + "<h3>Zx-Poly emulator stream</h3>"
        + "<hr>"
        + "<p>"
        + "Link: <b><a href=\"" + linkToVideoStream + "\">" + linkToVideoStream + "</a></b><br>"
        + "Mime: " + this.mime
        + "</p>"
        + "<video width=\"512\" height=\"384\" controls>\n"
        + "<source src=\"http://" + this.getHttpAddress() + "/" + STREAM_RESOURCE + "\" type=\"" +
        this.mime + "\">"
        + "Your browser does not support HTML video."
        + "</video>"
        + "</body>"
        + "</html>";

    final Headers headers = exchange.getResponseHeaders();
    headers.add("Content-Type", "text/html");

    final byte[] content = page.getBytes(StandardCharsets.UTF_8);

    exchange.sendResponseHeaders(200, content.length);
    final OutputStream out = exchange.getResponseBody();
    out.write(content);
    out.flush();
    out.close();
  }

  private void handleStreamData(final HttpExchange exchange) throws IOException {
    final Headers headers = exchange.getResponseHeaders();
    headers.add("Content-Type", this.mime);
    headers.add("Cache-Control", "no-cache, no-store");
    headers.add("Pragma", "no-cache");
    headers.add("Transfer-Encoding", "chunked");
    headers.add("Content-Transfer-Encoding", "binary");
    headers.add("Expires", "0");
    headers.add("Connection", "Keep-Alive");
    headers.add("Keep-Alive", "max");
    headers.add("Accept-Ranges", "none");

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
      final TcpReader reader = this.tcpReaderRef.getAndSet(null);
      if (reader != null) {
          reader.stop();
      }
      try {
        server.stop(0);
      } catch (Exception ex) {
        LOGGER.warning("Error on server stop: " + ex.getMessage());
      }

      if (this.stopConsumer != null) {
        this.stopConsumer.accept(this);
      }
    }
  }

  public String getTcpAddress() {
    final TcpReader reader = this.tcpReaderRef.get();
    return reader == null ? "none" : reader.getServerAddress();
  }
}
