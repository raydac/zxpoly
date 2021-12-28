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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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
import java.util.Locale;
import java.util.Set;

public class RomLoader {

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
      sslcontext.init(null, new TrustManager[]{tm}, null);
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

  private static RomData extractFromTarGz(final String url, final Set<String> rom48names, final Set<String> rom128names, final Set<String> trdosNames, final byte[] archive) throws IOException {
    try (final ArchiveInputStream archiveStream = new TarArchiveInputStream(new GzipCompressorInputStream(new ByteArrayInputStream(archive)))) {
      return extractFromArchive(url, rom48names, rom128names, trdosNames, archiveStream);
    }
  }

  private static RomData extractFromTarXz(final String url, final Set<String> rom48names, final Set<String> rom128names, final Set<String> trdosNames, final byte[] archive) throws IOException {
    try (final ArchiveInputStream archiveStream = new TarArchiveInputStream(new XZCompressorInputStream(new ByteArrayInputStream(archive)))) {
      return extractFromArchive(url, rom48names, rom128names, trdosNames, archiveStream);
    }
  }

  private static RomData extractFromDeb(final String url, final Set<String> rom48names, final Set<String> rom128names, final Set<String> trdosNames, final byte[] archive) throws IOException {
    byte[] data = null;
    try (final ArchiveInputStream archiveStream = new ArArchiveInputStream(new ByteArrayInputStream(archive))) {
      while (!Thread.currentThread().isInterrupted()) {
        final ArchiveEntry entry = archiveStream.getNextEntry();
        if (entry == null) {
          break;
        }

        if (!archiveStream.canReadEntryData(entry) || entry.isDirectory()) {
          continue;
        }

        if (entry.getName().equalsIgnoreCase("data.tar.xz")) {
          data = IOUtils.readFully(archiveStream, (int) entry.getSize());
          break;
        }
      }
    }
    if (data == null) {
      throw new IOException("Can't find data.tar.xz in deb archive: " + url);
    } else {
      return extractFromTarXz(url, rom48names, rom128names, trdosNames, data);
    }
  }

  private static RomData extractFromZip(final String url, final Set<String> rom48names, final Set<String> rom128names, final Set<String> trdosNames, final byte[] archive) throws IOException {
    try (final ArchiveInputStream archiveStream = new ZipArchiveInputStream(new ByteArrayInputStream(archive))) {
      return extractFromArchive(url, rom48names, rom128names, trdosNames, archiveStream);
    }
  }

  private static RomData extractFromArchive(final String url, final Set<String> rom48names, final Set<String> rom128names, final Set<String> trdosNames, final ArchiveInputStream archiveStream) throws IOException {
    byte[] rom48 = null;
    byte[] rom128 = null;
    byte[] romTrDos = null;

    while (!Thread.currentThread().isInterrupted()) {
      final ArchiveEntry entry = archiveStream.getNextEntry();
      if (entry == null) {
        break;
      }

      if (!archiveStream.canReadEntryData(entry) || entry.isDirectory()) {
        continue;
      }

      String normalizedEntryName = entry.getName().trim().toLowerCase(Locale.ENGLISH).replace('\\', '/');

      final int lastFolderChar = normalizedEntryName.lastIndexOf('/');
      if (lastFolderChar >= 0) {
        normalizedEntryName = normalizedEntryName.substring(lastFolderChar + 1);
      }

      if (rom48names.contains(normalizedEntryName)) {
        final int size = (int) entry.getSize();
        if (size > 16384) {
          throw new IOException("ROM 48 has too big size");
        }
        rom48 = new byte[16384];
        IOUtils.readFully(archiveStream, rom48, 0, size);
      } else if (rom128names.contains(normalizedEntryName)) {
        final int size = (int) entry.getSize();
        if (size > 16384) {
          throw new IOException("ROM 128 has too big size");
        }
        rom128 = new byte[16384];
        IOUtils.readFully(archiveStream, rom128, 0, size);
      } else if (trdosNames.contains(normalizedEntryName)) {
        final int size = (int) entry.getSize();
        if (size > 16384) {
          throw new IOException("ROM TR-DOS has too big size");
        }
        romTrDos = new byte[16384];
        IOUtils.readFully(archiveStream, romTrDos, 0, size);
      }
    }

    if (rom48 == null) {
      throw new IOException("Rom 48 not found");
    }

    if (rom128 == null) {
      throw new IOException("Rom 128 not found");
    }

    if (romTrDos == null) {
      return new RomData(url, rom128, rom48);
    } else {
      return new RomData(url, rom128, rom48, romTrDos);
    }
  }

  public static RomData getROMFrom(final String url, final Set<String> rom48names, final Set<String> rom128names, final Set<String> trdosNames) throws IOException {
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

    final String resourceInLowerCase = url.trim().toLowerCase(Locale.ENGLISH);
    if (resourceInLowerCase.endsWith(".zip")) {
      return extractFromZip(url, rom48names, rom128names, trdosNames, loaded);
    } else if (resourceInLowerCase.endsWith(".tar.gz")) {
      return extractFromTarGz(url, rom48names, rom128names, trdosNames, loaded);
    } else if (resourceInLowerCase.endsWith(".deb")) {
      return extractFromDeb(url, rom48names, rom128names, trdosNames, loaded);
    } else {
      throw new IOException("Can't process resource extension: " + url);
    }
  }
}
