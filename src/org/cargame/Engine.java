package org.cargame;

import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.geom.Rectangle;

//TODO
//// Apply input/physics and generate boosting ghosts
//for (Car c : mCars.values()) {
//c.think( delta );
//BoostGhost ghost = c.getBoostTimeout() > 2000 ? new BoostGhost( c.getX(), c
//    .getY(), c.getAngle(), c.getImage() ) : null;
//if (ghost != null)
//  mGhosts.add( ghost );
//}
//// Reduce effect life counts This belongs in rendering loop
//for (BoostGhost g : new ArrayList<BoostGhost>( mGhosts )) {
//g.life -= delta;
//if (g.life <= 0)
//  mGhosts.remove( g );
//}
//for (Message m : new ArrayList<Message>( mMessages )) {
//m.life -= delta;
//if (m.life <= 0)
//mMessages.remove( m );
//}

/*
 * Static rendering class
 */
public class Engine {

  private static final float CLOAK_ALPHA = 0.05f;

  private static CarGame game;

  public static void init( CarGame aGame ) {
    game = aGame;
  }

  public static void render( GameContainer container, Graphics g ) {
    Car playerCraft = game.getWorld().getPlayer();

    float scale_factor = 1 / (1 + (float)Math.pow( playerCraft.getAverageSpeed(), 3 ));

    if (scale_factor < 0.2)
      scale_factor = 0.2f;
    else if (scale_factor > 0.5f)
      scale_factor = 0.5f;

    g.scale( scale_factor, scale_factor );

    g.setColor( Color.white );

    // //////////////////
    // World Relative //
    // //////////////////

    g.translate( -(playerCraft.getX() - 320 / scale_factor),
                 -(playerCraft.getY() - 240 / scale_factor) );

    Rectangle viewPort = new Rectangle( playerCraft.getX() - 320 / scale_factor, playerCraft.getY()
        - 240 / scale_factor, 640 / scale_factor, 480 / scale_factor );
    game.getWorld().render( g, viewPort );

    // Draw boundaries
//    for (Boundary boundary : mWalls) {
//      g.drawLine( (float)boundary.a.x, (float)boundary.a.y, (float)boundary.b.x,
//                  (float)boundary.b.y );
//    }

    // Draw boost ghosts
    for (BoostGhost ghost : game.getGhosts()) {
      Image image = ghost.image;
      image.setRotation( (float)(ghost.angle * 180 / Math.PI) );
      int life = ghost.life;
      image.setAlpha( life / (float)250 * 0.3f );
      image.drawCentered( ghost.x, ghost.y );
    }

    // Draw rockets
    for (Rocket rk : new ArrayList<Rocket>( game.getWorld().getRockets() )) {
      Rocket.image.setRotation( (float)(rk.angle * 180 / Math.PI) );
      Rocket.image.drawCentered( (float)rk.x, (float)rk.y );
    }

    // Draw cars
    for (Car car : game.getWorld().getCars().values()) {
      if (car.isDead())
        continue;
      Image image = car.getImage();
      image.setRotation( (float)(car.getAngle() * 180 / Math.PI) );
      int jammer = car.getJammerEffect();
      if (jammer > 500)
        image.setAlpha( CLOAK_ALPHA );
      else if (jammer > 0)
        image.setAlpha( CLOAK_ALPHA + (500 - jammer) / (float)500 * (1 - CLOAK_ALPHA) );
      else {
        g.setColor( Color.white );
        if (car != playerCraft)
          g.drawString( car.getName(), car.getX() - g.getFont().getWidth( car.getName() ) / 2, car
              .getY() + 40 );
        image.setAlpha( 1.0f );
      }
      image.drawCentered( car.getX(), car.getY() );
    }

    // Draw explosions
    for (Explosion e : new ArrayList<Explosion>(game.getWorld().getExplosions())) {
      e.getImage().drawCentered( (float)e.x, (float)e.y );
    }

    // ///////////////////
    // Screen relative //
    // ///////////////////
    g.resetTransform();

    // draw indicator
    for (Car craft : game.getWorld().getCars().values()) {
      if (craft.getJammerEffect() <= 0) {
        int min_len = 33;
        int max_len = 220;
        double angle = Math.atan( (craft.getY() - playerCraft.getY())
            / (craft.getX() - playerCraft.getX()) );
        if (craft.getX() > playerCraft.getX())
          angle += Math.PI;

        double car_dist = Math.sqrt( Math.pow( craft.getY() - playerCraft.getY(), 2 )
            + Math.pow( craft.getX() - playerCraft.getX(), 2 ) );
        double len = min_len + car_dist / 33;
        if (len > max_len)
          len = max_len;

        float x = (float)((640 / 2) - len * Math.cos( angle ));
        float y = (float)((480 / 2) - len * Math.sin( angle ));

        g.setColor( Color.red );
        g.fillOval( x, y, (float)5.0, (float)5.0 );
        g.setColor( Color.orange );
        g.drawOval( x, y, (float)6.0, (float)6.0 );
      }
    }
    // Draw boost indicator
    g.setColor( Color.white );
    g.drawRect( 280, 15, 80, 5 );
    if (playerCraft.getBoostTimeout() > 0)
      g.setColor( Color.yellow );
    else
      g.setColor( Color.green );
    g.fillRect( 281, 16,
                79 * (1 - (playerCraft.getBoostTimeout() / (float)Car.BOOST_TIMEOUT)), 4 );

    // Draw jammer indicator
    g.setColor( Color.white );
    g.drawRect( 280, 22, 80, 5 );
    if (playerCraft.getJammerTimeout() > 0)
      g.setColor( Color.gray );
    else
      g.setColor( Color.cyan );
    g.fillRect( 281, 23,
                79 * (1 - (playerCraft.getJammerTimeout() / (float)Car.JAMMER_TIMEOUT)), 4 );

    g.setColor( Color.white );
    g.drawString( "Speed = " + playerCraft.getSpeed(), 10, 30 );
    g.drawString( String.format( "(%f,%f)", playerCraft.getX(), playerCraft.getY() ), 10, 45 );

    // Scoreboard
    g.setColor( Color.green );
    g.drawString( "You: " + playerCraft.getLives(), 10, container.getHeight() - 15 );

    if (playerCraft.isDead()) {
      g.setColor( Color.red );
      g.drawString( "!!!!BOOM SUCKA!!!!", 320 - g.getFont().getWidth( "!!!!BOOM SUCKA!!!" ) / 2,
                    240 );
    }

    // Chat log
    int i = 0;
    for (Message m : new ArrayList<Message>( game.getMessages() )) {
      g.setColor( m.color );
      g.drawString( m.text, 15, container.getHeight() - 45 - (i * 15) );
      ++i;
    }

    // Chat buffer
    if (game.chatModeEnabled()) {
      g.setColor( Color.gray );
      g.drawString( "> " + game.getChatBuffer().toString(), 15, container.getHeight() - 30 );
      g.fillRect( 17 + g.getFont().getWidth( "> " + game.getChatBuffer().toString() + "."),
                  container.getHeight() - 26, 2, 12 );
    }
  }
}
