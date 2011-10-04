package org.cargame.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

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
import org.cargame.CarGame;
import org.cargame.Region;

public class Editor extends BasicGame {

  public static final String WINDOW_TITLE = "CarGame Map Editor";
  public static final int WINDOW_WIDTH = 800;
  public static final int WINDOW_HEIGHT = 600;

  public static final String FONT_META = "./gfx/fonts/std14.fnt";
  public static final String FONT_IMG = "./gfx/fonts/std14_0.tga";
  public static final String FONTBOLD_META = "./gfx/fonts/std14bold.fnt";
  public static final String FONTBOLD_IMG = "./gfx/fonts/std14bold_0.tga";

  public static final Color curRegionBG = new Color(32,32,32);
  Font font,fontBold;

  public static final String TEXTURE_PATH = "./gfx/textures";
  public static final int MESSAGE_DURATION = 5000;

  static final String HELP_TEXT = "F12:Exit RMB:Pan [,]:Adj. Snap -,+:Zoom Arrows:Adj. Tex";

  float viewX = -WINDOW_WIDTH / 2, viewY = -WINDOW_HEIGHT / 2, cursorX = 0, cursorY = 0;
  float zoom = 1.0f;
  int snapSize = 32;
  boolean shift, control;

  ArrayList<Region> regions;
  LinkedList<Message> messages;
  Region curRegion;
  Image curTexture;
  String curTexKey;
  int curFlags;

  Polygon pendingShape;

  TreeMap<String, Image> textures;
  ArrayList<String> texKeys;

  GameContainer container;
  String filename;

  static class Message {

    String text;
    Color color;
    int life;

    public Message(String text, Color color) {
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

  EditorFunc editorFunc = EditorFunc.REGIONS;
  MouseFunc mouseFunc = MouseFunc.NONE;
  boolean snap = true, grid = true;
  float curScaleX = 1.0f, curScaleY = 1.0f;

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
    curFlags = Region.IMPASSABLE;

    font = new AngelCodeFont( FONT_META, FONT_IMG );
    fontBold = new AngelCodeFont( FONTBOLD_META, FONTBOLD_IMG );
  }

