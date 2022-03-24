package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import java.io.IOException;

public class TzxBlockZeroDataLength extends AbstractTzxSoundDataBlock{

    public TzxBlockZeroDataLength(int id) {
        super(id);
    }

    @Override
    public int getDataLength() {
        return 0;
    }

    @Override
    public byte[] extractData() throws IOException {
        return new byte[0];
    }
}
