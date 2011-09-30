package org.cargame;

import java.io.IOException;
import java.util.Map;

import org.cargame.Network.*;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class GameClient extends Listener {
  Map<Integer, HoverCraft> mPlayers;
  Client mClient;
  boolean mConnected;

  public GameClient(Map<Integer, HoverCraft> players) throws IOException {
    mPlayers = players;

    mClient = new Client();
    Network.registerClasses(mClient);

    mClient.start();
    mClient.connect(5000, CarGame.HOST_NAME, Network.TCP_PORT, Network.UDP_PORT);

    mClient.addListener(this);

    ControlMessage msg = new ControlMessage();
    msg.type = Network.CONTROL_CONNECT;
    msg.text = CarGame.playerName;
    mClient.sendTCP(msg);
    mConnected = true;
  }

  @Override
  public void disconnected(Connection connection) {
    mConnected = false;
  }

  @Override
  public void received(Connection connection, Object object) {
    if (object instanceof ControlMessage) {
      ControlMessage msg = (ControlMessage) object;
      System.out.println("Received ACK, graphic = " + msg.value);
      if (msg.type == Network.CONTROL_ACK) {
        mPlayers.get(connection.getID()).setImage(msg.value);
      } else if(msg.type == Network.CONTROL_NEW_PLAYER) {
        System.out.println("Received NEW_PLAYER: " + msg.value);
        mPlayers.put(msg.value,new HoverCraft(msg.value2,0,0,msg.text));
      } else if(msg.type == Network.CONTROL_RM_PLAYER) {
        System.out.println("Received RM_PLAYER: " + msg.value);
        mPlayers.remove(msg.value);
      }
    } else if (object instanceof MoveMessage) {
      MoveMessage msg = (MoveMessage) object;
      HoverCraft craft = mPlayers.get(msg.id);
      if(craft != null) {
        craft.moveTo(msg.x, msg.y);
        craft.setVel(msg.vx,msg.vy);
        craft.setAngle(msg.angle);
        craft.setBooster(HoverCraft.TOP, msg.t);
        craft.setBooster(HoverCraft.RIGHT, msg.r);
        craft.setBooster(HoverCraft.BOTTOM, msg.b);
        craft.setBooster(HoverCraft.LEFT, msg.l);
      }
    } else if (object instanceof StateMessage) {
      StateMessage msg = (StateMessage) object;
      HoverCraft craft = mPlayers.get(msg.id);
      if(craft != null) {
        switch(msg.state) {
        case Network.STATE_DEAD:
          if(msg.setting) craft.kill();
          else craft.restore();
          break;
        case Network.STATE_JAM:
          if(msg.setting) craft.jammer();
          else craft.setJammer(0);
          break;
        case Network.STATE_BOOST:
          if(msg.setting) craft.setBoostTimeout(HoverCraft.BOOST_TIMEOUT);
        }
      }
    }
  }

  public void sendMoveUpdate(double x, double y, double vx, double vy, double angle,
      boolean t,boolean r,boolean b,boolean l) {
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
      
      mClient.sendUDP(msg);
    }
  }

  public boolean isConnected() {
    return mConnected;
  }

  public int getPlayerId() {
    return mClient.getID();
  }

  public void sendStateUpdate(byte state, boolean setting) {
    if (mConnected) {
      StateMessage msg = new StateMessage();
      msg.state = state;
      msg.setting = setting;
      mClient.sendUDP(msg);
    }
  }
}
