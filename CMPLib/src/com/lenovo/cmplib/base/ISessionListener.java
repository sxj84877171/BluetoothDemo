package com.lenovo.cmplib.base;

public interface ISessionListener {
	public void onConnect(int sessionID, int errorCode);

	public void onDisconnect(int sessionID, int errorCode);

	public void onSend(int sessionID, int msgID, int errorCode);

	public void onRecv(int sessionID, String message);
}
