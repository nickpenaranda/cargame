package org.cargame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

public class CarGame extends BasicGame {

  public static final boolean _DEBUG_MODE = true;

  public static final String WINDOW_TITLE = "CAR GAME, SON";

  // These are potentially set to different values in constructor (via Sys Param)
  public static boolean multiplayerMode = false;
  public static String playerName = "Player";
  public static String hostName = "192.168.1.113";

  public static Random r;

  private World mWorld;
  
  private Car player;

  private List<BoostGhost> mGhosts;
  
  private List<Message> mMessages;

  private StringBuffer mChatBuffer;
  private boolean mChatMode;

  private GameContainer mContainer;
  private GameClient mGameClient;
  
  private int renderDelta = 0;
  private long lastRender = 0, ghostTimeout = 0;

  public CarGame() {
    super( WINDOW_TITLE );
  }

  @Override
  public void init( GameContainer container ) throws SlickException {
    // Set some fields from VM properties
    String player_name = System.getProperty( "cargame.player_name" );
    if (playerName != null)
      playerName = player_name;

    String host_name = System.getProperty( "cargame.host_name" );
    if (host_name != null)
      hostName = host_name;

    String mmode = System.getProperty( "cargame.multiplayer_mode" );
    if (mmode != null)
      multiplayerMode = Boolean.valueOf( mmode );
    else
      multiplayerMode = false;

    r = new Random();
    mGhosts = new ArrayList<BoostGhost>();

    // In general, any collection referenced by GameClient needs to be synchronized
    mMessages = (List<Message>)Collections.synchronizedList( new ArrayList<Message>() );

    mChatBuffer = new StringBuffer( "" );
    mChatMode = false;

    mContainer = container;
    
    // These init() functions MUST be called from within a Game init()
    Car.init();
    Explosion.init();
    Rocket.init();
    Engine.init( this );

    mWorld = new World(this);
    Car.attachTo(mWorld);
    Sounds.init( this.mWorld );

    if (multiplayerMode) {
      System.out.println( "MULTIPLAYER ENABLED" );
      try {
        mGameClient = new GameClient( this );
        mWorld.getCars().put( mGameClient.getPlayerId(), mWorld.getPlayer() );
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else
      mWorld.getCars().put( 0, mWorld.getPlayer() );
    
    player = mWorld.getPlayer();
    lastRender = ghostTimeout = System.nanoTime() / 1000000;
  }

  @Override
  public void update( GameContainer container, int delta ) throws SlickException {
    mWorld.update(delta);
    
    Car player = mWorld.getPlayer();

    // Send updates if applicable
    if (multiplayerMode) {
      mGameClient.sendMoveUpdate( player.getX(), player.getY(), player.getVX(),
                                  player.getVY(), player.getAngle(), 
                                  player.getThrustT(), player.getThrustR(), 
                                  player.getThrustB(), player.getThrustL() );
    }
    
    if (player.isDead() && player.getDeadCount() < 0) {
      player.restore();
      if (multiplayerMode)
        mGameClient.sendStateUpdate( Network.STATE_DEAD, false );
    }

    else if (player.isDead())
      return;

    // Check collision player car vs other cars
    ArrayList<Car> otherCars = new ArrayList<Car>( mWorld.getCars().values() );
    otherCars.remove( player );
    for (Car other : otherCars) {
      if (CarGame.distance( player.getX(), player.getY(), other.getX(), other.getY() ) < 64
          && Math.abs( player.getSpeed() ) < Math.abs( other.getSpeed() )) {
        
        player.kill();
        if (multiplayerMode)
          mGameClient.sendStateUpdate( Network.STATE_DEAD, true );

        message( "RUN OVER BY " + other.getName() );
      }
    }

    // Player only: collision vs walls
    mWorld.checkPlayerCollision(delta);
    
    // If stuck, die
    if(player.getStuckCount() > 200) {// Stuck for 200ms
      player.kill();
      if (multiplayerMode)
        mGameClient.sendStateUpdate( Network.STATE_DEAD, true);
    }
  }

  public static double distance( double x1, double y1, double x2, double y2 ) {
    return(Math.sqrt( (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) ));
  }

  /*
   * Moved rendering code out of this class; see Renderer.java
   */
  @Override
  public void render( GameContainer container, Graphics g ) throws SlickException {
    long now = System.nanoTime() / 1000000;
    renderDelta = (int)(now - lastRender);
    lastRender = now;
    
    ghostTimeout += renderDelta;
    if(ghostTimeout >= 75) {
      for(Car c:new ArrayList<Car>(mWorld.getCars().values())) {
        if(c.getBoostTimeout() > 2000) {
          mGhosts.add( new BoostGhost(c.getX(),c.getY(),c.getAngle(),c.getImage()) );
        }
      }
      ghostTimeout = 0;
    }
    
    for(BoostGhost gh:new ArrayList<BoostGhost>(mGhosts)) {
      gh.life -= renderDelta;
      if(gh.life <= 0)
        mGhosts.remove( gh );
    }
    
    for(Message m:new ArrayList<Message>(mMessages)) {
      m.life -= renderDelta;
      if(m.life <= 0)
        mMessages.remove( m );
    }
    
    Engine.render( container, g );
  }

  @Override
  public void keyPressed( int key, char c ) {
    if (mChatMode) {
      switch (key) {
        case Input.KEY_ESCAPE: // Close chat mode without sending, do not clear
          // buffer
          mChatMode = false;
          break;
        case Input.KEY_ENTER: // Send message, clear buffer, close chat mode
          if (multiplayerMode && mChatBuffer.length() > 0)
            mGameClient.sendChatMessage( mChatBuffer.toString() );
          mChatBuffer.delete( 0, mChatBuffer.length() );
          mChatMode = false;
          break;
        case Input.KEY_BACK:
          if (mChatBuffer.length() > 0)
            mChatBuffer.deleteCharAt( mChatBuffer.length() - 1 );
          break;
        default:
          if (c >= 32 && c <= 126) // Sane characters
            mChatBuffer.append( c );
          break;
      }
    } else {
      switch (key) {
        // Non player control stuff
        case Input.KEY_ESCAPE:
          System.exit( 0 );
          break;

        // Player control stuff
        case Input.KEY_W:
          player.setBooster( Car.BOTTOM, true );
          break;
        case Input.KEY_S:
          player.setBooster( Car.TOP, true );
          break;
        case Input.KEY_A:
          player.setBooster( Car.RIGHT, true );
          break;
        case Input.KEY_D:
          player.setBooster( Car.LEFT, true );
          break;
        case Input.KEY_Q:
          if (player.jammer() && multiplayerMode)
            mGameClient.sendStateUpdate( Network.STATE_JAM, true );
          break;
        case Input.KEY_SPACE:
          if (player.boost() && multiplayerMode)
            mGameClient.sendStateUpdate( Network.STATE_BOOST, true );
          break;
        case Input.KEY_F1:
          try {
            mContainer.setFullscreen( !mContainer.isFullscreen() );
          } catch (SlickException e) {
            e.printStackTrace();
          }
          break;
        case Input.KEY_F2:
          Sounds.mute = !Sounds.mute;
          message( "SOUNDS " + (Sounds.mute ? "OFF" : "ON") );
          break;
        case Input.KEY_F3:
          mWorld.movingRegions = !mWorld.movingRegions;
          message( "REGION MOVEMENT " + (mWorld.movingRegions ? "ENABLED" : "FROZEN"));
          break;
        case Input.KEY_K:
          player.kill();
          break;
        case Input.KEY_ENTER:
          mChatMode = true;
          break;
      }
    }
  }

  @Override
  public void keyReleased( int key, char c ) {
    switch (key) {
      // Player control stuff
      case Input.KEY_W:
        player.setBooster( Car.BOTTOM, false );
        break;
      case Input.KEY_S:
        player.setBooster( Car.TOP, false );
        break;
      case Input.KEY_A:
        player.setBooster( Car.RIGHT, false );
        break;
      case Input.KEY_D:
        player.setBooster( Car.LEFT, false );
        break;
    }
  }

  @Override
  public void mouseMoved( int oldx, int oldy, int newx, int newy ) {
    int x = newx - 320;
    int y = newy - 240;

    if (x > 0)
      player.setAngle( Math.atan( y / (double)x ) + Math.PI / 2 );
    else if (x < 0)
      player.setAngle( Math.atan( y / (double)x ) + Math.PI + Math.PI / 2 );
    else {
      if (y < 0)
        player.setAngle( 0 );
      else
        player.setAngle( Math.PI );
    }
  }
  
  

  @Override
  public void mousePressed( int button, int x, int y ) {
    switch (button) {
      case 0: // Left
        player.rocket();
        break;
    }
  }

  // private void flatLine(int offset, int start, int end, boolean horiz, int
  // val) {
  // boolean random = val == -1;
  // int y = offset;
  // for (int x = start; x <= end; x++) {
  // if (random) {
  // val = r.nextInt(4) + 1;
  // }
  // if (horiz)
  // mMap[x][y] = val;
  // else
  // mMap[y][x] = val;
  // }
  // }

  public static void main( String[] args ) {
    try {
      AppGameContainer appGameContainer = new AppGameContainer( new CarGame() );
      appGameContainer.setDisplayMode( Engine.WINDOW_WIDTH, Engine.WINDOW_HEIGHT, !_DEBUG_MODE );
      appGameContainer.setMinimumLogicUpdateInterval( 20 );
      appGameContainer.setAlwaysRender( true );
      appGameContainer.setTargetFrameRate( 100 );
      appGameContainer.setVSync( true );
      appGameContainer.start();
    } catch (SlickException e) {
      e.printStackTrace();
    }
  }

  public GameClient getClient() {
    return(mGameClient);
  }

  public void message( String string ) {
    mMessages.add(0, new Message(string) );
  }
  
  public void message( String string, Color color ) {
    mMessages.add(0, new Message(string, color));
  }

  public StringBuffer getChatBuffer() {
    return(mChatBuffer);
  }

  public boolean chatModeEnabled() {
    return(mChatMode);
  }

  public List<Message> getMessages() {
    return(mMessages);
  }

  public World getWorld() {
    // TODO Auto-generated method stub
    return (mWorld);
  }

  public List<BoostGhost> getGhosts() {
    // TODO Auto-generated method stub
    return mGhosts;
  }
}
