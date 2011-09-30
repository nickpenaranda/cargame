package org.cargame.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cargame.CarGame;
import org.cargame.Network;
import org.cargame.Network.*;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class GameServer extends Listener {
	Map<Integer, Player> players;
	Server mServer;
	int graphicCounter = 0;

	public GameServer() throws IOException {
		players = Collections.synchronizedMap(new TreeMap<Integer, Player>());

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
      
      //--- CONTROL MESSAGES ---
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
        players.put(connection.getID(),new Player(connection.getID(),graphic,msg.text));

        // Send NEW_PLAYER to all others
        resp.value = connection.getID();
        resp.value2 = graphic;
        resp.text = msg.text;
        mServer.sendToAllExceptTCP(connection.getID(), resp);
        break;
      }
    } 
    
    //--- MOVEMENT AND STATE MESSAGES (SIMPLE RELAY) ---
    else if(object instanceof MoveMessage) {
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
    
    //--- CHAT MESSAGES ---
    else if(object instanceof ChatMessage) {
    	ChatMessage msg = (ChatMessage)object;
    	System.out.println("MESSAGE FROM " + players.get(connection.getID()).name + ": " + msg.text);
    	if(msg.text.charAt(0) == Network.COMMAND_CHARACTER) {
    		List<String> parsedMsg = parseMessage(msg.text);
    		// Do something
    	} else { // Relay message to other clients
    	  msg.id = connection.getID();
    		mServer.sendToAllTCP(msg);
    	}
    		
    }

  }

	/*
	 * @author Jan Goyvaerts (Stack Overflow)
	 * http://stackoverflow.com/questions/366202
	 *   /regex-for-splitting-a-string-using
	 *   -space-when-not-surrounded-by-single-or-double
	 */
	private List<String> parseMessage(String s) {
		List<String> matchList = new ArrayList<String>();
		Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
		Matcher regexMatcher = regex.matcher(s);
		while (regexMatcher.find()) {
			if (regexMatcher.group(1) != null) {
				// Add double-quoted string without the quotes
				matchList.add(regexMatcher.group(1));
			} else if (regexMatcher.group(2) != null) {
				// Add single-quoted string without the quotes
				matchList.add(regexMatcher.group(2));
			} else {
				// Add unquoted word
				matchList.add(regexMatcher.group());
			}
		}

		return (matchList);
	}

	class Player {
		public String name;
		public int id;
		public int graphic;

		public Player(int id, int graphic, String name) {
			this.id = id;
			this.graphic = graphic;
			this.name = name;

		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return (true);
			else if (!(obj instanceof Player))
				return (false);

			Player other = (Player) obj;
			return (this.id == other.id);
		}
	}

	public static void main(String[] args) {
		// Log.set(Log.LEVEL_DEBUG);
		try {
			new GameServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
