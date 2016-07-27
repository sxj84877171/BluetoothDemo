package com.lenovo.cmplib.base;

import java.util.Calendar;

public class Utils {
	public static int getCurrentTimeBySecond() {
		Calendar c = Calendar.getInstance();
		
		int year = 0; //c.get(Calendar.YEAR) - 1;
		int month = c.get(Calendar.MONTH) - 1;
		int date = c.get(Calendar.DATE) - 1;
		int hour = c.get(Calendar.HOUR) - 1;
		int minute = c.get(Calendar.MINUTE) - 1;
		int second = c.get(Calendar.SECOND);
		
		return ((((year * 12 + month) * 30 + date) * 24 + hour) * 60 + minute) * 60 + second;
	}
	
	public static int createIdBySecond() {
		Calendar c = Calendar.getInstance();
		
		int hour = c.get(Calendar.HOUR) - 1;
		int minute = c.get(Calendar.MINUTE) - 1;
		int second = c.get(Calendar.SECOND);
		
		return (hour * 60 + minute) * 60 + second;
	}
	
	public static int unsignedByteToInt(byte b) {  
		return (int) b & 0xFF;  
	} 

	
	public static String getBufferString(byte[] buffer, int length) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<length; i++) {
			builder.append(String.format("%1$#2X ", buffer[i]));
		}
		
		return builder.toString();
	}
}
