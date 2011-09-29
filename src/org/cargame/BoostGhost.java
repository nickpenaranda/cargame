package org.cargame;

import java.io.Serializable;

public class BoostGhost implements Serializable {
  public float x,y;
  public double angle;
  public int life,player;
  
  private static final long serialVersionUID = 4737137790355070289L;
  public BoostGhost(float x,float y,double angle) {
    this.x = x;
    this.y = y;
    this.angle = angle;
    player = 0;
    life = 250;
  }
}
