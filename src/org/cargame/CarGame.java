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
  private static final float draw_offset_x = 320f, draw_offset_y = 240f;
  public static final int roadWidth = 6;
  public static final int buildingWidth = 10;
  public static Random r;

  private ArrayList<Car> mCars;
  private ArrayList<Boundary> mWalls;
  private Car mPlayerCar, mOtherCar;
  private static final int PLAYER_NUM = 1;
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
    mCars = new ArrayList<Car>();
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
    
        for(int x=1; x <= 16; ++x) {
      for(int y=1; y <= 16; ++y) {
        mWalls.add(new Boundary(
            -8192 + (roadWidth * 64 * x) + (buildingWidth * 64 * (x - 1)),
            -8192 + (roadWidth * 64 * y) + (buildingWidth * 64 * (y - 1)),
            -8192 + (roadWidth * 64 * x) + (buildingWidth * 64 * (x)),
            -8192 + (roadWidth * 64 * y) + (buildingWidth * 64 * (y - 1)),1));
        mWalls.add(new Boundary(
            -8192 + (roadWidth * 64 * x) + (buildingWidth * 64 * (x - 1)),
            -8192 + (roadWidth * 64 * y) + (buildingWidth * 64 * (y - 1)),
            -8192 + (roadWidth * 64 * x) + (buildingWidth * 64 * (x - 1)),
            -8192 + (roadWidth * 64 * y) + (buildingWidth * 64 * (y)),1));
        mWalls.add(new Boundary(
            -8192 + (roadWidth * 64 * x) + (buildingWidth * 64 * (x)),
            -8192 + (roadWidth * 64 * y) + (buildingWidth * 64 * (y - 1)),
            -8192 + (roadWidth * 64 * x) + (buildingWidth * 64 * (x)),
            -8192 + (roadWidth * 64 * y) + (buildingWidth * 64 * (y)),1));
        mWalls.add(new Boundary(
            -8192 + (roadWidth * 64 * x) + (buildingWidth * 64 * (x - 1)),
            -8192 + (roadWidth * 64 * y) + (buildingWidth * 64 * (y)),
            -8192 + (roadWidth * 64 * x) + (buildingWidth * 64 * (x)),
            -8192 + (roadWidth * 64 * y) + (buildingWidth * 64 * (y)),1));
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

    mCars.add(new Car("gfx/car1.png", -8192 + roadWidth*32 + (roadWidth + buildingWidth)*(r.nextInt(16)+1)*64,
        -8192 + roadWidth*32 + (roadWidth + buildingWidth)*(r.nextInt(16)+1)*64));
    mCars.add(new Car("gfx/car2.png", -8192 + roadWidth*32 + (roadWidth + buildingWidth)*(r.nextInt(16)+1)*64,
        -8192 + roadWidth*32 + (roadWidth + buildingWidth)*(r.nextInt(16)+1)*64));
    mPlayerCar = mCars.get(player_num);
    mOtherCar = mCars.get(player_num == 0 ? 1 :0);
  }

  @Override
  public void update(GameContainer container, int delta) throws SlickException {
    if (multiplayer_mode) {
      UpdateMessage message = null;
      try {
        message = mClient.doUpdate(mPlayerCar.getX(), mPlayerCar.getY(),
            mPlayerCar.getAngle(),mPlayerCar.getSpeed());
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (message != null) {
        mOtherCar.moveTo((float) message.x, (float) message.y);
        mOtherCar.setAngle(message.angle);
      }
    }

    // Think for all cars
    for (Car car : mCars) {
      car.think(delta);
    }
    
    // Check collision player car vs other cars
    if(!mPlayerCar.isDead()) {
      ArrayList<Car> otherCars = new ArrayList<Car>(mCars);
      otherCars.remove(mPlayerCar);
      for (Car other : otherCars) {
        if(distance(mPlayerCar.getX(),mPlayerCar.getY(),other.getX(),other.getY()) < 47 &&
          Math.abs(mPlayerCar.getSpeed()) < Math.abs(other.getSpeed()))
            mPlayerCar.kill();
      }
    }

    for (int i=0; i<mWalls.size(); i++) {
        if (mWalls.get(i).intersect(mPlayerCar.getX(), mPlayerCar.getY(), 31)) {
          mPlayerCar.kill();
          }
    }
  }
  
  static double distance(double x1,double y1,double x2,double y2) {
    return(Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2)));
  }

  public void render(GameContainer container, Graphics g) throws SlickException {
    int tx = (int) (8192 + mPlayerCar.getX() - (container.getWidth() / 2)) / 64;
    int ox = (int) (8192 + mPlayerCar.getX() - (container.getWidth() / 2)) % 64;
    int ty = (int) (8192 + mPlayerCar.getY() - (container.getHeight() / 2)) / 64;
    int oy = (int) (8192 + mPlayerCar.getY() - (container.getHeight() / 2)) % 64;

    // Draw tiles
    for (int x = 0; x < 11; x++) {
      for (int y = 0; y < 9; y++) {
        if (isInBounds(tx + x, ty + y) && mMap[tx + x][ty + y] != 0)
          g.drawImage(mTiles[mMap[tx + x][ty + y]], (x << 6) - ox, (y << 6)
              - oy);
      }
    }

    // Draw boundaries
    for (Boundary boundary : mWalls) {
      g.drawLine((float) (draw_offset_x + boundary.a.x - mPlayerCar.getX()),
          (float) (draw_offset_y + boundary.a.y - mPlayerCar.getY()),
          (float) (draw_offset_x + boundary.b.x - mPlayerCar.getX()),
          (float) (draw_offset_y + boundary.b.y - mPlayerCar.getY()));
    }

    // Draw cars
    for (Car car : mCars) {
      Image image = car.getImage();
      image.setRotation((float) (car.getAngle() * 180 / Math.PI));
      image.drawCentered(draw_offset_x + car.getX() - mPlayerCar.getX(),
          draw_offset_y + car.getY() - mPlayerCar.getY());
    }

    // g.setColor(Color.white);
    g.drawOval(draw_offset_x - 31, draw_offset_y - 31, 62, 62);

    // Info
    // draw indicator
    int min_len = 33;
    int max_len = 100;
    double angle = Math.atan((mOtherCar.getY() - mPlayerCar.getY())
        / (mOtherCar.getX() - mPlayerCar.getX()));
    if (mOtherCar.getX() > mPlayerCar.getX())
      angle += Math.PI;

    double car_dist = Math.sqrt(Math.pow(mOtherCar.getY() - mPlayerCar.getY(),
        2)
        + Math.pow(mOtherCar.getX() - mPlayerCar.getX(), 2));
    double len = min_len + car_dist / 100;
    if (len > max_len)
      len = max_len;

    float x = (float) ((640 / 2) - len * Math.cos(angle));
    float y = (float) ((480 / 2) - len * Math.sin(angle));

    g.setColor(Color.red);
    g.fillOval(x, y, (float) 5.0, (float) 5.0);
    g.setColor(Color.orange);
    g.drawOval(x, y, (float) 6.0, (float) 6.0);

    g.drawString("Steer angle = " + mPlayerCar.getSteerAngle(), 10, 30);
    g.drawString(
        String.format("(%f,%f)", mPlayerCar.getX(), mPlayerCar.getY()), 10, 45);
    g.drawString(String.format("Tile: (%d,%d)", tx, ty), 10, 60);
    
    if(mPlayerCar.isDead()) {
      g.setColor(Color.red);
      g.drawString("!!!!BOOM SUCKA!!!!",320 - g.getFont().getWidth("!!!!BOOM SUCKA!!!")/2,240);
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
      mPlayerCar.setAccelerating(true);
      break;
    case Input.KEY_S:
      mPlayerCar.setReversing(true);
      break;
    case Input.KEY_A:
      mPlayerCar.setTurning(Car.TURN_LEFT);
      break;
    case Input.KEY_D:
      mPlayerCar.setTurning(Car.TURN_RIGHT);
      break;
    case Input.KEY_SPACE:
      mPlayerCar.setBraking(true);
      break;

    }
  }

  @Override
  public void keyReleased(int key, char c) {
    switch (key) {
    // Player control stuff
    case Input.KEY_W:
      mPlayerCar.setAccelerating(false);
      break;
    case Input.KEY_S:
      mPlayerCar.setReversing(false);
      break;
    case Input.KEY_A:
      if (mPlayerCar.getTurning() == Car.TURN_LEFT)
        mPlayerCar.setTurning(Car.TURN_NONE);
      break;
    case Input.KEY_D:
      if (mPlayerCar.getTurning() == Car.TURN_RIGHT)
        mPlayerCar.setTurning(Car.TURN_NONE);
      break;
    case Input.KEY_SPACE:
      mPlayerCar.setBraking(false);
      break;
    }
  }

  public static void main(String[] args) {
    try {
      AppGameContainer appGameContainer = new AppGameContainer(new CarGame());
      appGameContainer.setDisplayMode(640, 480, !DEBUG_MODE);
      appGameContainer.setVSync(true);
      appGameContainer.start();
    } catch (SlickException e) {
      e.printStackTrace();
    }
  }
}
