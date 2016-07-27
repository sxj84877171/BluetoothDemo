package com.lenovo.cmplib.base;

public class SessionInfo {
	private String bluetoothMacAddr; // Bluetooth mac address, for pair device and create socket connection.
	private String ssid; // WIFI ssid, reserved for future.
	
	public String getBluetoothMacAddr() {
		return bluetoothMacAddr;
	}
	public void setBluetoothMacAddr(String bluetoothMacAddr) {
		this.bluetoothMacAddr = bluetoothMacAddr;
	}
	public String getSsid() {
		return ssid;
	}
	public void setSsid(String ssid) {
		this.ssid = ssid;
	}
	
	
}
