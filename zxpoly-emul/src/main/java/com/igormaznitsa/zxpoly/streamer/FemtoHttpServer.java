package com.igormaznitsa.zxpoly.streamer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;

public class FemtoHttpServer {

  private static final Logger LOGGER = Logger.getLogger(FemtoHttpServer.class.getName());
  private static final byte[] EOL = new byte[] {0x0D, 0x0A};
  private final ExecutorService executorService;
  private final InetSocketAddress socketAddress;
  private final int backlog;
  private final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();

  private final Map<Socket, Thread> activeConnections = new ConcurrentHashMap<>();
  private final Map<String, Consumer<FemtoHttpServer.HttpExchange>> contexts = new ConcurrentHashMap<>();

  public FemtoHttpServer(final ExecutorService executorService, final InetSocketAddress socketAddress, final int backlog) {
    this.executorService = Objects.requireNonNull(executorService);
    this.socketAddress = Objects.requireNonNull(socketAddress);
    this.backlog = backlog;
  }

  private static String readLine(final InputStream in) throws IOException {
    final StringBuilder result = new StringBuilder();
    while (!Thread.currentThread().isInterrupted()) {
      final int chr = in.read();
      if (chr < 0) break;
      if (Character.isISOControl(chr)) {
        if (chr == '\n') break;
      } else {
        result.append((char) chr);
      }
    }
    return result.toString();
  }

  public InetSocketAddress getAddress() {
    final ServerSocket mainSocket = this.serverSocket.get();
    return mainSocket == null ? null : new InetSocketAddress(mainSocket.getInetAddress(), mainSocket.getLocalPort());
  }

  public void createContext(final String prefix, final Consumer<FemtoHttpServer.HttpExchange> handler) {
    this.contexts.put(prefix, Objects.requireNonNull(handler));
  }

  public void start() throws IOException {
    final ServerSocket newServerSocket = new ServerSocket(this.socketAddress.getPort(), this.backlog, this.socketAddress.getAddress());
    newServerSocket.setReuseAddress(true);
    newServerSocket.setSoTimeout(600000);

    if (this.serverSocket.compareAndSet(null, newServerSocket)) {
      LOGGER.info("Starting HTTP server: " + this.socketAddress);
      this.executorService.submit(this::mainLoop);
    }
  }

  private void mainLoop() {
    final ServerSocket mainSocket = this.serverSocket.get();
    LOGGER.info("Created socket: " + mainSocket.getInetAddress() + ":" + mainSocket.getLocalPort());
    while (!Thread.currentThread().isInterrupted() && this.serverSocket.get() != null) {
      try {
        LOGGER.info("Awaiting incoming connection: " + mainSocket);
        final Socket socket = mainSocket.accept();
        socket.setKeepAlive(true);
        LOGGER.info("Incoming connection: " + socket);
        this.executorService.submit(() -> this.processIncoming(socket));
      } catch (SocketTimeoutException ex) {
        // ignoring
      } catch (IOException ex) {
        LOGGER.warning("Error during server socket accept: " + ex.getMessage());
      }
    }
  }

  private void processIncoming(final Socket socket) {
    try {
      final InputStream socketInputStream = socket.getInputStream();
      final OutputStream socketOutputStream = socket.getOutputStream();

      final String[] request = readLine(socketInputStream).split("\\s");
      if (request.length < 3) {
        throw new IOException("Illegal request header: " + Arrays.toString(request));
      }

      final String method = request[0].trim().toUpperCase(Locale.ENGLISH);
      final URI path = new URI(request[1].trim());
      final String version = request[2].trim().toUpperCase(Locale.ENGLISH);

      if (!version.startsWith("HTTP/")) {
        throw new IOException("Non-http request: " + Arrays.toString(request));
      }

      final Headers requestHeaders = new Headers();
      while (!Thread.currentThread().isInterrupted()) {
        final String headerLine = readLine(socketInputStream);
        if (headerLine.isEmpty()) break;
        requestHeaders.add(headerLine);
      }

      final String stringPath = path.getPath();

      String foundPath = null;
      Consumer<HttpExchange> foundConsumer = null;
      for (final Map.Entry<String, Consumer<HttpExchange>> ctx : this.contexts.entrySet()) {
        if (stringPath.startsWith(ctx.getKey())) {
          if (foundPath == null || ctx.getKey().length() >= foundPath.length()) {
            foundConsumer = ctx.getValue();
            if (stringPath.equals(ctx.getKey())) {
              foundPath = ctx.getKey();
              break;
            } else {
              foundPath = ctx.getKey();
            }
          }
        }
      }

      if (foundConsumer == null) {
        LOGGER.warning("Can't find context for path: " + stringPath);
      } else {
        LOGGER.info("Found '" + foundPath + "' context for path: " + stringPath);
        this.activeConnections.put(socket, Thread.currentThread());
        try {
          final HttpExchange exchange = new HttpExchange(method, path, requestHeaders, socketInputStream, socketOutputStream);
          foundConsumer.accept(exchange);
        } finally {
          this.activeConnections.remove(socket);
        }
      }
    } catch (Exception ex) {
      LOGGER.warning("Error during incoming connection processing: " + ex.getMessage());
    } finally {
      try {
        LOGGER.info("Closing socket: " + socket);
        socket.close();
      } catch (Exception e) {
        // do nothing
      }
    }
  }

