package org.cargame;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

public class CarGame extends BasicGame {

  public CarGame() {
    super("CAR GAME, SON");
    // TODO Auto-generated constructor stub
  }

  @Override
  public void init(GameContainer container) throws SlickException {
  }

  @Override
  public void update(GameContainer container, int delta) throws SlickException {
  }
  
  @Override
  public void render(GameContainer container, Graphics g) throws SlickException {
    g.drawString("Welcome to Car Game ho", 10, 30);
  }

  @Override
  public void keyPressed(int key, char c) {
    switch(key) {
    case Input.KEY_ESCAPE:
      System.exit(0);
      break;
    }
  }


  public static void main(String[] args) {
    try {
      AppGameContainer appGameContainer = new AppGameContainer(new CarGame());
      appGameContainer.setDisplayMode(640, 480, true);
      appGameContainer.start();
    } catch (SlickException e) {
      e.printStackTrace();
    }
  }
}
