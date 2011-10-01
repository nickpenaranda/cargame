package org.cargame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;

public class CarGame extends BasicGame {
  public static final boolean DEBUG_MODE = true;
  public static final int NUM_VEHICLES = 5;
  public static boolean multiplayer_mode;
  public static final int roadWidth = 6;
  public static final int buildingWidth = 10;
  public static final int tileSize = 64;
  public static Random r;

  public static String playerName = "Player";
  public static String HOST_NAME = "192.168.1.113";

  Map<Integer, HoverCraft> mCars;
  private ArrayList<Boundary> mWalls;
  HoverCraft mPlayerCraft;
  private static final float cloak_alpha = 0.05f;
  private Image[] mTiles;
  private WorldMap mWorldMap;
  GameClient mClient;
  private ArrayList<BoostGhost> mGhosts;
  ArrayList<Explosion> mExplosions;
  ArrayList<Rocket> mRockets;
  List<Message> mMessages;
  private long ticks;
  private StringBuffer mChatBuffer;
  private boolean mChatMode;

  private GameContainer mContainer;

  public CarGame() {
    super("CAR GAME, SON");

    String player_name = System.getProperty("cargame.player_name");
    if (playerName != null)
      playerName = player_name;

    String host_name = System.getProperty("cargame.host_name");
    if (host_name != null)
      HOST_NAME = host_name;

    String mmode = System.getProperty("cargame.multiplayer_mode");
    if (mmode != null)
      multiplayer_mode = Boolean.valueOf(mmode);
    else
      multiplayer_mode = false;
    System.out.println(multiplayer_mode);

    r = new Random();
    mCars = (Map<Integer, HoverCraft>) Collections
        .synchronizedMap(new TreeMap<Integer, HoverCraft>());
    mTiles = new Image[5];
    mWalls = new ArrayList<Boundary>();
    mGhosts = new ArrayList<BoostGhost>();
    mExplosions = new ArrayList<Explosion>();
    mRockets = new ArrayList<Rocket>();
    mWorldMap = new WorldMap();

    mMessages = (List<Message>) Collections
        .synchronizedList(new ArrayList<Message>());
    mChatBuffer = new StringBuffer("");
    mChatMode = false;
  }

