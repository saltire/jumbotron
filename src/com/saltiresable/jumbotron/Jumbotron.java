package com.saltiresable.jumbotron;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import java.util.ArrayDeque;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
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
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		if (getServer().getWorld(getConfig().getString("screen.world")) == null) {
			getLogger().warning("No valid world specified!");
		} else {
			screen = new BlockGrid(
					getServer().getWorld(getConfig().getString("screen.world")),
					getConfig().getInt("screen.x"),
					getConfig().getInt("screen.y"),
					getConfig().getInt("screen.z"),
					getConfig().getInt("screen.width"),
					getConfig().getInt("screen.height"),
					getConfig().getInt("display.width"),
					getConfig().getInt("display.height"),
					Dir.valueOf(getConfig().getString("screen.view-direction").toUpperCase()));
			updatePixels(screen.getPixels());
		}
		
		getServer().getPluginManager().registerEvents(new BlockListener(this), this);
		
		arduino = new ArduinoJSSC(this, getConfig().getString("arduino.port"));	
		if (screen != null && getConfig().getBoolean("arduino.enable-on-start")) {
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
				if (screen != null && !confirmed && !getServer().getScheduler().getPendingTasks().contains(monitor)) {
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
			else if (args[0].equalsIgnoreCase("set") && sender instanceof Player) {
				if (getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
					Player player = (Player) sender;
					
					WorldEditPlugin we = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
					
					if (we.getSelection(player) == null) {
						getLogger().info("Player has no selection.");
						return true;
					}
					
					float yaw = player.getLocation().getYaw() % 360;
					if (yaw < 0) {
						yaw += 360;
					}
					Dir dir;
					if (yaw < 45 || yaw > 315) {
						dir = Dir.SOUTH;
					} else if (yaw < 135) {
						dir = Dir.WEST;
					} else if (yaw < 225) {
						dir = Dir.NORTH;
					} else {
						dir = Dir.EAST;
					}
					
					screen = new BlockGrid(
							player.getLocation().getWorld(),
							we.getSelection(player),
							getConfig().getInt("screen.width"),
							getConfig().getInt("screen.height"),
							dir);
					updatePixels(screen.getPixels());
					
					getLogger().info("Set new screen.");
					getConfig().set("screen.world", player.getLocation().getWorld().getName());
					getConfig().set("screen.x", screen.x);
					getConfig().set("screen.y", screen.y);
					getConfig().set("screen.z", screen.z);
					getConfig().set("screen.width", screen.w);
					getConfig().set("screen.height", screen.h);
					getConfig().set("screen.view-direction", dir.toString());
					saveConfig();
					
					return true;
				}
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
			//getLogger().info("Adding pixel to queue at "+p[0]+","+p[1]+": "+p[2]+","+p[3]+","+p[4]);
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
