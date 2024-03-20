package com.igormaznitsa.zxpoly.streamer;

import com.igormaznitsa.zxpoly.Version;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;

public class HttpProcessor implements Version {

  private static final Logger LOGGER = Logger.getLogger(HttpProcessor.class.getName());

  private static final String STREAM_RESOURCE = "stream.ts";
  private static final String WS_STREAM_RESOURCE = "wsstream.ts";

  private final String mime;
  private final InetAddress tcpReaderAddress;
  private final InetAddress httpServerAddress;
  private final int tcpReaderPort;
  private final int httpServerPort;
  private final AtomicReference<FemtoHttpServer> httpServerRef = new AtomicReference<>();
  private final AtomicReference<TcpReader> tcpReaderRef = new AtomicReference<>();
  private final Consumer<HttpProcessor> stopConsumer;
  private final ExecutorService executorService = new ThreadPoolExecutor(3, 10, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>(5), r -> {
    final Thread thread = Thread.ofVirtual()
        .name("zxpoly-http-stream-" + System.nanoTime())
        .unstarted(r);
    thread.setDaemon(true);
    return thread;
  });
  private final List<ThreadDataBuffer> threadDataBuffers = new CopyOnWriteArrayList<>();
  private volatile boolean stopped;

  public HttpProcessor(
          final String mime,
          final InetAddress addressIn,
          final int portIn,
          final InetAddress addressOut,
          final int portOut,
          final Consumer<HttpProcessor> stopConsumer
  ) {
    this.mime = mime;
    this.tcpReaderAddress = addressIn;
    this.httpServerAddress = addressOut;
    this.tcpReaderPort = portIn;
    this.httpServerPort = portOut;
    this.stopConsumer = stopConsumer;
  }

