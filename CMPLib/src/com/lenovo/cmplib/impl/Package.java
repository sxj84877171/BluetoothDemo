package com.lenovo.cmplib.impl;

import android.support.v4.util.LruCache;

import com.lenovo.cmplib.base.Constants;
import com.lenovo.cmplib.base.Errors;
import com.lenovo.cmplib.base.PackageHeader;
import com.lenovo.cmplib.base.Utils;

public class Package {
	private PackageHeader header;
	private int msgID;
	private byte[] content;
	private long sendTime;
	private int count;

	public Package() {
		msgID = Utils.createIdBySecond();
		count = 0;
	}

	public Package(byte[] receive) throws Exception {

		if (!isCompletePackage(receive, receive.length)) {
			throw new Exception("Wrong package format");
		}
		int cmd = (Utils.unsignedByteToInt(receive[0]) << 8) + Utils.unsignedByteToInt(receive[1]);

		header = new PackageHeader();
		header.setCmd(cmd);
		header.setFlag(Utils.unsignedByteToInt(receive[2]));
		header.setId((Utils.unsignedByteToInt(receive[3]) << 8) + Utils.unsignedByteToInt(receive[4]));
		header.setLength(0);

		if (cmd == Constants.PACK_HEAD_DATA) {
			int len = (Utils.unsignedByteToInt(receive[5]) << 8) + Utils.unsignedByteToInt(receive[6]);
			header.setLength(len);

			content = new byte[len];
			System.arraycopy(receive, 7, content, 0, len);
		}
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public PackageHeader getHeader() {
		return header;
	}

	public void setHeader(PackageHeader header) {
		this.header = header;
	}

	public int getMsgID() {
		return msgID;
	}

	public void setMsgID(int msgID) {
		this.msgID = msgID;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public long getSendTime() {
		return sendTime;
	}

	public void setSendTime(long sendTime) {
		this.sendTime = sendTime;
	}

	public boolean checkPackageId(int id) {
		if (header.getId() == id) {
			return true;
		}

		return false;
	}

	public byte[] getBytes() {
		if (header == null) {
			return null;
		}

		int buflen = getPackageBufferLength();
		if (buflen <= 0) {
			return null;
		}

		byte[] data = new byte[buflen];

		// cmd
		data[0] = (byte) ((header.getCmd() >> 8) & 0xFF);
		data[1] = (byte) (header.getCmd() & 0xFF);
		// flag
		data[2] = (byte) (header.getFlag() & 0xFF);
		// id
		data[3] = (byte) ((header.getId() >> 8) & 0xFF);
		data[4] = (byte) (header.getId() & 0xFF);

		// data length
		if (content != null) {
			data[5] = (byte) ((content.length >> 8) & 0xFF);
			data[6] = (byte) (content.length & 0xFF);

			// data
			System.arraycopy(content, 0, data, 7, content.length);
		} else {
			data[5] = 0;
			data[6] = 0;
		}

		LruCache lru = null ;
		return data;
	}
	
	public static int getPackageLength(byte[] receive, int length) {
		if ((receive == null) || receive.length < 7 || length < 7) {
			return -1;
		}

		int cmd = (Utils.unsignedByteToInt(receive[0]) << 8) + Utils.unsignedByteToInt(receive[1]);
		switch (cmd) {
		case Constants.PACK_HEAD_ACK:
		case Constants.PACK_HEAD_FINISH:
		case Constants.PACK_HEAD_HEARTBEAT: {
			return 7;
		}
		case Constants.PACK_HEAD_DATA: {
			int msgLen = (Utils.unsignedByteToInt(receive[5]) << 8) + Utils.unsignedByteToInt(receive[6]);
			if (length <= receive.length) {
				if (length >= (7 + msgLen)) {
					return 7 + msgLen;
				} else {
					return -1;
				}
			} else {
				if (receive.length >= (7 + msgLen)) {
					return 7 + msgLen;
				} else {
					return -1;
				}
			}
		}
		default:
			return -1;
		}
	}

	private boolean isCompletePackage(byte[] receive, int length) {
		if ((receive == null) || receive.length < 7 || length < 7) {
			return false;
		}

		int cmd = (Utils.unsignedByteToInt(receive[0]) << 8) + Utils.unsignedByteToInt(receive[1]);
		int id = (Utils.unsignedByteToInt(receive[3]) << 8) + Utils.unsignedByteToInt(receive[4]);
		switch (cmd) {
		case Constants.PACK_HEAD_ACK:
		case Constants.PACK_HEAD_FINISH:
		case Constants.PACK_HEAD_HEARTBEAT: {
			if (id > 0) {
				return true;
			}
			break;
		}
		case Constants.PACK_HEAD_DATA: {
			if (id > 0) {
				int msgLen = (Utils.unsignedByteToInt(receive[5]) << 8) + receive[6];
				if (length <= receive.length) {
					if (length >= (7 + msgLen)) {
						return true;
					} else {
						return false;
					}
				} else {
					if (receive.length >= (7 + msgLen)) {
						return true;
					} else {
						return false;
					}
				}

			}
			break;
		}
		default:
			return false;
		}

		return false;
	}

	private int getPackageBufferLength() {
		int totallen = 0;

		if ((header == null) || header.getCmd() < 0) {
			return Errors.ERROR_INVALID_PARAMETER;

		} else {
			totallen = 2 + 1 + 2 + 2;
		}

		if (header.getCmd() == Constants.PACK_HEAD_DATA) {
			totallen += content.length;
		}

		return totallen;
	}
}
