package org.cargame;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public class HoverCraft {
  public static final int TOP = 0;
  public static final int RIGHT = 1;
  public static final int BOTTOM = 2;
  public static final int LEFT = 3;

  private static final double base_booster_force = 0.02;
  private static final double min_speed_before_stiction = base_booster_force / 2;
  private static final double friction = 0.99937; // This is actually 1 - friction
  private static final double wall_elasticity = 0.4;

  private static final int X = 0;
  private static final int Y = 1;

  private static final int RESPAWN_TIME = 1000;

  private Image mImage;
  private double mX, mY, mAngle;
  private double mPrevX, mPrevY;
  private double mSpeed;
  private double[] mVelocity = new double[2];
  private boolean[] mBoosters = new boolean[4];

  private int mLives;
  private int mDeadCount;
  private int mBoostTimeout;
  private double impulse_force = 2;

  public HoverCraft(String graphic_file, float x, float y) {
    try {
      mImage = new Image(graphic_file, Color.magenta);
    } catch (SlickException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    mX = x;
    mY = y;
    mAngle = 0;
    mSpeed = 0;
    mVelocity[X] = mVelocity[Y] = 0;
    mBoosters[TOP] = mBoosters[RIGHT] = mBoosters[BOTTOM] = mBoosters[LEFT] = false;
    mDeadCount = 0;
    mLives = 10;
  }

  public void setBooster(int which, boolean state) {
    mBoosters[which] = state;
  }

  public void think(int delta) {
    mBoostTimeout -= delta;
    if(mBoostTimeout < 0) mBoostTimeout = 0;
    
    mPrevX = mX;
    mPrevY = mY;
    mX += mVelocity[X] * delta;
    mY += mVelocity[Y] * delta;
    // Apply booster force
    if (mBoosters[TOP])
      mVelocity[Y] += base_booster_force;
    if (mBoosters[BOTTOM])
      mVelocity[Y] -= base_booster_force;

    if (mBoosters[RIGHT])
      mVelocity[X] -= base_booster_force;
    if (mBoosters[LEFT])
      mVelocity[X] += base_booster_force;

    mVelocity[X] *= Math.pow(friction, delta);
    mVelocity[Y] *= Math.pow(friction, delta);

    if (Math.abs(mVelocity[X]) < min_speed_before_stiction)
      mVelocity[X] = 0;
    if (Math.abs(mVelocity[Y]) < min_speed_before_stiction)
      mVelocity[Y] = 0;

    mSpeed = Math.sqrt(mVelocity[X] * mVelocity[X] + mVelocity[Y]
        * mVelocity[Y]);

    if (mDeadCount > 0)
      mDeadCount -= delta;

    if (mDeadCount < 0 && mLives-- > 0) {
      moveTo(-8192 + CarGame.roadWidth * 32
          + (CarGame.roadWidth + CarGame.buildingWidth)
          * (CarGame.r.nextInt(15) + 1) * 64, -8192 + CarGame.roadWidth * 32
          + (CarGame.roadWidth + CarGame.buildingWidth)
          * (CarGame.r.nextInt(15) + 1) * 64);
      mDeadCount = 0;
    }
  }

  public Image getImage() {
    return mImage;
  }

  public float getX() {
    return (float) mX;
  }

  public float getY() {
    return (float) mY;
  }

  public void moveTo(float x, float y) {
    mX = x;
    mY = y;
  }

  public void setAngle(double angle) {
    mAngle = angle;
  }

  public boolean isDead() {
    return (mDeadCount > 0);
  }

  public void kill() {
    if (mDeadCount <= 0)
      mDeadCount = RESPAWN_TIME;
  }

  public int getLives() {
    return (mLives);
  }

  public double getAngle() {
    return mAngle;
  }

  public double getSpeed() {
    return mSpeed;
  }

  public void setSpeed(double speed) {
    mSpeed = speed;
  }

  public void bounce(Line l, int delta) {
    //System.out.println("BOUNCE");

    double cross = (l.a.x - l.b.x) * mVelocity[Y] - (l.a.y - l.b.y)
        * mVelocity[X];
    double vec_length = Math.sqrt(mVelocity[X] * mVelocity[X] + mVelocity[Y]
        * mVelocity[Y]);
    // divide by zero fix
    if (vec_length == 0)
        vec_length = .00001;
    double a_dot_b = (l.a.x - l.b.x) * mVelocity[X] + (l.a.y - l.b.y)
        * mVelocity[Y];
    double angleBetween = Math.acos(a_dot_b / (l.length() * vec_length));

    //System.out.println("a . b " + a_dot_b);
    //System.out.println("cross " + cross);
    //System.out.println("angle " + Math.toDegrees(angleBetween));

    if (angleBetween > Math.PI / 2)
      angleBetween = Math.PI - angleBetween;

    if (cross > 0)
      angleBetween = -angleBetween;

    //System.out.println("xformed angle " + Math.toDegrees(angleBetween));

    double lVecY = l.a.y - l.b.y;
    double lVecX = l.a.x - l.b.x;
    double angleL = Math.atan(lVecY / lVecX);
    if (angleL < 0) {
        angleL += Math.PI;
    }
    //if (lVecY < 0 && lVecX < 0 || lVecY > 0 && lVecX < 0) {
    //    angleL += 2 * Math.PI;
    //}

    //System.out.println("angleL " + Math.toDegrees(angleL));

    double angleNew;
    if (a_dot_b < 0)
        angleNew = angleL - angleBetween;
    else
        angleNew = angleL + (Math.PI + angleBetween);

    //System.out.println("angleNew " + Math.toDegrees(angleNew));

    mVelocity[X] = vec_length * Math.cos(angleNew) * wall_elasticity;
    mVelocity[Y] = vec_length * Math.sin(angleNew) * wall_elasticity;

    // actually give the player an extra move to get them out
    // of the wall if they are there. A hack certainly, but
    // shouldn't matter too much as long as delta stays low.
    mX += mVelocity[X] * delta;
    mY += mVelocity[Y] * delta;

    // HACK: move the player out of the wall to their previous spot.
    moveTo((float)mPrevX, (float)mPrevY);

    //System.out.printf("%f %f\n", lVecX, lVecY);
    //System.out.printf("new (%f,%f)\n", mVelocity[X], mVelocity[Y]);
  }

  public void boost() {
    if(mBoostTimeout <= 0) {
      Sounds.boost.play();
      mVelocity[X] += Math.cos(mAngle - Math.PI / 2) * impulse_force;
      mVelocity[Y] += Math.sin(mAngle - Math.PI / 2) * impulse_force;
      mBoostTimeout = 2500;
    }
  }

  public void setLives(int lives) {
    mLives = lives;
  }

  public int getBoostTimeout() {
    return mBoostTimeout;
  }
}
