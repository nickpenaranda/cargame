package org.cargame;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;

public class Explosion {
  private static final int EXPLOSION_DURATION = 1000;
  public double x,y;
  public int life;
  
  public Explosion(double x,double y) {
    this.x = x;
    this.y = y;
    life = EXPLOSION_DURATION;
  }
  
  public Image getImage() {
    return(getFrame((int)(16 * (1 - (life/(float)EXPLOSION_DURATION)))));
  }
  
  private static SpriteSheet mSpriteSheet;
  
  public static void init() {
    try {
      mSpriteSheet = new SpriteSheet("gfx/explode1.png",64,64,Color.magenta);
    } catch (SlickException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private static Image getFrame(int frame) {
    return mSpriteSheet.getSprite(frame % 4, frame / 4);
  }
}
