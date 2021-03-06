package org.cargame;

import java.io.IOException;
import org.cargame.Network.*;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class GameClient extends Listener {

  CarGame mCarGame;
  Client mClient;
  boolean mConnected;

  public GameClient(CarGame game) throws IOException {
    mCarGame = game;

    mClient = new Client();
    Network.registerClasses( mClient );

    mClient.start();
    mClient.connect( 5000, CarGame.hostName, Network.TCP_PORT, Network.UDP_PORT );

    mClient.addListener( this );

    ControlMessage msg = new ControlMessage();
    msg.type = Network.CONTROL_CONNECT;
    msg.text = CarGame.playerName;
    mClient.sendTCP( msg );
    mConnected = true;
  }

  @Override
  public void disconnected( Connection connection ) {
    mConnected = false;
  }

  @Override
  public void received( Connection connection, Object object ) {
    if (object instanceof ControlMessage) {
      ControlMessage msg = (ControlMessage)object;
      System.out.println( "Received ACK, graphic = " + msg.value );
      if (msg.type == Network.CONTROL_ACK) {
        mCarGame.getWorld().getCars().get( connection.getID() ).setImage( msg.value );
      } else if (msg.type == Network.CONTROL_NEW_PLAYER) {
        System.out.println( "Received NEW_PLAYER: " + msg.value );
        mCarGame.getWorld().getCars().put( msg.value, 
                                           new Car( msg.value2, 0, 0, msg.text ) );
        mCarGame.message( msg.text + " JOINS THE GAME" );
      } else if (msg.type == Network.CONTROL_RM_PLAYER) {
        System.out.println( "Received RM_PLAYER: " + msg.value );
        mCarGame.message( mCarGame.getWorld().getCars().get( msg.value ).getName()
                          + " LEAVES THE GAME" );
        mCarGame.getWorld().getCars().remove( msg.value );
      }
    } else if (object instanceof MoveMessage) {
      MoveMessage msg = (MoveMessage)object;
      Car craft = mCarGame.getWorld().getCars().get( msg.id );
      if (craft != null) {
        craft.setPosition( msg.x, msg.y );
        craft.setVel( msg.vx, msg.vy );
        craft.setAngle( msg.angle );
        craft.setBooster( Car.TOP, msg.t );
        craft.setBooster( Car.RIGHT, msg.r );
        craft.setBooster( Car.BOTTOM, msg.b );
        craft.setBooster( Car.LEFT, msg.l );
      }
    } else if (object instanceof StateMessage) {
      StateMessage msg = (StateMessage)object;
      Car craft = mCarGame.getWorld().getCars().get( msg.id );
      if (craft != null) {
        switch (msg.state) {
          case Network.STATE_DEAD:
            if (msg.setting) {
              craft.kill();
              mCarGame.message(craft.getName() + " DIED" );
            } else
              craft.restore();
            break;
          case Network.STATE_JAM:
            if (msg.setting)
              craft.jammer();
            else
              craft.setJammerEffect( 0 );
            break;
          case Network.STATE_BOOST:
            if (msg.setting) {
              craft.setBoostTimeout( Car.BOOST_TIMEOUT );
              Sounds.boost.playWorld(craft.getX(),craft.getY());
            }
            break;
        }
      }
    } else if (object instanceof ChatMessage) {
      System.out.println( "Chat message received" );
      ChatMessage msg = (ChatMessage)object;
      mCarGame.message( mCarGame.getWorld().getCars().get( msg.id ).getName() + ": "
          + msg.text );
      Sounds.chat.play();
      System.out.println( "Chat message received" );
    } else if (object instanceof CommandMessage) {
      CommandMessage msg = (CommandMessage)object;
      switch (msg.type) {
        case Network.COMMAND_KILL_SELF:
          mCarGame.getWorld().getPlayer().kill();
          mCarGame.message( "KILLED BY SERVER. RIP SUCKA" );
          break;
      }
    } else if (object instanceof RocketMessage) {
      RocketMessage msg = (RocketMessage)object;
      mCarGame.getWorld().createRocket( mCarGame.getWorld().getCars().get( msg.id ),
                                        msg.x, msg.y, msg.vx, msg.vy );
    }
  }

  public void sendMoveUpdate( double x, double y, double vx, double vy, double angle, boolean t,
                              boolean r, boolean b, boolean l ) {
    if (mConnected) {
      MoveMessage msg = new MoveMessage();
      msg.x = x;
      msg.y = y;
      msg.vx = vx;
      msg.vy = vy;
      msg.angle = angle;
      msg.t = t;
      msg.r = r;
      msg.b = b;
      msg.l = l;

      mClient.sendUDP( msg );
    }
  }

  public boolean isConnected() {
    return mConnected;
  }

  public int getPlayerId() {
    return mClient.getID();
  }

  public void sendStateUpdate( byte state, boolean setting ) {
    if (mConnected) {
      StateMessage msg = new StateMessage();
      msg.state = state;
      msg.setting = setting;
      mClient.sendUDP( msg );
    }
  }

  public void sendChatMessage( String text ) {
    if (mConnected) {
      ChatMessage msg = new ChatMessage();
      msg.text = text;
      mClient.sendTCP( msg );
      System.out.println( "Message sent." );
    }
  }

  public void sendRocket( Rocket rk ) {
    if (mConnected) {
      RocketMessage msg = new RocketMessage();
      msg.x = rk.x;
      msg.y = rk.y;
      msg.vx = rk.vx;
      msg.vy = rk.vy;
      msg.angle = rk.angle;
      mClient.sendTCP( msg );
    }
  }
}
