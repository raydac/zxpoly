package com.igormaznitsa.zxpoly.streamer;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
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

  private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
    final Thread thread = new Thread(r, "zxpoly-stream-" + System.nanoTime());
    thread.setDaemon(true);
    return thread;
  });

  private final List<ThreadDataBuffer> threadDataBuffers = new CopyOnWriteArrayList<>();

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

  private void startTcpServer() {
    final TcpReader newReader =
            new TcpReader("tcp-reader", 0x10000, 10, InetAddress.getLoopbackAddress(), 0, new TcpReader.TcpReaderDataProcessor() {
              @Override
              public boolean onIncomingData(final TcpReader source, final byte[] data) {
                InternalHttpServer.this.threadDataBuffers.forEach(b -> b.offer(data));
                return false;
              }
            });

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

  public void start() throws IOException {
    startTcpServer();
    startHttpServer();
  }

  private void startHttpServer() throws IOException {
    final HttpServer server =
            HttpServer.create(new InetSocketAddress(this.httpServerAddress, this.httpServerPort), 3);
    if (this.httpServerRef.compareAndSet(null, server)) {
      server.createContext("/" + STREAM_RESOURCE, this::handleStreamData);
      server.createContext("/", this::handleMainPage);
      server.setExecutor(this.executorService);
      server.start();
    }
  }

  public String getHttpAddress() {
    final HttpServer server = this.httpServerRef.get();
    final InetSocketAddress address = server == null ? null : server.getAddress();
    return address == null ? "none" :
            address.getAddress().getHostAddress() + ":" + address.getPort();
  }

  private byte[] readResource(final String path) {
    try {
      return IOUtils.resourceToByteArray("/com/igormaznitsa/zxpoly/streamer" + path);
    } catch (IOException ex) {
      LOGGER.warning("Can't find resource for path: " + path);
      return null;
    }
  }

  private void handleMainPage(final HttpExchange exchange) throws IOException {
    final String linkToVideoStream = "http://" + this.getHttpAddress() + "/" + STREAM_RESOURCE;

    String path = exchange.getRequestURI().getPath();
    LOGGER.info("Incoming request for resource: " + path);
    if ("/".equals(path)) {
      path = "/streamer.html";
    }

    String mime = "text/plain";
    boolean binary = false;
    if (path.endsWith(".css")) {
      mime = "text/css";
    } else if (path.endsWith(".js")) {
      mime = "text/javascript";
    } else if (path.endsWith(".htm") || path.endsWith(".html")) {
      mime = "text/html";
    } else if (path.endsWith(".png")) {
      mime = "image/png";
      binary = true;
    } else if (path.endsWith(".ico")) {
      mime = "image/x-icon";
      binary = true;
    }

    final Headers headers = exchange.getResponseHeaders();
    headers.add("Content-Type", mime);

    byte[] data = readResource(path);
    if (data == null) {
      exchange.sendResponseHeaders(404, 0L);
      final OutputStream out = exchange.getResponseBody();
      out.write(new byte[0]);
      out.flush();
      out.close();
    } else {
      if (!binary) {
        final String text = new String(data, StandardCharsets.UTF_8).replace("${video.link}", linkToVideoStream)
                .replace("${video.mime}", this.mime);
        data = text.getBytes(StandardCharsets.UTF_8);
      }
      exchange.sendResponseHeaders(200, data.length);
      final OutputStream out = exchange.getResponseBody();
      out.write(data);
      out.flush();
      out.close();
    }
  }

  private void handleStreamData(final HttpExchange exchange) {
    if ("head".equalsIgnoreCase(exchange.getRequestMethod())) {
      LOGGER.info("Incoming HEAD request for video stream: " + exchange.getRequestURI());
      final Headers headers = exchange.getResponseHeaders();
      headers.add("Content-Type", this.mime);
      headers.add("Cache-Control", "no-cache, no-store");
      headers.add("Pragma", "no-cache");
      headers.add("Transfer-Encoding", "chunked");
      headers.add("Expires", "0");
      headers.add("Connection", "Keep-Alive");
      headers.add("Keep-Alive", "max");
      headers.add("Accept-Ranges", "none");
      try {
        exchange.sendResponseHeaders(200, -1L);
      } catch (IOException ex) {
        exchange.close();
      }
    } else if ("get".equalsIgnoreCase(exchange.getRequestMethod())) {
      final ThreadDataBuffer buffer = new ThreadDataBuffer(Thread.currentThread().getName(), 32);
      this.threadDataBuffers.add(buffer);
      try {

        final List<String> headerConnection = exchange.getRequestHeaders().get("Connection");
        final List<String> headerUpgrade = exchange.getRequestHeaders().get("Upgrade");

        if (headerUpgrade != null && headerConnection.stream().anyMatch(x -> "websocket".equalsIgnoreCase(x))) {
          LOGGER.warning("Incoming GET request for Websocket protocol: " + exchange.getRequestURI());
          exchange.sendResponseHeaders(400, -1L);
        } else {

          LOGGER.info("Incoming GET request for video stream: " + exchange.getRequestURI());

          final Headers headers = exchange.getResponseHeaders();
          headers.add("Content-Type", this.mime);
          headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
          headers.add("Expires", "0");
          headers.add("Pragma", "no-cache");
          headers.add("Accept-Ranges", "none");

          exchange.sendResponseHeaders(200, 0);

          try (final OutputStream responseStream = exchange.getResponseBody()) {
            while (!this.stopped && !Thread.currentThread().isInterrupted()) {
              final byte[] data = buffer.poll();
              if (data != null) {
                responseStream.write(data);
              }
            }
          }
        }
      } catch (Exception ex) {
        LOGGER.warning("Exception during streaming: " + ex.getMessage());
      } finally {
        exchange.close();
        this.threadDataBuffers.remove(buffer);
        LOGGER.info("Streaming thread completed");
      }
    } else {
      LOGGER.warning("Incoming unsupported " + exchange.getRequestMethod() + " request for video stream: " + exchange.getRequestURI());
      try {
        exchange.sendResponseHeaders(405, -1L);
      } catch (Exception ex) {
        // DO NOTHING
      } finally {
        exchange.close();
      }
    }
  }

  public void stop() {
    stopped = true;
    final HttpServer server = this.httpServerRef.getAndSet(null);
    if (server != null) {
      final TcpReader reader = this.tcpReaderRef.getAndSet(null);
      if (reader != null) {
        reader.stop();
      }
      try {
        LOGGER.info("Stopping server");
        server.stop(0);
      } catch (Exception ex) {
        LOGGER.warning("Error on server stop: " + ex.getMessage());
      } finally {
        this.executorService.shutdownNow();
      }

      if (this.stopConsumer != null) {
        this.stopConsumer.accept(this);
      }
    }
  }

  private static final class ThreadDataBuffer {
    private final BlockingQueue<byte[]> buffer;
    private final String id;

    ThreadDataBuffer(final String id, final int bufferSize) {
      this.buffer = new ArrayBlockingQueue<>(bufferSize);
      this.id = id;
    }

    @Override
    public int hashCode() {
      return this.id.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      return obj instanceof ThreadDataBuffer && Objects.equals(this.id, ((ThreadDataBuffer) obj).id);
    }

    byte[] poll() {
      return this.buffer.poll();
    }

    boolean offer(final byte[] data) {
      if (data == null) {
        return false;
      } else {
        return this.buffer.offer(data);
      }
    }
  }

  public String getTcpAddress() {
    final TcpReader reader = this.tcpReaderRef.get();
    return reader == null ? "none" : reader.getServerAddress();
  }
}
