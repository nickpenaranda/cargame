package org.cargame.server;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import java.net.*;

import org.cargame.UpdateMessage;

import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.clientserver.*;
import com.captiveimagination.jgn.event.*;


public class Server extends DynamicMessageAdapter {
    private static int NUM_PLAYERS = 2;
    private boolean[] playersWhoSent = new boolean[NUM_PLAYERS];
    private int cur_seq = 0;
    private int connected_players = 0;

    Queue<UpdateMessage> prev_messages = new LinkedList<UpdateMessage>();
    JGNServer server;

    public Server() throws Exception {
        JGN.register(UpdateMessage.class);
        InetSocketAddress reliableAddress = new InetSocketAddress(InetAddress.getLocalHost(), 2000);
        InetSocketAddress fastAddress = new InetSocketAddress(InetAddress.getLocalHost(), 2001);
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

    private void validateMessage(UpdateMessage message) {
        // TODO: validate player isn't sending the same seq again
        if (message.seq != cur_seq) {
            assert (allPlayersSent());
            assert (cur_seq + 1 == message.seq);
            cur_seq++;
            clearPlayersWhoSent();
        }
    }

    private boolean allPlayersSent() {
        for (int i=0; i<playersWhoSent.length; i++)
            if (!playersWhoSent[i])
                return false;
        return true;
    }

    private void clearPlayersWhoSent() {
        for (int i=0; i<playersWhoSent.length; i++)
            playersWhoSent[i] = false;
    }

    public void messageReceived(UpdateMessage message) {
        if (message.connecting) {
            connected();
        } else {
            validateMessage(message);
            System.out.println("MESSAGE: " + message);
            validateMessage(message);
        }
    }

    public static void main(String[] args) throws Exception {
        new Server();
    }
}
