package com.lenovo.cmplib.base;

public interface ISessionRole {
	public int registerListener(ISessionListener listener);
	
	public int unregisterListener(ISessionListener listener);

	public int init(InitInfo info);
	
	public int uninit();

	public int connect(SessionInfo info);

	public int disconnect(int sessionID);

	public int send(int sessionID, String message);
}
