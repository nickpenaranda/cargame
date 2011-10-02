package org.cargame;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public class Car {
  public static final boolean _ROCKETS_INHERIT_SPEED = false;
  
  // private static final Random r = new Random();

  // === Class properties ===
  // --- Thruster indices ---
  public static final int TOP = 0;
  public static final int RIGHT = 1;
  public static final int BOTTOM = 2;
  public static final int LEFT = 3;

  // --- Physics constants ---
  //private static final double BASE_THRUSTER_FORCE = 0.001;
  private static final double BASE_THRUSTER_FORCE = 0.005;
  private static final double MIN_SPEED_BEFORE_STICTION = BASE_THRUSTER_FORCE / 2;
  //private static final double FRICTION = 0.99937;
  private static final double FRICTION = .998;
  private static final double WALL_ELASTICITY = 0.4;
  private static final double DISTANCE_INTEGRAL_MILLIS = 500;

  // --- Life/ability constants ---
  public static final int START_LIVES = 10;
  public static final int RESPAWN_TIMEOUT = 1000;
  public static final int JAMMER_TIMEOUT = 30000;
  public static final int JAMMER_EFFECT = 10000;
  public static final int BOOST_TIMEOUT = 2500;
  public static final int ROCKET_TIMEOUT = 500;

  private static final double ROCKET_LAUNCH_FACTOR = 2.5;
  private static final double IMPULSE_FORCE = 2;

  public static final int NUM_VEHICLES = 5;
  private static Image[] vehicleGraphics;
  private static World mWorld;

  // === Object fields ====
  // --- General ---
  private Image mImage;
  private String mName;

  // --- Respawn/life ---
  private int mLives;
  private int mDeadTimeout;
  private int mStuckCount;
  private boolean mDead;

  // --- Physics and movement related ---
  private double mX, mY, mVX, mVY, mAngle;
  private double mPrevX, mPrevY;
  private double mSpeed;
  private boolean[] mThrusters = new boolean[4];

  // --- Abilities ---
  private int mBoostTimeout;
  private int mJammerTimeout;
  private int mRocketTimeout;
  private int mJammerEffect;

  private double mDistanceLastNSecs;
  private int mDistanceMillisSaved;
  private double mAverageSpeed;

  public static void init() {
    try {
      vehicleGraphics = new Image[] { new Image( "gfx/craft1.png", Color.magenta ),
          new Image( "gfx/craft2.png", Color.magenta ),
          new Image( "gfx/craft3.png", Color.magenta ),
          new Image( "gfx/craft4.png", Color.magenta ), new Image( "gfx/craft5.png", Color.magenta ) };
    } catch (SlickException e) {
      e.printStackTrace();
    }
  }

  public Car(int graphic_index, double x, double y, String name) {
    mImage = vehicleGraphics[graphic_index];
    mX = x;
    mY = y;
    mName = name;

    mAngle = 0;
    mSpeed = 0;
    mVX = mVY = 0;
    mThrusters[TOP] = mThrusters[RIGHT] = mThrusters[BOTTOM] = mThrusters[LEFT] = false;
    mDeadTimeout = 0;

    mLives = START_LIVES;
    mStuckCount = 0;
  }

  public void setBooster( int which, boolean state ) {
    mThrusters[which] = state;
  }

  public void think( int delta ) {
    mStuckCount--;
    if(mStuckCount < 0)
      mStuckCount = 0;

    // Update timers
    mBoostTimeout -= delta;
    mJammerTimeout -= delta;
    mRocketTimeout -= delta;
    mJammerEffect -= delta;
    if (mDeadTimeout >= 0)
      mDeadTimeout -= delta;

    if (mRocketTimeout < 0)
      mRocketTimeout = 0;
    if (mBoostTimeout < 0)
      mBoostTimeout = 0;
    if (mJammerTimeout < 0)
      mJammerTimeout = 0;
    if (mJammerEffect < 0)
      mJammerEffect = 0;

    mPrevX = mX;
    mPrevY = mY;

    mX += mVX * delta;
    mY += mVY * delta;

    // Apply booster force -- non exclusive to avoid arbitrary "preference"
    if (mThrusters[TOP])
      mVY += BASE_THRUSTER_FORCE * delta;
    if (mThrusters[BOTTOM])
      mVY -= BASE_THRUSTER_FORCE * delta;
    if (mThrusters[RIGHT])
      mVX -= BASE_THRUSTER_FORCE * delta;
    if (mThrusters[LEFT])
      mVX += BASE_THRUSTER_FORCE * delta;

    mVX *= Math.pow( FRICTION, delta );
    mVY *= Math.pow( FRICTION, delta );

    if (Math.abs( mVX ) < MIN_SPEED_BEFORE_STICTION)
      mVX = 0;
    if (Math.abs( mVY ) < MIN_SPEED_BEFORE_STICTION)
      mVY = 0;

    mSpeed = Math.sqrt( mVX * mVX + mVY * mVY );

    // average speed over last distance_integral_seconds
    mDistanceLastNSecs += mSpeed * delta;
    mDistanceMillisSaved += delta;
    mAverageSpeed = mDistanceLastNSecs / mDistanceMillisSaved;

    // This is an approximation but it should work.
    if (mDistanceMillisSaved > DISTANCE_INTEGRAL_MILLIS) {
      mDistanceLastNSecs -= mAverageSpeed * 200;
      mDistanceMillisSaved -= 200;
    }

  }

  public void setPosition( double x, double y ) {
    mX = x;
    mY = y;
  }

  public void setAngle( double angle ) {
    mAngle = angle;
  }

  public void setSpeed( double speed ) {
    mSpeed = speed;
  }

  public void kill() {
    if (mDeadTimeout <= 0) {
      mDeadTimeout = RESPAWN_TIMEOUT;
      mDead = true;
      mVX = 0;
      mVY = 0;
      // Perhaps this should be automated elsewhere
      mWorld.createExplosion( mX, mY );
    }
  }

  public void moveToSpawn() {
    // TODO Make this spawn player at a spawn point item, when those are implemented...
    setPosition( -8192 + World.ROAD_WIDTH * 32 + (World.ROAD_WIDTH + World.BUILDING_WIDTH)
        * (CarGame.r.nextInt( 15 ) + 1) * 64, -8192 + World.ROAD_WIDTH * 32
        + (World.ROAD_WIDTH + World.BUILDING_WIDTH) * (CarGame.r.nextInt( 15 ) + 1) * 64 );
  }

  public void restore() {
    if (mLives > 0) {
      System.out.println( "Restoring" );
      --mLives;
      moveToSpawn();
      mDeadTimeout = 0;
      mDead = false;
    }
  }

  public Image getImage() {
    return mImage;
  }

  public float getX() {
    return (float)mX;
  }

  public float getY() {
    return (float)mY;
  }

  public boolean isDead() {
    return(mDead);
  }

  public int getLives() {
    return(mLives);
  }

  public double getAngle() {
    return mAngle;
  }

  public double getSpeed() {
    return mSpeed;
  }

  public double getAverageSpeed() {
    return mAverageSpeed;
  }

  public void bounce( double x1, double y1, double x2, double y2, Region rg, int delta ) {
    double ldx = x1 - x2, ldy = y1 - y2;

    double line_length = Math.sqrt( ldx * ldx + ldy * ldy );
    double ndx = ldx / line_length, ndy = ldy / line_length;

    // System.out.printf( "ndx = %f, ndy = %f\n",ndx,ndy );
    double line_dot_vector = ndx * mVX + ndy * mVY;

    mVX = -mVX + ndx * 2 * line_dot_vector;
    mVY = -mVY + ndy * 2 * line_dot_vector;

    if (mWorld.movingRegions && rg.hasFlag( Region.MOVABLE )) {// Movable region, must add linear and angular velocities
      double rdvx = 0, rdvy = 0;
      double impulseF = Math.sqrt( delta );
      double magicHackF = 1.2; // Anti-sticking coefficient

      rdvx = rg.getVX() * impulseF; // Linear vel
      rdvy = rg.getVY() * impulseF;

      // Angular vel

      // Find pivot -> player position vector, then linear velocity vector of surface contact
      // point
      double dx = mX - rg.getPivotX();
      double dy = mY - rg.getPivotY();
      // double dlen = Math.sqrt(dx * dx + dy * dy);

      rdvx -= dy * rg.getDTheta() * impulseF * magicHackF;
      rdvy -= -dx * rg.getDTheta() * impulseF * magicHackF;
      
      mVX += rdvx;
      mVY += rdvy;
    }

    mVX *= WALL_ELASTICITY;
    mVY *= WALL_ELASTICITY;
    // System.out.printf( "line dot velocity = %f\n", line_dot_vector );

    // HACK: move the player out of the wall to their previous spot.
    setPosition( mPrevX, mPrevY );
    
    mStuckCount += 2;
  }

  // public void bounce( Line l, int delta ) {
  // // System.out.println("BOUNCE");
  //
  // double cross = (l.a.x - l.b.x) * mVY - (l.a.y - l.b.y) * mVX;
  // double vec_length = Math.sqrt( mVX * mVX + mVY * mVY );
  // // divide by zero fix
  // if (vec_length == 0)
  // vec_length = .00001;
  // double a_dot_b = (l.a.x - l.b.x) * mVX + (l.a.y - l.b.y) * mVY;
  // double angleBetween = Math.acos( a_dot_b / (l.length() * vec_length) );
  //
  // // System.out.println("a . b " + a_dot_b);
  // // System.out.println("cross " + cross);
  // // System.out.println("angle " + Math.toDegrees(angleBetween));
  //
  // if (angleBetween > Math.PI / 2)
  // angleBetween = Math.PI - angleBetween;
  //
  // if (cross > 0)
  // angleBetween = -angleBetween;
  //
  // // System.out.println("xformed angle " + Math.toDegrees(angleBetween));
  //
  // double lVecY = l.a.y - l.b.y;
  // double lVecX = l.a.x - l.b.x;
  // double angleL = Math.atan( lVecY / lVecX );
  // if (angleL < 0) {
  // angleL += Math.PI;
  // }
  // // if (lVecY < 0 && lVecX < 0 || lVecY > 0 && lVecX < 0) {
  // // angleL += 2 * Math.PI;
  // // }
  //
  // // System.out.println("angleL " + Math.toDegrees(angleL));
  //
  // double angleNew;
  // if (a_dot_b < 0)
  // angleNew = angleL - angleBetween;
  // else
  // angleNew = angleL + (Math.PI + angleBetween);
  //
  // // System.out.println("angleNew " + Math.toDegrees(angleNew));
  //
  // mVX = vec_length * Math.cos( angleNew ) * WALL_ELASTICITY;
  // mVY = vec_length * Math.sin( angleNew ) * WALL_ELASTICITY;
  //
  // // actually give the player an extra move to get them out
  // // of the wall if they are there. A hack certainly, but
  // // shouldn't matter too much as long as delta stays low.
  // mX += mVX * delta;
  // mY += mVY * delta;
  //
  // // HACK: move the player out of the wall to their previous spot.
  // setPosition( mPrevX, mPrevY );
  //
  // // System.out.printf("%f %f\n", lVecX, lVecY);
  // // System.out.printf("new (%f,%f)\n", mVX, mVY);
  // }

  public boolean boost() {
    if (mBoostTimeout > 0)
      return false;
    mVX += Math.cos( mAngle - Math.PI / 2 ) * IMPULSE_FORCE;
    mVY += Math.sin( mAngle - Math.PI / 2 ) * IMPULSE_FORCE;
    mBoostTimeout = BOOST_TIMEOUT;
    Sounds.boost.playWorld(mX, mY);
    return true;
  }

  public boolean rocket() {
    if (mRocketTimeout > 0)
      return false;
    
    //Sounds.rocket.play();
    if(_ROCKETS_INHERIT_SPEED)
      mWorld.createRocket( this, mX, mY, mVX + Math.cos( mAngle - Math.PI / 2 )
          * ROCKET_LAUNCH_FACTOR, mVY + Math.sin( mAngle - Math.PI / 2 ) * ROCKET_LAUNCH_FACTOR );
    else
      mWorld.createRocket(  this, mX, mY, 
                            Math.cos( mAngle - Math.PI / 2 ) * ROCKET_LAUNCH_FACTOR,
                            Math.sin( mAngle - Math.PI / 2 ) * ROCKET_LAUNCH_FACTOR );
                            
    mRocketTimeout = ROCKET_TIMEOUT;
    return true;
  }

  public boolean jammer() {
    if (mJammerTimeout > 0)
      return false;

    mJammerTimeout = JAMMER_TIMEOUT;
    mJammerEffect = JAMMER_EFFECT;
    Sounds.cloak.playWorld( mX, mY );
    return true;
  }

  public void setLives( int lives ) {
    mLives = lives;
  }

  public int getBoostTimeout() {
    return mBoostTimeout;
  }

  // Used for rendering boost "ghosts"
  public void setBoostTimeout( int boostTimeout ) {
    mBoostTimeout = boostTimeout;
  }

  public void setJammerEffect( int jammer ) {
    mJammerEffect = jammer;
  }

  public int getJammerEffect() {
    return mJammerEffect;
  }

  public int getJammerTimeout() {
    return mJammerTimeout;
  }

  public void setImage( int index ) {
    mImage = vehicleGraphics[index];
  }

  public void setVel( double vx, double vy ) {
    mVX = vx;
    mVY = vy;
  }

  public double getVX() {
    return mVX;
  }

  public double getVY() {
    return mVY;
  }

  public boolean getThrustT() {
    return(mThrusters[TOP]);
  }

  public boolean getThrustR() {
    return(mThrusters[RIGHT]);
  }

  public boolean getThrustB() {
    return(mThrusters[BOTTOM]);
  }

  public boolean getThrustL() {
    return(mThrusters[LEFT]);
  }

  public int getDeadCount() {
    return(mDeadTimeout);
  }

  public String getName() {
    return mName;
  }

  public static void attachTo( World world ) {
    mWorld = world;
  }

  public int getStuckCount() {
    return mStuckCount;
  }
}
