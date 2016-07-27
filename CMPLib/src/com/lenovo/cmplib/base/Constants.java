package com.lenovo.cmplib.base;

public class Constants {
	public static final int INVALID_SESSIONID = -1;
	public static final int INVALID_MESSAGEID = 0;
	
	public static final int PACK_INCOMPLETE = -2;
	public static final int PACK_INVALID = -1;
	public static final int PACK_HEAD_ACK = 0;
	public static final int PACK_HEAD_FINISH = 1;
	public static final int PACK_HEAD_ONE_EXIST = 2;
	public static final int PACK_HEAD_HEARTBEAT = 3;
	public static final int PACK_HEAD_DATA = 4;
	
	public static final int CONFIRM_LIST_SIZE = 5;
	public static final int PACKAGE_QUEUE_SIZE = 512;
	
	public static final int HEARTBEAT_OP_IDLE = 20 * 1000; 
	
	public static final int CHECK_SERVER_INTERVAL = 2 * 1000;
	
	public static final int RECEIVE_BUFFER_SIZE = 1024;
	public static final int RECEIVE_CONFIRM_SIZE = 256;
	
	public static final int CONNECT_CLOSE_ACTIVE = 0;
	public static final int CONNECT_CLOSE_PASSIVE = 1;
	public static final int CONNECT_CLOSE_BREAK = 2;
	public static final int CONNECT_CLOSE_FORCE = 3;
	public static final int CONNECT_CLOSE_ONE_EXIST = 4;
	
	public static final int CONNECT_APP = 0;
	public static final int CONNECT_RECONNECT = 1;
	public static final int CONNECT_AUTO = 2;
	
	public static final int RESEND_INTERVAL = 2 * 1000;
	public static final int RESEND_COUNT = 5;
	
	public static final int CONNECT_BREAK_TIMEOUT = 30 * 1000;
	public static final int RECONNECT_INTERVAL = 3 * 1000;
	
	public static final int OP_ONCONNECT = 1;
	public static final int OP_ONDISCONNECT = 2;
	public static final int OP_ONSEND = 3;
	public static final int OP_ONRECV = 4;
}
