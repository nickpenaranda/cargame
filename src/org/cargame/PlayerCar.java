package org.cargame;

public class PlayerCar extends Car {
  public static final int TURN_NONE = 0;
  public static final int TURN_LEFT = 1;
  public static final int TURN_RIGHT = 2;

  private boolean mIsBraking, mIsAccelerating;
  private int mTurning;

  public PlayerCar(String graphicFile, float x, float y) {
    super(graphicFile, x, y);
    // TODO Auto-generated constructor stub
  }

  public void setTurning(int turn) {
    mTurning = turn;
  }

  public int getTurning() {
    return (mTurning);
  }

  public void setBraking(boolean brake) {
    mIsBraking = brake;
  }

  public void setAccelerating(boolean accel) {
    mIsAccelerating = accel;
  }

  @Override
  public void think(int delta) {
    if (mIsBraking)
      brake(delta);
    if (mIsAccelerating)
      accelerate(delta);
    switch (mTurning) {
    case TURN_LEFT:
      turn_left(delta);
      break;
    case TURN_RIGHT:
      turn_right(delta);
      break;
    case TURN_NONE:
      turn_none(delta);
      break;
    }

    super.think(delta);
  }


}
