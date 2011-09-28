package org.cargame;

import java.net.*;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.clientserver.*;
import com.captiveimagination.jgn.event.*;

/**
 * @author Matthew D. Hicks
 */
public class Client extends DynamicMessageAdapter {
  private JGNClient client;
  private int seq = 0;
  private UpdateMessage newMessage = null;

  public Client() throws Exception {
    JGN.register(UpdateMessage.class);

    client = new JGNClient();
    client.addMessageListener(this);
    // client.addMessageListener(new DebugListener("ChatClient>"));
    JGN.createThread(1, client).start();

    InetSocketAddress reliableServerAddress = new InetSocketAddress(InetAddress
        .getLocalHost(), 2000);
    InetSocketAddress fastServerAddress = new InetSocketAddress(InetAddress
        .getLocalHost(), 2001);

    System.out.println("connecting");
    client.connectAndWait(reliableServerAddress, fastServerAddress, 1000);
    System.out.println("connected");

    UpdateMessage message = new UpdateMessage();
    message.connecting = true;
    client.sendToServer(message);
    waitForPlayers();
  }

  public void waitForPlayers() throws Exception {
    UpdateMessage message = nextMessage(0);
    System.out.println("WAITING");
    if (message.ready) {
      System.out.println("READY!");
    } else {
      System.out.println("WTF");
    }
  }

  public UpdateMessage doUpdate(double x, double y, double angle) throws Exception {
    UpdateMessage message = new UpdateMessage();
    message.setPlayerId(client.getPlayerId());
    message.seq = seq;
    message.x = x;
    message.y = y;
    message.angle = angle;

    client.broadcast(message);

    UpdateMessage response = nextMessage(seq);
    seq++;
    return response;
  }

  private UpdateMessage nextMessage(int expected_seq) throws Exception {
    // validate seq?
    while (true) {
      if (newMessage != null) {
         System.out.println("got it");
        UpdateMessage message = newMessage;
        newMessage = null;
        return message;
      }
      Thread.sleep(1);
    }
  }

  public void messageReceived(UpdateMessage message) {
     System.out.println("RECEIVED MESSAGE: " + message);
     newMessage = message;
  }
}
