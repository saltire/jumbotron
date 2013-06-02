package com.saltiresable.jumbotron;

import java.util.ArrayDeque;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Jumbotron extends JavaPlugin {
	
	ArduinoJSSC arduino;
	BukkitTask monitor;
	boolean confirmed = false;
	boolean waiting = false;
	
	BlockGrid screen;
	ArrayDeque<byte[]> pixelQueue = new ArrayDeque<byte[]>();
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		
		if (getServer().getWorld(getConfig().getString("screen.world")) == null) {
			getLogger().severe("No valid world specified! Aborting.");
			setEnabled(false);
			return;
		}
		
		getServer().getPluginManager().registerEvents(new BlockListener(this), this);
		
		screen = new BlockGrid(
				getServer().getWorld(getConfig().getString("screen.world")),
				getConfig().getInt("screen.x"),
				getConfig().getInt("screen.y"),
				getConfig().getInt("screen.z"),
				getConfig().getInt("screen.width"),
				getConfig().getInt("screen.height"),
				Dir.valueOf(getConfig().getString("screen.direction").toUpperCase()));
		updatePixels(screen.getPixels());
		
		arduino = new ArduinoJSSC(this, getConfig().getString("arduino.port"));	
		if (getConfig().getBoolean("arduino.enable-on-start")) {
			startMonitoring();
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("jumbo") && args.length > 0) {
			if (args[0].equalsIgnoreCase("refresh")) {
				updatePixels(screen.getPixels());
				return true;
			}
			else if (args[0].equalsIgnoreCase("on")) {
				if (!confirmed && !getServer().getScheduler().getPendingTasks().contains(monitor)) {
					updatePixels(screen.getPixels());
					startMonitoring();
				}
				return true;
			}
			else if (args[0].equalsIgnoreCase("off")) {
				if (confirmed || getServer().getScheduler().getPendingTasks().contains(monitor)) {
					confirmed = false;
					monitor.cancel();
					pixelQueue.clear();
					arduino.closePort();
				}
				return true;
			}
		}
		return false;
	}
	
	private void startMonitoring() {
		monitor = new SerialMonitor(this).runTaskTimer(this, 0,
				getConfig().getInt("arduino.retry-interval") * 20);
	}
	
	void updatePixel(byte[] p) {
		//getLogger().info("Adding pixel to queue at "+p[0]+","+p[1]);
		pixelQueue.add(p);
		if (confirmed) {
			sendNextPixel();
		}
	}
	
	void updatePixels(byte[][] pixels) {
		for (byte[] p : pixels) {
			pixelQueue.add(p);
		}
		if (confirmed) {
			sendNextPixel();
		}
	}
	
	void sendNextPixel() {
		if (!confirmed && pixelQueue.size() > 0) {
			byte[] p = pixelQueue.peek();
			//getLogger().info("Sending pixel "+p[0]+","+p[1]+": "+p[2]+","+p[3]+","+p[4]);
			arduino.sendBytes(p);
		}
		else if (confirmed) {
			while (pixelQueue.size() > 0) {
				byte[] p = pixelQueue.remove();
				arduino.sendBytes(p);
			}
		}
	}
	
	void confirmPixelSent(byte[] coords) {
		//getLogger().info("Acknowledged pixel update at " + coords[0] + "," + coords[1]);
		if (!confirmed) {
			getLogger().info("Pixel update confirmed.");
			confirmed = true;
			monitor.cancel();
			pixelQueue.remove();
	    	sendNextPixel();
		}
	}
	
	@Override
	public void onDisable() {
		if (arduino != null) {
			arduino.disable();
		}
		getLogger().info("Disabled Jumbotron");
	}
}