  public static String makeAcceptKey(final String key) {
    try {
      return Base64.getEncoder()
              .encodeToString(
                      MessageDigest.getInstance("SHA-1")
                              .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new Error("Can't find SHA-1 provider");
    }
  }

  public void start() throws IOException {
    startTcpServer();
    startHttpServer();
  }

  private void startTcpServer() {
    final TcpReader newReader =
            new TcpReader("tcp-reader", 0x10000, 10, InetAddress.getLoopbackAddress(), 0, (source, data) -> {
              HttpProcessor.this.threadDataBuffers.forEach(b -> b.offer(data));
              return false;
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

  private void startHttpServer() throws IOException {
    final FemtoHttpServer server = new FemtoHttpServer(this.executorService, new InetSocketAddress(this.httpServerAddress, this.httpServerPort), 3);
    if (this.httpServerRef.compareAndSet(null, server)) {
      server.createContext("/" + STREAM_RESOURCE, this::handleStreamData);
      server.createContext("/" + WS_STREAM_RESOURCE, this::handleWsStreamData);
      server.createContext("/", this::handleStaticResource);
      server.start();
    }
  }

  private byte[] readResource(final String path) {
    try {
      return IOUtils.resourceToByteArray("/com/igormaznitsa/zxpoly/streamer" + path);
    } catch (IOException ex) {
      LOGGER.warning("Can't find resource for path: " + path);
      return null;
    }
  }

  public String getHttpAddress() {
    final FemtoHttpServer server = this.httpServerRef.get();
    final InetSocketAddress address = server == null ? null : server.getAddress();
    return address == null ? "none" :
            address.getAddress().getHostAddress() + ":" + address.getPort();
  }

  private void handleStaticResource(final FemtoHttpServer.HttpExchange exchange) {
    try {
      final String linkToVideoStream = "http://" + this.getHttpAddress() + "/" + STREAM_RESOURCE;
      final String linkToWsVideoStream = "ws://" + this.getHttpAddress() + "/" + WS_STREAM_RESOURCE;
      final String linkToPlaylist = "http://" + this.getHttpAddress() + "/playlist.m3u8";

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
      } else if (path.endsWith(".m3u8")) {
        mime = "application/x-mpegURL";
      } else if (path.endsWith(".js.map")) {
        mime = "application/octet-stream";
      }

      exchange.getResponseHeaders().add("Origin", "http://" + this.getHttpAddress());
      exchange.getResponseHeaders().add("Content-Type", mime);
      exchange.getResponseHeaders().add("Access-Control-Allow-Origin ", "*");

      byte[] data = readResource(path);
      if (data == null) {
        exchange.sendResponseHeaders(HttpStatus.SC_NOT_FOUND, -1L);
      } else {
        if (!binary) {
          final String text = new String(data, StandardCharsets.UTF_8)
              .replace("${version.major}", Integer.toString(VERSION_MAJOR))
              .replace("${version.minor}", Integer.toString(VERSION_MINOR))
              .replace("${version.build}", Integer.toString(VERSION_BUILD))
                  .replace("${video.link}", linkToVideoStream)
                  .replace("${wsvideo.link}", linkToWsVideoStream)
                  .replace("${playlist.link}", linkToPlaylist)
                  .replace("${video.mime}", this.mime);
          data = text.getBytes(StandardCharsets.UTF_8);
        }
        exchange.sendResponseHeaders(HttpStatus.SC_OK, data.length);
        final OutputStream out = exchange.getResponseBody();
        out.write(data);
      }
    } catch (IOException ex) {
      LOGGER.warning("IOException during resource processing: " + ex.getMessage());
    } finally {
      exchange.close();
    }
  }

  private void handleWsStreamData(final FemtoHttpServer.HttpExchange exchange) {
    final ThreadDataBuffer buffer = new ThreadDataBuffer(Thread.currentThread().getName(), 32);
    this.threadDataBuffers.add(buffer);
    try {
      if ("get".equalsIgnoreCase(exchange.getRequestMethod())) {
        final Optional<String> connection = exchange.getRequestHeaders().getFirst("Connection");
        final Optional<String> upgrade = exchange.getRequestHeaders().getFirst("Upgrade");
        final Optional<String> webSocketKey = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Key");
        final Optional<String> webSocketProtocol = exchange.getRequestHeaders().getFirst("Sec-WebSocket-Protocol");

        if (!connection.isPresent()
                || !connection.get().toLowerCase(Locale.ENGLISH).contains("upgrade")
                || !upgrade.isPresent()
                || !upgrade.get().equalsIgnoreCase("WebSocket")
                || !webSocketKey.isPresent()) {
          exchange.sendResponseHeaders(HttpStatus.SC_BAD_REQUEST, -1L);
        } else {

          exchange.getResponseHeaders().add("Connection", "Upgrade");
          exchange.getResponseHeaders().add("Upgrade", "websocket");
          exchange.getResponseHeaders().add("Sec-WebSocket-Version", 13);
          exchange.getResponseHeaders().add("Origin", "http://" + this.getHttpAddress());
          exchange.getResponseHeaders().add("Access-Control-Allow-Origin ", "*");
          exchange.getResponseHeaders().add("Sec-WebSocket-Accept", makeAcceptKey(webSocketKey.get()));

          webSocketProtocol.ifPresent(x -> exchange.getResponseHeaders().add("Sec-WebSocket-Protocol", x));

          exchange.sendResponseHeaders(101, -1L);

          final InputStream inputStream = exchange.getRequestBody();
          final OutputStream outputStream = exchange.getResponseBody();

          final AtomicBoolean wsChannelActive = new AtomicBoolean(true);

          final WebSocketStreamWrapper wrapper = new WebSocketStreamWrapper(new WebSocketStreamWrapper.WsSignalReceiver() {
            @Override
            public void onClose(final WebSocketStreamWrapper source, final byte[] data) {
              wsChannelActive.set(false);
            }
          }, inputStream, outputStream);
          wrapper.start();

          while (!Thread.currentThread().isInterrupted() && wsChannelActive.get()) {
            final byte[] data = buffer.poll();
            if (data == null) {
              Thread.yield();
            } else {
              wrapper.writeBinary(false, data);
            }
          }
        }
      } else {
        exchange.sendResponseHeaders(HttpStatus.SC_METHOD_NOT_ALLOWED, -1);
      }
    } catch (IOException ex) {
      LOGGER.warning("IOException during WebSocket stream: " + ex.getMessage());
    } finally {
      LOGGER.info("Streaming thread completed");
      this.threadDataBuffers.remove(buffer);
      exchange.close();
    }
  }

  private void handleStreamData(final FemtoHttpServer.HttpExchange exchange) {
    if ("head".equalsIgnoreCase(exchange.getRequestMethod())) {
      LOGGER.info("Incoming HEAD request for video stream: " + exchange.getRequestURI());
      final FemtoHttpServer.Headers headers = exchange.getResponseHeaders();
      headers.add("Content-Type", this.mime);
      headers.add("Cache-Control", "no-cache, no-store");
      headers.add("Pragma", "no-cache");
      headers.add("Expires", "0");
      headers.add("Connection", "Keep-Alive");
      headers.add("Keep-Alive", "max");
      headers.add("Accept-Ranges", "none");
      try {
        exchange.sendResponseHeaders(HttpStatus.SC_OK, -1L);
      } catch (IOException ex) {
        exchange.close();
      }
    } else if ("get".equalsIgnoreCase(exchange.getRequestMethod())) {
      final ThreadDataBuffer buffer = new ThreadDataBuffer(Thread.currentThread().getName(), 32);
      this.threadDataBuffers.add(buffer);
      try {
        LOGGER.info("Incoming GET request for video stream: " + exchange.getRequestURI());

        exchange.getResponseHeaders().add("Content-Type", this.mime);
        exchange.getResponseHeaders().add("Cache-Control", "no-cache, no-store");
        exchange.getResponseHeaders().add("Pragma", "no-cache");
        exchange.getResponseHeaders().add("Expires", "0");
        exchange.getResponseHeaders().add("Connection", "Keep-Alive");
        exchange.getResponseHeaders().add("Keep-Alive", "max");
        exchange.getResponseHeaders().add("Accept-Ranges", "none");
        exchange.getResponseHeaders().add("Origin", "http://" + this.getHttpAddress());
        exchange.getResponseHeaders().add("Content-Type", mime);
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin ", "*");

        exchange.sendResponseHeaders(HttpStatus.SC_OK, Long.MAX_VALUE);

        try (final OutputStream responseStream = exchange.getResponseBody()) {
          while (!this.stopped && !Thread.currentThread().isInterrupted()) {
            final byte[] data = buffer.poll();
            if (data == null) {
              Thread.yield();
            } else {
              responseStream.write(data);
            }
          }
        }
      } catch (Exception ex) {
        LOGGER.warning("Exception during streaming: " + ex.getMessage());
      } finally {
        LOGGER.info("Streaming thread completed");
        this.threadDataBuffers.remove(buffer);
        exchange.close();
      }
    } else {
      LOGGER.warning("Incoming unsupported " + exchange.getRequestMethod() + " request for video stream: " + exchange.getRequestURI());
      try {
        exchange.sendResponseHeaders(HttpStatus.SC_METHOD_NOT_ALLOWED, -1L);
      } catch (Exception ex) {
        // DO NOTHING
      } finally {
        exchange.close();
      }
    }
  }

  public void stop() {
    this.stopped = true;
    final FemtoHttpServer server = this.httpServerRef.getAndSet(null);
    if (server != null) {
      final TcpReader reader = this.tcpReaderRef.getAndSet(null);
      if (reader != null) {
        reader.stop();
      }

      try {
        LOGGER.info("Stopping server");
        server.stop();
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

  public String getTcpAddress() {
    final TcpReader reader = this.tcpReaderRef.get();
    return reader == null ? "none" : reader.getServerAddress();
  }

  public static class WebSocketStreamWrapper {

    private static final byte[] EMPTY_ARRAY = new byte[0];
    private final Random rnd = new Random();
    private final AtomicReference<Thread> thread = new AtomicReference<>();
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final WebSocketStreamWrapper.WsSignalReceiver receiver;

    private final Lock writeLock = new ReentrantLock();

    public WebSocketStreamWrapper(final WebSocketStreamWrapper.WsSignalReceiver receiver, final InputStream inputStream, final OutputStream outputStream) {
      this.receiver = Objects.requireNonNull(receiver);
      this.inputStream = inputStream;
      this.outputStream = outputStream;
    }

    static void writeWebSocketFrame(final OutputStream stream, final int opCode, final OptionalInt frameMask, final byte[] data) throws IOException {
      stream.write(0x80 | (opCode & 0xF));

      final int maskBit;
      final byte[] mask;
      if (frameMask.isPresent()) {
        maskBit = 0x80;
        mask = new byte[4];
        final int value = frameMask.getAsInt();
        mask[0] = (byte) (value >> 24);
        mask[1] = (byte) (value >> 16);
        mask[2] = (byte) (value >> 8);
        mask[3] = (byte) value;
      } else {
        maskBit = 0;
        mask = null;
      }

      if (data.length < 126) {
        stream.write(maskBit | data.length);
      } else if (data.length < 0x10000) {
        stream.write(maskBit | 0x7E);
        writePayloadLength(stream, data.length);
      } else {
        stream.write(maskBit | 0x7F);
        writePayloadLength(stream, data.length);
      }

      if (mask != null) {
        stream.write(mask);
        for (int i = 0; i < data.length; i++) {
          final int j = i % 4;
          data[i] = (byte) (data[i] ^ mask[j]);
        }
      }
      stream.write(data);

      if (mask != null) {
        for (int i = 0; i < data.length; i++) {
          final int j = i % 4;
          data[i] = (byte) (data[i] ^ mask[j]);
        }
      }
      stream.flush();
    }

    private static long readPayloadLengthBytes(final InputStream in, final int bytes) throws IOException {
      long result = 0L;
      int count = bytes;
      while (--count > 0) {
        final int next = in.read();
        if (next < 0) throw new EOFException();
        result |= (long) next << (8 * count);
      }
      return result;
    }

    private static void writePayloadLength(final OutputStream out, final long length) throws IOException {
      if (length < 0x10000) {
        out.write((byte) (length >>> 8));
        out.write((byte) length);
      } else {
        out.write((byte) (length >>> 56));
        out.write((byte) (length >>> 48));
        out.write((byte) (length >>> 40));
        out.write((byte) (length >>> 32));
        out.write((byte) (length >>> 24));
        out.write((byte) (length >>> 16));
        out.write((byte) (length >>> 8));
        out.write((byte) length);
      }
    }

    public void start() {
      final Thread newThread =
          Thread.ofVirtual().name("ws-wrapper-read-thread-" + System.nanoTime())
              .unstarted(this::readRun);
      if (this.thread.compareAndSet(null, newThread)) {
        newThread.start();
      } else {
        throw new IllegalStateException("Already started");
      }
    }

    private void assertStarted() {
      if (this.thread.get() == null) {
        throw new IllegalStateException("Not started");
      }
    }

    private OptionalInt generateMask() {
      int result = 0;
      do {
        result = this.rnd.nextInt();
      } while (result == 0);
      return OptionalInt.of(result);
    }

    public void writeFrame(final int opCode, final boolean masked, final byte[] data) throws IOException {
      this.assertStarted();
      this.writeLock.lock();
      try {
        writeWebSocketFrame(this.outputStream, opCode, masked ? this.generateMask() : OptionalInt.empty(), data);
      } finally {
        this.writeLock.unlock();
      }
    }

    public void writeText(final boolean masked, final String text) throws IOException {
      this.writeFrame(0x01, masked, text.getBytes(StandardCharsets.UTF_8));
    }

    public void writeBinary(final boolean masked, final byte[] data) throws IOException {
      this.writeFrame(0x02, masked, data);
    }

    private void readRun() {
      this.receiver.onStart(this);
      try {
        while (this.thread.get() != null && !Thread.currentThread().isInterrupted()) {
          final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
          int opcode = 0;
          while (this.thread.get() != null && !Thread.currentThread().isInterrupted()) {
            final int maskOpcode = this.inputStream.read();
            if (maskOpcode >= 0) {
              final boolean fin = (maskOpcode & 0x80) != 0;
              opcode |= maskOpcode & 0xF;

              final int maskPayloadLen = this.inputStream.read();
              if (maskPayloadLen < 0) throw new EOFException();

              final boolean mask = (maskPayloadLen & 0x80) != 0;
              long payloadLen = maskPayloadLen & 0x7F;
              if (payloadLen == 0x7EL) {
                payloadLen = readPayloadLengthBytes(this.inputStream, 2);
              } else if (payloadLen == 0x7FL) {
                payloadLen = readPayloadLengthBytes(this.inputStream, 8);
              }
              final byte[] maskValue;
              if (mask) {
                maskValue = IOUtils.readFully(this.inputStream, 4);
              } else {
                maskValue = EMPTY_ARRAY;
              }
              final byte[] payload = IOUtils.readFully(this.inputStream, (int) payloadLen);

              if (mask) {
                for (int i = 0; i < payload.length; i++) {
                  final int j = i % 4;
                  payload[i] = (byte) (payload[i] ^ maskValue[j]);
                }
              }

              buffer.write(payload);

              if (fin) {
                this.processIncomingPacket(opcode, buffer.toByteArray());
                break;
              }
            } else {
              throw new EOFException("WebSocket input stream is closed");
            }
          }
        }
      } catch (Exception ex) {
        LOGGER.warning("Web socket read error: " + ex.getMessage());
      } finally {
        this.receiver.onStop(this);
      }
    }

    private void processIncomingPacket(final int opCode, final byte[] data) throws IOException {
      switch (opCode) {
        case 0x00: {  // continuation frame
          this.receiver.onUnexpected(this, opCode, data);
        }
        break;
        case 0x01: { // text frame
          this.receiver.onText(this, new String(data, StandardCharsets.UTF_8));
        }
        break;
        case 0x02: { // binary frame
          this.receiver.onBinary(this, data);
        }
        break;
        case 0x08: { // connection close
          this.receiver.onClose(this, data);
        }
        break;
        case 0x09: { // ping
          this.writeFrame(0x0A, false, EMPTY_ARRAY);
        }
        break;
        case 0x0A: { // pong

        }
        break;
        default: { // reserved
          this.receiver.onReserved(this, opCode, data);
        }
        break;
      }
    }

    public void close(final boolean closeStreams) {
      final Thread startedThread = this.thread.getAndSet(null);
      if (startedThread != null) {
        IOUtils.closeQuietly(this.inputStream);
        IOUtils.closeQuietly(this.outputStream);
        try {
          startedThread.interrupt();
          startedThread.join();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }

    public interface WsSignalReceiver {
      default void onText(WebSocketStreamWrapper source, String text) {
      }

      default void onBinary(WebSocketStreamWrapper source, byte[] data) {
      }

      default void onClose(WebSocketStreamWrapper source, byte[] data) {
      }

      default void onUnexpected(WebSocketStreamWrapper source, int code, byte[] data) {
      }

      default void onReserved(WebSocketStreamWrapper source, int code, byte[] data) {
      }

      default void onStart(WebSocketStreamWrapper source) {
      }

      default void onStop(WebSocketStreamWrapper source) {
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
}
