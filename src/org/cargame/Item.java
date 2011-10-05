package org.cargame;


public class Item {
  private static final int NONE = 0;
  private static final int VIS_EDITOR_ONLY = 1;

  public static enum Type {
    START("Player Start", null, new String[] { "p1" }, VIS_EDITOR_ONLY),

    JAMMER("Jamming Computer", new Collectable() {
      public void onPickup( Item item, Car op, World world ) {
        // Destroy item, give car jammer ability
      }
    }, new String[] { "jammer1", "jammer2" }, NONE);   
    
    private Collectable collectable;
    private String[] spriteKeys;
    private String label;
    private int flags;
    
    private Type(String label, Collectable c, String[] spriteKeys, int flags) {
      this.label = label;
      this.spriteKeys = spriteKeys;
      this.collectable = c;
      this.flags = flags;
    }
    
    public boolean isCollectable() {
      return(collectable != null);
    }
    
    public String getLabel() {
      return(label);
    }
    
    public Collectable getCollectable() {
      return(collectable);
    }
    
    public String getSpriteKey(int time) {
      if(spriteKeys == null) {
        return("none");
      }
      int phase = time % 1000;
      int numFrames = spriteKeys.length;
      int frame = (int)(numFrames * (phase / (float)1000));
      return(spriteKeys[frame]);
    }
    
    public String[] getSpriteKeys() {
      return spriteKeys;
    }
    
    public int getFlags() {
      return(flags);
    }
  }
    
  private Type type;
  private float mX, mY, mVX, mVY, mAngle, mTheta;
  
  public Item() {
    this(null,0f,0f,0f,0f,0f,0f);
  }
  
  public Item(Type type,float x,float y, float vx, float vy, float angle, float theta) {
    this.type = type;
    this.mX = x;
    this.mY = y;
    this.mVX = vx;
    this.mVY = vy;
    this.mAngle = angle;
    this.mTheta = theta;
  }
  
  public Type getType() {
    return type;
  }

  public float getX() {
    return mX;
  }

  public float getY() {
    return mY;
  }

  public float getVX() {
    return mVX;
  }

  public float getVY() {
    return mVY;
  }

  public float getAngle() {
    return mAngle;
  }

  public float getTheta() {
    return mTheta;
  }

  public void setX( float x ) {
    mX = x;
  }

  public void setY( float y ) {
    mY = y;
  }

  public void setVX( float vX ) {
    mVX = vX;
  }
  
  public void setVY( float vY ) {
    mVY = vY;
  }

  public void setAngle( float angle ) {
    mAngle = angle;
  }

  public void setTheta( float theta ) {
    mTheta = theta;
  }

//  public static void main(String[] args) {
//    Random r = new Random();
//    Item item = new Item(Type.START,0f,0f,0f,0f,0f,0f);
//    for(int i=0;i<3000;i+=r.nextInt(12)+4) {
//      System.out.println("i = " + i + " : " + item.getType().getSpriteKey( i ));
//    }
//  }
}
