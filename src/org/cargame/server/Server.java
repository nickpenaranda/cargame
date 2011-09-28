package org.cargame.server;

import java.net.*;

import org.cargame.UpdateMessage;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.clientserver.*;
import com.captiveimagination.jgn.event.*;

public class Server extends DynamicMessageAdapter {
  private static int NUM_PLAYERS = 2;
  private int connected_players = 0;

  JGNServer server;

  public Server() throws Exception {
    InetAddress HOST_ADDRESS = InetAddress.getByName("192.168.0.4");
    JGN.register(UpdateMessage.class);
    InetSocketAddress reliableAddress = new InetSocketAddress(HOST_ADDRESS,
        2000);
    InetSocketAddress fastAddress = new InetSocketAddress(HOST_ADDRESS, 2001);
    server = new JGNServer(reliableAddress, fastAddress);
    server.addMessageListener(this);
    JGN.createThread(1, server).start();
  }

  public void connected() {
    System.out.println("CONNECTED");
    connected_players++;
    if (connected_players == NUM_PLAYERS) {
      UpdateMessage message = new UpdateMessage();
      message.ready = true;
      server.sendToAll(message);
    }
  }

  public void messageReceived(UpdateMessage message) {
    if (message.connecting)
      connected();
  }

  public static void main(String[] args) throws Exception {
    new Server();
  }
}
