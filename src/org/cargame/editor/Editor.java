package org.cargame.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

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
  public static final int WINDOW_HALFWIDTH = WINDOW_WIDTH / 2;
  public static final int WINDOW_HALFHEIGHT = WINDOW_HEIGHT / 2;

  public static final String FONT_META = "./gfx/fonts/std14.fnt";
  public static final String FONT_IMG = "./gfx/fonts/std14_0.tga";
  public static final String FONTBOLD_META = "./gfx/fonts/std14bold.fnt";
  public static final String FONTBOLD_IMG = "./gfx/fonts/std14bold_0.tga";

  public static final Color curRegionBG = new Color(32,32,32);
  Font font,fontBold;

  public static final String TEXTURE_PATH = "./gfx/textures";
  public static final int MESSAGE_DURATION = 5000;

  static final String HELP_TEXT = "F12:Exit RMB:Pan [,]:Adj. Snap -,+:Zoom Arrows:Adj. Tex";

  int texSelectLeft = 15;
  int texSelectX = 0;
  int texSelectY = 0; 
  int texSelectTop = 55;
  int texWinPadding = 2;
  int texWinWidth = (64 + texWinPadding) * 5 + 8;
  int texWinHeight = WINDOW_HEIGHT - 105;
  int texRowSize = texWinWidth / (64 + texWinPadding);
  int texColSize = texWinHeight / (64 + texWinPadding + 15);
  int texPage = 0;
  int maxTexPage;

  float viewX = -WINDOW_WIDTH / 2, viewY = -WINDOW_HEIGHT / 2, cursorX = 0, cursorY = 0;
  float dragX = 0,dragY = 0, originX = 0, originY = 0, cursorOriginX = 0, cursorOriginY = 0;
  float zoom = 1.0f;
  int snapSize = 32;
  boolean shift, control;
  boolean autoScale = true;

  ArrayList<Region> regions;
  LinkedList<Message> messages;
  Region curRegion;
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
    NONE(""), PAN("PAN"), 
    MOVEREGIONVERT("MOVE VERTEX"), 
    MOVEREGION("MOVE REGION"),
    MOVEPENDINGVERT("MOVE VERTEX"),
    MOVEPENDING("MOVE PENDING REGION")
    ;

    String label;

    private MouseFunc(String label) {
      this.label = label;
    }

  }

  static enum EditorFunc {
    REGIONS("REGIONS", "LMB:Select  Shift+LMB:Add/move  Ctrl+LMB:Delete"),
    ITEMS("ITEMS", "???"),
    TEXTURES("TEXTURES", "LMB:Select  W,S:Page");

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
          return(EditorFunc.TEXTURES);
        case TEXTURES:
          return(EditorFunc.REGIONS);
      }
      return EditorFunc.REGIONS;
    }

  }

  EditorFunc editorFunc = EditorFunc.REGIONS;
  MouseFunc mouseFunc = MouseFunc.NONE;
  boolean snap = true, grid = true;
  float curScaleX = 1.0f, curScaleY = 1.0f;
  private boolean texOutOfBounds = false;

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
          //texKeys.add( i.getName() );
          System.out.println( "OK!" );
        }
      }
    }
    
    maxTexPage = textures.size() / (texColSize * texRowSize);
    texKeys.addAll( textures.keySet() );
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
    Rectangle clip = new Rectangle( viewX , viewY, WINDOW_WIDTH / zoom, WINDOW_HEIGHT / zoom);

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
            g.texture( region.getRealPolygon(), textures.get(region.getTexKey() ), region.getScaleX(), region
                .getScaleY(), true );
          }
          g.popTransform();
        }
        
        // Current region
        if(curRegion != null) {
          g.pushTransform(); {
            g.setColor( Color.red );
            g.draw( curRegion.getRealPolygon() );
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
          if(!autoScale)
            g.texture( pendingShape, textures.get( curTexKey ), curScaleX, curScaleY, true );
          else {
            float scaleX = pendingShape.getWidth() / 64;
            float scaleY = pendingShape.getHeight() / 64;
            g.texture( pendingShape, textures.get( curTexKey ), scaleX, scaleY, true );
          }
            
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
    
    // Texture selector
    if(editorFunc == EditorFunc.TEXTURES) {
      int i=0, j=0;
      
      g.setColor( curRegionBG );
      g.fillRoundRect( 10, 50, texWinWidth, texWinHeight, 3);
      g.setColor( Color.lightGray );
      g.drawRoundRect( 10, 50, texWinWidth, texWinHeight, 3);

      g.setColor(Color.white);
      for(Entry<String,Image> e:textures.entrySet()) {
        if(i < texRowSize * texColSize * texPage) {
          ++i;
          continue;
        }
        
        if((curRegion != null && curRegion.getTexKey() == e.getKey()) ||
           (curRegion == null && curTexKey == e.getKey())) {
          Rectangle texRect = new Rectangle(texSelectLeft + (i % texRowSize) * (64 + texWinPadding),
                                            15 + texSelectTop + (j % texColSize) * (64 + texWinPadding + 15),
                                            64,64);
          g.texture( texRect, e.getValue(),true);
          g.setColor( Color.red );
          g.drawString( e.getKey(), 
                        texSelectLeft + (i % texRowSize) * (64 + texWinPadding), 
                        texSelectTop + (j % texColSize) * (64 + texWinPadding + 15) );
          g.draw( texRect );
          g.setColor(Color.white);
        } else {
          g.drawString( e.getKey(), 
                        texSelectLeft + (i % texRowSize) * (64 + texWinPadding), 
                        texSelectTop + (j % texColSize) * (64 + texWinPadding + 15) );
          g.texture( new Rectangle(texSelectLeft + (i % texRowSize) * (64 + texWinPadding),
                                   15 + texSelectTop + (j % texColSize) * (64 + texWinPadding + 15),
                                   64,64),
                                   e.getValue(),true);
        }
        
        ++i;
        if(i > 0 && i % texRowSize == 0)
          ++j;
        
        if(i >= texRowSize * texColSize * (texPage + 1))
          break;
      }
      g.setColor( Color.yellow );
      g.drawRect( texSelectLeft + texSelectX * (64 + texWinPadding) - 1,
                  15 + texSelectTop + texSelectY * (64 + texWinPadding + 15) - 1, 
                  64, 64 );
      
    }

    // Current REGION texture, flags
    if(curRegion != null) {
      g.setColor( curRegionBG );
      g.fillRoundRect( WINDOW_WIDTH - 170, WINDOW_HEIGHT - 252, 160, 115, 3 );
      g.setColor( Color.gray );
      g.drawRoundRect( WINDOW_WIDTH - 170, WINDOW_HEIGHT - 252, 160, 115, 3 );
      g.setColor( Color.white );
      g.drawString( "Reg #" + regions.indexOf( curRegion ), WINDOW_WIDTH - 80, WINDOW_HEIGHT - 250 );
      g.drawString( curRegion.getTexKey(), WINDOW_WIDTH - 80, WINDOW_HEIGHT - 235 );
      g.drawString( curRegion.getScaleX() + " x " + curRegion.getScaleY(), WINDOW_WIDTH - 80, WINDOW_HEIGHT - 151 );
      Rectangle texView = new Rectangle( WINDOW_WIDTH - 80, WINDOW_HEIGHT - 220, 64, 64 );
      g.texture( texView, textures.get( curRegion.getTexKey() ), curRegion.getScaleX(), curRegion.getScaleY(), true );
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
    if(!autoScale)
      g.drawString( curScaleX + " x " + curScaleY, WINDOW_WIDTH - 80, WINDOW_HEIGHT - 31 );
    else
      g.drawString( "AUTOSCALE", WINDOW_WIDTH - 80, WINDOW_HEIGHT - 31 );
    
    Rectangle texView = new Rectangle( WINDOW_WIDTH - 80, WINDOW_HEIGHT - 100, 64, 64 );
    g.texture( texView, textures.get(curTexKey), curScaleX, curScaleY, true );
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
      if (region.overlaps( rect ))
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
      case Input.KEY_W:
        if (editorFunc == EditorFunc.TEXTURES) {
          if (texPage > 0)
            texPage--;
          else
            texPage = maxTexPage;
        }
        break;
      case Input.KEY_A:
        autoScale = !autoScale;
        break;
      case Input.KEY_S:
        if (editorFunc == EditorFunc.TEXTURES) {
          if(texPage < maxTexPage)
            texPage++;
          else
            texPage = 0;
        } else
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
        } else if(!autoScale) {
          if (curScaleY > 1)
            curScaleY--;
        }
        break;
      case Input.KEY_UP:
        if(curRegion != null)
          curRegion.setScale( -1, curRegion.getScaleY() + 1);
        else if(!autoScale)
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
          curTexKey = curRegion.getTexKey();
          //curScaleX = curRegion.getScaleX();
          //curScaleY = curRegion.getScaleY();
          curFlags = curRegion.getFlags();
        }
        break;
      case Input.KEY_V:
        if (curRegion != null) {
          curRegion.setTexKey(curTexKey);
          //curRegion.setScale(curScaleX,curScaleY);
          curRegion.setRawFlags(curFlags);
        }
        break;
      case Input.KEY_1:
        if (curRegion != null)
          curRegion.toggleFlag( Region.IMPASSABLE );
        else
          curFlags ^= Region.IMPASSABLE;
        break;
      case Input.KEY_2:
        if (curRegion != null)
          curRegion.toggleFlag( Region.OVERHEAD );
        else
          curFlags ^= Region.OVERHEAD;
        break;
      case Input.KEY_3:
        if (curRegion != null)
          curRegion.toggleFlag( Region.MOVABLE );
        else
          curFlags ^= Region.MOVABLE;
        break;
    }
  }

  private void nextTex() {
    if (curRegion != null) {
      int index = texKeys.indexOf( curRegion.getTexKey() );
      if (index < texKeys.size() - 1)
        curRegion.setTexKey( texKeys.get( index + 1 ) );
      else
        curRegion.setTexKey( texKeys.get( 0 ) );
    } else {
      int index = texKeys.indexOf( curTexKey );
      if (index < texKeys.size() - 1)
        curTexKey = texKeys.get( index + 1 );
      else
        curTexKey = texKeys.get( 0 );
    }
    texPage = texKeys.indexOf( curTexKey ) / (texRowSize * texColSize);
    System.out.println("texPage = " + texPage);
  }

  private void prevTex() {
    if (curRegion != null) {
      int index = texKeys.indexOf( curRegion.getTexKey() );
      if (index > 0)
        curRegion.setTexKey( texKeys.get( index - 1 ) );
      else
        curRegion.setTexKey( texKeys.get( texKeys.size() - 1 ) );
    } else {
      int index = texKeys.indexOf( curTexKey );
      if (index > 0)
        curTexKey = texKeys.get( index - 1 );
      else
        curTexKey = texKeys.get( texKeys.size() - 1 );
    }
    texPage = texKeys.indexOf( curTexKey ) / (texRowSize * texColSize);
    System.out.println("texPage = " + texPage);
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
          if (curRegion == null) {
            if (pendingShape == null) {
              pendingShape = new Polygon();
              pendingShape.addPoint( cursorX, cursorY );
            } else if(pendingShape.hasVertex( cursorX, cursorY )) {
              mouseFunc = MouseFunc.MOVEPENDINGVERT;
              dragX = cursorX;
              dragY = cursorY;
            } else if(pendingShape.contains( cursorX,cursorY )) {
              mouseFunc = MouseFunc.MOVEPENDING;
              originX = pendingShape.getX();
              originY = pendingShape.getY();
              dragX = cursorOriginX = cursorX;
              dragY = cursorOriginY = cursorY;
            } else {
              pendingShape.addPoint( cursorX, cursorY );
            }
          } else {
            if(curRegion.getRealPolygon().hasVertex( cursorX, cursorY )) {
              mouseFunc = MouseFunc.MOVEREGIONVERT;
              dragX = cursorX;
              dragY = cursorY;
            } else if(curRegion.getRealPolygon().contains( cursorX, cursorY )) {
              mouseFunc = MouseFunc.MOVEREGION;
              originX = curRegion.getX();
              originY = curRegion.getY();
              dragX = cursorOriginX = cursorX;
              dragY = cursorOriginY = cursorY;
            } else {
              curRegion.getRealPolygon().addPoint( cursorX, cursorY );
            }
          }
        } else if (button == 0 && !shift && control) { // Delete pending or delete current region
          if(pendingShape != null) {
            if(pendingShape.hasVertex( cursorX, cursorY )) {
              int delIndex = pendingShape.indexOf( cursorX, cursorY );
              Polygon n = new Polygon();
              for(int i=0;i< pendingShape.getPointCount();++i) {
                float point[] = pendingShape.getPoint( i );
                if(i != delIndex) n.addPoint( point[0], point[1] );
              }
              if(pendingShape.getPointCount() == 1)
                pendingShape = null;
              else
                pendingShape = n;
            } else {
              pendingShape = null;
            }
          }
          if(curRegion != null && curRegion.getRealPolygon().hasVertex( cursorX,cursorY )) {
            Polygon p = curRegion.getRealPolygon();
            int delIndex = p.indexOf( cursorX, cursorY ); 
            Polygon n = new Polygon();
            for(int i=0;i < p.getPointCount();++i) {
              float point[] = p.getPoint( i );
              if(i != delIndex) n.addPoint( point[0],point[1] );
            }
            if(n.getWidth() == 0 || n.getHeight() == 0 || n.getPointCount() < 3) {
              regions.remove( curRegion );
              curRegion = null;
            }
            else
              curRegion.setPolygon(n);
          } else if (curRegion != null && curRegion.getRealPolygon().contains( cursorX, cursorY )) {
            regions.remove( curRegion );
            curRegion = null;
          }
        } else if (button == 0 && !shift && !control) { // Complete or select
          if(pendingShape != null) { 
            Region region = new Region( pendingShape, curTexKey, curScaleX, curScaleY, curFlags );
            if(autoScale) {
              region.setScale( pendingShape.getWidth() / 64, pendingShape.getHeight() / 64 );
            }
            region.setTexKey( curTexKey );
            regions.add( region );
          } else {
            for(Region region:regions) {
              if(region != curRegion 
                  && (region.getRealPolygon().contains( cursorX,cursorY ) 
                      || region.getRealPolygon().includes( cursorX, cursorY )
                      || region.getRealPolygon().hasVertex( cursorX, cursorY) ) ) {
                curRegion = region;
                return;
              }
            }
            curRegion = null;
          }
          pendingShape = null;
        }
        break;
      case TEXTURES:
        if(!texOutOfBounds) {
          int index = (texColSize * texRowSize * texPage ) +
          (texSelectY * texRowSize) + texSelectX;
          if(index < texKeys.size()) {
            if(curRegion != null) {
              curRegion.setTexKey( texKeys.get( index ) );
            } else {
              curTexKey = texKeys.get( index );
              texPage = texKeys.indexOf( curTexKey ) / (texRowSize * texColSize);
            }
          }
        } else {
          editorFunc = EditorFunc.REGIONS;
        }
        break;
    }
  }

  @Override
  public void mouseMoved( int oldx, int oldy, int newx, int newy ) {
    switch(editorFunc) {
      case TEXTURES:
        if(newx - texSelectLeft > texWinWidth - 10 || newy - texSelectTop > texWinHeight - 15)
          texOutOfBounds = true;
        else
          texOutOfBounds = false;
        if(!texOutOfBounds ) {
          texSelectX = 
            Math.round( (newx - texSelectLeft) / (64 + texWinPadding) );
          texSelectY = 
            Math.round( (newy - texSelectTop) / (64 + texWinPadding + 15));
        }
        //System.out.printf("TexSelect = (%d,%d)\n",texSelectX,texSelectY);
        break;
      default:
        cursorX = viewX + newx / zoom;
        cursorY = viewY + newy / zoom;
    
        for (Region rg : regions) {
          Polygon p = rg.getRealPolygon();
          //TODO Add radius pre-checking
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
        break;
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
      case MOVEREGIONVERT: {// TODO
        dragX += (newx - oldx) / zoom;
        dragY += (newy - oldy) / zoom;
        
        Polygon p = curRegion.getRealPolygon();
        Polygon n = new Polygon();
        int moveIndex = p.indexOf( cursorX, cursorY );
        if(snap) {
          cursorX = (float)(snapSize * Math.round( dragX / (float)snapSize ));
          cursorY = (float)(snapSize * Math.round( dragY / (float)snapSize ));
        } else {
          cursorX = dragX;
          cursorY = dragY;
        }
        //System.out.printf("(%f,%f) by (%f, %f),moveIndex = %d\n",dragX,dragY,moveX,moveY,moveIndex);
        for(int i=0;i<p.getPointCount();++i) {
          float point[] = p.getPoint( i );
          if(i != moveIndex)
            n.addPoint( point[0], point[1] );
          else
            n.addPoint( cursorX, cursorY );
        }
        curRegion.setPolygon( n );
        if(autoScale)
          curRegion.setScale( curRegion.getRealPolygon().getWidth() / 64, 
                              curRegion.getRealPolygon().getHeight() / 64);
        break;
      }
      case MOVEREGION: // TODO
        dragX += (newx - oldx) / zoom;
        dragY += (newy - oldy) / zoom;
        
        if(snap) {
          cursorX = (float)(snapSize * Math.round( dragX / (float)snapSize ));
          cursorY = (float)(snapSize * Math.round( dragY / (float)snapSize ));
        } else {
          cursorX = dragX;
          cursorY = dragY;
        }

        curRegion.getRealPolygon().setX( originX + (cursorX - cursorOriginX) );
        curRegion.getRealPolygon().setY( originY + (cursorY - cursorOriginY) );
        break;
      case MOVEPENDINGVERT: {// TODO
        dragX += (newx - oldx) / zoom;
        dragY += (newy - oldy) / zoom;
        
        Polygon p = pendingShape;
        Polygon n = new Polygon();
        int moveIndex = p.indexOf( cursorX, cursorY );
        if(snap) {
          cursorX = (float)(snapSize * Math.round( dragX / (float)snapSize ));
          cursorY = (float)(snapSize * Math.round( dragY / (float)snapSize ));
        } else {
          cursorX = dragX;
          cursorY = dragY;
        }
        //System.out.printf("(%f,%f) by (%f, %f),moveIndex = %d\n",dragX,dragY,moveX,moveY,moveIndex);
        for(int i=0;i<p.getPointCount();++i) {
          float point[] = p.getPoint( i );
          if(i != moveIndex)
            n.addPoint( point[0], point[1] );
          else
            n.addPoint( cursorX, cursorY );
        }
        pendingShape = n;
        if(autoScale) {
          curScaleX = pendingShape.getWidth() / 64;
          curScaleY = pendingShape.getHeight() / 64;
        }
        break;
      }
      case MOVEPENDING: // TODO
        dragX += (newx - oldx) / zoom;
        dragY += (newy - oldy) / zoom;
        
        if(snap) {
          cursorX = (float)(snapSize * Math.round( dragX / (float)snapSize ));
          cursorY = (float)(snapSize * Math.round( dragY / (float)snapSize ));
        } else {
          cursorX = dragX;
          cursorY = dragY;
        }

        pendingShape.setX( originX + (cursorX - cursorOriginX) );
        pendingShape.setY( originY + (cursorY - cursorOriginY) );
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
