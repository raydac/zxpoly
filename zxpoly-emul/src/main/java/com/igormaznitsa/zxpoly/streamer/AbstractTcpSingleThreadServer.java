package com.igormaznitsa.zxpoly.streamer;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;

public abstract class AbstractTcpSingleThreadServer {
  protected final PreemptiveBuffer buffer;

  public String getId() {
    return this.id;
  }

  public interface TcpServerListener {

    default void onEstablishing(AbstractTcpSingleThreadServer server, ServerSocket socket, Throwable error) {

    }

    default void onConnected(AbstractTcpSingleThreadServer server, Socket socket) {

    }

    default void onClientError(AbstractTcpSingleThreadServer server, Throwable error) {

    }

    default void onDone(AbstractTcpSingleThreadServer server) {

    }
  }

  private final String id;
  private final InetAddress address;
  private final int port;
  private final AtomicReference<Thread> currentThread = new AtomicReference<>();
  private final AtomicReference<ServerSocket> serverSocket = new AtomicReference<>();
  private final AtomicReference<Socket> currentSocket = new AtomicReference<>();
  private final List<TcpServerListener> listeners = new CopyOnWriteArrayList<>();
  private volatile boolean stopped;

  public AbstractTcpSingleThreadServer(
      final String id,
      final int bufferSize,
      final InetAddress address,
      final int port
  ) {
    this.id = id;
    this.buffer = new PreemptiveBuffer(bufferSize);
    this.address = address;
    this.port = port;
  }

  protected boolean isStopped() {
    return this.stopped;
  }

  public void addListener(final TcpServerListener listener) {
    this.listeners.add(listener);
  }

  public void removeListener(final TcpServerListener listener) {
    this.listeners.remove(listener);
  }

  public void start() {
    final Thread thread = new Thread(this::doWork, this.id + '-' + this.hashCode());
    thread.setDaemon(true);
    if (this.currentThread.compareAndSet(null, thread)) {
      this.stopped = false;
      thread.start();
      this.buffer.start();
    }
  }

  public void stop() {
    this.stopped = true;
    this.buffer.suspend();
    final Thread thread = this.currentThread.getAndSet(null);
    if (thread != null) {
      thread.interrupt();
      IOUtils.closeQuietly(this.serverSocket.get());
      IOUtils.closeQuietly(this.currentSocket.get());
      try {
        thread.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public String getServerAddress() {
    final ServerSocket serverSocket = this.serverSocket.get();
    return serverSocket == null ? "none" :
        serverSocket.getInetAddress().getHostAddress() + ':' + serverSocket.getLocalPort();
  }

  private void doWork() {
    try {
      final ServerSocket serverSocket;
      try {
        serverSocket = new ServerSocket(this.port, 1, this.address);
        this.serverSocket.set(serverSocket);
        this.listeners.forEach(x -> x.onEstablishing(this, serverSocket, null));
      } catch (Exception ex) {
        this.listeners.forEach(x -> x.onEstablishing(this, null, ex));
        return;
      }

      while (!Thread.currentThread().isInterrupted() && !stopped) {
        final Socket socket;
        try {
          socket = serverSocket.accept();
          socket.setTcpNoDelay(true);
          socket.setKeepAlive(true);
        } catch (Exception ex) {
          this.listeners.forEach(x -> x.onClientError(this, ex));
          break;
        }
        this.currentSocket.set(socket);
        this.listeners.forEach(x -> x.onConnected(this, socket));

        try {
          this.doBusiness(socket);
        } catch (Exception ex) {
          if (!this.stopped && !Thread.currentThread().isInterrupted()) {
            this.listeners.forEach(x -> x.onClientError(this, ex));
          }
        }
      }
    } finally {
      this.listeners.forEach(x -> x.onDone(this));
    }
  }

  protected abstract void doBusiness(Socket socket) throws Exception;
}
