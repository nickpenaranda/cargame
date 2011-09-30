package org.cargame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

public class CarGame extends BasicGame {
  public static final boolean DEBUG_MODE = true;
  public static final int NUM_VEHICLES = 5;
  public static boolean multiplayer_mode;
  public static final int roadWidth = 6;
  public static final int buildingWidth = 10;
  public static Random r;

  private Map<Integer, HoverCraft> mCars;
  private ArrayList<Boundary> mWalls;
  private HoverCraft mPlayerCraft;
  private static final float cloak_alpha = 0.05f;
  private int[][] mMap;
  private Image[] mTiles;
  private GameClient mClient;
  private ArrayList<BoostGhost> mGhosts;
  ArrayList<Explosion> mExplosions;
  private long ticks;

  private GameContainer mContainer;

  public CarGame() {
    super("CAR GAME, SON");

    String mmode = System.getProperty("cargame.multiplayer_mode");
    if (mmode != null)
      multiplayer_mode = Boolean.valueOf(mmode);
    else
      multiplayer_mode = false;
    System.out.println(multiplayer_mode);

    r = new Random();
    mCars = (Map<Integer, HoverCraft>) Collections
        .synchronizedMap(new TreeMap<Integer, HoverCraft>());
    mMap = new int[256][256];
    mTiles = new Image[5];
    mWalls = new ArrayList<Boundary>();
    mGhosts = new ArrayList<BoostGhost>();
    mExplosions = new ArrayList<Explosion>();
    genMap();
  }

