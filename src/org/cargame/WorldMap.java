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
		private float tex_scale;
		
		Wall(Polygon polygon, Image texture, float scale) {
			this.polygon = polygon;
			this.texture = texture;
			this.tex_scale = scale;
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
		
		public float getScale() {
		  return(tex_scale);
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
			g.texture(wall.getPolygon(), wall.getTexture(), wall.getScale(), wall.getScale(), true);
		}
	}
}
