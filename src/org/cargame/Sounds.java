package org.cargame;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;

public class Sounds {
  public static Sound death,bounce,boost,cloak,chat;
  
  private Sounds() {
    
  }
  
  public static void init() {
    try {
    death = new Sound("sound/death.wav");
    bounce = new Sound("sound/bounce.wav");
    boost = new Sound("sound/boost.wav");
    cloak = new Sound("sound/cloak.wav");
    chat = new Sound("sound/chat.wav");
    } catch(SlickException e) {
      e.printStackTrace();
    }
  }
}
