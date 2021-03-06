/*
 * Copyright (C) 2014-2019 Igor Maznitsa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.igormaznitsa.zxpoly.utils;

import com.igormaznitsa.zxpoly.components.RomData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

public class RomLoader {

  private static final String ROM_48 = "48.rom";
  private static final String ROM_128TR = "128tr.rom";
  private static final String ROM_TRDOS = "trdos.rom";

  public RomLoader() {

  }

  static byte[] loadHTTPArchive(final String url) throws IOException {
    final SSLContext sslcontext;
    try {
      sslcontext = SSLContext.getInstance("TLS");
    } catch (NoSuchAlgorithmException ex) {
      throw new IOException("Can't find TLS: " + ex.getMessage());
    }
    X509TrustManager tm = new X509TrustManager() {
      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      @Override
      public void checkClientTrusted(final X509Certificate[] arg0, final String arg1)
          throws CertificateException {
      }

      @Override
      public void checkServerTrusted(final X509Certificate[] arg0, String arg1)
          throws CertificateException {
      }
    };
    try {
      sslcontext.init(null, new TrustManager[] {tm}, null);
    } catch (KeyManagementException ex) {
      throw new IOException("Can't init ssl context: " + ex.getMessage());
    }

    final SSLConnectionSocketFactory sslfactory =
        new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
    final Registry<ConnectionSocketFactory> registry =
        RegistryBuilder.<ConnectionSocketFactory>create()
            .register("https", sslfactory)
            .register("http", new PlainConnectionSocketFactory())
            .build();

    final HttpClient client = HttpClientBuilder.create()
        .setUserAgent("zx-poly-emulator/2.0")
        .disableCookieManagement()
        .setConnectionManager(new BasicHttpClientConnectionManager(registry))
        .setSSLSocketFactory(sslfactory)
        .setSSLContext(sslcontext)
        .build();

    final HttpContext context = HttpClientContext.create();
    final HttpGet get = new HttpGet(url);
    get.setConfig(RequestConfig.copy(RequestConfig.DEFAULT)
        .setAuthenticationEnabled(false)
        .setRedirectsEnabled(true)
        .setRelativeRedirectsAllowed(true)
        .setConnectTimeout(2000)
        .setSocketTimeout(2000)
        .build());
    final HttpResponse response = client.execute(get, context);

    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      final HttpEntity entity = response.getEntity();
      try (final InputStream in = entity.getContent()) {
        return entity.getContentLength() < 0L ? IOUtils.toByteArray(in) :
            IOUtils.toByteArray(in, entity.getContentLength());
      }
    } else {
      throw new IOException("Can't download from http '" + url + "' code [" + url + ']');
    }
  }

  static byte[] loadFTPArchive(final String host, final String path, final String name,
                               final String password) throws IOException {
    final FTPClient client = new FTPClient();
    client.connect(host);
    int replyCode = client.getReplyCode();

    if (FTPReply.isPositiveCompletion(replyCode)) {
      try {
        client.login(name == null ? "" : name, password == null ? "" : password);
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.enterLocalPassiveMode();

        final ByteArrayOutputStream out = new ByteArrayOutputStream(300000);
        if (client.retrieveFile(path, out)) {
          return out.toByteArray();
        } else {
          throw new IOException(
              "Can't load file 'ftp://" + host + path + "\' status=" + client.getReplyCode());
        }
      } finally {
        client.disconnect();
      }
    } else {
      client.disconnect();
      throw new IOException("Can't connect to ftp '" + host + "'");
    }
  }

  public static RomData getROMFrom(final String url) throws IOException {
    final URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException ex) {
      throw new IOException("Error in URL '" + url + "\'", ex);
    }
    final String scheme = uri.getScheme();
    final String userInfo = uri.getUserInfo();
    final String name;
    final String password;
    if (userInfo != null) {
      final String[] splitted = userInfo.split("\\:", -1);
      name = splitted[0];
      password = splitted[1];
    } else {
      name = null;
      password = null;
    }

    final byte[] loaded;
    if (scheme.startsWith("http")) {
      loaded = loadHTTPArchive(url);
    } else if (scheme.startsWith("ftp")) {
      loaded = loadFTPArchive(uri.getHost(), uri.getPath(), name, password);
    } else {
      throw new IllegalArgumentException("Unsupported scheme [" + scheme + ']');
    }

    final ZipArchiveInputStream in = new ZipArchiveInputStream(new ByteArrayInputStream(loaded));

    byte[] rom48 = null;
    byte[] rom128 = null;
    byte[] romTrDos = null;

    while (!Thread.currentThread().isInterrupted()) {
      final ZipArchiveEntry entry = in.getNextZipEntry();
      if (entry == null) {
        break;
      }

      if (entry.isDirectory()) {
        continue;
      }

      if (ROM_48.equalsIgnoreCase(entry.getName())) {
        final int size = (int) entry.getSize();
        if (size > 16384) {
          throw new IOException("ROM 48 has too big size");
        }
        rom48 = new byte[16384];
        IOUtils.readFully(in, rom48, 0, size);
      } else if (ROM_128TR.equalsIgnoreCase(entry.getName())) {
        final int size = (int) entry.getSize();
        if (size > 16384) {
          throw new IOException("ROM 128TR has too big size");
        }
        rom128 = new byte[16384];
        IOUtils.readFully(in, rom128, 0, size);
      } else if (ROM_TRDOS.equalsIgnoreCase(entry.getName())) {
        final int size = (int) entry.getSize();
        if (size > 16384) {
          throw new IOException("ROM TRDOS has too big size");
        }
        romTrDos = new byte[16384];
        IOUtils.readFully(in, romTrDos, 0, size);
      }
    }

    if (rom48 == null) {
      throw new IOException(ROM_48 + " not found");
    }
    if (rom128 == null) {
      throw new IOException(ROM_128TR + " not found");
    }
    if (romTrDos == null) {
      throw new IOException(ROM_TRDOS + " not found");
    }

    return new RomData(url, rom128, rom48, romTrDos);
  }
}
