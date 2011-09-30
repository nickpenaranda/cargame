package org.cargame;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.cargame.Network.*;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class GameServer extends Listener {
  Map<Integer,Player> players;
  Server mServer;
  int graphicCounter = 0;

  public GameServer() throws IOException {
    players = Collections.synchronizedMap(new TreeMap<Integer,Player>());
    
    mServer = new Server();
    
    Network.registerClasses(mServer);
    
    mServer.start();
    mServer.bind(Network.TCP_PORT, Network.UDP_PORT);

    mServer.addListener(this);
  }
  
  @Override
  public void connected(Connection connection) {
    System.out.println("CONNECT: " + connection);
  }

  @Override
  public void disconnected(Connection connection) {
    System.out.println("DISCONNECT: " + connection);
    
    // Remove player from player map
    players.remove(connection.getID());
    // Send RM_PLAYER to all others
    ControlMessage resp = new ControlMessage();
    resp.type = Network.CONTROL_RM_PLAYER;
    resp.value = connection.getID();
    mServer.sendToAllTCP(resp);
  }

  @Override
  public void received(Connection connection, Object object) {
    if(object instanceof ControlMessage) {
      ControlMessage msg = (ControlMessage)object;
      switch(msg.type) {
      case Network.CONTROL_CONNECT:
        System.out.println(connection + " REPORTS CONNECT");
        
        int graphic = graphicCounter++ % CarGame.NUM_VEHICLES;
        
        // Send ACK to connecting player
        ControlMessage resp = new ControlMessage();
        resp.type = Network.CONTROL_ACK;
        resp.value = graphic;
        connection.sendTCP(resp);
        
        resp.type = Network.CONTROL_NEW_PLAYER;

        // Send NEW_PLAYERs to connecting player
        for(Player p:players.values()) {
          resp.value = p.id;
          resp.value2 = p.graphic;
          resp.text = p.name;
          connection.sendTCP(resp);
        }
        
        // Add newly connected player to list of players
        players.put(connection.getID(),new Player(connection.getID(),graphic,"Player " + connection.getID()));

        // Send NEW_PLAYER to all others
        resp.value = connection.getID();
        resp.value2 = graphic;
        resp.text = "Player " + connection.getID();
        mServer.sendToAllExceptTCP(connection.getID(), resp);
        break;
      }
    } else if(object instanceof MoveMessage) {
      MoveMessage msg = (MoveMessage)object;
      
      // Tag incoming message with connection ID and broadcast to others UDP
      msg.id = connection.getID();
      mServer.sendToAllExceptUDP(connection.getID(), msg);
    } else if(object instanceof StateMessage) {
      StateMessage msg = (StateMessage)object;
      
      // Tag incoming message with connection ID and broadcast to others UDP
      msg.id = connection.getID();
      mServer.sendToAllExceptUDP(connection.getID(), msg);
    }
  }
  
  class Player {
    public String name;
    public int id;
    public int graphic;
    
    public Player(int id,int graphic,String name) {
      this.id = id;
      this.graphic = graphic;
      this.name = name;
      
    }

    @Override
    public boolean equals(Object obj) {
      if(obj == this) return(true);
      else if(!(obj instanceof Player)) return(false);
      
      Player other = (Player)obj;
      return(this.id == other.id);
    }
  }
  
  public static void main(String[] args) {
    //Log.set(Log.LEVEL_DEBUG);
    try {
      new GameServer();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
