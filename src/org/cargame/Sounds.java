package org.cargame;

import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;

public class Sounds {
  public static final int MAX_DISTANCE = 5000;
  
  public static World world;
  public static WorldSound death, bounce, boost, cloak, rocket;
  public static WrappedSound chat;
  public static boolean mute = false;

  private Sounds() {

  }

  public static class WrappedSound extends Sound {

    public WrappedSound(String file) throws SlickException {
      super( file );
    }

    @Override
    public void play() {
      // TODO Auto-generated method stub
      if (!Sounds.mute)
        super.play();
    }

    @Override
    public void play( float pitch, float volume ) {
      // TODO Auto-generated method stub
      if (!Sounds.mute)
        super.play( pitch, volume );
    }
  }

  public static class WorldSound extends Sound {

    public WorldSound(String file) throws SlickException {
      super( file );
    }
    
    @Override
    public void play( float pitch, float volume ) {
      // TODO Auto-generated method stub
      if (!Sounds.mute)
        super.play( pitch, volume );
    }
    
    public void playWorld(double x,double y) {
      playWorld(1.0f, 1.0f, x , y ); // To prevent divide by 0
    }
    
    public void playWorld( float pitch, float volume, double x, double y) {
      double dx = x - world.getPlayer().getX();
      double dy = y - world.getPlayer().getY();
      double dist = Math.sqrt( dx * dx + dy * dy );
      float vol = 1 - (float)(dist/MAX_DISTANCE);
      if(vol < 0)
        vol = 0;
      vol = (float) Math.pow( vol, 6 );
      System.out.println("Playing sound at " + vol + " volume");
      if (!Sounds.mute)
        play(pitch, vol);
    }
  }
  
  public static void init( World aWorld ) {
    world = aWorld;
    try {
      death = new WorldSound( "sound/death.wav" );
      bounce = new WorldSound( "sound/bounce.wav" );
      boost = new WorldSound( "sound/boost.wav" );
      cloak = new WorldSound( "sound/cloak.wav" );
      rocket = new WorldSound( "sound/rocket.wav" );
      chat = new WrappedSound( "sound/chat.wav" );

    } catch (SlickException e) {
      e.printStackTrace();
    }
  }
}
