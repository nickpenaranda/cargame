/**
 * 
 */
package org.cargame;

import org.newdawn.slick.Color;

public class Message {

  private static final int MESSAGE_DURATION = 5000;

  public int life;
  public String text;
  public Color color;

  public Message(String text) {
    this(text,Color.white);
  }
  
  public Message(String text,Color color) {
    this.text = text;
    this.color = color;
    life = MESSAGE_DURATION;
  }
}