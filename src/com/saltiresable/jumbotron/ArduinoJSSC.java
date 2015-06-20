package com.saltiresable.jumbotron;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class ArduinoJSSC implements SerialPortEventListener {

	SerialPort serialPort;
	Jumbotron plugin;
	int baudRate = 9600;

	public ArduinoJSSC(Jumbotron plugin, String portName) {
		serialPort = new SerialPort(portName);
		this.plugin = plugin;
	}

	public boolean portOpen() {
		return serialPort.isOpened();
	}

	public boolean openPort() {
		try {
			serialPort.openPort();
			serialPort.setParams(baudRate, 8, 1, 0);
			serialPort.addEventListener(this, SerialPort.MASK_RXCHAR);
			return true;
		} catch (SerialPortException e) {
			plugin.getLogger().severe(e.getMessage());
			return false;
		}
	}

	public void closePort() {
		try {
			serialPort.closePort();
		} catch (SerialPortException e) {
			plugin.getLogger().severe(e.getMessage());
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventValue() >= 2) {
			try {
				plugin.confirmPixelSent(serialPort.readBytes(2));
	        } catch (SerialPortException e) {
	        	plugin.getLogger().severe(e.getMessage());
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
			plugin.getLogger().severe(e.getMessage());
			return false;
		}
	}

	public void disable() {
		try {
			serialPort.closePort();
		} catch (SerialPortException e) {
			plugin.getLogger().severe(e.getMessage());
		}
	}
}
