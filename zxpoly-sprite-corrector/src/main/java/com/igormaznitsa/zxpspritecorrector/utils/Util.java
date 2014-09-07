package com.igormaznitsa.zxpspritecorrector.utils;

import com.igormaznitsa.zxpspritecorrector.components.EditorComponent;

public class Util {

  public static byte[] makeDataForCPU(EditorComponent _editor, int _cpuIndex) {
    byte[] ab_source = null;// _editor.getDataArray();
    byte[] ab_mask = null; //_editor.getMaskArray();
    int[] ai_color = null;//_editor.getColorArray();

    int i_length = ab_source.length;

    byte[] ab_result = new byte[i_length];

    for (int li = 0; li < i_length; li++) {
      int i_zxdata = ab_source[li] & 0xFF;
      int i_mask = ab_mask[li] & 0xFF;
      int i_colordata = ai_color[li];

      int i_acc = 0;

      for (int ld = 0; ld < 8; ld++) {
        i_acc <<= 1;

        int i_color = (i_colordata >>> 28) & 0xF;
        boolean lg_mask = (i_mask & 0x80) != 0;
        boolean lg_zx = (i_zxdata & 0x80) != 0;

        if (lg_mask) {
          // has color
          switch (_cpuIndex) {
            case 0:
              lg_zx = (i_color & 4) != 0;
              break; // R
            case 1:
              lg_zx = (i_color & 2) != 0;
              break; // G
            case 2:
              lg_zx = (i_color & 1) != 0;
              break; // B
            case 3:
              lg_zx = (i_color & 8) != 0;
              break; // Y
          }
        }

        i_acc |= (lg_zx ? 1 : 0);

        i_colordata <<= 4;
        i_mask <<= 1;
        i_zxdata <<= 1;
      }

      ab_result[li] = (byte) i_acc;
    }

    return ab_result;
  }

}
