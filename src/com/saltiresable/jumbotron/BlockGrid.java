package com.saltiresable.jumbotron;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;

enum Dir { NORTH, SOUTH, EAST, WEST }

public class BlockGrid {

	World world;
	int x;
	int y;
	int z;
	int w;
	int h;
	int maxw;
	int maxh;
	Dir dir;

	public BlockGrid(World world, int x, int y, int z, int w, int h, int maxw, int maxh, Dir dir) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = Math.min(w, maxw);
		this.h = Math.min(h, maxh);
		this.maxw = maxw;
		this.maxh = maxh;
		this.dir = dir;
	}

	public BlockGrid(World world, Selection selection, int maxw, int maxh, Dir dir) {
		this.world = world;
		this.dir = dir;
		this.maxw = maxw;
		this.maxh = maxh;

		Vector min = selection.getNativeMinimumPoint();
		Vector max = selection.getNativeMaximumPoint();
		switch (dir) {
		case NORTH:
			x = min.getBlockX();
			z = max.getBlockZ();
			w = Math.min(maxw, selection.getWidth());
			break;
		case SOUTH:
			x = max.getBlockX();
			z = min.getBlockZ();
			w = Math.min(maxw, selection.getWidth());
			break;
		case EAST:
			x = min.getBlockX();
			z = min.getBlockZ();
			w = Math.min(maxw, selection.getLength());
			break;
		case WEST:
			x = max.getBlockX();
			z = max.getBlockZ();
			w = Math.min(maxw, selection.getLength());
			break;
		}
		y = max.getBlockY();
		h = Math.min(maxh, selection.getHeight());
	}

	public byte[][] getPixels() {
		byte[][] pixels = new byte[maxw * maxh][];
		int bx = x, by = y, bz = z;
		int ou = (maxw - w) / 2;
		int ov = (maxh - h) / 2;

		for (int v = 0; v < maxh; v++) {
			by = y - (v - ov);
			for (int u = 0; u < maxw; u++) {

				if (u < ou || u >= w + ou || v < ov || v >= h + ov) {
					pixels[u + v * maxw] = new byte[] { (byte) u, (byte) v, 0, 0, 0 };
				}
				else {
					int bu = u - ou;
					switch (dir) {
					case WEST:
						bz = z - bu;
						break;
					case EAST:
						bz = z + bu;
						break;
					case NORTH:
						bx = x + bu;
						break;
					case SOUTH:
						bx = x - bu;
						break;
					}
					int[] color = getBlockColor(world.getBlockAt(bx, by, bz));
					pixels[u + v * maxw] = new byte[] {
							(byte) u, (byte) v,
							(byte) color[0], (byte) color[1], (byte) color[2]
						};
				}
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
		case WEST:
			return (bz <= z && bz > z - w && by <= y && by > y - h && bx == x);
		case EAST:
			return (bz >= z && bz < z + w && by <= y && by > y - h && bx == x);
		case NORTH:
			return (bx >= x && bx < x + w && by <= y && by > y - h && bz == z);
		case SOUTH:
			return (bx <= x && bx > x - w && by <= y && by > y - h && bz == z);
		default:
			return false;
		}
	}

	public int[] areaCoords(int bx, int by, int bz) {
		int ou = (maxw - w) / 2;
		int ov = (maxh - h) / 2;
		switch (dir) {
		case WEST:
			return new int[] {z - bz + ou, y - by + ov};
		case EAST:
			return new int[] {bz - z + ou, y - by + ov};
		case NORTH:
			return new int[] {bx - x + ou, y - by + ov};
		case SOUTH:
			return new int[] {x - bx + ou, y - by + ov};
		default:
			return new int[] {-1, -1};
		}
	}

}
