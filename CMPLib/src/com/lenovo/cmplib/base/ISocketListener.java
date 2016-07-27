package com.lenovo.cmplib.base;

import android.bluetooth.BluetoothSocket;

import com.lenovo.cmplib.impl.Package;

public interface ISocketListener {
	public void onNotify(int param);
	public void onConnect(BluetoothSocket socket, String address, int param, int errorCode);
	public void onDisconnect(int sessionId, int param, int errorCode);
	public void onSend(int sessionId, int cmd, int packageId, int param);
	public void onRecv(int sessionId, Package pkg, int param);
}
