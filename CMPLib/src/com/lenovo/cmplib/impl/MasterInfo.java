package com.lenovo.cmplib.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.lenovo.cmplib.base.Constants;
import com.lenovo.cmplib.base.Errors;
import com.lenovo.cmplib.base.ISocketListener;
import com.lenovo.cmplib.base.PackageHeader;
import com.lenovo.cmplib.base.Utils;

@SuppressLint("DefaultLocale")
public class MasterInfo {
	private String TAG = "MasterInfo";
	private ISocketListener mSocketListener;
	private Bluetooth mBluetoothObject;
	
	private boolean mThreadRun;
	private Thread mRecvThread;
	private Thread mSendThread;
	private Thread mRepeatThread;

	private int mSessionId;
	private BluetoothSocket mSocket;
	private OutputStream mOutputStream;
	private InputStream mInputStream;
	private long mLastOpTime;
	private long mLastBeatTime;
	private long mReconnectTime;
	private boolean mIsConnectBreak;
	private boolean mReconnectFinish;

	private int mLen = 0;
	private byte[] mBuffer = new byte[Constants.RECEIVE_BUFFER_SIZE];
	private int mMaxSize;
	private BlockingQueue<Package> mPackageQueue;
	private List<Package> mSendingList;
	private Object mQueueLock;
	private Object mListLock;

	public MasterInfo(BluetoothSocket socket) throws IOException {
		mSocketListener = null;
		mBluetoothObject = null;
		mSessionId = 0;
		mSocket = socket;
		mInputStream = mSocket.getInputStream();
		mOutputStream = mSocket.getOutputStream();
		
		mThreadRun = false;
		mRecvThread = null;
		mSendThread = null;
		mRepeatThread = null;
		
		mIsConnectBreak = false;
		mReconnectFinish = true;
		mLastOpTime = System.currentTimeMillis() + Constants.HEARTBEAT_OP_IDLE; // check
																				// heartbeat
																				// a
																				// interval
																				// later
		mQueueLock = new Object();
		mListLock = new Object();
		mPackageQueue = null;
		mSendingList = null;
		mMaxSize = 0;
	}

	// private
	private int createSessionId() {
		return Utils.getCurrentTimeBySecond();
	}

	private int sendHeartbeat() {
		PackageHeader header = new PackageHeader();
		header.setCmd(Constants.PACK_HEAD_HEARTBEAT);

		Package pkg = new Package();
		pkg.setHeader(header);

		Log.i(TAG, "Send heartbeat...");

		return addSendPackage(pkg);
	}

	private int sendRespond(int dataId) {
		PackageHeader header = new PackageHeader();
		header.setCmd(Constants.PACK_HEAD_ACK);
		header.setId(dataId);
		header.setLength(0);

		Package pkg = new Package();
		pkg.setHeader(header);

		return addSendPackage(pkg);
	}

	private byte[] receive() {
		// if a complete package is in mBuffer
		int pkglen;
		byte[] pkgBuf;
		if (mLen >= 7) {
			pkglen = Package.getPackageLength(mBuffer, mLen);
			if (pkglen >= 7) {
				pkgBuf = new byte[pkglen];
				System.arraycopy(mBuffer, 0, pkgBuf, 0, pkglen);

				mLen = mLen - pkglen;
				byte[] temp = new byte[Constants.RECEIVE_BUFFER_SIZE];
				System.arraycopy(mBuffer, pkglen, temp, 0, mLen);
				System.arraycopy(temp, 0, mBuffer, 0, mLen);

				return pkgBuf;
			}
		}

		// if mBuffer is not a complete package
		byte[] buffer = null;
		try {
			buffer = mBluetoothObject.receive(mInputStream);
			Log.i(TAG, "Receive...");
		} catch (IOException e) {
			mIsConnectBreak = true;
			mReconnectTime = System.currentTimeMillis();

			Log.i(TAG, "Receive fail, reconnect...");
			reconnect();
		}

		if (buffer == null) {
			return null;
		}

		Log.i(TAG, "Recv :" + Utils.getBufferString(buffer, buffer.length));

		//mBuffer 长度1024， 如果buffer 为1024， men >0 
		System.arraycopy(buffer, 0, mBuffer, mLen, buffer.length);
		mLen += buffer.length;

		pkglen = Package.getPackageLength(mBuffer, mLen);
		if (pkglen >= 7) {
			pkgBuf = new byte[pkglen];
			System.arraycopy(mBuffer, 0, pkgBuf, 0, pkglen);

			mLen = mLen - pkglen;
			byte[] temp = new byte[Constants.RECEIVE_BUFFER_SIZE];
			System.arraycopy(mBuffer, pkglen, temp, 0, mLen);
			System.arraycopy(temp, 0, mBuffer, 0, mLen);

			return pkgBuf;
		}

		return null;
	}
	