  @Override
  public void init(GameContainer container) throws SlickException {
    mContainer = container;
    mTiles[0] = new Image("gfx/road.png");
    mTiles[1] = new Image("gfx/wall1.png");
    mTiles[2] = new Image("gfx/wall2.png");
    mTiles[3] = new Image("gfx/wall3.png");
    mTiles[4] = new Image("gfx/wall4.png");

    HoverCraft.init(this);
    Explosion.init();
    
    ticks = 0;

    // mCars.put(1, new HoverCraft("gfx/craft2.png", -8192 + roadWidth * 32
    // + (roadWidth + buildingWidth) * (r.nextInt(15) + 1) * 64, -8192
    // + roadWidth * 32 + (roadWidth + buildingWidth) * (r.nextInt(15) + 1)
    // * 64));

    System.out.println("Multiplayer Mode? " + multiplayer_mode);

    mPlayerCraft = new HoverCraft("gfx/craft1.png", -8192 + roadWidth * 32
        + (roadWidth + buildingWidth) * (r.nextInt(15) + 1) * 64, -8192
        + roadWidth * 32 + (roadWidth + buildingWidth) * (r.nextInt(15) + 1)
        * 64);

    if (multiplayer_mode) {
      try {
        mClient = new GameClient(mCars);
        mCars.put(mClient.getPlayerId(), mPlayerCraft);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else
      mCars.put(0, mPlayerCraft);

    Sounds.init();
  }

  @Override
  public void update(GameContainer container, int delta) throws SlickException {
    ++ticks;

    for (HoverCraft c : mCars.values()) {
      c.think(delta);
      BoostGhost ghost = c.getBoostTimeout() > 2000 && ticks % 3 == 0 ? new BoostGhost(
          c.getX(), c.getY(), c.getAngle(), c.getImage())
          : null;
      if (ghost != null)
        mGhosts.add(ghost);
    }

    if (multiplayer_mode) {
      mClient.sendMoveUpdate(mPlayerCraft.getX(), mPlayerCraft.getY(),
          mPlayerCraft.getVX(), mPlayerCraft.getVY(), mPlayerCraft.getAngle(),
          mPlayerCraft.getThrustT(), mPlayerCraft.getThrustR(), mPlayerCraft
              .getThrustB(), mPlayerCraft.getThrustL());
    }

    for (BoostGhost g : new ArrayList<BoostGhost>(mGhosts)) {
      g.life -= delta;
      if (g.life <= 0)
        mGhosts.remove(g);
    }

    for (Explosion e : new ArrayList<Explosion>(mExplosions)) {
      e.life -= delta;
      if (e.life <= 0)
        mExplosions.remove(e);
    }

    // Check collision player car vs other cars
    if (!mPlayerCraft.isDead()) {
      ArrayList<HoverCraft> otherCars = new ArrayList<HoverCraft>(mCars
          .values());
      otherCars.remove(mPlayerCraft);
      for (HoverCraft other : otherCars) {
        if (CarGame.distance(mPlayerCraft.getX(), mPlayerCraft.getY(), other
            .getX(), other.getY()) < 64
            && Math.abs(mPlayerCraft.getSpeed()) < Math.abs(other.getSpeed())) {
          mPlayerCraft.kill();
          mClient.sendStateUpdate(Network.STATE_DEAD, true);
          Sounds.death.play();
        }
      }
    }

    for (int i = 0; i < mWalls.size(); i++) {
      if (mWalls.get(i).intersect(mPlayerCraft.getX(), mPlayerCraft.getY(), 31)) {
        mPlayerCraft.bounce(mWalls.get(i), delta);
        Sounds.bounce.play((float) (1 + r.nextGaussian() / 5), 1.0f);
      }
    }
  }

  static double distance(double x1, double y1, double x2, double y2) {
    return (Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
  }

  public void render(GameContainer container, Graphics g) throws SlickException {
    float scale_factor = 1 / (1 + (float) Math.pow(mPlayerCraft
        .getAverageSpeed(), 3));
    if (scale_factor < 0.25)
      scale_factor = 0.25f;

    float draw_offset_x = 320f / scale_factor, draw_offset_y = 240f / scale_factor;

    int tx = (int) (8192 + mPlayerCraft.getX() - draw_offset_x) / 64;
    int ty = (int) (8192 + mPlayerCraft.getY() - draw_offset_y) / 64;
    int ox = (int) (8192 + mPlayerCraft.getX() - draw_offset_x) % 64;
    int oy = (int) (8192 + mPlayerCraft.getY() - draw_offset_y) % 64;

    g.scale(scale_factor, scale_factor);

    float s = 64f;

    // Draw tiles

    for (int x = 0; x < 10 / scale_factor + 1; x++) {
      for (int y = 0; y < 8 / scale_factor + 1; y++) {
        if (isInBounds(tx + x, ty + y) /* && mMap[tx + x][ty + y] != 0 */)
          g.drawImage(mTiles[mMap[tx + x][ty + y]], (x * s) - ox, (y * s) - oy);
      }
    }

    // Draw boundaries
    for (Boundary boundary : mWalls) {
      g.drawLine((float) (draw_offset_x + boundary.a.x - mPlayerCraft.getX()),
          (float) (draw_offset_y + boundary.a.y - mPlayerCraft.getY()),
          (float) (draw_offset_x + boundary.b.x - mPlayerCraft.getX()),
          (float) (draw_offset_y + boundary.b.y - mPlayerCraft.getY()));
    }

    // Draw boost ghosts
    for (BoostGhost ghost : mGhosts) {
      Image image = ghost.image;
      image.setRotation((float) (ghost.angle * 180 / Math.PI));
      int life = ghost.life;
      image.setAlpha(life / (float) 250 * 0.3f);
      image.drawCentered(draw_offset_x + ghost.x - mPlayerCraft.getX(),
          draw_offset_y + ghost.y - mPlayerCraft.getY());
    }

    // Draw cars
    for (HoverCraft car : mCars.values()) {
      if(car.isDead()) continue;
      Image image = car.getImage();
      image.setRotation((float) (car.getAngle() * 180 / Math.PI));
      int jammer = car.getJammer();
      if (jammer > 500)
        image.setAlpha(cloak_alpha);
      else if (jammer > 0)
        image.setAlpha(cloak_alpha + (500 - jammer) / (float) 500
            * (1 - cloak_alpha));
      else
        image.setAlpha(1.0f);
      image.drawCentered(draw_offset_x + car.getX() - mPlayerCraft.getX(),
          draw_offset_y + car.getY() - mPlayerCraft.getY());
    }

    // Draw explosions
    for (Explosion e: mExplosions) {
      e.getImage().drawCentered(draw_offset_x + (float)e.x - mPlayerCraft.getX(),
          draw_offset_y + (float)e.y - mPlayerCraft.getY());
    }

    g.scale(1 / scale_factor, 1 / scale_factor);
    // Info
    // draw indicator
    for (HoverCraft craft : mCars.values()) {
      if (craft.getJammer() <= 0) {
        int min_len = 33;
        int max_len = 220;
        double angle = Math.atan((craft.getY() - mPlayerCraft.getY())
            / (craft.getX() - mPlayerCraft.getX()));
        if (craft.getX() > mPlayerCraft.getX())
          angle += Math.PI;

        double car_dist = Math.sqrt(Math.pow(
            craft.getY() - mPlayerCraft.getY(), 2)
            + Math.pow(craft.getX() - mPlayerCraft.getX(), 2));
        double len = min_len + car_dist / 33;
        if (len > max_len)
          len = max_len;

        float x = (float) ((640 / 2) - len * Math.cos(angle));
        float y = (float) ((480 / 2) - len * Math.sin(angle));

        g.setColor(Color.red);
        g.fillOval(x, y, (float) 5.0, (float) 5.0);
        g.setColor(Color.orange);
        g.drawOval(x, y, (float) 6.0, (float) 6.0);
      }
    }
    // Draw boost indicator
    g.setColor(Color.white);
    g.drawRect(280, 15, 80, 5);
    if (mPlayerCraft.getBoostTimeout() > 0)
      g.setColor(Color.yellow);
    else
      g.setColor(Color.green);
    g
        .fillRect(
            281,
            16,
            79 * (1 - (mPlayerCraft.getBoostTimeout() / (float) HoverCraft.BOOST_TIMEOUT)),
            4);

    // Draw jammer indicator
    g.setColor(Color.white);
    g.drawRect(280, 22, 80, 5);
    if (mPlayerCraft.getJammerTimeout() > 0)
      g.setColor(Color.gray);
    else
      g.setColor(Color.cyan);
    g
        .fillRect(
            281,
            23,
            79 * (1 - (mPlayerCraft.getJammerTimeout() / (float) HoverCraft.JAMMER_TIMEOUT)),
            4);

    g.setColor(Color.white);
    g.drawString("Speed = " + mPlayerCraft.getSpeed(), 10, 30);
    g.drawString(String.format("(%f,%f)", mPlayerCraft.getX(), mPlayerCraft
        .getY()), 10, 45);
    g.drawString(String.format("Tile: (%d,%d)", tx, ty), 10, 60);

    // Scoreboard
    g.setColor(Color.green);
    g.drawString("You: " + mPlayerCraft.getLives(), 10,
        container.getHeight() - 15);

    if (mPlayerCraft.isDead()) {
      g.setColor(Color.red);
      g.drawString("!!!!BOOM SUCKA!!!!", 320 - g.getFont().getWidth(
          "!!!!BOOM SUCKA!!!") / 2, 240);
    }
  }

  private boolean isInBounds(int x, int y) {
    return (x >= 0 && x < 256 && y >= 0 && y < 256);
  }

  @Override
  public void keyPressed(int key, char c) {
    switch (key) {
    // Non player control stuff
    case Input.KEY_ESCAPE:
      System.exit(0);
      break;

    // Player control stuff
    case Input.KEY_W:
      mPlayerCraft.setBooster(HoverCraft.BOTTOM, true);
      break;
    case Input.KEY_S:
      mPlayerCraft.setBooster(HoverCraft.TOP, true);
      break;
    case Input.KEY_A:
      mPlayerCraft.setBooster(HoverCraft.RIGHT, true);
      break;
    case Input.KEY_D:
      mPlayerCraft.setBooster(HoverCraft.LEFT, true);
      break;
    case Input.KEY_Q:
      if (multiplayer_mode)
        mClient.sendStateUpdate(Network.STATE_JAM, true);
      mPlayerCraft.jammer();
      break;
    case Input.KEY_F1:
      try {
        mContainer.setFullscreen(!mContainer.isFullscreen());
      } catch (SlickException e) {
        e.printStackTrace();
      }
      break;
    case Input.KEY_K:
      mPlayerCraft.kill();
      break;
    }
  }

  @Override
  public void keyReleased(int key, char c) {
    switch (key) {
    // Player control stuff
    case Input.KEY_W:
      mPlayerCraft.setBooster(HoverCraft.BOTTOM, false);
      break;
    case Input.KEY_S:
      mPlayerCraft.setBooster(HoverCraft.TOP, false);
      break;
    case Input.KEY_A:
      mPlayerCraft.setBooster(HoverCraft.RIGHT, false);
      break;
    case Input.KEY_D:
      mPlayerCraft.setBooster(HoverCraft.LEFT, false);
      break;
    }
  }

  @Override
  public void mouseMoved(int oldx, int oldy, int newx, int newy) {
    int x = newx - 320;
    int y = newy - 240;

    if (x > 0)
      mPlayerCraft.setAngle(Math.atan(y / (double) x) + Math.PI / 2);
    else if (x < 0)
      mPlayerCraft.setAngle(Math.atan(y / (double) x) + Math.PI + Math.PI / 2);
    else {
      if (y < 0)
        mPlayerCraft.setAngle(0);
      else
        mPlayerCraft.setAngle(Math.PI);
    }
  }

  @Override
  public void mouseClicked(int button, int x, int y, int clickCount) {
    switch (button) {
    case 0: // Left
      if (multiplayer_mode)
        mClient.sendStateUpdate(Network.STATE_BOOST, true);
      mPlayerCraft.boost();
      break;
    }
  }

  // Generates a map with grid roads.
  private void genMap() {
    // Set tiles
    for (int x = 0; x < 256; ++x) {
      for (int y = 0; y < 256; ++y) {
        mMap[x][y] = r.nextInt(4) + 1;
      }
    }
    for (int x = 0; x < 256; ++x) {
      if (x % (roadWidth + buildingWidth) < roadWidth) {
        flatLine(x, 0, 255, true, 0);
        flatLine(x, 0, 255, false, 0);
      }
    }

    // Set boundaries
    mWalls.add(new Boundary(-8192, -8192, 8192, -8192, 1));
    mWalls.add(new Boundary(-8192, -8192, -8192, 8192, 1));
    mWalls.add(new Boundary(8192, -8192, 8192, 8192, 1));
    mWalls.add(new Boundary(-8192, 8192, 8192, 8192, 1));

    for (int x = 1; x <= 16; ++x) {
      for (int y = 1; y <= 16; ++y) {
        mWalls.add(new Boundary(-8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x - 1)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y - 1)), -8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y - 1)), 1));
        mWalls.add(new Boundary(-8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x - 1)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y - 1)), -8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x - 1)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y)), 1));
        mWalls.add(new Boundary(-8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y - 1)), -8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y)), 1));
        mWalls.add(new Boundary(-8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x - 1)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y)), -8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y)), 1));
      }
    }
    int cityBlockWidth = roadWidth + buildingWidth;
    int numRoads = 256 / cityBlockWidth;
    int numBarriers = r.nextInt(40) + 10;

    // Remove to reenable barriers once sending to server works.
    numBarriers = 0;
    for (int i = 0; i < numBarriers; i++) {
      // specifices intersection: (0, 0) topleft
      int barrierX = r.nextInt(numRoads - 1);
      int barrierY = r.nextInt(numRoads - 1);
      boolean barrierHoriz = r.nextInt(2) == 1;
      System.out.println(barrierX + " " + barrierY + " " + barrierHoriz);

      int barrierTop = barrierX * cityBlockWidth + cityBlockWidth / 2
          + roadWidth / 2 - 1;
      int barrierLeft = barrierY * cityBlockWidth;
      int barrierRight = barrierY * cityBlockWidth + roadWidth - 1;
      int barrierBottom = barrierX * cityBlockWidth + cityBlockWidth / 2
          + roadWidth / 2;
      flatLine(barrierBottom, barrierLeft, barrierRight, barrierHoriz, -1);
      flatLine(barrierTop, barrierLeft, barrierRight, barrierHoriz, -1);
      int boundStartX = tileToPixel(barrierLeft);
      int boundStartY = tileToPixel(barrierTop);
      int boundEndX = tileToPixel(barrierRight);
      int boundEndY = tileToPixel(barrierTop);
      mWalls.add(new Boundary(boundStartX, boundStartY, boundEndX + 64,
          boundEndY, 1, !barrierHoriz));
      mWalls.add(new Boundary(boundStartX, boundStartY + 128, boundEndX + 64,
          boundEndY + 128, 1, !barrierHoriz));
    }
  }

  private int tileToPixel(int tileNum) {
    return -8192 + tileNum * 64;
  }

  private void flatLine(int offset, int start, int end, boolean horiz, int val) {
    boolean random = val == -1;
    int y = offset;
    for (int x = start; x <= end; x++) {
      if (random) {
        val = r.nextInt(4) + 1;
      }
      if (horiz)
        mMap[x][y] = val;
      else
        mMap[y][x] = val;
    }
  }

  public static void main(String[] args) {
    try {
      AppGameContainer appGameContainer = new AppGameContainer(new CarGame());
      appGameContainer.setDisplayMode(640, 480, !DEBUG_MODE);
      appGameContainer.setMinimumLogicUpdateInterval(20);
      appGameContainer.setAlwaysRender(true);
      // appGameContainer.setMaximumLogicUpdateInterval(60);
      // appGameContainer.setVSync(true);
      appGameContainer.start();
    } catch (SlickException e) {
      e.printStackTrace();
    }
  }
}
