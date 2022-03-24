package com.igormaznitsa.zxpoly.formats;

import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.video.VideoController;

import java.io.File;
import java.io.IOException;

public abstract class ArrayUnsaveable extends Snapshot{

    public abstract void loadFromArray(File srcFile, Motherboard board, VideoController vc,
                                       byte[] array) throws IOException;
}