	private boolean isReceiveAlready(int id) {
		/*for (int recvId : mReceiveList) {
			if (recvId == id) {
				return true;
			}
		}
		
		mReceiveList[mReceivePos++] = id;		
		if (mReceivePos == Constants.RECEIVE_CONFIRM_SIZE) {
			mReceivePos = 0;
		}*/
		
		return false;
	}

	private int reconnect() {
		mReconnectFinish = false;

		return mBluetoothObject.reconnectServer(mSocket.getRemoteDevice()
				.getAddress());
	}

	// public
	public void setBluetooth(Bluetooth bluetooth) {
		mBluetoothObject = bluetooth;
	}

	public int getSessionId() {
		return mSessionId;
	}

	public BluetoothSocket getSocket() {
		return mSocket;
	}

	public long getLastOpTime() {
		return mLastOpTime;
	}

	public void reconnectFail() {
		mReconnectFinish = true;
	}

	public int refreshSocket(BluetoothSocket socket) {
		mSocket = socket;

		if (mInputStream == null) {
			try {
				mInputStream = mSocket.getInputStream();
			} catch (IOException e) {
				return Errors.ERROR_CONNECT_BREAK;
			}
		}

		if (mOutputStream == null) {
			try {
				mOutputStream = mSocket.getOutputStream();
			} catch (IOException e) {
				return Errors.ERROR_CONNECT_BREAK;
			}
		}

		mIsConnectBreak = false;
		mReconnectFinish = true;
		mLastOpTime = System.currentTimeMillis();

		return Errors.ERROR_SUCCESS;
	}

	public boolean isMatchAddress(String address) {
		return mSocket.getRemoteDevice().getAddress().toUpperCase()
				.equals(address.toUpperCase().trim());
	}

	public int release() {
		if (mRecvThread != null || mSendThread != null || mRepeatThread != null) {
			mThreadRun = false;
		}

		try {
			if (mInputStream != null) {
				mInputStream.close();
				mInputStream = null;
			}

			if (mOutputStream != null) {
				mOutputStream.close();
				mOutputStream = null;
			}

			if (mSocket != null) {
				mBluetoothObject.disconnect(mSocket);
				mSocket = null;
			}

			Log.i(TAG, "Connect close...");

		} catch (IOException e) {

		}
		

		return Errors.ERROR_SUCCESS;
	}

	public void notifySendMessageFail() {
		synchronized (mListLock) {
			for (Package pkg : mSendingList) {
				PackageHeader header = pkg.getHeader();
				if (header.getCmd() == Constants.PACK_HEAD_DATA) {
					mSocketListener.onSend(mSessionId, Constants.PACK_HEAD_DATA,
							pkg.getMsgID(), Errors.ERROR_CONNECT_CLOSE);
				}
			}
		}

		synchronized (mQueueLock) {
			while (mPackageQueue.size() > 0) {
				Package pkg = mPackageQueue.remove();
				PackageHeader header = pkg.getHeader();
				if (header.getCmd() == Constants.PACK_HEAD_DATA) {
					mSocketListener.onSend(mSessionId, Constants.PACK_HEAD_DATA,
							pkg.getMsgID(), Errors.ERROR_CONNECT_CLOSE);
				}
			}
		}
	}

	public void clearAutoSearch() {
		if (mSocket != null) {
			mBluetoothObject.removeFromDeviceList(mSocket.getRemoteDevice());
		}
	}

	public void init() {
		mThreadRun = true;

		mSessionId = createSessionId();

		mMaxSize = Constants.PACKAGE_QUEUE_SIZE;
		mPackageQueue = new ArrayBlockingQueue<Package>(
				Constants.PACKAGE_QUEUE_SIZE);
		mSendingList = new ArrayList<Package>();

		if (mRecvThread == null) {
			mRecvThread = new Thread(new ReceiveRunnable());
		}
		mRecvThread.start();

		if (mSendThread == null) {
			mSendThread = new Thread(new SendRunnable());
		}
		mSendThread.start();

		if (mRepeatThread == null) {
			mRepeatThread = new Thread(new RepeatRunnable());
		}
		mRepeatThread.start();
	}

	public void setListener(ISocketListener listener) {
		mSocketListener = listener;
	}

	public int addSendPackage(Package pkg) {
		if (pkg == null) {
			return Errors.ERROR_INVALID_PARAMETER;
		}

		synchronized (mQueueLock) {
			// queue not enough
			if (mPackageQueue.size() == mMaxSize) {
				mMaxSize += mMaxSize;
				BlockingQueue<Package> newQueue = new ArrayBlockingQueue<Package>(
						mMaxSize);

				newQueue.addAll(mPackageQueue);
				mPackageQueue = newQueue;
			}

			mPackageQueue.add(pkg);
		}

		return Errors.ERROR_SUCCESS;
	}

