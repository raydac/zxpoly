package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import java.util.logging.Logger;

public interface ITzxBlock {
    public TzxWavRenderer.RenderResult.NamedOffsets renderBlockProcess (Logger logger, Long dataStreamCounter);
}
