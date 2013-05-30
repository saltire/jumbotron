package com.saltiresable.jumbotron;

import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {
	
	Jumbotron plugin;

	public BlockListener(Jumbotron jumbo) {
		plugin = jumbo;
	}
	
	@EventHandler
	public void breakBlock(BlockBreakEvent event) {
		Block block = event.getBlock();
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		if (plugin.coordsInArea(x, y, z)) {
			int[] coords = plugin.areaCoords(x, y, z);
			plugin.updatePixel(new byte[] {
				(byte) coords[0], (byte) coords[1],
				(byte) 0, (byte) 0, (byte) 0
			});
		}
	}
	
	@EventHandler
	public void placeBlock(BlockPlaceEvent event) {
		Block block = event.getBlockPlaced();
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		if (plugin.coordsInArea(x, y, z)) {
			int[] color = plugin.getBlockColor(block);
			int[] coords = plugin.areaCoords(x, y, z);
			plugin.updatePixel(new byte[] {
				(byte) coords[0], (byte) coords[1],
				(byte) color[0], (byte) color[1], (byte) color[2]
			});
		}
	}
}
