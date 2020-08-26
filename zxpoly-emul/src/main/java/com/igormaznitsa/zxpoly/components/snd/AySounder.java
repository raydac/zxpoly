package com.igormaznitsa.zxpoly.components.snd;

import static java.lang.Math.round;


import java.util.Arrays;

interface AySounder {
  int[] AY_AMPLITUDE = Arrays.stream(new double[] {
      0.0000d,
      0.0137d,
      0.0205d,
      0.0291d,
      0.0423d,
      0.0618d,
      0.0847d,
      0.1369d,
      0.1691d,
      0.2647d,
      0.3527d,
      0.4499d,
      0.5704d,
      0.6873d,
      0.8482d,
      1.0000d
  }).mapToInt(d -> (int) round(Beeper.AMPLITUDE_MAX * d)).toArray();

}
