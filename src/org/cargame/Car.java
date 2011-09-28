package org.cargame;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public class Car {
  private static final double accel_factor = 0.01;
  private static final double brake_factor = 0.02;
  private static final double turn_factor = 0.005;
  private static final double steer_factor = 0.00029 * 4;
  private static final double speed_scalar = 0.05;
  private static final double max_speed = 10;
  private static final double max_steer_angle = 0.34907;
  private static final double steer_center_snap_angle = 0.06;
  
  private Image mImage;
  private double mX, mY;
  private double mAngle;
  private double mSteerAngle;
  private double mSpeed;
  
  public Car(String graphic_file, float x, float y) {
    try {
      mImage = new Image(graphic_file,Color.magenta);
    } catch (SlickException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    mImage.setCenterOfRotation(16, 14);
    
    mX = x;
    mY = y;
    mAngle = 0;
    mSpeed = 0;
    mSteerAngle = 0;
  }
  
  public void accelerate(int delta) {
    mSpeed += accel_factor * delta; 
    if(mSpeed > max_speed) mSpeed = max_speed;
  }
  
  public void brake(int delta) {
    mSpeed -= brake_factor * delta;
    if(mSpeed < 0) mSpeed = 0;
  }
  
  public void apply_turn(int delta) {
    if(mSpeed < 0.01 || Math.abs(mSteerAngle) < steer_center_snap_angle) return;
    mAngle += delta * mSteerAngle * turn_factor;
    if(mAngle > Math.PI * 2) mAngle -= Math.PI * 2;
    else if(mAngle < Math.PI * 2) mAngle += Math.PI * 2;
  }
  
  public void turn_left(int delta) {
    mSteerAngle -= steer_factor * delta;
    if(mSteerAngle < -max_steer_angle) mSteerAngle = -max_steer_angle;
  }
  
  public void turn_right(int delta) {
    mSteerAngle += steer_factor * delta;
    if(mSteerAngle > max_steer_angle) mSteerAngle = max_steer_angle; 
  }
  
  public void turn_none(int delta) {
    mSteerAngle += mSteerAngle > 0 ? -(turn_factor * delta) : turn_factor * delta;
    if(Math.abs(mSteerAngle) < steer_center_snap_angle) mSteerAngle = 0;
  }
  
  public void think(int delta) {
    apply_turn(delta);
    mX += Math.sin(mAngle) * delta * mSpeed * speed_scalar;
    mY -= Math.cos(mAngle) * delta * mSpeed * speed_scalar;
  }
  
  public Image getImage() { return mImage; }
  public float getAngle() { return (float)mAngle; }
  public float getX() { return (float)mX; }
  public float getY() { return (float)mY; }
  public double getSteerAngle() {
    return mSteerAngle;
  }
  public void moveTo(float x, float y) {
    mX = x;
    mY = y;
  }
}
