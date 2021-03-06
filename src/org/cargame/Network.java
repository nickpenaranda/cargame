package org.cargame;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class Network {

  public static final int TCP_PORT = 30241;
  public static final int UDP_PORT = 30341;

  public static final byte STATE_DEAD = 1;
  public static final byte STATE_JAM = 2;
  public static final byte STATE_BOOST = 3;

  public static final byte CONTROL_ERR = -1;
  public static final byte CONTROL_ACK = 1;
  public static final byte CONTROL_CONNECT = 2;
  public static final byte CONTROL_NEW_PLAYER = 3;
  public static final byte CONTROL_RM_PLAYER = 4;

  public static final char COMMAND_CHARACTER = '/';
  public static final byte COMMAND_KILL_SELF = 1;

  public static void registerClasses( EndPoint context ) {
    Kryo kryo = context.getKryo();
    kryo.register( MoveMessage.class );
    kryo.register( StateMessage.class );
    kryo.register( ControlMessage.class );
    kryo.register( ChatMessage.class );
    kryo.register( CommandMessage.class );
    kryo.register( RocketMessage.class );
  }

  public static class MoveMessage {

    public double x, y, vx, vy, angle;
    public boolean t, r, b, l;
    public int id;
  }

  public static class StateMessage {

    public byte state;
    public boolean setting;
    public int id;
  }

  public static class ControlMessage {

    public byte type = 0;
    public int value = 0;
    public int value2 = 0;
    public String text = null;
  }

  public static class CommandMessage {

    public byte type = 0;
    public int value = 0;
    public int value2 = 0;
  }

  public static class ChatMessage {

    public String text;
    public int id;
  }

  public static class RocketMessage {

    public double x, y, vx, vy, angle;
    public int id;
  }
}