  public void stop() {
    final ServerSocket mainSocket = this.serverSocket.getAndSet(null);
    if (mainSocket != null) {
      LOGGER.info("Stopping HTTP server: " + this.socketAddress);
      try {
        mainSocket.close();
      } catch (IOException ex) {
        // do nothing
      }
      this.activeConnections.forEach((key, value) -> {
        LOGGER.info("Closing socket: " + key);
        try {
          key.close();
        } catch (Exception ex) {
          // ignore
        }
      });
      this.activeConnections.clear();
    }
  }

  public int getConnectionsCount() {
    return this.activeConnections.size();
  }

  public static final class Headers {

    private final Map<String, List<String>> map = new HashMap<>();

    Headers() {

    }

    private static String restoreName(final String name) {
      final StringBuilder buffer = new StringBuilder();
      boolean nextUpperCased = true;

      for (int i = 0; i < name.length(); i++) {
        final char c = name.charAt(i);
        buffer.append(nextUpperCased ? Character.toUpperCase(c) : c);
        nextUpperCased = c == '-';
      }

      return buffer.toString();
    }

    private static String normalizeName(final String name) {
      return name.trim().toLowerCase(Locale.ENGLISH);
    }

    public int size() {
      return this.map.size();
    }

    public void clear() {
      this.map.clear();
    }

    public Set<String> allHeaders() {
      return this.map.keySet().stream().map(Headers::restoreName).collect(Collectors.toSet());
    }

    public void add(final String header, final String value) {
      map.computeIfAbsent(normalizeName(header), x -> new ArrayList<>()).add(Objects.requireNonNull(value));
    }

    public void add(final String header, final long value) {
      map.computeIfAbsent(normalizeName(header), x -> new ArrayList<>()).add(Long.toString(value));
    }

    public List<String> get(final String header) {
      return map.getOrDefault(normalizeName(header), Collections.emptyList());
    }

    public Optional<String> getFirst(String header) {
      final List<String> headerList = this.get(normalizeName(header));
      return headerList.isEmpty() ? Optional.empty() : Optional.ofNullable(headerList.get(0));
    }

    public void add(final String headerLine) {
      if (headerLine == null || headerLine.isEmpty()) return;
      final int delimiter = headerLine.indexOf(':');
      if (delimiter < 0) {
        this.add(normalizeName(headerLine), "");
      } else {
        final String name = normalizeName(headerLine.substring(0, delimiter));
        final String value = headerLine.substring(delimiter + 1).trim();
        this.add(name, value);
      }
    }
  }

  public static class HttpExchange {

    private final String method;
    private final URI path;
    private final Headers requestHeaders;
    private final Headers responseHeaders;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    HttpExchange(final String method,
                 final URI path,
                 final Headers requestHeaders,
                 final InputStream inputStream,
                 final OutputStream outputStream) {
      this.method = Objects.requireNonNull(method);
      this.path = Objects.requireNonNull(path);
      this.requestHeaders = Objects.requireNonNull(requestHeaders);
      this.responseHeaders = new Headers();
      this.inputStream = Objects.requireNonNull(inputStream);
      this.outputStream = Objects.requireNonNull(outputStream);
    }

    private static void writeLine(final String line, final OutputStream out) throws IOException {
      final byte[] codes = line.getBytes(StandardCharsets.US_ASCII);
      out.write(codes);
      out.write(EOL);
    }

    public Headers getResponseHeaders() {
      return this.responseHeaders;
    }

    public Headers getRequestHeaders() {
      return this.requestHeaders;
    }

    public String getRequestMethod() {
      return this.method;
    }

    public URI getRequestURI() {
      return this.path;
    }

    public void sendResponseHeaders(final int code, final long contentLength) throws IOException {
      String reason = "Unknown";
      if (code < 600) {
        reason = "Server error";
      }
      if (code < 500) {
        reason = "Client error";
      }
      if (code < 400) {
        reason = "Redirect";
      }
      if (code < 300) {
        reason = "OK";
      }
      if (code < 200) {
        reason = "Information";
      }
      if (code < 100) {
        reason = "Special";
      }

      if (code == 101) {
        reason = "Switching Protocols";
      }

      writeLine(String.format("HTTP/1.1 %d %s", code, reason), this.outputStream);

      if (contentLength >= 0 && this.responseHeaders.get("Content-Length").isEmpty()) {
        this.responseHeaders.add("Content-Length", contentLength);
      }

      for (final String header : this.responseHeaders.allHeaders()) {
        for (final String value : this.responseHeaders.get(header)) {
          writeLine(String.format("%s: %s", header, value), this.outputStream);
        }
      }
      writeLine("", this.outputStream);
      this.outputStream.flush();
    }

    public void close() {
      try {
        this.outputStream.flush();
      } catch (Exception ex) {
        // ignore
      } finally {
        IOUtils.closeQuietly(this.inputStream);
        IOUtils.closeQuietly(this.outputStream);
      }
    }

    public InputStream getRequestBody() {
      return this.inputStream;
    }

    public OutputStream getResponseBody() {
      return this.outputStream;
    }
  }
}
