package org.cargame;

import java.util.ArrayList;
import java.util.Random;

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
  private static boolean multiplayer_mode;
  public static final int roadWidth = 6;
  public static final int buildingWidth = 10;
  public static Random r;

  private ArrayList<HoverCraft> mCars;
  private ArrayList<Boundary> mWalls;
  private HoverCraft mPlayerCraft, mOtherCar;
  private static final int PLAYER_NUM = 1;
  private static final float cloak_alpha = 0.05f;
  private int[][] mMap;
  private Image[] mTiles;
  private Client mClient;

  public CarGame() {
    super("CAR GAME, SON");

    String mmode = System.getProperty("cargame.multiplayer_mode");
    if (mmode != null)
      multiplayer_mode = Boolean.valueOf(mmode);
    else
      multiplayer_mode = false;
    System.out.println(multiplayer_mode);

    r = new Random();
    mCars = new ArrayList<HoverCraft>();
    mMap = new int[256][256];
    mTiles = new Image[5];
    mWalls = new ArrayList<Boundary>();

    genMap();
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
  }

  private void flatLine(int offset, int start, int end, boolean horiz, int val) {
    int y = offset;
    for (int x = start; x <= end; x++) {
      if (horiz)
        mMap[x][y] = val;
      else
        mMap[y][x] = val;
    }
  }

  @Override
  public void init(GameContainer container) throws SlickException {
    mTiles[0] = null;
    mTiles[1] = new Image("gfx/wall1.png");
    mTiles[2] = new Image("gfx/wall2.png");
    mTiles[3] = new Image("gfx/wall3.png");
    mTiles[4] = new Image("gfx/wall4.png");

    int player_num = PLAYER_NUM;
    System.out.println(multiplayer_mode);
    if (multiplayer_mode) {
      try {
        mClient = new Client();
        player_num = mClient.getPlayerId();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    mCars.add(new HoverCraft("gfx/craft1.png", -8192 + roadWidth * 32
        + (roadWidth + buildingWidth) * (r.nextInt(15) + 1) * 64, -8192
        + roadWidth * 32 + (roadWidth + buildingWidth) * (r.nextInt(15) + 1)
        * 64));
    mCars.add(new HoverCraft("gfx/craft2.png", -8192 + roadWidth * 32
        + (roadWidth + buildingWidth) * (r.nextInt(15) + 1) * 64, -8192
        + roadWidth * 32 + (roadWidth + buildingWidth) * (r.nextInt(15) + 1)
        * 64));
    mPlayerCraft = mCars.get(player_num);
    mOtherCar = mCars.get(player_num == 0 ? 1 : 0);
    
    Sounds.init();
  }

  @Override
  public void update(GameContainer container, int delta) throws SlickException {
    if (multiplayer_mode) {
      UpdateMessage message = null;
      try {
        message = mClient.doUpdate(mPlayerCraft.getX(), mPlayerCraft.getY(),
            mPlayerCraft.getAngle(), mPlayerCraft.getSpeed(), mPlayerCraft
                .getLives(),mPlayerCraft.getJammer());
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      if (message != null) {
        mOtherCar.moveTo((float) message.x, (float) message.y);
        mOtherCar.setSpeed(message.speed);
        mOtherCar.setAngle(message.angle);
        mOtherCar.setLives(message.lives);
        mOtherCar.setJammer(message.jammer);
      }
    }

    // Think for all cars
//    for (HoverCraft car : mCars) {
//      car.think(delta);
//    }
    mPlayerCraft.think(delta);

    // Check collision player car vs other cars
    if (!mPlayerCraft.isDead()) {
      ArrayList<HoverCraft> otherCars = new ArrayList<HoverCraft>(mCars);
      otherCars.remove(mPlayerCraft);
      for (HoverCraft other : otherCars) {
        if (CarGame.distance(mPlayerCraft.getX(), mPlayerCraft.getY(), other.getX(),
            other.getY()) < 64
            && Math.abs(mPlayerCraft.getSpeed()) < Math.abs(other.getSpeed())) {
          mPlayerCraft.kill();
          Sounds.death.play();
        }
      }
    }

    for (int i = 0; i < mWalls.size(); i++) {
      if (mWalls.get(i).intersect(mPlayerCraft.getX(), mPlayerCraft.getY(), 31)) {
        mPlayerCraft.bounce(mWalls.get(i), delta);
        Sounds.bounce.play((float)(1 + r.nextGaussian()/5),1.0f);
      }
    }
  }

  static double distance(double x1, double y1, double x2, double y2) {
    return (Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
  }

  public void render(GameContainer container, Graphics g) throws SlickException {
    float scale_factor = 1 / (1 + (float)Math.pow(mPlayerCraft.getAverageSpeed(),3));
    if(scale_factor < 0.25) scale_factor = 0.25f;
    
    
    float draw_offset_x = 320f / scale_factor, draw_offset_y = 240f / scale_factor;

//    int tx = (int) (8192 + mPlayerCraft.getX() - (container.getWidth() / 2)) / 64;
//    int ox = (int) (8192 + mPlayerCraft.getX() - (container.getWidth() / 2)) % 64;
//    int ty = (int) (8192 + mPlayerCraft.getY() - (container.getHeight() / 2)) / 64;
//    int oy = (int) (8192 + mPlayerCraft.getY() - (container.getHeight() / 2)) % 64;
    int tx = (int) (8192 + mPlayerCraft.getX() - draw_offset_x) / 64;
    int ty = (int) (8192 + mPlayerCraft.getY() - draw_offset_y) / 64;
    int ox = (int) (8192 + mPlayerCraft.getX() - draw_offset_x) % 64;
    int oy = (int) (8192 + mPlayerCraft.getY() - draw_offset_y) % 64;
    
    g.scale(scale_factor,scale_factor);
 
//    ox /= scale_factor;
//    oy /= scale_factor;
    
    //float s = 64 * scale_factor;
    float s = 64f;
    
    // Draw tiles
    
    for (int x = 0; x < 10 / scale_factor + 1; x++) {
      for (int y = 0; y < 8 / scale_factor + 1; y++) {
        if (isInBounds(tx + x, ty + y) && mMap[tx + x][ty + y] != 0)
          g.drawImage(mTiles[mMap[tx + x][ty + y]], 
              (x * s) - ox, 
              (y * s) - oy);
      }
    }

    // Draw boundaries
    for (Boundary boundary : mWalls) {
      g.drawLine((float) (draw_offset_x + boundary.a.x - mPlayerCraft.getX()),
          (float) (draw_offset_y + boundary.a.y - mPlayerCraft.getY()),
          (float) (draw_offset_x + boundary.b.x - mPlayerCraft.getX()),
          (float) (draw_offset_y + boundary.b.y - mPlayerCraft.getY()));
    }

    // Draw cars
    for (HoverCraft car : mCars) {
      Image image = car.getImage();
      image.setRotation((float) (car.getAngle() * 180 / Math.PI));
      int jammer = car.getJammer();
      if(jammer > 500)
        image.setAlpha(cloak_alpha);
      else if(jammer > 0)
        image.setAlpha(cloak_alpha + (500-jammer)/(float)500 * (1-cloak_alpha));
      else
        image.setAlpha(1.0f);
      image.drawCentered(draw_offset_x + car.getX() - mPlayerCraft.getX(),
          draw_offset_y + car.getY() - mPlayerCraft.getY());
    }

    g.scale(1/scale_factor,1/scale_factor);
    // Info
    // draw indicator
    if(mOtherCar.getJammer() <= 0) {
      int min_len = 33;
      int max_len = 220;
      double angle = Math.atan((mOtherCar.getY() - mPlayerCraft.getY())
          / (mOtherCar.getX() - mPlayerCraft.getX()));
      if (mOtherCar.getX() > mPlayerCraft.getX())
        angle += Math.PI;
  
      double car_dist = Math.sqrt(Math.pow(
          mOtherCar.getY() - mPlayerCraft.getY(), 2)
          + Math.pow(mOtherCar.getX() - mPlayerCraft.getX(), 2));
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

    // Draw boost indicator
    g.setColor(Color.white);
    g.drawRect(280, 15, 80, 5);
    if(mPlayerCraft.getBoostTimeout() > 0)
      g.setColor(Color.yellow);
    else
      g.setColor(Color.green);
    g.fillRect(281,16, 79 * (1 - (mPlayerCraft.getBoostTimeout() / (float)2500)), 4);

    g.drawString(String.format("(%f,%f)", mPlayerCraft.getX(), mPlayerCraft
        .getY()), 10, 45);
    g.drawString(String.format("Tile: (%d,%d)", tx, ty), 10, 60);

    // Draw jammer indicator
    g.setColor(Color.white);
    g.drawRect(280, 22, 80, 5);
    if(mPlayerCraft.getJammerTimeout() > 0)
      g.setColor(Color.gray);
    else
      g.setColor(Color.cyan);
    g.fillRect(281,23, 79 * (1 - (mPlayerCraft.getJammerTimeout() / (float)7500)), 4);
    
    g.drawString("Speed = " + mPlayerCraft.getSpeed(), 10, 30);
    // Scoreboard
    g.setColor(Color.green);
    g.drawString("You: " + mPlayerCraft.getLives(), 10,
        container.getHeight() - 15);

    String dem = "Dem: " + mOtherCar.getLives();
    g.setColor(Color.red);
    g.drawString(dem, 640 - g.getFont().getWidth(dem) - 10, container
        .getHeight() - 15);
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
      mPlayerCraft.jammer();
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
      if(y < 0) mPlayerCraft.setAngle(0);
      else mPlayerCraft.setAngle(Math.PI);
    }
  }

  @Override
  public void mouseClicked(int button, int x, int y, int clickCount) {
    switch(button) {
    case 0: // Left
      mPlayerCraft.boost();
      break;
    }
  }

  public static void main(String[] args) {
    try {
      AppGameContainer appGameContainer = new AppGameContainer(new CarGame());
      appGameContainer.setDisplayMode(640, 480, !DEBUG_MODE);
      appGameContainer.setMinimumLogicUpdateInterval(20);
      //appGameContainer.setMaximumLogicUpdateInterval(60);
      //appGameContainer.setVSync(true);
      appGameContainer.start();
    } catch (SlickException e) {
      e.printStackTrace();
    }
  }
}
