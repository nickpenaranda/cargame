package org.cargame.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.AngelCodeFont;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;

import org.cargame.CarGame;
import org.cargame.Region;

public class Editor extends BasicGame {

  public static final String WINDOW_TITLE = "CarGame Map Editor";
  public static final int WINDOW_WIDTH = 640;
  public static final int WINDOW_HEIGHT = 480;

  public static final String FONT_META = "./gfx/fonts/std14.fnt";
  public static final String FONT_IMG = "./gfx/fonts/std14_0.tga";

  private Font font;

  public static final String TEXTURE_PATH = "./gfx/textures";
  public static final int MESSAGE_DURATION = 5000;

  private static final String HELP_TEXT = "F12: Exit RMB: Pan [,]: Adj. Snap -,+: Zoom";

  private float viewX = -WINDOW_WIDTH / 2, viewY = -WINDOW_HEIGHT / 2, cursorX = 0, cursorY = 0;
  private float zoom = 1.0f;
  private int snapSize = 32;
  private boolean shift, control;

  private ArrayList<Region> regions;
  private LinkedList<Message> messages;
  private Region curRegion;
  private Image curTexture;
  private String curTexKey;
  
  private Polygon pendingShape;

  private TreeMap<String, Image> textures;
  private ArrayList<String> texKeys;
  
  private GameContainer container;
  private String filename;
  
  static class Message {
    String text;
    Color color;
    int life;
    public Message(String text,Color color) {
      this.text = text;
      this.color = color;
      this.life = MESSAGE_DURATION;
    }
  }

  static enum MouseFunc {
    NONE(""), PAN("PAN"), MOVE("MOVE");

    String label;

    private MouseFunc(String label) {
      this.label = label;
    }

  }

  static enum EditorFunc {
    REGIONS("REGIONS", "LMB:Add Ctrl+LMB:Finish Shift+LMB:Move"), ITEMS("ITEMS", "???");

    String label;
    String mouseHelp;

    private EditorFunc(String label, String mouseHelp) {
      this.label = label;
      this.mouseHelp = mouseHelp;
    }

    /*
     * Kosher? Prob not.
     */
    public static EditorFunc next( EditorFunc editorFunc ) {
      switch (editorFunc) {
        case REGIONS:
          return(EditorFunc.ITEMS);
        case ITEMS:
          return(EditorFunc.REGIONS);
      }
      return EditorFunc.REGIONS;
    }

  }

  private EditorFunc editorFunc = EditorFunc.REGIONS;
  private MouseFunc mouseFunc = MouseFunc.NONE;
  private boolean snap = false;
  private float curScale = 1.0f;

  public Editor() {
    super( WINDOW_TITLE );
  }

  @Override
  public void init( GameContainer container ) throws SlickException {
    this.container = container;
    regions = new ArrayList<Region>();
    messages = new LinkedList<Message>();
    textures = new TreeMap<String, Image>();
    texKeys = new ArrayList<String>();
    
    System.out.println( "Loading textures:" );
    File textureDir = new File( TEXTURE_PATH );

    File[] texFileList = textureDir.listFiles();

    if (texFileList == null) {
      System.err.println( "Texture directory not found: " + TEXTURE_PATH );
      System.exit( -1 );
    } else {
      for (File f : texFileList) {
        String path = f.getPath();
        System.out.print( "  " + path + ": " );
        Image i = null;
        try {
          i = new Image( path );
        } catch (SlickException e) {
          System.out.println( "ERROR--Texture not loaded: " + e.getMessage() );
        }

        if (i == null) {
          System.out.println( "ERROR--Texture not loaded: Unknown cause." );
        } else {
          i.setName( f.getName().substring( 0, f.getName().length() - 4 ) );
          textures.put( i.getName(), i );
          texKeys.add( i.getName() );
          System.out.println( "OK!" );
        }
      }
    }
    
    curTexture = textures.firstEntry().getValue();
    curTexKey = textures.firstKey();
    
    font = new AngelCodeFont( FONT_META, FONT_IMG );
  }

  @Override
  public void update( GameContainer container, int delta ) throws SlickException {
    for(Message m:new LinkedList<Message>(messages)) {
      m.life -= delta;
      if(m.life <= 0)
        messages.remove(m);
    }
  }

