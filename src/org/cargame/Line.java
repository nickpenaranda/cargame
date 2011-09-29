package org.cargame;

public class Line {
  public Point a,b;

  public Line(double x1, double y1, double x2, double y2) {
    a = new Point(x1,y1);
    b = new Point(x2,y2);
  }

  public Line(double x1, double y1, double x2, double y2, boolean flip) {
    if (flip) {
        a = new Point(y1,x1);
        b = new Point(y2,x2);
    } else {
        a = new Point(x1,y1);
        b = new Point(x2,y2);
    }
  }

  public double length() {
    return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
  }

  public boolean intersect(Line other) {
    return ccw(this.a,other.a,other.b) != ccw(this.b,other.a,other.b) &&
      ccw(this.a,this.b,other.a) != ccw(this.a,this.b,other.b);
  }

  static double distance(Point p1,Point p2) {
    return(Math.sqrt((p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y)));
  }

  public double distanceToPoint(Point other) {
    double l2 = Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2);
    if (l2 == 0.0)
        return distance(other, a);   // v == w case
    double t = (other.x - a.x) * (b.x - a.x) + (other.y - a.y) * (b.y - a.y);
    t /= l2;
    if (t < 0.0)
        return distance(other, a);   // v == w case
    if (t > 1.0)
        return distance(other, b);   // v == w case

    double proj_x = a.x + t * (b.x - a.x);
    double proj_y = a.y + t * (b.y - a.y);

    return distance(other, new Point(proj_x, proj_y));
  }

  public boolean intersect(double x, double y, float radius) {
    double distance = distanceToPoint(new Point(x, y));
    return distance < radius;
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