  @Override
  public void update( GameContainer container, int delta ) throws SlickException {
    for (Message m : new LinkedList<Message>( messages )) {
      m.life -= delta;
      if (m.life <= 0)
        messages.remove( m );
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

    g.pushTransform();
    {
      g.scale( zoom, zoom );
      g.pushTransform();
      {
        g.translate( -viewX, -viewY );

        // Grid
        if (grid) {
          g.setColor( Color.darkGray );
          for (float x = sx; x < viewX + adjusted_win_width; x += snapSize)
            g.drawLine( x, viewY, x, viewY + adjusted_win_height );

          for (float y = sy; y < viewY + adjusted_win_height; y += snapSize)
            g.drawLine( viewX, y, viewX + adjusted_win_width, y );
        }
        // Existing geometry
        g.setColor( Color.white );
        for (Region region : getWallsWithin( clip )) {
          g.pushTransform();
          {
            g.translate( region.getX(), region.getY() );
            g.rotate( region.getPivotX(), region.getPivotY(),
                      (float)(region.getTheta() * 180 / Math.PI) );
            g.texture( region.getGraphicsPolygon(), region.getTexture(), region.getScaleX(), region
                .getScaleY(), true );
          }
          g.popTransform();
        }
        
        // Current region
        if(curRegion != null) {
          g.pushTransform(); {
            g.setColor( Color.red );
            g.draw( curRegion.getGraphicsPolygon() );
          } g.popTransform();
        }

        // Pending
        if (pendingShape != null) {
          g.setColor( Color.yellow );
          for (int i = 0; i < pendingShape.getPointCount(); ++i) {
            float p[] = pendingShape.getPoint( i );
            g.drawRect( p[0] - 1, p[1] - 1, 2, 2 );
          }
          g.setColor( Color.white );
          g.texture( pendingShape, curTexture, curScaleX, curScaleY, true );
          g.setColor( Color.red );
          g.draw( pendingShape );
        }
      }
      g.popTransform();

      g.pushTransform();
      {
        g.translate( -viewX, -viewY );

        // Cursor
        g.setColor( Color.cyan );
        g.drawLine( cursorX - 4, cursorY - 4, cursorX + 4, cursorY + 4 );
        g.drawLine( cursorX - 4, cursorY + 4, cursorX + 4, cursorY - 4 );
      }
      g.popTransform();
    }
    g.popTransform();

    // Current REGION texture, flags
    if(curRegion != null) {
      g.setColor( curRegionBG );
      g.fillRoundRect( WINDOW_WIDTH - 170, WINDOW_HEIGHT - 252, 160, 115, 3 );
      g.setColor( Color.gray );
      g.drawRoundRect( WINDOW_WIDTH - 170, WINDOW_HEIGHT - 252, 160, 115, 3 );
      g.setColor( Color.white );
      g.drawString( "Reg. tex", WINDOW_WIDTH - 80, WINDOW_HEIGHT - 250 );
      g.drawString( curRegion.getTexKey(), WINDOW_WIDTH - 80, WINDOW_HEIGHT - 235 );
      g.drawString( curRegion.getScaleX() + " x " + curRegion.getScaleY(), WINDOW_WIDTH - 80, WINDOW_HEIGHT - 151 );
      Rectangle texView = new Rectangle( WINDOW_WIDTH - 80, WINDOW_HEIGHT - 220, 64, 64 );
      g.texture( texView, curRegion.getTexture(), curRegion.getScaleX(), curRegion.getScaleY(), true );
      g.draw( texView ); // Border for contrast
  
      g.setFont( fontBold );
      if (curRegion.hasFlag( Region.IMPASSABLE )) {
        g.setColor( Color.red );
        g.drawString( "IMPASSABLE", WINDOW_WIDTH - 85 - g.getFont().getWidth( "IMPASSABLE" ),
                      WINDOW_HEIGHT - 220 );
      }
      if (curRegion.hasFlag( Region.OVERHEAD )) {
        g.setColor( Color.blue );
        g.drawString( "OVERHEAD", WINDOW_WIDTH - 85 - g.getFont().getWidth( "OVERHEAD" ),
                      WINDOW_HEIGHT - 205 );
      }
      if (curRegion.hasFlag( Region.MOVABLE )) {
        g.setColor( Color.green );
        g.drawString( "MOVABLE", WINDOW_WIDTH - 85 - g.getFont().getWidth( "MOVABLE" ),
                      WINDOW_HEIGHT - 190 );
      }
    }
    
    g.setFont( font );
    
    // Current texture, flags
    g.setColor( Color.white );
    g.drawString( "Cur. tex", WINDOW_WIDTH - 80, WINDOW_HEIGHT - 130 );
    g.drawString( curTexKey, WINDOW_WIDTH - 80, WINDOW_HEIGHT - 115 );
    g.drawString( curScaleX + " x " + curScaleY, WINDOW_WIDTH - 80, WINDOW_HEIGHT - 31 );
    Rectangle texView = new Rectangle( WINDOW_WIDTH - 80, WINDOW_HEIGHT - 100, 64, 64 );
    g.texture( texView, curTexture, curScaleX, curScaleY, true );
    g.draw( texView ); // Border for contrast

    g.setFont( fontBold );
    if ((curFlags & Region.IMPASSABLE) == Region.IMPASSABLE) {
      g.setColor( Color.red );
      g.drawString( "IMPASSABLE", WINDOW_WIDTH - 85 - g.getFont().getWidth( "IMPASSABLE" ),
                    WINDOW_HEIGHT - 100 );
    }
    if ((curFlags & Region.OVERHEAD) == Region.OVERHEAD) {
      g.setColor( Color.blue );
      g.drawString( "OVERHEAD", WINDOW_WIDTH - 85 - g.getFont().getWidth( "OVERHEAD" ),
                    WINDOW_HEIGHT - 85 );
    }
    if ((curFlags & Region.MOVABLE) == Region.MOVABLE) {
      g.setColor( Color.green );
      g.drawString( "MOVABLE", WINDOW_WIDTH - 85 - g.getFont().getWidth( "MOVABLE" ),
                    WINDOW_HEIGHT - 70 );
    }
    
    g.setFont( font );
    // Persistent help
    g.setColor( Color.white );
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
    
    String zoominfo = String.format( "Zoom: %.0f%%", (1 / zoom) * 100 );
    g.drawString( zoominfo, WINDOW_WIDTH - 15- g.getFont().getWidth( zoominfo), 45);
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
      case Input.KEY_G:
        grid = !grid;
        break;
      case Input.KEY_LBRACKET:
        if (snapSize > 8)
          snapSize = snapSize >> 1;
        break;
      case Input.KEY_RBRACKET:
        snapSize = snapSize << 1;
        break;
      case Input.KEY_MINUS: // Zoom out
        if(zoom > 0.1f) {
          zoom -= 0.1f;
          viewX = cursorX - (WINDOW_WIDTH / 2) / zoom;
          viewY = cursorY - (WINDOW_HEIGHT / 2) / zoom;
        }
        break;
      case Input.KEY_EQUALS: // Zoom in
        zoom += 0.1f;
        viewX = cursorX - (WINDOW_WIDTH / 2) / zoom;
        viewY = cursorY - (WINDOW_HEIGHT / 2) / zoom;
        break;
      case Input.KEY_0:
        zoom = 1.0f;
        viewX = cursorX - (WINDOW_WIDTH / 2) / zoom;
        viewY = cursorY - (WINDOW_HEIGHT / 2) / zoom;
        break;
        
      case Input.KEY_TAB:
        editorFunc = EditorFunc.next( editorFunc );
        break;
      case Input.KEY_DOWN:
        if (curRegion != null) {
          if (curRegion.getScaleY() > 1)
            curRegion.setScale( -1 , curRegion.getScaleY() - 1 );
        } else {
          if (curScaleY > 1)
            curScaleY--;
        }
        break;
      case Input.KEY_UP:
        if(curRegion != null)
          curRegion.setScale( -1, curRegion.getScaleY() + 1);
        else
          curScaleY++;
        break;
      case Input.KEY_LEFT:
        if (curRegion != null) {
          if (curRegion.getScaleX() > 1)
            curRegion.setScale( curRegion.getScaleX() - 1, -1 );
        } else {
          if (curScaleX > 1)
            curScaleX--;
        }
        break;
      case Input.KEY_RIGHT:
        if(curRegion != null)
          curRegion.setScale( curRegion.getScaleX() + 1, -1 );
        else
          curScaleX++;
        break;
      case Input.KEY_COMMA:
        prevTex();
        break;
      case Input.KEY_PERIOD:
        nextTex();
        break;
      case Input.KEY_C:
        if (curRegion != null) {
          curTexture = curRegion.getTexture();
          curScaleX = curRegion.getScaleX();
          curScaleY = curRegion.getScaleY();
          curFlags = curRegion.getFlags();
        }
        break;
      case Input.KEY_V:
        if (curRegion != null) {
          curRegion.setTexture(curTexture);
          curRegion.setScale(curScaleX,curScaleY);
          curRegion.setRawFlags(curFlags);
        }
      case Input.KEY_1:
        curFlags ^= Region.IMPASSABLE;
        break;
      case Input.KEY_2:
        curFlags ^= Region.OVERHEAD;
        break;
      case Input.KEY_3:
        curFlags ^= Region.MOVABLE;

    }
  }