  @Override
  public void render( GameContainer container, Graphics g ) throws SlickException {
    g.setFont( font );
    Rectangle clip = new Rectangle( viewX, viewY, WINDOW_WIDTH, WINDOW_HEIGHT );

    float sx = (float)(snapSize * Math.ceil( viewX / (float)snapSize ));
    float sy = (float)(snapSize * Math.ceil( viewY / (float)snapSize ));
    float adjusted_win_width = WINDOW_WIDTH / zoom;
    float adjusted_win_height = WINDOW_HEIGHT / zoom;

    g.pushTransform(); {
      g.scale( zoom, zoom );
      g.pushTransform(); {
        g.translate( -viewX, -viewY );

        // Grid
        g.setColor( Color.darkGray );
        for (float x = sx; x < viewX + adjusted_win_width; x += snapSize)
          g.drawLine( x, viewY, x, viewY + adjusted_win_height );

        for (float y = sy; y < viewY + adjusted_win_height; y += snapSize)
          g.drawLine( viewX, y, viewX + adjusted_win_width, y );


        for (Region region : getWallsWithin( clip )) {
          if (region == curRegion)
            g.setColor( Color.cyan );
          else
            g.setColor( Color.white );
  
          g.pushTransform(); {
            g.translate( region.getX(), region.getY() );
            g.rotate( region.getPivotX(), region.getPivotY(),
                      (float)(region.getTheta() * 180 / Math.PI) );
            g.texture( region.getGraphicsPolygon(), region.getTexture(), region.getScale(), region
                .getScale(), true );
          } g.popTransform();
        }
        
        if(pendingShape != null) {
          g.setColor( Color.yellow );
          for(int i=0;i<pendingShape.getPointCount();++i) {
            float p[] = pendingShape.getPoint( i );
            g.drawRect(p[0]-1,p[1]-1,2,2);
          }
          g.setColor( Color.red );
          g.draw( pendingShape );
        }
      } g.popTransform();

      g.pushTransform(); {
        g.translate( -viewX, -viewY );

        // Cursor
        g.setColor( Color.cyan );
        g.drawLine( cursorX - 4, cursorY - 4, cursorX + 4, cursorY + 4 );
        g.drawLine( cursorX - 4, cursorY + 4, cursorX + 4, cursorY - 4 );
      } g.popTransform();
    } g.popTransform();

    // Current texture
    g.setColor( Color.white );
    g.drawString( "Cur. tex", WINDOW_WIDTH - 80, WINDOW_HEIGHT - 130 );
    g.drawString( curTexKey + " " + curScale, WINDOW_WIDTH - 80, WINDOW_HEIGHT - 115 );
    Rectangle texView = new Rectangle(WINDOW_WIDTH - 80, WINDOW_HEIGHT - 100,64,64);
    g.texture( texView, curTexture,curScale,curScale,true);
    g.draw( texView ); // Border for contrast
    
    // Persistent help
    g.drawString( HELP_TEXT, 15, WINDOW_HEIGHT - 20 );

    // Contextual info and cues
    g.setColor( Color.white );
    g.drawString( "MODE: " + editorFunc.label, 15, 15 );
    if (mouseFunc != MouseFunc.NONE)
      g.drawString( "DRAG: " + mouseFunc.label, 15, 30 );
    else
      g.drawString( editorFunc.mouseHelp, 15, 30 );

    // Mouse coords and snap
    String location = String.format( "(%.2f, %.2f)", cursorX, cursorY );
    g.drawString( location, WINDOW_WIDTH - 15 - g.getFont().getWidth( location ), 15 );

    String snapinfo = snap ? String.format( "Snap: %d", snapSize ) : "Snap: OFF";
    g.drawString( snapinfo, WINDOW_WIDTH - 15 - g.getFont().getWidth( snapinfo ), 30 );
  }

  public List<Region> getWallsWithin( Rectangle rect ) {
    LinkedList<Region> within = new LinkedList<Region>();
    for (Region region : regions) {
      if (region.overLaps( rect ))
        within.add( region );
    }
    return within;
  }

