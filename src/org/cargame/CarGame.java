package org.cargame;

import java.util.ArrayList;

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
  private PlayerCar mPlayerCar;
  private static final int PLAYER_NUM = 1;
  
  public CarGame() {
    super("CAR GAME, SON");
    mCars = new ArrayList<Car>();
    // TODO Auto-generated constructor stub
  }

  @Override
  public void init(GameContainer container) throws SlickException {
    mCars.add(new PlayerCar("gfx/car1.png", -100, 0));
    mCars.add(new PlayerCar("gfx/car2.png", 100, 0));
    mPlayerCar = (PlayerCar)mCars.get(PLAYER_NUM);
  }

  @Override
  public void update(GameContainer container, int delta) throws SlickException {
    for(Car car:mCars) {
      car.think(delta);
    }
  }
  
  @Override
  public void render(GameContainer container, Graphics g) throws SlickException {
    for(Car car:mCars) {
      Image image = car.getImage();
      image.setRotation((float)(car.getAngle()*180/Math.PI));
      image.drawCentered(draw_offset_x + car.getX(), draw_offset_y + car.getY());
    }
    g.drawString("Welcome to Car Game ho", 10, 30);
  }

  @Override
  public void keyPressed(int key, char c) {
    switch(key) {
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
    switch(key) {
    // Player control stuff
    case Input.KEY_W:
      mPlayerCar.setAccelerating(false);
      break;
    case Input.KEY_S:
      mPlayerCar.setBraking(false);
      break;
    case Input.KEY_A:
      if(mPlayerCar.getTurning() == PlayerCar.TURN_LEFT) mPlayerCar.setTurning(PlayerCar.TURN_NONE);
      break;
    case Input.KEY_D:
      if(mPlayerCar.getTurning() == PlayerCar.TURN_RIGHT) mPlayerCar.setTurning(PlayerCar.TURN_NONE);
      break;
    }
  }

  public static void main(String[] args) {
    try {
      AppGameContainer appGameContainer = new AppGameContainer(new CarGame());
      appGameContainer.setDisplayMode(640, 480, !DEBUG_MODE);
      appGameContainer.start();
    } catch (SlickException e) {
      e.printStackTrace();
    }
  }
}
