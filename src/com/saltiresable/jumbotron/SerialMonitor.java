package com.saltiresable.jumbotron;

import org.bukkit.scheduler.BukkitRunnable;

public class SerialMonitor extends BukkitRunnable {

	private Jumbotron plugin;
	
	public SerialMonitor(Jumbotron plugin) {
		this.plugin = plugin;
	}
	
	public void run() {
		if (plugin.arduino.portOpen()) {
			plugin.getLogger().info("Attempting to send pixel to new connection.");
			plugin.sendNextPixel();
		} else {
			if (plugin.arduino.openPort()) {
				plugin.getLogger().info("Opened serial port successfully.");
			}
		}
	}
}
