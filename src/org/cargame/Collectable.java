package org.cargame;


public interface Collectable {
  public void onPickup(Item item, Car op, World world);
}
