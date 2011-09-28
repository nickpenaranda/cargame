package org.cargame;

import java.util.ArrayList;
import java.util.Random;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

public class CarGame extends BasicGame {
  public static final boolean DEBUG_MODE = true;
  private static final float draw_offset_x = 320f, draw_offset_y = 240f;
  private ArrayList<Car> mCars;
  private PlayerCar mPlayerCar,mOtherCar;
  private static final int PLAYER_NUM = 1;
  private int[][] mMap;
  private Random r;
  private Image[] mTiles;
  private Client mClient;

  public CarGame() {
    super("CAR GAME, SON");

    r = new Random();
    mCars = new ArrayList<Car>();
    mMap = new int[256][256];
    mTiles = new Image[3];

    for (int x = 0; x < 256; ++x) {
      for (int y = 0; y < 256; ++y) {
        mMap[x][y] = r.nextInt(3);
      }
    }
    // TODO Auto-generated constructor stub
  }

  @Override
  public void init(GameContainer container) throws SlickException {
    mCars.add(new PlayerCar("gfx/car1.png", -100, 0));
    mCars.add(new PlayerCar("gfx/car2.png", 100, 0));
    mPlayerCar = (PlayerCar) mCars.get(PLAYER_NUM);
    mOtherCar = (PlayerCar) mCars.get(PLAYER_NUM == 1 ? 0 : 1);

    mTiles[0] = null;
    mTiles[1] = new Image("gfx/tile1.png");
    mTiles[2] = new Image("gfx/tile2.png");
    
    try {
      mClient = new Client();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void update(GameContainer container, int delta) throws SlickException {
    UpdateMessage message = null;
    try {
      message = mClient.doUpdate(mPlayerCar.getX(), mPlayerCar.getY());
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    if(message != null) {
      mOtherCar.moveTo((float)message.x, (float)message.y);
    }
    
    // Think for all cars
    for (Car car : mCars) {
      car.think(delta);
    }
  }

  @Override
  public void render(GameContainer container, Graphics g) throws SlickException {
    int tx = (int) (8192 + mPlayerCar.getX() - (container.getWidth() / 2)) / 64;
    int ox = (int) (8192 + mPlayerCar.getX() - (container.getWidth() / 2)) % 64;
    int ty = (int) (8192 + mPlayerCar.getY() - (container.getHeight() / 2)) / 64;
    int oy = (int) (8192 + mPlayerCar.getY() - (container.getHeight() / 2)) % 64;


    for (int x = 0; x < 11; x++) {
      for (int y = 0; y < 9; y++) {
        if (isInBounds(tx+x,ty+y) && mMap[tx + x][ty + y] != 0)
          g.drawImage(mTiles[mMap[tx + x][ty + y]], (x << 6) - ox, (y << 6)
              - oy);
      }
    }

    for (Car car : mCars) {
      Image image = car.getImage();
      image.setRotation((float) (car.getAngle() * 180 / Math.PI));
      image.drawCentered(draw_offset_x + car.getX() - mPlayerCar.getX(),
          draw_offset_y + car.getY() - mPlayerCar.getY());
    }

    g.drawString("Steer angle = " + mPlayerCar.getSteerAngle(), 10, 30);
    g.drawString(
        String.format("(%f,%f)", mPlayerCar.getX(), mPlayerCar.getY()), 10, 45);
    g.drawString(String.format("Tile: (%d,%d)", tx, ty), 10, 60);
  }

  private boolean isInBounds(int x, int y) {
    return(x >= 0 && x < 256 && y >= 0 && y < 256);
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
      mPlayerCar.setBraking(true);
      break;
    case Input.KEY_A:
      mPlayerCar.setTurning(PlayerCar.TURN_LEFT);
      break;
    case Input.KEY_D:
      mPlayerCar.setTurning(PlayerCar.TURN_RIGHT);
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
      mPlayerCar.setBraking(false);
      break;
    case Input.KEY_A:
      if (mPlayerCar.getTurning() == PlayerCar.TURN_LEFT)
        mPlayerCar.setTurning(PlayerCar.TURN_NONE);
      break;
    case Input.KEY_D:
      if (mPlayerCar.getTurning() == PlayerCar.TURN_RIGHT)
        mPlayerCar.setTurning(PlayerCar.TURN_NONE);
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