  @Override
  public void keyPressed( int key, char c ) {
    switch (key) {
      case Input.KEY_LSHIFT:
      case Input.KEY_RSHIFT:
        shift = true;
        break;
      case Input.KEY_LCONTROL:
      case Input.KEY_RCONTROL:
        control = true;
        break;

      case Input.KEY_F12:
        System.exit( 0 );
        break;
      case Input.KEY_S:
        snap = !snap;
        break;
      case Input.KEY_LBRACKET:
        if (snapSize > 8)
          snapSize = snapSize >> 1;
        break;
      case Input.KEY_RBRACKET:
        snapSize = snapSize << 1;
        break;
      case Input.KEY_MINUS:
        zoom -= 0.1f;
        break;
      case Input.KEY_EQUALS:
        zoom += 0.1f;
        break;
      case Input.KEY_TAB:
        editorFunc = EditorFunc.next( editorFunc );
        break;
      case Input.KEY_UP:
        curScale++;
        break;
      case Input.KEY_DOWN:
        if(curScale > 1) curScale--;
        break;
      case Input.KEY_LEFT:
        prevTex();
        break;
      case Input.KEY_RIGHT:
        nextTex();
        break;
    }
  }

  private void nextTex() {
    int index = texKeys.indexOf( curTexKey );
    if(index < texKeys.size() - 1)
      curTexKey = texKeys.get( index + 1 );
    else
      curTexKey = texKeys.get( 0 );
    curTexture = textures.get( curTexKey );
  }

  private void prevTex() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void keyReleased( int key, char c ) {
    switch (key) {
      case Input.KEY_LSHIFT:
      case Input.KEY_RSHIFT:
        shift = false;
        break;
      case Input.KEY_LCONTROL:
      case Input.KEY_RCONTROL:
        control = false;
        break;
    }
  }

  @Override
  public void mousePressed( int button, int x, int y ) {
    switch (button) {
      case 0: // Left
        handleClick( button );
        break;
      case 1: // Right?
        mouseFunc = MouseFunc.PAN;
        break;
    }
  }

  private void handleClick( int button ) {
    switch (editorFunc) {
      case REGIONS:
        if (button == 0 && !shift && !control) {
          if (pendingShape == null) {
            pendingShape = new Polygon();
          }
          pendingShape.addPoint( cursorX, cursorY );
        } else if (button == 0 && !shift && control) {
          pendingShape = null;
        } else if (button == 0 && shift && !control) {
          Region region = new Region(pendingShape,curTexture,curScale);
          regions.add( region );
          pendingShape = null;
        }
        break;
    }
  }

  @Override
  public void mouseMoved( int oldx, int oldy, int newx, int newy ) {
    cursorX = viewX + newx / zoom;
    cursorY = viewY + newy / zoom;

    for (Region rg : regions) {
      Polygon p = rg.getPolygon();
      for (int i = 0; i < p.getPointCount(); ++i) {
        float pt[] = p.getPoint( i );
        if (CarGame.distance( cursorX, cursorY, pt[0], pt[1] ) < 8) {
          cursorX = pt[0];
          cursorY = pt[1];
          return;
        }
      }
    }
    if (snap) {
      cursorX = (float)(snapSize * Math.round( cursorX / (float)snapSize ));
      cursorY = (float)(snapSize * Math.round( cursorY / (float)snapSize ));
    }
  }

  @Override
  public void mouseReleased( int button, int x, int y ) {
    mouseFunc = MouseFunc.NONE;
  }

  @Override
  public void mouseDragged( int oldx, int oldy, int newx, int newy ) {
    switch (mouseFunc) {
      case PAN:
        viewX -= (newx - oldx) / zoom;
        viewY -= (newy - oldy) / zoom;
        break;
      case MOVE: // TODO
        break;
      case NONE: // TODO
        break;
    }
  }

  public static void main( String[] args ) {
    try {
      AppGameContainer container = new AppGameContainer( new Editor() );
      container.setDisplayMode( 640, 480, false );
      container.setShowFPS( false );
      container.setMinimumLogicUpdateInterval( 100 );
      container.start();
    } catch (SlickException e) {
      e.printStackTrace();
    }
  }
}
