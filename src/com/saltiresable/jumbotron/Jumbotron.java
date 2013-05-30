package com.saltiresable.jumbotron;

import java.util.ArrayDeque;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class Jumbotron extends JavaPlugin {
	
	String worldname = "uberworld";
	int x = 278;
	int y = 87;
	int z = -246;
	int w = 32;
	int h = 16;
	enum Dir { NORTH, EAST, SOUTH, WEST }
	Dir dir = Dir.EAST;
	
	ArrayDeque<byte[]> pixelQueue = new ArrayDeque<byte[]>(w * h);
	
	ArduinoJSSC arduino = new ArduinoJSSC("COM5");
	BukkitTask monitor;
	boolean confirmed = false;
	boolean waiting = false;
	
	@Override
	public void onEnable() {
		World world = getServer().getWorld(worldname);
		
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
				pixelQueue.add(new byte[] {
						(byte) u, (byte) v,
						(byte) color[0], (byte) color[1], (byte) color[2]
					});
			}
		}
		
		getServer().getPluginManager().registerEvents(new BlockListener(this), this);
		monitor = new SerialMonitor(arduino).runTaskTimer(this, 0, 30);
		
		getLogger().info("Enabled Jumbotron");
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
	
	public void updatePixel(byte[] p) {
		getLogger().info("Adding pixel to queue at "+p[0]+","+p[1]);
		pixelQueue.add(p);
		if (!waiting) {
			sendNextPixel();
		}
	}
	
	private void sendNextPixel() {
		if (pixelQueue.size() > 0) {
			byte[] p = pixelQueue.peek();
			waiting = true;
			getLogger().info("Sending pixel "+p[0]+","+p[1]+": "+p[2]+","+p[3]+","+p[4]);
			arduino.sendBytes(p);
		}
	}
	
	private void onConfirmPixelSent(byte[] coords) {
		getLogger().info("Acknowledged pixel update at " + coords[0] + "," + coords[1]);
		if (!confirmed) {
			confirmed = true;
			monitor.cancel();
		}
		waiting = false;
		pixelQueue.remove();
    	sendNextPixel();
	}
	
	private class SerialMonitor extends BukkitRunnable {
		private ArduinoJSSC arduino;
		
		public SerialMonitor(ArduinoJSSC ard) {
			arduino = ard;
		}
		
		public void run() {
			if (arduino.portOpen()) {
				getLogger().info("Attempting to send pixel to new connection.");
				sendNextPixel();
			} else {
				if (arduino.openPort()) {
					getLogger().info("Opened serial port successfully.");
				}
			}
		}
	}
	
	private class ArduinoJSSC implements SerialPortEventListener {
		
		SerialPort serialPort;
		int baudRate = 9600;
		
		public ArduinoJSSC(String portName) {
			serialPort = new SerialPort(portName);
		}
		
		public boolean portOpen() {
			return serialPort.isOpened();
		}
		
		private boolean openPort() {
			try {
				serialPort.openPort();
				serialPort.setParams(baudRate, 8, 1, 0);
				serialPort.addEventListener(this, SerialPort.MASK_RXCHAR);
				return true;
			} catch (SerialPortException e) {
				getLogger().severe(e.getMessage());
				return false;
			}
		}
		
		@Override
		public void serialEvent(SerialPortEvent event) {
			if (event.getEventValue() >= 2) {
				try {
					onConfirmPixelSent(serialPort.readBytes(2));
		        } catch (SerialPortException e) {
		        	getLogger().severe(e.getMessage());
		        }
	        }
		}
		
		public boolean sendBytes(byte[] bytes) {
			if (!serialPort.isOpened()) {
				return false;
			}
			try {
				serialPort.writeBytes(bytes);
				return true;
			} catch (SerialPortException e) {
				getLogger().severe(e.getMessage());
				return false;
			}
		}
		
		public void disable() {
			try {
				serialPort.closePort();
			} catch (SerialPortException e) {
				getLogger().severe(e.getMessage());
			}
		}
	}
	
	@Override
	public void onDisable() {
		arduino.disable();
		getLogger().info("Disabled Jumbotron");
	}
}
