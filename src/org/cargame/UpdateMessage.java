package org.cargame;

import com.captiveimagination.jgn.message.*;
import com.captiveimagination.jgn.message.type.*;

public class UpdateMessage extends Message implements CertifiedMessage, PlayerMessage, TimestampedMessage {
    private String text;

    // used only for the initial "all players ready message"
    public boolean ready = false;
    
    // used only for the client telling the server its connected.
    public boolean connecting = false;

    // Server processes messages from different clients in lockstep based
    // on sequence number.
    public int seq;

    public double x, y, angle, speed;
    
    public int lives;

	public String toString() {
	  return String.format("text=%s ready=%s connecting=%s seq=%d (%f,%f)",text,ready,connecting,seq,x,y);
		//return "id=" + getId() + " playerId:" + getPlayerId() + " destPl:" + getDestinationPlayerId() + "seq: " + seq + " " + x + " " + y;
	}
}

