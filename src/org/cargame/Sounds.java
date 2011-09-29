package org.cargame;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;

public class Sounds {
  public static Sound death,bounce;
  
  private Sounds() {
    
  }
  
  public static void init() {
    try {
    death = new Sound("sound/death.wav");
    bounce = new Sound("sound/bounce.wav");
    } catch(SlickException e) {
      e.printStackTrace();
    }
  }
}