	public int closeConnection() {
		PackageHeader header = new PackageHeader();
		header.setCmd(Constants.PACK_HEAD_FINISH);
		header.setFlag(Constants.CONNECT_CLOSE_ACTIVE);

		Package pkg = new Package();
		pkg.setHeader(header);

		return addSendPackage(pkg);
	}

	public int forceCloseConnection() {
		PackageHeader header = new PackageHeader();
		header.setCmd(Constants.PACK_HEAD_FINISH);
		header.setFlag(Constants.CONNECT_CLOSE_FORCE);

		Package pkg = new Package();
		pkg.setHeader(header);

		return addSendPackage(pkg);
	}

	public Package getPackageByDataId(int pkgId) {
		synchronized (mListLock) {
			Iterator<Package> it = mSendingList.iterator();
			while (it.hasNext()) {
				Package pkg = it.next();
				if (pkg.checkPackageId(pkgId)) {
					return pkg;
				}
			}
		}

		return null;
	}

	// internal class
	class ReceiveRunnable implements Runnable {

		@Override
		public void run() {
			Log.i(TAG, "Receive thread is running...");

			while (mThreadRun) {
				if (mIsConnectBreak) {
					try {
						Thread.sleep(100);

						continue;
					} catch (Exception e) {

					}
				}

				byte[] buffer = receive();
				if (buffer != null) {
					mLastOpTime = System.currentTimeMillis();

					int cmd = (Utils.unsignedByteToInt(buffer[0]) << 8)
							+ Utils.unsignedByteToInt(buffer[1]);
					int id = (Utils.unsignedByteToInt(buffer[3]) << 8)
							+ Utils.unsignedByteToInt(buffer[4]);

					switch (cmd) {
					case Constants.PACK_HEAD_ACK: {
						// respond for : 1. data; 2. finish; 3. heartbeat
						synchronized (mListLock) {
							Iterator<Package> iterator = mSendingList.iterator();
							while (iterator.hasNext()) {
								Package pkg = iterator.next();

								if (pkg.getHeader().getId() == id) {
									switch (pkg.getHeader().getCmd()) {
									case Constants.PACK_HEAD_FINISH: {
										mSendingList.remove(pkg);

										mSocketListener.onDisconnect(mSessionId,
												Constants.CONNECT_CLOSE_ACTIVE,
												Errors.ERROR_SUCCESS);
										break;
									}
									case Constants.PACK_HEAD_HEARTBEAT: {
										// do nothing
										mSendingList.remove(pkg);
										break;
									}
									case Constants.PACK_HEAD_DATA: {
										mSendingList.remove(pkg);

										int msgID = pkg.getMsgID();
										mSocketListener.onSend(mSessionId,
												Constants.PACK_HEAD_DATA, msgID,
												Errors.ERROR_SUCCESS);
										break;
									}
									}
									break;
								}
							}
						}
						
						break;
					}
					case Constants.PACK_HEAD_FINISH: {
						// send back respond & close socket
						if (sendRespond(id) != Errors.ERROR_SUCCESS) {
							//sendRespond(id);
						}//?????

						mSocketListener.onDisconnect(mSessionId,
								Constants.CONNECT_CLOSE_PASSIVE,
								Errors.ERROR_SUCCESS);

						break;
					}
					case Constants.PACK_HEAD_ONE_EXIST:{
						// send back respond & close socket
						if (sendRespond(id) != Errors.ERROR_SUCCESS) {
							sendRespond(id);
						}

						mSocketListener.onDisconnect(mSessionId,
								Constants.CONNECT_CLOSE_ONE_EXIST,
								Errors.ERROR_CONNECT_ONE_EXIST);

						break;
					}
					case Constants.PACK_HEAD_HEARTBEAT: {
						// do nothing
						break;
					}
					case Constants.PACK_HEAD_DATA: {
						// send back respond & onsend
						try {
							Package pkg = new Package(buffer);

							if (pkg != null && !isReceiveAlready(pkg.getHeader().getId())) {
								mSocketListener.onRecv(mSessionId, pkg, 0);
							}
							
							if (pkg != null) {
								if (sendRespond(id) != Errors.ERROR_SUCCESS) {
									sendRespond(id);
								}
							}
						} catch (Exception e) {

						}						

						break;
					}
					}
				}
			}
			Log.i(TAG, "Receive thread is stoped...");
		}

	}

	class SendRunnable implements Runnable {

