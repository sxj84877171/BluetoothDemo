package com.lenovo.cmplib.base;

public class Errors {
	private static final int ERROR_DEFINE_BASE = -10000;
	private static final int ERROR_BLUETOOTH_BASE = -20400;
	
	public static final int ERROR_SUCCESS = 0;
	public static final int ERROR_INVALID_HANDLE = -6;
	public static final int ERROR_INVALID_PARAMETER = -87;
	public static final int ERROR_NOT_FOUND = -1168;
	public static final int ERROR_TIMEOUT = -1460;
	
	public static final int ERROR_FAIL = ERROR_DEFINE_BASE - 1;
	public static final int ERROR_NOT_IMPLEMENT = ERROR_DEFINE_BASE - 2;
	public static final int ERROR_CONNECT_BREAK = ERROR_DEFINE_BASE - 3;
	public static final int ERROR_CONNECT_CLOSE = ERROR_DEFINE_BASE - 4;
	public static final int ERROR_CONNECT_ONE_EXIST = ERROR_DEFINE_BASE - 5;
	public static final int ERROR_SERVER_NOT_FOUND = ERROR_DEFINE_BASE - 6;
	public static final int ERROR_ALREADY_CONNECTED = ERROR_DEFINE_BASE - 7;
	
	public static final int ERROR_BLUETOOTH_FAIL = ERROR_BLUETOOTH_BASE - 1;
	public static final int ERROR_BLUETOOTH_NOTSUPPORT = ERROR_BLUETOOTH_BASE - 2;
	public static final int ERROR_BLUETOOTH_BONDED = ERROR_BLUETOOTH_BASE - 3;
}
