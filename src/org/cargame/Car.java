package org.cargame;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public class Car {
  private static final double accel_factor = 0.01;
  private static final double decel_factor = 0.02;
  private static final double turn_factor = 0.005;
  private static final double steer_factor = 0.00029 * 5;
  private static final double speed_scalar = 0.05;
  private static final double max_speed = 15;
  //private static final double max_steer_angle = 0.34907;
  private static final double max_steer_angle = Math.PI / 6;
  private static final double steer_center_snap_angle = Math.PI / 18;
  private static final double max_reverse_speed = -5;
  private static final double speed_stop_snap = 0.1;

  private Image mImage;
  private double mX, mY;
  private double mAngle;
  private double mSteerAngle;
  private double mSpeed;
  private int mDeadCount;

  public Car(String graphic_file, float x, float y) {
    try {
      mImage = new Image(graphic_file, Color.magenta);
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
    mDeadCount = 0;
  }

  public void accelerate(int delta) {
    mSpeed += accel_factor * delta;
    if (mSpeed > max_speed)
      mSpeed = max_speed;
  }

  public void reverse(int delta) {
    mSpeed -= decel_factor * delta;
    if (mSpeed < max_reverse_speed)
      mSpeed = max_reverse_speed;
  }
  
  public void brake(int delta) {
    mSpeed -= decel_factor * delta * (mSpeed > 0 ? 1 : -1);
    if (Math.abs(mSpeed) < speed_stop_snap)
      mSpeed = 0;
  }

  public void apply_turn(int delta) {
    if (Math.abs(mSpeed) < 0.01)
      return;
    mAngle += delta * mSteerAngle * turn_factor;
    if (mAngle > Math.PI * 2)
      mAngle -= Math.PI * 2;
    else if (mAngle < Math.PI * 2)
      mAngle += Math.PI * 2;
  }

  public void turn_left(int delta) {
    mSteerAngle -= steer_factor * delta;
    if (mSteerAngle < -max_steer_angle)
      mSteerAngle = -max_steer_angle;
  }

  public void turn_right(int delta) {
    mSteerAngle += steer_factor * delta;
    if (mSteerAngle > max_steer_angle)
      mSteerAngle = max_steer_angle;
  }

  public void turn_none(int delta) {
    if (Math.abs(mSteerAngle) < steer_center_snap_angle) {
      mSteerAngle = 0;
    } else {
        mSteerAngle += mSteerAngle > 0 ? -(turn_factor * delta) : turn_factor
            * delta;
    }
  }

  public void think(int delta) {
    if(isDead()) {
      brake(delta);
    }
    apply_turn(delta);
    mX += Math.sin(mAngle) * delta * mSpeed * speed_scalar;
    mY -= Math.cos(mAngle) * delta * mSpeed * speed_scalar;
    
    if(mDeadCount > 0)
      mDeadCount -= delta;
    
    if(mDeadCount < 0) {
      moveTo(-8192 + CarGame.roadWidth*32 + (CarGame.roadWidth + CarGame.buildingWidth)*(CarGame.r.nextInt(16)+1)*64,
          -8192 + CarGame.roadWidth*32 + (CarGame.roadWidth + CarGame.buildingWidth)*(CarGame.r.nextInt(16)+1)*64);
      mDeadCount = 0;
    }

  }

  public Image getImage() {
    return mImage;
  }

  public float getAngle() {
    return (float) mAngle;
  }

  public float getX() {
    return (float) mX;
  }

  public float getY() {
    return (float) mY;
  }

  public double getSteerAngle() {
    return mSteerAngle;
  }
  
  public double getSpeed() { return mSpeed; }

  public void moveTo(float x, float y) {
    mX = x;
    mY = y;
  }

  public void setAngle(double angle) {
    mAngle = angle;
  }

  public boolean isDead() {
    return(mDeadCount > 0);
  }

  public void setDeadCount(int i) {
    mDeadCount = i;
  }

}