  private void nextTex() {
    if (curRegion != null) {
      int index = texKeys.indexOf( curRegion.getTexKey() );
      if (index < texKeys.size() - 1)
        curRegion.setTexKey( texKeys.get( index + 1 ) );
      else
        curRegion.setTexKey( texKeys.get( 0 ) );
      curRegion.setTexture( textures.get( curRegion.getTexKey() ) );
    } else {
      int index = texKeys.indexOf( curTexKey );
      if (index < texKeys.size() - 1)
        curTexKey = texKeys.get( index + 1 );
      else
        curTexKey = texKeys.get( 0 );
      curTexture = textures.get( curTexKey );
    }
  }

  private void prevTex() {
    if (curRegion != null) {
      int index = texKeys.indexOf( curRegion.getTexKey() );
      if (index > 0)
        curRegion.setTexKey( texKeys.get( index - 1 ) );
      else
        curRegion.setTexKey( texKeys.get( texKeys.size() - 1 ) );
      curRegion.setTexture( textures.get( curRegion.getTexKey() ) );
    } else {
      int index = texKeys.indexOf( curTexKey );
      if (index > 0)
        curTexKey = texKeys.get( index - 1 );
      else
        curTexKey = texKeys.get( texKeys.size() - 1 );
      curTexture = textures.get( curTexKey );
    }
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
        if (button == 0 && shift && !control) { // Add/move points
          if (pendingShape == null) {
            pendingShape = new Polygon();
          }
          pendingShape.addPoint( cursorX, cursorY );
        } else if (button == 0 && !shift && control) { // Delete pending or delete current region
          pendingShape = null;
          if(curRegion != null && curRegion.getPolygon().contains( cursorX,cursorY )) {
            regions.remove( curRegion );
            curRegion = null;
          }
        } else if (button == 0 && !shift && !control) { // Complete or select
          if(pendingShape != null) {
            Region region = new Region( pendingShape, curTexture, curScaleX, curScaleY, curFlags );
            region.setTexKey( curTexKey );
            regions.add( region );
          } else {
            for(Region region:regions) {
              if(region != curRegion 
                  && (region.getPolygon().contains( cursorX,cursorY ) 
                      || region.getPolygon().includes( cursorX, cursorY )
                      || region.getPolygon().hasVertex( cursorX, cursorY) ) ) {
                curRegion = region;
                return;
              }
            }
            curRegion = null;
          }
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
      container.setDisplayMode( WINDOW_WIDTH, WINDOW_HEIGHT, false );
      container.setShowFPS( false );
      container.setMinimumLogicUpdateInterval( 100 );
      container.start();
    } catch (SlickException e) {
      e.printStackTrace();
    }
  }
}
