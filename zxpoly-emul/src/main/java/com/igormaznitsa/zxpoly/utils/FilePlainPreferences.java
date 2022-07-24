package com.igormaznitsa.zxpoly.utils;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

public final class FilePlainPreferences extends Preferences {
  private static final Logger LOGGER = Logger.getLogger(FilePlainPreferences.class.getName());
  private static final NumberFormat formatter = NumberFormat.getInstance(Locale.US);
  private final File file;
  private final Properties properties;

  private final String name;
  private final List<PreferenceChangeListener> preferenceChangeListeners =
      new CopyOnWriteArrayList<>();
  private final List<NodeChangeListener> nodeChangeListeners = new CopyOnWriteArrayList<>();

  private volatile boolean changed;

  public FilePlainPreferences(final String name, final File file, final boolean create)
      throws IOException {
    LOGGER.info(
        "Creating file preferences " + name + " in file " + file + ", create flag is " + create);
    this.name = requireNonNull(name);
    this.properties = new Properties();
    this.file = requireNonNull(file);
    if (file.isFile()) {
      try (var reader = new FileReader(file, StandardCharsets.UTF_8)) {
        this.properties.load(reader);
      }
    } else {
      if (create) {
        final File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
          throw new IOException("Can't create folder: " + parent);
        }
        if (!file.createNewFile()) {
          throw new IOException("Can't create file: " + file);
        }
      } else {
        throw new FileNotFoundException("Can't find properties file: " + file);
      }
    }
  }

  private void firePreferenceChange(final String key, final String newValue) {
    var event = new PreferenceChangeEvent(this, key, newValue);
    this.preferenceChangeListeners.forEach(x -> x.preferenceChange(event));
  }

  @Override
  public synchronized void put(final String key, final String value) {
    this.properties.setProperty(key, value);
    this.changed = true;
    this.firePreferenceChange(key, value);
  }

  @Override
  public synchronized String get(final String key, final String def) {
    return this.properties.getProperty(key, def);
  }

  @Override
  public synchronized void remove(final String key) {
    if (this.properties.remove(key) != null) {
      this.changed = true;
      this.firePreferenceChange(key, null);
    }
  }

  @Override
  public synchronized void clear() throws BackingStoreException {
    var keys = this.keys();
    this.properties.clear();
    Stream.of(keys).forEach(k -> firePreferenceChange(k, null));
    if (keys.length != 0) {
      this.changed = true;
    }
  }

  @Override
  public synchronized void putInt(final String key, final int value) {
    this.put(key, formatter.format(value));
  }

  @Override
  public synchronized int getInt(final String key, final int def) {
    var value = this.properties.getProperty(key, formatter.format(def));
    try {
      return formatter.parse(value).intValue();
    } catch (ParseException ex) {
      LOGGER.log(Level.SEVERE, "Parse exception " + key, ex);
      return def;
    }
  }

  @Override
  public synchronized void putLong(final String key, final long value) {
    this.put(key, formatter.format(value));
  }

  @Override
  public synchronized long getLong(String key, long def) {
    var value = this.properties.getProperty(key, formatter.format(def));
    try {
      return formatter.parse(value).longValue();
    } catch (ParseException ex) {
      LOGGER.log(Level.SEVERE, "Parse exception " + key, ex);
      return def;
    }
  }

  @Override
  public synchronized void putBoolean(final String key, final boolean value) {
    this.put(key, Boolean.toString(value));
  }

  @Override
  public synchronized boolean getBoolean(final String key, final boolean def) {
    var value = this.properties.getProperty(key, Boolean.toString(def));
    return Boolean.parseBoolean(value.trim());
  }

  @Override
  public synchronized void putFloat(String key, float value) {
    this.put(key, formatter.format(value));
  }

  @Override
  public synchronized float getFloat(final String key, final float def) {
    var value = this.properties.getProperty(key, formatter.format(def));
    try {
      return formatter.parse(value).floatValue();
    } catch (final ParseException ex) {
      LOGGER.log(Level.SEVERE, "Parse exception " + key, ex);
      return def;
    }
  }

  @Override
  public synchronized void putDouble(final String key, final double value) {
    this.put(key, formatter.format(value));
  }

  @Override
  public synchronized double getDouble(String key, double def) {
    var value = this.properties.getProperty(key, formatter.format(def));
    try {
      return formatter.parse(value).doubleValue();
    } catch (final ParseException ex) {
      LOGGER.log(Level.SEVERE, "Parse exception " + key, ex);
      return def;
    }
  }

  @Override
  public synchronized void putByteArray(final String key, final byte[] value) {
    this.put(key, Base64.getEncoder().encodeToString(value));
  }

  @Override
  public synchronized byte[] getByteArray(String key, byte[] def) {
    var value = this.properties.getProperty(key, key);
    if (value == null) {
      return def;
    }
    try {
      return Base64.getDecoder().decode(value);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Decode exception " + key, ex);
      return def;
    }
  }

  @Override
  public synchronized String[] keys() throws BackingStoreException {
    return this.properties.keySet().toArray(String[]::new);
  }

  @Override
  public synchronized String[] childrenNames() throws BackingStoreException {
    return new String[0];
  }

  @Override
  public synchronized Preferences parent() {
    return null;
  }

  @Override
  public synchronized Preferences node(final String pathName) {
    return null;
  }

  @Override
  public synchronized boolean nodeExists(final String pathName) throws BackingStoreException {
    return false;
  }

  @Override
  public synchronized void removeNode() throws BackingStoreException {
    throw new BackingStoreException("Can't remove the root node");
  }

  @Override
  public synchronized String name() {
    return this.name;
  }

  @Override
  public synchronized String absolutePath() {
    return this.name;
  }

  @Override
  public synchronized boolean isUserNode() {
    return false;
  }

  @Override
  public synchronized String toString() {
    return null;
  }

  @Override
  public synchronized void flush() throws BackingStoreException {
    if (this.changed) {
      try (var writer = new FileWriter(this.file, StandardCharsets.UTF_8)) {
        this.properties.store(writer, String.format("Properties file for '%s'", this.name));
        this.changed = false;
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, "Can't save properties", ex);
        throw new BackingStoreException(ex);
      }
    }
  }

  @Override
  public void sync() throws BackingStoreException {
    try (var reader = new FileReader(this.file, StandardCharsets.UTF_8)) {
      this.properties.clear();
      this.properties.load(reader);
      this.changed = false;
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Can't load properties", ex);
      throw new BackingStoreException(ex);
    }
  }

  @Override
  public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
    this.preferenceChangeListeners.add(pcl);
  }

  @Override
  public void removePreferenceChangeListener(final PreferenceChangeListener pcl) {
    this.preferenceChangeListeners.remove(pcl);
  }

  @Override
  public void addNodeChangeListener(final NodeChangeListener ncl) {
    this.nodeChangeListeners.add(ncl);
  }

  @Override
  public void removeNodeChangeListener(final NodeChangeListener ncl) {
    this.nodeChangeListeners.remove(ncl);
  }

  @Override
  public synchronized void exportNode(final OutputStream os)
      throws IOException, BackingStoreException {
    this.properties.store(os, String.format("Properties for '%s'", this.name));
  }

  @Override
  public void exportSubtree(final OutputStream os) throws IOException, BackingStoreException {

  }
}
