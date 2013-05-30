package com.saltiresable.jumbotron;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;

public final class Jumbotron extends JavaPlugin {
	
	ArduinoJSSC arduino = new ArduinoJSSC("COM5");
	
	String worldname = "uberworld";
	int x = 278;
	int y = 87;
	int z = -246;
	int w = 32;
	int h = 16;
	enum Dir { NORTH, EAST, SOUTH, WEST }
	Dir dir = Dir.EAST;
	
	byte[][] bytes = new byte[w * h][5];
	int to_send = w * h;
	
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
				bytes[u * h + v] = new byte[] {
					(byte) color[0], (byte) color[1], (byte) color[2],
					(byte) u, (byte) v
				};
			}
		}
		
		sendNextPixel();
		
		getServer().getPluginManager().registerEvents(new BlockListener(this), this);
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
	
	@Override
	public void onDisable() {
		arduino.disable();
		getLogger().info("Disabled Jumbotron");
	}
	
	public boolean sendPixel(byte[] p) {
		//getLogger().info("Sending pixel:"+p[3]+","+p[4]+": "+p[0]+","+p[1]+","+p[2]);
		return arduino.sendBytes(p);
	}
	
	private void sendNextPixel() {
		if (to_send > 0) {
			byte[] p = bytes[w * h - to_send];
			if (sendPixel(p)) {
				to_send--;
			}
		}
	}
	
	private class ArduinoJSSC implements SerialPortEventListener {
		
		SerialPort serialPort;
		boolean open = false;
		
		public ArduinoJSSC(String portName) {
			serialPort = new SerialPort(portName);
			open = openPort();
		}
		
		private boolean openPort() {
			try {
				serialPort.openPort();
				serialPort.setParams(9600, 8, 1, 0);
				serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
				serialPort.addEventListener(this);
				return true;
			} catch (SerialPortException e) {
				getLogger().severe(e.getMessage());
				return false;
			}
		}
		
		@Override
		public void serialEvent(SerialPortEvent event) {
			if (event.isRXCHAR()) {
				if (event.getEventValue() > 0) {
					try {
						serialPort.readHexString(3);
						//getLogger().info(serialPort.readHexString(3));
			        	sendNextPixel();
			        	
			        	//String string = serialPort.readString();
			            //getLogger().info("Got string: " + string);
			        } catch (SerialPortException e) {
			        	getLogger().severe(e.getMessage());
			        }
		        }
		    }
		}
		
		public boolean sendBytes(byte[] bytes) {
			if (!open && !openPort()) {
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
}
