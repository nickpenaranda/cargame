package org.cargame;

import java.net.*;
import java.util.Calendar;

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
        .getByName("192.168.0.4"), 2000);
    InetSocketAddress fastServerAddress = new InetSocketAddress(InetAddress
        .getByName("192.168.0.4"), 2001);

    System.out.println("connecting");
    client.connectAndWait(reliableServerAddress, fastServerAddress, 1000);
    System.out.println("connected");

    UpdateMessage message = new UpdateMessage();
    message.connecting = true;
    client.sendToServer(message);
    //waitForPlayers();
  }

  public int getPlayerId() {
    return client.getPlayerId();
  }

  // Waits until the server sends us the ready message.
  public void waitForPlayers() throws Exception {
    UpdateMessage message = nextMessage(0, 10000);
    System.out.println("WAITING");
    if (message.ready) {
      System.out.println("READY!");
    } else {
      System.out.println("WTF");
    }
  }

  // Sends an update to the server, returns the other player's update.
  public UpdateMessage doUpdate(double x, double y, double angle) throws Exception {
    UpdateMessage message = new UpdateMessage();
    message.setPlayerId(client.getPlayerId());
    message.seq = seq;
    message.x = x;
    message.y = y;
    message.angle = angle;

    client.broadcast(message);

    UpdateMessage response = nextMessage(seq, 50);
    seq++;
    return response;
  }

  // Polls for next message.
  private UpdateMessage nextMessage(int expected_seq, int timeout) throws Exception {
    long start_time = Calendar.getInstance().getTimeInMillis();
    long cur_time = start_time;
    while (cur_time < start_time + timeout) {
      if (newMessage != null) {
        UpdateMessage message = newMessage;
        newMessage = null;
        //long latency = cur_time - start_time;
        //System.out.println("latency: " + latency);
        return message;
      }
      Thread.sleep(1);
      cur_time = Calendar.getInstance().getTimeInMillis();
    }
    return null;
  }

  public void messageReceived(UpdateMessage message) {
     // System.out.println("RECEIVED MESSAGE: " + message);
     newMessage = message;
  }
}
