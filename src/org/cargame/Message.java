/**
 * 
 */
package org.cargame;

public class Message {

  private static final int MESSAGE_DURATION = 5000;

  public int life;
  public String text;

  public Message(String text) {
    this.text = text;
    life = MESSAGE_DURATION;
  }
}