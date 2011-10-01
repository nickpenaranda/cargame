package org.cargame;

import java.util.LinkedList;

import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

public class WorldMap {
	// Unlike Doom, sectors are areas the player cannot enter.
	public static class Wall {
		private Polygon polygon;
		private Image texture;
		
		Wall(Polygon polygon, Image texture) {
			this.polygon = polygon;
			this.texture = texture;
		}
		
		public Polygon getPolygon() {
			return polygon;
		}

		public Image getTexture() {
			return texture;
		}
		
		public boolean overLaps(Rectangle rect) {
			return (polygon.intersects(rect)
					|| polygon.contains(rect)
					|| rect.contains(polygon));
		}
	}
	
	private LinkedList<Wall> walls = new LinkedList<Wall>();
	
	public void AddWall(Wall wall) {
		walls.add(wall);
	}
	
	public LinkedList<Wall> getWallsWithin(Rectangle rect) {
		LinkedList<Wall> within = new LinkedList<Wall>();
		for (Wall wall : walls) {
			if (wall.overLaps(rect))
				within.add(wall);
		}
		return within;
	}
	
	public void render(Graphics g, Rectangle clip) {
		for (Wall wall : getWallsWithin(clip)) {
			g.texture(wall.getPolygon(), wall.getTexture());
		}
	}
}