  @Override
  public void init(GameContainer container) throws SlickException {
    mContainer = container;
    mTiles[0] = new Image("gfx/road.png");
    mTiles[1] = new Image("gfx/wall1.png");
    mTiles[2] = new Image("gfx/wall2.png");
    mTiles[3] = new Image("gfx/wall3.png");
    mTiles[4] = new Image("gfx/wall4.png");

    genMap();

    HoverCraft.init(this);
    Explosion.init();
    Rocket.init();

    ticks = 0;

    // mCars.put(1, new HoverCraft("gfx/craft2.png", -8192 + roadWidth * 32
    // + (roadWidth + buildingWidth) * (r.nextInt(15) + 1) * 64, -8192
    // + roadWidth * 32 + (roadWidth + buildingWidth) * (r.nextInt(15) + 1)
    // * 64));

    System.out.println("Multiplayer Mode? " + multiplayer_mode);

    mPlayerCraft = new HoverCraft("gfx/craft1.png", -8192 + roadWidth * 32
        + (roadWidth + buildingWidth) * (r.nextInt(15) + 1) * 64, -8192
        + roadWidth * 32 + (roadWidth + buildingWidth) * (r.nextInt(15) + 1)
        * 64, playerName);

    if (multiplayer_mode) {
      try {
        mClient = new GameClient(this);
        mCars.put(mClient.getPlayerId(), mPlayerCraft);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else
      mCars.put(0, mPlayerCraft);

    Sounds.init();
  }

  @Override
  public void update(GameContainer container, int delta) throws SlickException {
    ++ticks;

    // Apply input/physics and generate boosting ghosts
    for (HoverCraft c : mCars.values()) {
      c.think(delta);
      BoostGhost ghost = c.getBoostTimeout() > 2000 && ticks % 3 == 0 ? new BoostGhost(
          c.getX(), c.getY(), c.getAngle(), c.getImage())
          : null;
      if (ghost != null)
        mGhosts.add(ghost);
    }

    // Send updates if applicable
    if (multiplayer_mode) {
      mClient.sendMoveUpdate(mPlayerCraft.getX(), mPlayerCraft.getY(),
          mPlayerCraft.getVX(), mPlayerCraft.getVY(), mPlayerCraft.getAngle(),
          mPlayerCraft.getThrustT(), mPlayerCraft.getThrustR(), mPlayerCraft
              .getThrustB(), mPlayerCraft.getThrustL());
    }

    // Reduce effect life counts
    for (BoostGhost g : new ArrayList<BoostGhost>(mGhosts)) {
      g.life -= delta;
      if (g.life <= 0)
        mGhosts.remove(g);
    }

    for (Explosion e : new ArrayList<Explosion>(mExplosions)) {
      e.life -= delta;
      if (e.life <= 0)
        mExplosions.remove(e);
    }

    for (Message m : new ArrayList<Message>(mMessages)) {
      m.life -= delta;
      if (m.life <= 0)
        mMessages.remove(m);
    }
    
    for (Rocket rk : new ArrayList<Rocket>(mRockets)) {
      rk.life -= delta;
      if (rk.life <= 0) {
        mRockets.remove(rk);
        continue;
      }
      rk.x += rk.vx * delta;
      rk.y += rk.vy * delta;
      
      boolean rk_removed = false;
      // Check collision player car vs other cars
      if (rk.owner != mPlayerCraft) {
        for (HoverCraft other : new ArrayList<HoverCraft>(mCars.values())) {
          if(other == rk.owner)
            continue;
          if (CarGame.distance(rk.x, rk.y, other.getX(), other.getY()) < 32) {
            if (other == mPlayerCraft)
              mMessages.add(new Message("BLOWN UP BY " + rk.owner.getName()));
              mPlayerCraft.kill();
              if(multiplayer_mode)
                mClient.sendStateUpdate(Network.STATE_DEAD, true);
            mRockets.remove(rk);
            rk_removed = true;

            for (int i = 0; i < 5; i++)
              mExplosions.add(new Explosion(rk.x + 32 * r.nextGaussian(), rk.y
                  + 32 * r.nextGaussian()));

            break;
          }
        }
      }
      
      if(!rk_removed) {
        // Collision vs walls
        // TODO: Do collisions against mWorldMap instead of mWalls
        for (int i = 0; i < mWalls.size(); i++) {
          if (mWalls.get(i).intersect(rk.x, rk.y, 16)) {
            mRockets.remove(rk);
            for (int j = 0; j < 5; j++)
              mExplosions.add(new Explosion(rk.x + 32 * r.nextGaussian(), rk.y
                  + 32 * r.nextGaussian()));
            break;
          }
        }
      }
    }

    if (mPlayerCraft.isDead() && mPlayerCraft.getDeadCount() < 0) {
      if(multiplayer_mode)
        mClient.sendStateUpdate(Network.STATE_DEAD, false);
      mPlayerCraft.restore();
    }

    else if (mPlayerCraft.isDead())
      return;

    // Check collision player car vs other cars
    ArrayList<HoverCraft> otherCars = new ArrayList<HoverCraft>(mCars.values());
    otherCars.remove(mPlayerCraft);
    for (HoverCraft other : otherCars) {
      if (CarGame.distance(mPlayerCraft.getX(), mPlayerCraft.getY(), other
          .getX(), other.getY()) < 64
          && Math.abs(mPlayerCraft.getSpeed()) < Math.abs(other.getSpeed())) {
        mMessages.add(new Message("RUN OVER BY " + other.getName()));
        mPlayerCraft.kill();
        if(multiplayer_mode)
          mClient.sendStateUpdate(Network.STATE_DEAD, true);
      }
    }

    // Collision vs walls
    // TODO: Do collisions against mWorldMap instead of mWalls
    for (int i = 0; i < mWalls.size(); i++) {
      if (mWalls.get(i).intersect(mPlayerCraft.getX(), mPlayerCraft.getY(), 31)) {
        mPlayerCraft.bounce(mWalls.get(i), delta);
        Sounds.bounce.play((float) (1 + r.nextGaussian() / 5), 1.0f);
      }
    }
  }

  static double distance(double x1, double y1, double x2, double y2) {
    return (Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
  }

  public void render(GameContainer container, Graphics g) throws SlickException {
    
    g.setAntiAlias(true);
    
    float scale_factor = 1 / (1 + (float) Math.pow(
        mPlayerCraft.getAverageSpeed(), 3));
    
    if (scale_factor < 0.2)
      scale_factor = 0.2f;
    else if(scale_factor > 0.5f)
      scale_factor = 0.5f;

    g.scale(scale_factor, scale_factor);
    
    g.setColor(Color.white);

    // //////////////////
    // World Relative //
    // //////////////////

    g.translate(-(mPlayerCraft.getX() - 320 / scale_factor),
        -(mPlayerCraft.getY() - 240 / scale_factor));

    Rectangle viewPort = new Rectangle(
        mPlayerCraft.getX() - 320 / scale_factor, mPlayerCraft.getY() - 240
            / scale_factor, 640 / scale_factor, 480 / scale_factor);
    mWorldMap.render(g, viewPort);

    // Draw boundaries
    for (Boundary boundary : mWalls) {
      g.drawLine((float) boundary.a.x, (float) boundary.a.y,
          (float) boundary.b.x, (float) boundary.b.y);
    }

    // Draw boost ghosts
    for (BoostGhost ghost : mGhosts) {
      Image image = ghost.image;
      image.setRotation((float) (ghost.angle * 180 / Math.PI));
      int life = ghost.life;
      image.setAlpha(life / (float) 250 * 0.3f);
      image.drawCentered(ghost.x, ghost.y);
    }

    // Draw rockets
    for (Rocket rk : new ArrayList<Rocket>(mRockets)) {
      Rocket.image.setRotation((float) (rk.angle * 180 / Math.PI));
      Rocket.image.drawCentered((float)rk.x,(float)rk.y);
    }


    // Draw cars
    for (HoverCraft car : mCars.values()) {
      if (car.isDead())
        continue;
      Image image = car.getImage();
      image.setRotation((float) (car.getAngle() * 180 / Math.PI));
      int jammer = car.getJammer();
      if (jammer > 500)
        image.setAlpha(cloak_alpha);
      else if (jammer > 0)
        image.setAlpha(cloak_alpha + (500 - jammer) / (float) 500
            * (1 - cloak_alpha));
      else {
        g.setColor(Color.white);
        if (car != mPlayerCraft)
          g.drawString(car.getName(),
              car.getX() - g.getFont().getWidth(car.getName()) / 2,
              car.getY() + 40);
        image.setAlpha(1.0f);
      }
      image.drawCentered(car.getX(), car.getY());
    }
    
    // Draw explosions
    for (Explosion e : mExplosions) {
      e.getImage().drawCentered((float) e.x, (float) e.y);
    }

    // ///////////////////
    // Screen relative //
    // ///////////////////
    g.resetTransform();

    // draw indicator
    for (HoverCraft craft : mCars.values()) {
      if (craft.getJammer() <= 0) {
        int min_len = 33;
        int max_len = 220;
        double angle = Math.atan((craft.getY() - mPlayerCraft.getY())
            / (craft.getX() - mPlayerCraft.getX()));
        if (craft.getX() > mPlayerCraft.getX())
          angle += Math.PI;

        double car_dist = Math.sqrt(Math.pow(
            craft.getY() - mPlayerCraft.getY(), 2)
            + Math.pow(craft.getX() - mPlayerCraft.getX(), 2));
        double len = min_len + car_dist / 33;
        if (len > max_len)
          len = max_len;

        float x = (float) ((640 / 2) - len * Math.cos(angle));
        float y = (float) ((480 / 2) - len * Math.sin(angle));

        g.setColor(Color.red);
        g.fillOval(x, y, (float) 5.0, (float) 5.0);
        g.setColor(Color.orange);
        g.drawOval(x, y, (float) 6.0, (float) 6.0);
      }
    }
    // Draw boost indicator
    g.setColor(Color.white);
    g.drawRect(280, 15, 80, 5);
    if (mPlayerCraft.getBoostTimeout() > 0)
      g.setColor(Color.yellow);
    else
      g.setColor(Color.green);
    g.fillRect(
        281,
        16,
        79 * (1 - (mPlayerCraft.getBoostTimeout() / (float) HoverCraft.BOOST_TIMEOUT)),
        4);

    // Draw jammer indicator
    g.setColor(Color.white);
    g.drawRect(280, 22, 80, 5);
    if (mPlayerCraft.getJammerTimeout() > 0)
      g.setColor(Color.gray);
    else
      g.setColor(Color.cyan);
    g.fillRect(
        281,
        23,
        79 * (1 - (mPlayerCraft.getJammerTimeout() / (float) HoverCraft.JAMMER_TIMEOUT)),
        4);

    g.setColor(Color.white);
    g.drawString("Speed = " + mPlayerCraft.getSpeed(), 10, 30);
    g.drawString(
        String.format("(%f,%f)", mPlayerCraft.getX(), mPlayerCraft.getY()), 10,
        45);

    // Scoreboard
    g.setColor(Color.green);
    g.drawString("You: " + mPlayerCraft.getLives(), 10,
        container.getHeight() - 15);

    if (mPlayerCraft.isDead()) {
      g.setColor(Color.red);
      g.drawString("!!!!BOOM SUCKA!!!!",
          320 - g.getFont().getWidth("!!!!BOOM SUCKA!!!") / 2, 240);
    }

    // Chat log
    int i = 0;
    for (Message m : new ArrayList<Message>(mMessages)) {
      g.drawString(m.text, 15, container.getHeight() - 45 - (i * 15));
      ++i;
    }

    // Chat buffer
    if (mChatMode) {
      g.drawString("> " + mChatBuffer.toString(), 15,
          container.getHeight() - 30);
      g.fillRect(17 + g.getFont().getWidth("> " + mChatBuffer.toString()),
          container.getHeight() - 26, 2, 12);
    }
  }

  @Override
  public void keyPressed(int key, char c) {
    if (mChatMode) {
      switch (key) {
      case Input.KEY_ESCAPE: // Close chat mode without sending, do not clear
                             // buffer
        mChatMode = false;
        break;
      case Input.KEY_ENTER: // Send message, clear buffer, close chat mode
        if (multiplayer_mode && mChatBuffer.length() > 0)
          mClient.sendChatMessage(mChatBuffer.toString());
        mChatBuffer.delete(0, mChatBuffer.length());
        mChatMode = false;
        break;
      case Input.KEY_BACK:
        if (mChatBuffer.length() > 0)
          mChatBuffer.deleteCharAt(mChatBuffer.length() - 1);
        break;
      default:
        if (c >= 32 && c <= 126) // Sane characters
          mChatBuffer.append(c);
        break;
      }
    } else {
      switch (key) {
      // Non player control stuff
      case Input.KEY_ESCAPE:
        System.exit(0);
        break;

      // Player control stuff
      case Input.KEY_W:
        mPlayerCraft.setBooster(HoverCraft.BOTTOM, true);
        break;
      case Input.KEY_S:
        mPlayerCraft.setBooster(HoverCraft.TOP, true);
        break;
      case Input.KEY_A:
        mPlayerCraft.setBooster(HoverCraft.RIGHT, true);
        break;
      case Input.KEY_D:
        mPlayerCraft.setBooster(HoverCraft.LEFT, true);
        break;
      case Input.KEY_Q:
        if (multiplayer_mode)
          mClient.sendStateUpdate(Network.STATE_JAM, true);
        mPlayerCraft.jammer();
        break;
      case Input.KEY_E:
        mPlayerCraft.rocket();
        break;
      case Input.KEY_F1:
        try {
          mContainer.setFullscreen(!mContainer.isFullscreen());
        } catch (SlickException e) {
          e.printStackTrace();
        }
        break;
      case Input.KEY_F2:
        Sounds.mute = !Sounds.mute;
        mMessages.add(new Message("SOUNDS " + (Sounds.mute ? "OFF" : "ON")));
        break;
      case Input.KEY_K:
        mPlayerCraft.kill();
        break;
      case Input.KEY_ENTER:
        mChatMode = true;
        break;
      }
    }
  }

  @Override
  public void keyReleased(int key, char c) {
    switch (key) {
    // Player control stuff
    case Input.KEY_W:
      mPlayerCraft.setBooster(HoverCraft.BOTTOM, false);
      break;
    case Input.KEY_S:
      mPlayerCraft.setBooster(HoverCraft.TOP, false);
      break;
    case Input.KEY_A:
      mPlayerCraft.setBooster(HoverCraft.RIGHT, false);
      break;
    case Input.KEY_D:
      mPlayerCraft.setBooster(HoverCraft.LEFT, false);
      break;
    }
  }

  @Override
  public void mouseMoved(int oldx, int oldy, int newx, int newy) {
    int x = newx - 320;
    int y = newy - 240;

    if (x > 0)
      mPlayerCraft.setAngle(Math.atan(y / (double) x) + Math.PI / 2);
    else if (x < 0)
      mPlayerCraft.setAngle(Math.atan(y / (double) x) + Math.PI + Math.PI / 2);
    else {
      if (y < 0)
        mPlayerCraft.setAngle(0);
      else
        mPlayerCraft.setAngle(Math.PI);
    }
  }

  @Override
  public void mouseClicked(int button, int x, int y, int clickCount) {
    switch (button) {
    case 0: // Left
      if (multiplayer_mode)
        mClient.sendStateUpdate(Network.STATE_BOOST, true);
      mPlayerCraft.boost();
      break;
    }
  }
  
  private Image genRandomTexture(int numTilesWidth, int numTilesHeight) {
    Image texture = null;
    Graphics g = null;
    System.out.println("CALLED");
    try {
      texture = new Image(numTilesWidth * tileSize, numTilesHeight * tileSize);
      g = texture.getGraphics();
    } catch (SlickException e) {
      e.printStackTrace();
    }
    for (int i=0; i<numTilesWidth; i++) {
      for (int j=0; j<numTilesHeight; j++) {
        g.drawImage(mTiles[r.nextInt(4)+1], i * tileSize, j * tileSize);
      }
    }
    return texture;
  }

  // Generates a map with grid roads.
  private void genMap() {
    // Set tiles
    int numBuildingsAcross = 16;
    int cityBlockWidth = roadWidth + buildingWidth;
    int offset = numBuildingsAcross / 2; // center city at (0, 0)
    
    Image textures[] = new Image[8];
    for (int i=0; i<textures.length;i++) {
      textures[i] = genRandomTexture(8, 8);
    }

    for (int i = 0; i < numBuildingsAcross; i++) {
      int buildingL = ((i - offset) * cityBlockWidth + roadWidth) * tileSize;
      for (int j = 0; j < numBuildingsAcross; j++) {
        int buildingT = ((j - offset) * cityBlockWidth + roadWidth) * tileSize;
        Rectangle rect = new Rectangle(buildingL, buildingT, buildingWidth
            * tileSize, buildingWidth * tileSize);
        // TODO: create random texture for each building.
        WorldMap.Wall wall = new WorldMap.Wall(
            new Polygon(rect.getPoints()),
            textures[r.nextInt(8)],
            1.0f);
        mWorldMap.AddWall(wall);
      }
    }

    // Set boundaries
    mWalls.add(new Boundary(-8192, -8192, 8192, -8192, 1));
    mWalls.add(new Boundary(-8192, -8192, -8192, 8192, 1));
    mWalls.add(new Boundary(8192, -8192, 8192, 8192, 1));
    mWalls.add(new Boundary(-8192, 8192, 8192, 8192, 1));

    for (int x = 1; x <= 16; ++x) {
      for (int y = 1; y <= 16; ++y) {
        mWalls.add(new Boundary(-8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x - 1)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y - 1)), -8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y - 1)), 1));
        mWalls.add(new Boundary(-8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x - 1)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y - 1)), -8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x - 1)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y)), 1));
        mWalls.add(new Boundary(-8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y - 1)), -8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y)), 1));
        mWalls.add(new Boundary(-8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x - 1)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y)), -8192 + (roadWidth * 64 * x)
            + (buildingWidth * 64 * (x)), -8192 + (roadWidth * 64 * y)
            + (buildingWidth * 64 * (y)), 1));
      }
    }
    int numRoads = 256 / cityBlockWidth;
    int numBarriers = r.nextInt(40) + 10;

    // Remove to reenable barriers once sending to server works.
    numBarriers = 0;
    for (int i = 0; i < numBarriers; i++) {
      // specifices intersection: (0, 0) topleft
      int barrierX = r.nextInt(numRoads - 1);
      int barrierY = r.nextInt(numRoads - 1);
      boolean barrierHoriz = r.nextInt(2) == 1;
      System.out.println(barrierX + " " + barrierY + " " + barrierHoriz);

      int barrierTop = barrierX * cityBlockWidth + cityBlockWidth / 2
          + roadWidth / 2 - 1;
      int barrierLeft = barrierY * cityBlockWidth;
      int barrierRight = barrierY * cityBlockWidth + roadWidth - 1;
      // flatLine(barrierBottom, barrierLeft, barrierRight, barrierHoriz, -1);
      // flatLine(barrierTop, barrierLeft, barrierRight, barrierHoriz, -1);
      int boundStartX = tileToPixel(barrierLeft);
      int boundStartY = tileToPixel(barrierTop);
      int boundEndX = tileToPixel(barrierRight);
      int boundEndY = tileToPixel(barrierTop);
      mWalls.add(new Boundary(boundStartX, boundStartY, boundEndX + 64,
          boundEndY, 1, !barrierHoriz));
      mWalls.add(new Boundary(boundStartX, boundStartY + 128, boundEndX + 64,
          boundEndY + 128, 1, !barrierHoriz));
    }
  }

  private int tileToPixel(int tileNum) {
    return -8192 + tileNum * 64;
  }

  // private void flatLine(int offset, int start, int end, boolean horiz, int
  // val) {
  // boolean random = val == -1;
  // int y = offset;
  // for (int x = start; x <= end; x++) {
  // if (random) {
  // val = r.nextInt(4) + 1;
  // }
  // if (horiz)
  // mMap[x][y] = val;
  // else
  // mMap[y][x] = val;
  // }
  // }

  public static void main(String[] args) {
    try {
      AppGameContainer appGameContainer = new AppGameContainer(new CarGame());
      appGameContainer.setDisplayMode(640, 480, !DEBUG_MODE);
      appGameContainer.setMinimumLogicUpdateInterval(20);
      appGameContainer.setAlwaysRender(true);
      // appGameContainer.setMaximumLogicUpdateInterval(60);
      // appGameContainer.setVSync(true);
      appGameContainer.start();
    } catch (SlickException e) {
      e.printStackTrace();
    }
  }

  public static class Message {
    private static final int MESSAGE_DURATION = 5000;

    public int life;
    public String text;

    public Message(String text) {
      this.text = text;
      life = MESSAGE_DURATION;
    }
  }
}
