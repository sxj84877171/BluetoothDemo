package com.lenovo.cmplib.base;

public class PackageHeader {
	private int cmd;
	private int flag;
	private int id;
	private int length;
	
	public PackageHeader() {
		this.cmd = Constants.PACK_INVALID;
		this.id = Utils.createIdBySecond();
		this.length = 0;
		this.flag = 0;
	}
	
	public int getCmd() {
		return cmd;
	}
	public void setCmd(int cmd) {
		this.cmd = cmd;
	}
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	
	
}
