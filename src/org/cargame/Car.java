package org.cargame;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public class Car {
  private static final double accel_factor = 0.01;
  private static final double brake_factor = 0.05;
  private static final double turn_factor = 0.005;
  private static final double speed_scalar = 0.05;
  private static final double max_speed = 10;
  
  private Image mImage;
  private double mX, mY;
  private double mAngle;
  private double mSpeed;
  
  public Car(String graphic_file, float x, float y) {
    try {
      mImage = new Image(graphic_file,Color.magenta);
    } catch (SlickException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    mX = x;
    mY = y;
    mAngle = 0;
    mSpeed = 0;
  }
  
  public void accelerate(int delta) {
    mSpeed += accel_factor * delta; 
    if(mSpeed > max_speed) mSpeed = max_speed;
  }
  
  public void brake(int delta) {
    mSpeed -= brake_factor * delta;
    if(mSpeed < 0) mSpeed = 0;
  }
  
  public void turn_left(int delta) {
    mAngle -= turn_factor * delta;
    if(mAngle > Math.PI * 2) mAngle -= Math.PI * 2;
  }
  
  public void turn_right(int delta) {
    mAngle += turn_factor * delta;
    if(mAngle < Math.PI * 2) mAngle += Math.PI * 2;
  }
  
  public void think(int delta) {
    mX += Math.sin(mAngle) * delta * mSpeed * speed_scalar;
    mY -= Math.cos(mAngle) * delta * mSpeed * speed_scalar;
  }
  
  public Image getImage() { return mImage; }
  public float getAngle() { return (float)mAngle; }
  public float getX() { return (float)mX; }
  public float getY() { return (float)mY; }
  public void moveTo(float x, float y) {
    mX = x;
    mY = y;
  }
}