		@Override
		public void run() {
			Log.i(TAG, "Send message thread is running...");

			while (mThreadRun) {
				if (mIsConnectBreak) {
					try {
						Thread.sleep(100);
					} catch (Exception e) {

					}

					continue;
				}

				synchronized (mListLock) {
					if (mSendingList.size() == Constants.CONFIRM_LIST_SIZE) {
						try {
							Thread.sleep(5);
						} catch (Exception e) {
	
						}
	
						continue;
					}
				}
				
				Package pkg = null;
				synchronized (mQueueLock) {
					if (mPackageQueue.size() == 0) {
						continue;
					}
					
					pkg = mPackageQueue.remove();
				}

				try {
					int ret = mBluetoothObject.send(mOutputStream,
							pkg.getBytes());

					Log.i(TAG, "Send result: " + ret);

					pkg.setCount(pkg.getCount() + 1);
					pkg.setSendTime(System.currentTimeMillis());

					if (ret == Errors.ERROR_SUCCESS) {
						mLastOpTime = System.currentTimeMillis();
					}

					if (pkg.getHeader().getCmd() == Constants.PACK_HEAD_HEARTBEAT) {
						continue;
					} else if ((pkg.getHeader().getCmd() == Constants.PACK_HEAD_ACK)
							&& (ret == Errors.ERROR_SUCCESS)) {
						continue;
					} else {
						synchronized (mListLock) {
							mSendingList.add(pkg);
						}
					}
				} catch (IOException e) {
					mIsConnectBreak = true;
					mReconnectTime = System.currentTimeMillis();
					Log.i(TAG, "Send fail, reconnect...");
					reconnect();
				}
				
			}
			Log.i(TAG, "Send message thread is stoped...");
		}

	}

	class RepeatRunnable implements Runnable {

		@Override
		public void run() {
			Log.i(TAG, "Repeat thread is running...");

			while (mThreadRun) {
				long curtime = System.currentTimeMillis();

				if (mIsConnectBreak) {
					if (curtime - mLastOpTime > Constants.CONNECT_BREAK_TIMEOUT) {
						Log.i(TAG, "Break...");
						// Notify connect break
						mSocketListener.onDisconnect(mSessionId,
								Constants.CONNECT_CLOSE_BREAK,
								Errors.ERROR_CONNECT_BREAK);
					} else if (mReconnectFinish
							&& (curtime - mReconnectTime > Constants.RECONNECT_INTERVAL)) {
						Log.i(TAG, "Reconnect...");
						mReconnectTime = System.currentTimeMillis();

						reconnect();
					} else {
						Log.i(TAG, "Sleep...");
						try {
							Thread.sleep(500);
						} catch (Exception e) {

						}
					}

					continue;
				} else {
					// heartbeat
					if ((curtime - mLastOpTime > Constants.HEARTBEAT_OP_IDLE)
							&& (curtime - mLastBeatTime > Constants.HEARTBEAT_OP_IDLE)) {
						mLastBeatTime = System.currentTimeMillis();
						sendHeartbeat();
					}

					// confirm list no package
					synchronized (mListLock) {
						if (mSendingList.size() == 0) {
							try {
								Thread.sleep(5);
							} catch (Exception e) {

							} 

							continue;
						}

						Iterator<Package> iterator = mSendingList.iterator();
						while (iterator.hasNext()) {
							Package pkg = iterator.next();
	
							long nowtime = System.currentTimeMillis();
	
							if (pkg.getCount() >= Constants.RESEND_COUNT) {
								mSendingList.remove(pkg);
								mSocketListener.onSend(mSessionId, pkg.getHeader()
										.getCmd(), pkg.getHeader().getId(),
										Errors.ERROR_TIMEOUT);
	
							} else if (nowtime - pkg.getSendTime() > Constants.RESEND_INTERVAL) {
								try {
									int ret = mBluetoothObject.send(mOutputStream,
											pkg.getBytes());
									pkg.setCount(pkg.getCount() + 1);
									pkg.setSendTime(System.currentTimeMillis());
	
									if ((pkg.getHeader().getCmd() == Constants.PACK_HEAD_ACK)
											&& (ret == Errors.ERROR_SUCCESS)) {
										mSendingList.remove(pkg);
									}
	
								} catch (IOException e) {
									if ((e.getMessage().trim()
											.equals("Broken pipe"))
											|| (e.getMessage().trim()
													.equals("socket closed"))) {
	
										mIsConnectBreak = true;
										mReconnectTime = System.currentTimeMillis();
										reconnect();
									}
								}
							}
						}
					}
				}
			}
			Log.i(TAG, "Repeat thread is stoped...");
		}
	}
}
