package com.saltiresable.jumbotron;

import java.util.ArrayDeque;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Jumbotron extends JavaPlugin {
	
	String worldname = "uberworld";
	int x = 278;
	int y = 87;
	int z = -246;
	int w = 32;
	int h = 16;
	Dir dir = Dir.EAST;
	
	BlockGrid screen;
	ArrayDeque<byte[]> pixelQueue = new ArrayDeque<byte[]>();
	
	ArduinoJSSC arduino = new ArduinoJSSC(this, "COM5");
	BukkitTask monitor;
	boolean confirmed = false;
	boolean waiting = false;
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new BlockListener(this), this);
		monitor = new SerialMonitor(this).runTaskTimer(this, 0, 30);
		screen = new BlockGrid(getServer().getWorld(worldname), x, y, z, w, h, dir);
		updatePixels(screen.getPixels());
		
		getLogger().info("Enabled Jumbotron");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("jumbo")) {
			if (args.length > 0 && args[0].equalsIgnoreCase("refresh")) {
				updatePixels(screen.getPixels());
				return true;
			}
		}
		return false;
	}
	
	public void updatePixel(byte[] p) {
		getLogger().info("Adding pixel to queue at "+p[0]+","+p[1]);
		pixelQueue.add(p);
		if (confirmed && !waiting) {
			sendNextPixel();
		}
	}
	
	public void updatePixels(byte[][] pixels) {
		for (byte[] p : pixels) {
			pixelQueue.add(p);
		}
		if (confirmed && !waiting) {
			sendNextPixel();
		}
	}
	
	public void sendNextPixel() {
		if (pixelQueue.size() > 0) {
			byte[] p = pixelQueue.peek();
			waiting = true;
			getLogger().info("Sending pixel "+p[0]+","+p[1]+": "+p[2]+","+p[3]+","+p[4]);
			arduino.sendBytes(p);
		}
	}
	
	public void confirmPixelSent(byte[] coords) {
		getLogger().info("Acknowledged pixel update at " + coords[0] + "," + coords[1]);
		if (!confirmed) {
			confirmed = true;
			monitor.cancel();
		}
		waiting = false;
		pixelQueue.remove();
    	sendNextPixel();
	}
	
	@Override
	public void onDisable() {
		arduino.disable();
		getLogger().info("Disabled Jumbotron");
	}
}
