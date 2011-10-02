package org.cargame;

import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.geom.Transform;
import org.newdawn.slick.Image;

public class Region {
  public static final int IMPASSABLE = 1;
  public static final int OVERHEAD   = 2;
  public static final int MOVABLE    = 4;
  
  protected Polygon mPolygon, mGraphicsPolygon;
  private Image mTexture;
  private float mScale;
  private int mFlags;
  private float mPivotX, mPivotY, mTheta, mX, mY;
  private float mVX, mVY, mDTheta;

  // Default regions are impassable
  public Region(Polygon polygon, Image texture, float scale) {
    this(polygon,texture,scale,IMPASSABLE,polygon.getCenterX(),polygon.getCenterY());
  }
  
  public Region(Polygon polygon, Image texture, float scale, int flags) {
    this(polygon,texture,scale,flags,polygon.getCenterX(),polygon.getCenterY());
  }
  
  Region(Polygon polygon, Image texture, float scale, int flags, float pivotX, float pivotY) {
    this.mPolygon = polygon;
    this.mTexture = texture;
    this.mScale = scale;
    this.mFlags = flags;
    this.mPivotX = pivotX;
    this.mPivotY = pivotY;
    mTexture.setCenterOfRotation( pivotX, pivotY );
    mTheta = 0;
    mGraphicsPolygon = polygon;
  }
  
  public void setVelocity(float vx,float vy) {
    mVX = vx;
    mVY = vy;
  }
  
  public void setRotationRate(float dTheta) {
    mDTheta = dTheta;
  }
  
  public void doMovement(int delta) {
    translate(mVX * delta, mVY * delta);
    rotate(mDTheta * delta);
  }
  
  private void translate(float dx,float dy) {
    mPolygon = (Polygon)mPolygon.transform(Transform.createTranslateTransform( dx, dy ));
    mX += dx;
    mY += dy;
  }
  
  private void rotate(float theta) {
    mPolygon = (Polygon)mPolygon.transform( Transform.createRotateTransform( theta, mPivotX, mPivotY ) );
    mTheta += theta;
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

  /*
   * General collision method, useful for checking collision against small objects 
   */
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
  
  public boolean hasFlag(int flagmask) {
    return (mFlags & flagmask) == flagmask;
  }
  
  public void setFlag(int flagmask, boolean state) {
    if(state)
      mFlags |= flagmask;
    else
      mFlags &= flagmask;
  }
    
  public boolean toggleFlag(int flagmask) {
    mFlags ^= flagmask;
    return((mFlags & flagmask) == flagmask);
  }

  
  public float getPivotX() {
    return mPivotX;
  }

  
  public float getPivotY() {
    return mPivotY;
  }

  
  public float getVX() {
    return mVX;
  }

  
  public float getVY() {
    return mVY;
  }

  public double getDTheta() {
    return mDTheta;
  }

  public float getTheta() {
    return mTheta;
  }
  
  public float getX() {
    return mX;
  }
  
  public float getY() {
    return mY;
  }

  public Polygon getGraphicsPolygon() {
    return mGraphicsPolygon;
  }
}
