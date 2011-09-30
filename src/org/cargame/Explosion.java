package org.cargame;

import java.util.Random;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;

public class Explosion {
  private static final Random r = new Random();
  private static final int EXPLOSION_DURATION = 1000;
  public double x,y;
  public int life;
  private int variant;
  
  public Explosion(double x,double y) {
    this.x = x;
    this.y = y;
    life = EXPLOSION_DURATION;
    variant = r.nextInt(4);
  }
  
  public Image getImage() {
    return(getFrame(variant,(int)(16 * (1 - (life/(float)EXPLOSION_DURATION)))));
  }
  
  private static SpriteSheet[] mSpriteSheet;
  
  public static void init() {
    mSpriteSheet = new SpriteSheet[4];
    
    try {
      for(int i=0;i<4;i++) {
        mSpriteSheet[i] = new SpriteSheet("gfx/explode" + (i + 1) + ".png",64,64,Color.magenta);
      }
    } catch (SlickException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private static Image getFrame(int variant, int frame) {
    return mSpriteSheet[variant].getSprite(frame % 4, frame / 4);
  }
}
