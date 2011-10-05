package org.cargame;

import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Transform;

public class Region {
  public static final int IMPASSABLE = 1;
  public static final int OVERHEAD   = 2;
  public static final int MOVABLE    = 4;
  
  protected Polygon mTransformedPolygon, mGraphicsPolygon;
  //private Image mTexture;
  private float mScaleX,mScaleY;
  private int mFlags;
  private float mPivotX, mPivotY, mTheta, mX, mY;
  private float mVX, mVY, mDTheta;
  private String mTexKey;

  public Region() {
    this(null,null,1.0f,1.0f,IMPASSABLE,0f,0f);
  }

  // Default regions are impassable
  
  public Region(SPolygon polygon, String texKey, float scaleX, float scaleY) {
    this(polygon,texKey,scaleX,scaleY,IMPASSABLE,polygon.getCenterX(),polygon.getCenterY());
  }
  
  public Region(SPolygon polygon, String texKey, float scaleX, float scaleY, int flags) {
    this(polygon,texKey,scaleX,scaleY,flags,polygon.getCenterX(),polygon.getCenterY());
  }
  
  Region(SPolygon polygon, String texKey, float scaleX, float scaleY, int flags, float pivotX, float pivotY) {
    this.mTransformedPolygon = polygon;
    this.mTexKey = texKey;
    this.mScaleX = scaleX;
    this.mScaleY = scaleY;
    this.mFlags = flags;
    this.mPivotX = pivotX;
    this.mPivotY = pivotY;
    //mTexture.setCenterOfRotation( pivotX, pivotY );
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
    mTransformedPolygon = (Polygon)mTransformedPolygon.transform(Transform.createTranslateTransform( dx, dy ));
    mX += dx;
    mY += dy;
  }
  
  private void rotate(float theta) {
    mTransformedPolygon = (Polygon)mTransformedPolygon.transform( Transform.createRotateTransform( theta, mPivotX, mPivotY ) );
    mTheta += theta;
  }

  public boolean overlaps( Rectangle rect ) {
    return(mTransformedPolygon.intersects( rect ) || mTransformedPolygon.contains( rect ) || rect.contains( mTransformedPolygon ));
  }

  public Polygon getTransformedPolygon() {
    return mTransformedPolygon;
  }

  public float getScaleX() {
    return(mScaleX);
  }

  public float getScaleY() {
    return(mScaleY);
  }
  /*
   * General collision method, useful for checking collision against small objects 
   */
  public boolean checkForCollision( float x, float y, double radius ) {
    int numVerts = mTransformedPolygon.getPointCount();
    float points[] = mTransformedPolygon.getPoints();
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
      if (mTransformedPolygon.contains( (float)(x + nx * radius), (float)(y + ny * radius) ))
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

  public Polygon getRealPolygon() {
    return mGraphicsPolygon;
  }

  public int getFlags() {
    return mFlags;
  }
  
  public void setRawFlags( int flags ) {
    mFlags = flags;
  }

  public void setScale( float sx, float sy ) {
    if(sx > 0) mScaleX = sx;
    if(sy > 0) mScaleY = sy;
  }

  // Currently only useful in level editor
  public String getTexKey() {
    return mTexKey;
  }

  public void setTexKey(String key) {
    mTexKey = key;
  }

  public void setPolygon( SPolygon polygon ) {
    mGraphicsPolygon = polygon;
  }
}
