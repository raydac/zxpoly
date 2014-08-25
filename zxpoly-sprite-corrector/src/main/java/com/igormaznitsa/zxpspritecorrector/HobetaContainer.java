package com.igormaznitsa.zxpspritecorrector;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.*;

public class HobetaContainer {

  @Bin
  private byte[] name;
  @Bin
  private byte[] data;

  @Bin(type = BinType.UBYTE)
  private int type;
  @Bin(type = BinType.USHORT)
  private int start;
  @Bin(type = BinType.USHORT)
  private int crc;
  @Bin(type = BinType.USHORT)
  private int sectors;

  public HobetaContainer(final File file) throws IOException {
    FileInputStream fileInputStream = null;

    try {
      fileInputStream = new FileInputStream(file);
      JBBPParser.prepare("byte [8] name; ubyte type; <ushort start; <ushort length; <ushort sectors; ushort crc; byte [length] data;").parse(fileInputStream).mapTo(this);
      if (crc != calculateHeaderCRC()) throw new IOException("Wrong file format or the file is broken");
    }
    finally {
      JBBPUtils.closeQuietly(fileInputStream);
    }

  }

  public HobetaContainer() {

  }

  public HobetaContainer(SelectFileFromTapDialog.TapItem _tapItem, byte[] _data) {
    sectors = -1;
    data = _data;
    name = _tapItem.s_Name.getBytes();
    type = (_tapItem.i_BlockType << 8) | _tapItem.i_Type;
    start = (_tapItem.i_SpecialWord1 << 16) | (_tapItem.i_SpecialWord2 & 0xFFFF);
  }

  public String getName() {
    return new String(name);
  }

  public char getType() {
    return (char) type;
  }

  public byte[] getDataArray() {
    return data;
  }

  public int getStart() {
    return start;
  }

  public int getLength() {
    return data.length;
  }

  public int getSectors() {
    return sectors;
  }

  protected int tapType2hobeta(int _type) {
    switch (_type) {
      case SelectFileFromTapDialog.TapItem.TYPE_BASIC:
        return 'B';
      case SelectFileFromTapDialog.TapItem.TYPE_BINFILE:
        return 'C';
      case SelectFileFromTapDialog.TapItem.TYPE_NUMARRAY:
      case SelectFileFromTapDialog.TapItem.TYPE_SYMARRAY:
        return 'D';
      default:
        return ' ';
    }
  }

  protected int hobeta2tapType(int _type) {
    switch (_type) {
      case 'B':
        return SelectFileFromTapDialog.TapItem.TYPE_BASIC;
      case 'C':
        return SelectFileFromTapDialog.TapItem.TYPE_BINFILE;
      case 'D':
      case '#':
        return SelectFileFromTapDialog.TapItem.TYPE_NUMARRAY;
      default:
        return SelectFileFromTapDialog.TapItem.TYPE_BINFILE;
    }
  }

  public int calculateHeaderCRC() {
    int i_acc = 0;
    for (int li = 0; li < name.length; li++) {
      i_acc += (name[li] & 0xFF);
    }
    i_acc += (sectors >= 0 ? type : tapType2hobeta(type & 0xFF));
    i_acc += (start & 0xFF);
    i_acc += (start >>> 8);
    i_acc += (data.length & 0xFF);
    i_acc += (data.length >>> 8);

    int i_sectors = sectors >= 0 ? sectors : (data.length / 256);

    i_acc += (i_sectors & 0xFF);
    i_acc += (i_sectors >>> 8);

    i_acc *= 257;
    i_acc += 105;

    i_acc &= 0xFFFF;

    i_acc = ((i_acc & 0xFF) << 8) | (i_acc >>> 8);

    return i_acc;
  }

  protected void saveAsTapHeader(DataOutputStream _out, String _name) throws IOException {
    int i_xor = 0;

    byte[] ab_name = _name.getBytes();
    if (ab_name.length > 10) {
      throw new Error("Too long name");
    }

    // length
    _out.writeByte(19);
    _out.writeByte(0);

    // header flag
    _out.writeByte(0);

    // block type
    int i_type = type & 0xFF;

    if (sectors >= 0) {
      i_type = hobeta2tapType(i_type);
    }

    // flag
    i_xor ^= i_type;
    _out.writeByte(i_type);

    // name
    for (int li = 0; li < ab_name.length; li++) {
      i_xor ^= (ab_name[li] & 0xFF);
      _out.writeByte(ab_name[li]);
    }
    for (int li = 0; li < (10 - ab_name.length); li++) {
      i_xor ^= ' ';
      _out.writeByte(' ');
    }

    // length
    i_xor ^= (data.length & 0xFF);
    i_xor ^= (data.length >>> 8);
    _out.writeByte(data.length & 0xFF);
    _out.writeByte(data.length >>> 8);

    // word1
    int i_word = start >>> 16;
    i_xor ^= i_word & 0xFF;
    i_xor ^= i_word >>> 8;
    _out.writeByte(i_word & 0xFF);
    _out.writeByte(i_word >>> 8);

    // word2
    i_word = start & 0xFFFF;
    i_xor ^= i_word & 0xFF;
    i_xor ^= i_word >>> 8;
    _out.writeByte(i_word & 0xFF);
    _out.writeByte(i_word >>> 8);

    _out.writeByte(i_xor);
  }

  protected void saveAsTapBlock(DataOutputStream _out, byte[] _data) throws IOException {
    if (_data.length != data.length) {
      throw new Error("Wrong size");
    }

    int i_len = data.length + 2;
    _out.writeByte(i_len & 0xFF);
    _out.writeByte(i_len >>> 8);

    int i_xor = 0xFF;

    _out.writeByte(0xFF);

    for (int li = 0; li < data.length; li++) {
      i_xor ^= (_data[li] & 0xFF);
      _out.write(_data[li]);
    }

    _out.writeByte(i_xor);
  }

  public void saveToStream(DataOutputStream _out) throws IOException {
    JBBPOut.BeginBin(_out).Int(start,sectors,type).Byte(name).Int(data.length).Byte(data).Flush();
  }

  public void loadFromStream(DataInputStream _in) throws IOException {
    start = _in.readInt();
    sectors = _in.readInt();
    type = _in.readInt();
    name = new byte[8];
    _in.readFully(name);
    final int len = _in.readInt();
    data = new byte[len];
    _in.readFully(data);
  }
}
