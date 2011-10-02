package org.cargame;


public class GeoUtil {
  public static float[] rotateCW90(float x,float y) {
    return(new float[] {y, -x});
  }
}
