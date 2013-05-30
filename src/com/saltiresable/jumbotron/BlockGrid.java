package com.saltiresable.jumbotron;

import java.util.ArrayList;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;

enum Dir { NORTH, EAST, SOUTH, WEST }

public class BlockGrid {
	
	World world;
	int x;
	int y;
	int z;
	int w;
	int h;
	
	Dir dir;
	
	public BlockGrid(World world, int x, int y, int z, int w, int h, Dir dir) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		this.h = h;
		this.dir = dir;
	}
	
	public ArrayList<byte[]> getPixels() {
		ArrayList<byte[]> pixels = new ArrayList<byte[]>(w * h);
		int bx = x, by = y, bz = z;
		for (int u = 0; u < w; u++) {
			switch (dir) {
			case NORTH:
				bz = z - u;
				break;
			case SOUTH:
				bz = z + u;
				break;
			case EAST:
				bx = x + u;
				break;
			case WEST:
				bx = x - u;
				break;
			}
			for (int v = 0; v < h; v++) {
				by = y - v;
				int[] color = getBlockColor(world.getBlockAt(bx, by, bz));
				pixels.add(new byte[] {
						(byte) u, (byte) v,
						(byte) color[0], (byte) color[1], (byte) color[2]
					});
			}
		}
		return pixels;
	}

	public int[] getBlockColor(Block block) {
		MaterialData matdata = block.getState().getData();
		if (matdata instanceof Wool) {
			Wool wool = (Wool) matdata;
			int color = wool.getColor().getColor().asRGB();
			return new int[] {
				color >> 16 & 0xff,
				color >> 8 & 0xff,
				color & 0xff
			};
		} else {
			return new int[] {0, 0, 0};
		}
	}

	public boolean coordsInArea(int bx, int by, int bz) {
		switch (dir) {
		case NORTH:
			return (bz <= z && bz > z - w && by <= y && by > y - h && bx == x);
		case SOUTH:
			return (bz >= z && bz < z + w && by <= y && by > y - h && bx == x);
		case EAST:
			return (bx >= x && bx < x + w && by <= y && by > y - h && bz == z);
		case WEST:
			return (bx <= x && bx > x - w && by <= y && by > y - h && bz == z);
		default:
			return false;
		}
	}
	
	public int[] areaCoords(int bx, int by, int bz) {
		switch (dir) {
		case NORTH:
			return new int[] {z - bz, y - by};
		case SOUTH:
			return new int[] {bz - z, y - by};
		case EAST:
			return new int[] {bx - x, y - by};
		case WEST:
			return new int[] {x - bx, y - by};
		default:
			return new int[] {-1, -1};
		}
	}
	
}
