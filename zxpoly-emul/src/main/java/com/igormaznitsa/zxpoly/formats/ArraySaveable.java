package com.igormaznitsa.zxpoly.formats;

import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.video.VideoController;

import java.io.IOException;

public abstract class ArraySaveable extends Snapshot{
    public abstract byte[] saveToArray(Motherboard board, VideoController vc) throws IOException;
}
