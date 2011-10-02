package org.cargame;

import java.io.Serializable;

import org.newdawn.slick.Image;

public class BoostGhost implements Serializable {

  public float x, y;
  public double angle;
  public int life;
  public Image image;

  private static final long serialVersionUID = 4737137790355070289L;

  public BoostGhost(float x, float y, double angle, Image image) {
    this.x = x;
    this.y = y;
    this.angle = angle;
    this.image = image;
    life = 250;
  }
}
