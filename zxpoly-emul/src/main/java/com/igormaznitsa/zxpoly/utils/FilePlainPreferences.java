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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

  private final Lock locker = new ReentrantLock();

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
  public void put(final String key, final String value) {
    this.locker.lock();
    try {
      this.properties.setProperty(key, value);
      this.changed = true;
      this.firePreferenceChange(key, value);
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public String get(final String key, final String def) {
    this.locker.lock();
    try {
      return this.properties.getProperty(key, def);
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public void remove(final String key) {
    this.locker.lock();
    try {
      if (this.properties.remove(key) != null) {
        this.changed = true;
        this.firePreferenceChange(key, null);
      }
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public void clear() throws BackingStoreException {
    this.locker.lock();
    try {
      var keys = this.keys();
      this.properties.clear();
      Stream.of(keys).forEach(k -> firePreferenceChange(k, null));
      if (keys.length != 0) {
        this.changed = true;
      }
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public void putInt(final String key, final int value) {
    this.locker.lock();
    try {
      this.put(key, formatter.format(value));
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public int getInt(final String key, final int def) {
    this.locker.lock();
    try {
      var value = this.properties.getProperty(key, formatter.format(def));
      try {
        return formatter.parse(value).intValue();
      } catch (ParseException ex) {
        LOGGER.log(Level.SEVERE, "Parse exception " + key, ex);
        return def;
      }
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public void putLong(final String key, final long value) {
    this.locker.lock();
    try {
      this.put(key, formatter.format(value));
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public long getLong(String key, long def) {
    this.locker.lock();
    try {
      var value = this.properties.getProperty(key, formatter.format(def));
      try {
        return formatter.parse(value).longValue();
      } catch (ParseException ex) {
        LOGGER.log(Level.SEVERE, "Parse exception " + key, ex);
        return def;
      }
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public void putBoolean(final String key, final boolean value) {
    this.locker.lock();
    try {
      this.put(key, Boolean.toString(value));
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public boolean getBoolean(final String key, final boolean def) {
    this.locker.lock();
    try {
      var value = this.properties.getProperty(key, Boolean.toString(def));
      return Boolean.parseBoolean(value.trim());
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public void putFloat(String key, float value) {
    this.locker.lock();
    try {
      this.put(key, formatter.format(value));
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public float getFloat(final String key, final float def) {
    this.locker.lock();
    try {
      var value = this.properties.getProperty(key, formatter.format(def));
      try {
        return formatter.parse(value).floatValue();
      } catch (final ParseException ex) {
        LOGGER.log(Level.SEVERE, "Parse exception " + key, ex);
        return def;
      }
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public void putDouble(final String key, final double value) {
    this.locker.lock();
    try {
      this.put(key, formatter.format(value));
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public double getDouble(String key, double def) {
    this.locker.lock();
    try {
      var value = this.properties.getProperty(key, formatter.format(def));
      try {
        return formatter.parse(value).doubleValue();
      } catch (final ParseException ex) {
        LOGGER.log(Level.SEVERE, "Parse exception " + key, ex);
        return def;
      }
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public void putByteArray(final String key, final byte[] value) {
    this.locker.lock();
    try {
      this.put(key, Base64.getEncoder().encodeToString(value));
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public byte[] getByteArray(String key, byte[] def) {
    this.locker.lock();
    try {
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
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public String[] keys() {
    this.locker.lock();
    try {
      return this.properties.keySet().toArray(String[]::new);
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public String[] childrenNames() {
    return new String[0];
  }

  @Override
  public Preferences parent() {
    return null;
  }

  @Override
  public Preferences node(final String pathName) {
    return null;
  }

  @Override
  public boolean nodeExists(final String pathName) {
    return false;
  }

  @Override
  public void removeNode() throws BackingStoreException {
    throw new BackingStoreException("Can't remove the root node");
  }

  @Override
  public String name() {
    return this.name;
  }

  @Override
  public String absolutePath() {
    return this.name;
  }

  @Override
  public boolean isUserNode() {
    return false;
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public void flush() throws BackingStoreException {
    this.locker.lock();
    try {
      if (this.changed) {
        try (var writer = new FileWriter(this.file, StandardCharsets.UTF_8)) {
          this.properties.store(writer, String.format("Properties file for '%s'", this.name));
          this.changed = false;
        } catch (IOException ex) {
          LOGGER.log(Level.SEVERE, "Can't save properties", ex);
          throw new BackingStoreException(ex);
        }
      }
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public void sync() throws BackingStoreException {
    this.locker.lock();
    try {
      try (var reader = new FileReader(this.file, StandardCharsets.UTF_8)) {
        this.properties.clear();
        this.properties.load(reader);
        this.changed = false;
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, "Can't load properties", ex);
        throw new BackingStoreException(ex);
      }
    } finally {
      this.locker.unlock();
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
  public void exportNode(final OutputStream os)
      throws IOException {
    this.locker.lock();
    try {
      this.properties.store(os, String.format("Properties for '%s'", this.name));
    } finally {
      this.locker.unlock();
    }
  }

  @Override
  public void exportSubtree(final OutputStream os) {

  }
}
