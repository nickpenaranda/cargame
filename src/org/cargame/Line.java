package org.cargame;

public class Line {
  public Point a,b;

  public Line(double x1, double y1, double x2, double y2) {
    a = new Point(x1,y1);
    b = new Point(x2,y2);
  }
  
  public boolean intersect(Line other) {
    return ccw(this.a,other.a,other.b) != ccw(this.b,other.a,other.b) &&
      ccw(this.a,this.b,other.a) != ccw(this.a,this.b,other.b);
  }
  
  public static class Point {
    public double x,y;
    
    public Point(double x,double y) {
      this.x = x; this.y = y;
    }
  }
  
  private static boolean ccw(Point a,Point b,Point c) {
    return (c.y-a.y) * (b.x-a.x) > (b.y-a.y) * (c.x-a.x);
    
  }
}
