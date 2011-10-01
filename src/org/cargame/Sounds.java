package org.cargame;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;

public class Sounds {
  public static WrappedSound death,bounce,boost,cloak,chat,rocket;
  public static boolean mute = false;
  
  private Sounds() {
    
  }
  
  public static class WrappedSound extends Sound {

    public WrappedSound(String file) throws SlickException {
      super(file);
    }

    @Override
    public void play() {
      // TODO Auto-generated method stub
      if(!Sounds.mute) 
        super.play();
    }

    @Override
    public void play(float pitch, float volume) {
      // TODO Auto-generated method stub
      if(!Sounds.mute) 
        super.play(pitch, volume);
    }
  }
  
  public static void init() {
    try {
    death = new WrappedSound("sound/death.wav");
    bounce = new WrappedSound("sound/bounce.wav");
    boost = new WrappedSound("sound/boost.wav");
    cloak = new WrappedSound("sound/cloak.wav");
    chat = new WrappedSound("sound/chat.wav");
    rocket = new WrappedSound("sound/rocket.wav");
    
    } catch(SlickException e) {
      e.printStackTrace();
    }
  }
}
