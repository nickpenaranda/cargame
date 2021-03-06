package org.cargame;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public class Rocket {

  public static Image image;
  public Car owner;
  public double x, y, vx, vy, angle;
  public int life;

  public Rocket(Car owner, double x, double y, double vx, double vy) {
    this.owner = owner;
    this.x = x;
    this.y = y;
    this.vx = vx;
    this.vy = vy;
    // double speed = Math.sqrt(vx * vx + vy * vy);
    if (vx == 0)
      vx += 0.000001;

    if (vx > 0)
      this.angle = Math.atan( vy / vx ) + Math.PI / 2;
    else if (vx < 0)
      this.angle = Math.atan( vy / vx ) + Math.PI + Math.PI / 2;
    this.life = 5000;
  }

  public static void init() {
    try {
      image = new Image( "gfx/rocket.png", Color.magenta );
    } catch (SlickException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
