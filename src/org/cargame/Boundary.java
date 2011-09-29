package org.cargame;

import java.io.Serializable;

public class Boundary extends Line implements Serializable {

  private static final long serialVersionUID = -7506671174053054681L;
  public int mType;

  public Boundary(double x1, double y1, double x2, double y2,int type, boolean flip) {
    super(x1, y1, x2, y2, flip);
    mType = type;
  }

  public Boundary(double x1, double y1, double x2, double y2,int type) {
    super(x1, y1, x2, y2);
    mType = type;
  }
  
  public Boundary(Point a,Point b,int type) {
    super(a.x,a.y,b.x,b.y);
    mType = type;
  }

  public Boundary(Line l,int type) {
    super(l.a.x,l.a.y,l.b.x,l.b.y);
    mType = type;
  }
}
