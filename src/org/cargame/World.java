package org.cargame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;

public class World {

  private static final Random r = new Random();

  public static final int EXPLOSION_DENSITY = 5;

  // World generation constants
  public static final int ROAD_WIDTH = 6;
  public static final int BUILDING_WIDTH = 10;
  public static final int TILE_SIZE = 64;

  private static final float PLAYER_RADIUS = 32;

  private CarGame mGame;

  private HoverCraft mPlayer;
  private Map<Integer, HoverCraft> mCrafts;

  private List<Region> mRegions;

  private Image[] mTiles;

  private List<Rocket> mRockets;
  private List<Explosion> mExplosions;

  public boolean movingRegions = true;

  public World(CarGame game) {
    mGame = game;

    mTiles = new Image[5];

    // TODO Put tiles in a Map, access by name (e.g., wall1)
    try {
      mTiles[0] = new Image( "gfx/road.png" );
      mTiles[1] = new Image( "gfx/wall1.png" );
      mTiles[2] = new Image( "gfx/wall2.png" );
      mTiles[3] = new Image( "gfx/wall3.png" );
      mTiles[4] = new Image( "gfx/wall4.png" );
    } catch (SlickException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    mRegions = new LinkedList<Region>();

    mCrafts = (Map<Integer, HoverCraft>)Collections
        .synchronizedMap( new TreeMap<Integer, HoverCraft>() );

    mRockets = (List<Rocket>)Collections.synchronizedList( new ArrayList<Rocket>() );
    mExplosions = new ArrayList<Explosion>();

    genWorldMap();

    mPlayer = new HoverCraft( 0, 0, 0, CarGame.playerName );
    mPlayer.moveToSpawn();

  }

  public void update( int delta ) {
    // Transform any MOVABLE regions
    if(movingRegions) {
      for (Region rg : mRegions) {
        if(rg.hasFlag( Region.MOVABLE ))
          rg.doMovement( delta );
      }
    }
    
    // Apply input/physics for vehicles
    for (HoverCraft c : mCrafts.values()) {
      c.think( delta );
    }

    // Age effects, e.g. explosions
    for (Explosion e : new ArrayList<Explosion>( mExplosions )) {
      e.life -= delta;
      if (e.life <= 0)
        mExplosions.remove( e );
    }

    // Rocket physics
    for (Rocket rk : new ArrayList<Rocket>( mRockets )) {
      rk.life -= delta;
      if (rk.life <= 0) {
        mRockets.remove( rk );
        createExplosion( rk.x, rk.y );
      } else {
      
        boolean collide = false;

        rk.x += rk.vx * delta;
        rk.y += rk.vy * delta;
  
        // Check collision vs player car
        // Only interested in others' rockets
        for (HoverCraft other : new ArrayList<HoverCraft>( mCrafts.values() )) {
          if (other == rk.owner) // No collision with owner
            continue;
  
          // Contact with other
          if (CarGame.distance( rk.x, rk.y, other.getX(), other.getY() ) < 32) {
            // If player, report kill
            if (other == mPlayer) {
              mGame.message( "BLOWN UP BY " + rk.owner.getName() );
              mPlayer.kill();
  
              if (CarGame.multiplayerMode)
                mGame.getClient().sendStateUpdate( Network.STATE_DEAD, true );
            }
  
            // In any case, remove rocket and create explosion
            System.out.println( "Rocket collide with a player" );
            mRockets.remove( rk );
            createExplosion( rk.x, rk.y );
            collide = true;
            break;
          }
        }

        // If no player collisions, check vs walls...
        // Collision vs walls
        if(!collide) {
          for (Region r : mRegions) {
            if (r.checkForCollision( (float)rk.x, (float)rk.y, 16 )) {
              mRockets.remove( rk );
              createExplosion( rk.x, rk.y );
              break;
            }
          }
        }
      }
    }
  }

  /*
   * This method is separate because it must happen after other local client specific checks done in
   * CarGame.java
   */
  public void checkPlayerCollision( int delta ) {
    float x = mPlayer.getX();
    float y = mPlayer.getY();
    for (Region rg : mRegions) {
      if(rg.hasFlag( Region.IMPASSABLE )) {
        Polygon p = rg.getPolygon();
        float cdx = p.getCenterX() - x;
        float cdy = p.getCenterY() - y;
        if (p.getBoundingCircleRadius() + PLAYER_RADIUS >= Math.sqrt( cdx * cdx + cdy * cdy )) {
  
          //System.out.println( "In range of poly" );
          int numVerts = p.getPointCount();
          float points[] = p.getPoints();
  
          // Check all verts first
          for (int i = 0; i < numVerts; i++) {
            float ax = points[i * 2];
            float ay = points[i * 2 + 1];
            
            float adx = ax - x;
            float ady = ay - y;
            if(Math.sqrt( adx * adx + ady * ady) < PLAYER_RADIUS) {
              float n[] = p.getNormal( i );
              mPlayer.bounce( new Line(ax + n[1],ay - n[0] ,ax - n[1],ay + n[0]), rg, delta );
              Sounds.bounce.play( (float)(1 + r.nextGaussian() / 5), 1.0f );
            }
          }
          
          // Then check segments
          for (int i = 0; i < numVerts; i++) {
            // Select clockwise line segment
            float ax = points[i * 2];
            float ay = points[i * 2 + 1];
            
            float bx, by;
            if (i != numVerts - 1) {
              bx = points[(i + 1) * 2];
              by = points[(i + 1) * 2 + 1];
            } else {
              bx = points[0];
              by = points[1];
            }
  
            // Find slope,
            float dx = bx - ax;
            float dy = by - ay;
  
            // Calculate unit length internal normal vector
            float len = (float)Math.sqrt( dx * dx + dy * dy );
            float nx = -dy / len;
            float ny = dx / len;
  
            // If (x,y) + normal vector * radius projects into polygon, collision!
            if (p.contains( (float)(x + nx * PLAYER_RADIUS), (float)(y + ny * PLAYER_RADIUS) )) {
              //System.out.printf("Bouncing off of (%.2f,%.2f) -> (%.2f,%.2f)\n",ax,ay,bx,by);
              mPlayer.bounce( new Line( ax, ay, bx, by ), rg, delta );
              Sounds.bounce.play( (float)(1 + r.nextGaussian() / 5), 1.0f );
            }
          }
        }
      }
    }
  }

  // TODO: Do collisions against mWorldMap instead of mWalls
  // for (int i = 0; i < mWalls.size(); i++) {
  // if (mWalls.get( i ).intersect( mPlayer.getX(), mPlayer.getY(), 31 )) {
  // mPlayer.bounce( mWalls.get( i ), delta );
  // Sounds.bounce.play( (float)(1 + r.nextGaussian() / 5), 1.0f );
  // }
  // }

  public void createExplosion( double x, double y ) {
    for (int i = 0; i < EXPLOSION_DENSITY; i++)
      mExplosions.add( new Explosion( x + 32 * r.nextGaussian(), y + 32 * r.nextGaussian() ) );
    mExplosions.add( new Explosion( x, y ) );
  }

  public void createRocket( HoverCraft owner, double x, double y, double vx, double vy ) {
    Rocket rk = new Rocket( owner, x, y, vx, vy );
    mRockets.add( rk );
    Sounds.rocket.play();
    if (CarGame.multiplayerMode && owner == mPlayer)
      mGame.getClient().sendRocket( rk );
  }

  private void genWorldMap() {
    // Set tiles
    int numBuildingsAcross = 16;
    int cityBlockWidth = ROAD_WIDTH + BUILDING_WIDTH;
    int offset = numBuildingsAcross / 2; // center city at (0, 0)

    Image textures[] = new Image[8];
    for (int i = 0; i < textures.length; i++) {
      textures[i] = genRandomTexture( 8, 8 );
    }

    for (int i = 0; i < numBuildingsAcross; i++) {
      int buildingL = ((i - offset) * cityBlockWidth + ROAD_WIDTH) * TILE_SIZE;
      for (int j = 0; j < numBuildingsAcross; j++) {
        int buildingT = ((j - offset) * cityBlockWidth + ROAD_WIDTH) * TILE_SIZE;
        Rectangle rect = new Rectangle( buildingL, buildingT, BUILDING_WIDTH * TILE_SIZE,
            BUILDING_WIDTH * TILE_SIZE );
        Region region = new Region( new Polygon( rect.getPoints() ), textures[r.nextInt( 8 )], 1.0f );
        if(!CarGame.multiplayerMode) {
          if(r.nextBoolean()) {
            region.setFlag( Region.MOVABLE, true );
            if(r.nextBoolean()) 
              region.setVelocity( 0.01f * (float)r.nextGaussian(), 0.01f  * (float)r.nextGaussian());
            else
              region.setRotationRate( 0.001f * (float)r.nextGaussian() );
          }
        }
        addRegion( region );
      }
      
    }
    //TODO This is just for demo
    if(!CarGame.multiplayerMode){ 
      mGame.message( "!!!MOVING REGION TEST ACTIVE!!! F3 to toggle" );
    }

    // Set boundaries
    // mWalls.add( new Boundary( -8192, -8192, 8192, -8192, 1 ) );
    // mWalls.add( new Boundary( -8192, -8192, -8192, 8192, 1 ) );
    // mWalls.add( new Boundary( 8192, -8192, 8192, 8192, 1 ) );
    // mWalls.add( new Boundary( -8192, 8192, 8192, 8192, 1 ) );

    // for (int x = 1; x <= 16; ++x) {
    // for (int y = 1; y <= 16; ++y) {
    // mWalls.add( new Boundary( -8192 + (ROAD_WIDTH * 64 * x) + (BUILDING_WIDTH * 64 * (x - 1)),
    // -8192 + (ROAD_WIDTH * 64 * y) + (BUILDING_WIDTH * 64 * (y - 1)), -8192
    // + (ROAD_WIDTH * 64 * x) + (BUILDING_WIDTH * 64 * (x)), -8192 + (ROAD_WIDTH * 64 * y)
    // + (BUILDING_WIDTH * 64 * (y - 1)), 1 ) );
    // mWalls.add( new Boundary( -8192 + (ROAD_WIDTH * 64 * x) + (BUILDING_WIDTH * 64 * (x - 1)),
    // -8192 + (ROAD_WIDTH * 64 * y) + (BUILDING_WIDTH * 64 * (y - 1)), -8192
    // + (ROAD_WIDTH * 64 * x) + (BUILDING_WIDTH * 64 * (x - 1)), -8192
    // + (ROAD_WIDTH * 64 * y) + (BUILDING_WIDTH * 64 * (y)), 1 ) );
    // mWalls.add( new Boundary( -8192 + (ROAD_WIDTH * 64 * x) + (BUILDING_WIDTH * 64 * (x)), -8192
    // + (ROAD_WIDTH * 64 * y) + (BUILDING_WIDTH * 64 * (y - 1)), -8192 + (ROAD_WIDTH * 64 * x)
    // + (BUILDING_WIDTH * 64 * (x)),
    // -8192 + (ROAD_WIDTH * 64 * y) + (BUILDING_WIDTH * 64 * (y)), 1 ) );
    // mWalls.add( new Boundary( -8192 + (ROAD_WIDTH * 64 * x) + (BUILDING_WIDTH * 64 * (x - 1)),
    // -8192 + (ROAD_WIDTH * 64 * y) + (BUILDING_WIDTH * 64 * (y)), -8192 + (ROAD_WIDTH * 64 * x)
    // + (BUILDING_WIDTH * 64 * (x)), -8192 + (ROAD_WIDTH * 64 * y)
    // + (BUILDING_WIDTH * 64 * (y)), 1 ) );
    // }
    // }
    int numRoads = 256 / cityBlockWidth;
    int numBarriers = r.nextInt( 40 ) + 10;

    // Remove to reenable barriers once sending to server works.
    numBarriers = 0;
    for (int i = 0; i < numBarriers; i++) {
      // specifices intersection: (0, 0) topleft
      int barrierX = r.nextInt( numRoads - 1 );
      int barrierY = r.nextInt( numRoads - 1 );
      boolean barrierHoriz = r.nextInt( 2 ) == 1;
      System.out.println( barrierX + " " + barrierY + " " + barrierHoriz );

      int barrierTop = barrierX * cityBlockWidth + cityBlockWidth / 2 + ROAD_WIDTH / 2 - 1;
      int barrierLeft = barrierY * cityBlockWidth;
      int barrierRight = barrierY * cityBlockWidth + ROAD_WIDTH - 1;
      // flatLine(barrierBottom, barrierLeft, barrierRight, barrierHoriz, -1);
      // flatLine(barrierTop, barrierLeft, barrierRight, barrierHoriz, -1);
      int boundStartX = tileToPixel( barrierLeft );
      int boundStartY = tileToPixel( barrierTop );
      int boundEndX = tileToPixel( barrierRight );
      int boundEndY = tileToPixel( barrierTop );
      // mWalls.add( new Boundary( boundStartX, boundStartY, boundEndX + 64, boundEndY, 1,
      // !barrierHoriz ) );
      // mWalls.add( new Boundary( boundStartX, boundStartY + 128, boundEndX + 64, boundEndY + 128,
      // 1,
      // !barrierHoriz ) );
    }
  }

  // Generates a map with grid roads.
  private static int tileToPixel( int tileNum ) {
    return -8192 + tileNum * 64;
  }

  private Image genRandomTexture( int numTilesWidth, int numTilesHeight ) {
    Image texture = null;
    Graphics g = null;
    System.out.println( "CALLED" );
    try {
      texture = new Image( numTilesWidth * TILE_SIZE, numTilesHeight * TILE_SIZE );
      g = texture.getGraphics();
    } catch (SlickException e) {
      e.printStackTrace();
    }
    for (int i = 0; i < numTilesWidth; i++) {
      for (int j = 0; j < numTilesHeight; j++) {
        g.drawImage( mTiles[r.nextInt( 4 ) + 1], i * TILE_SIZE, j * TILE_SIZE );
      }
    }
    g.flush();
    return texture;
  }

  public void addRegion( Region region ) {
    mRegions.add( region );
  }

  public List<Region> getWallsWithin( Rectangle rect ) {
    LinkedList<Region> within = new LinkedList<Region>();
    for (Region region : mRegions) {
      if (region.overLaps( rect ))
        within.add( region );
    }
    return within;
  }

  public void render( Graphics g, Rectangle clip ) {
    for (Region region : getWallsWithin( clip )) {
      g.texture( region.getPolygon(), region.getTexture(), region.getScale(), region.getScale(),
                 true );
    }
  }

  public Map<Integer, HoverCraft> getCars() {
    return mCrafts;
  }

  public HoverCraft getPlayer() {
    return mPlayer;
  }

  public List<Rocket> getRockets() {
    return(mRockets);
  }

  public List<Explosion> getExplosions() {
    // TODO Auto-generated method stub
    return(mExplosions);
  }
}