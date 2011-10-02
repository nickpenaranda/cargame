package org.cargame;

import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.Image;

public class Region {

  private Polygon mPolygon;
  private Image mTexture;
  private float mScale;

  Region(Polygon polygon, Image texture, float scale) {
    this.mPolygon = polygon;
    this.mTexture = texture;
    this.mScale = scale;
  }

  public boolean overLaps( Rectangle rect ) {
    return(mPolygon.intersects( rect ) || mPolygon.contains( rect ) || rect.contains( mPolygon ));
  }

  public Polygon getPolygon() {
    return mPolygon;
  }

  public Image getTexture() {
    return mTexture;
  }

  public float getScale() {
    return(mScale);
  }

  public boolean checkForCollision( float x, float y, double radius ) {
    int numVerts = mPolygon.getPointCount();
    float points[] = mPolygon.getPoints();
    for (int i = 0; i < numVerts; i++) {
      
      // Select clockwise line segment
      float ax = points[i * 2];
      float ay = points[i * 2 + 1];
      float bx,by;
      if(i != numVerts - 1) {
        bx = points[(i + 1) * 2];
        by = points[(i + 1) * 2 + 1];
      } else {
        bx = points[0];
        by = points[1];
      }

      // Find slope,
      float dx = bx - ax;
      float dy = by - ay;

      // Calculate unit length internal normal vector
      float len = (float)Math.sqrt( dx * dx + dy * dy );
      float nx = -dy / len;
      float ny = dx / len;

      // If (x,y) + normal vector * radius projects into polygon, collision!
      if (mPolygon.contains( (float)(x + nx * radius), (float)(y + ny * radius) ))
        return true;
    }   
    return false;
  }
}
