package com.lenovo.cmplib.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.lenovo.cmplib.base.Constants;
import com.lenovo.cmplib.base.Errors;
import com.lenovo.cmplib.base.ISessionListener;
import com.lenovo.cmplib.base.ISessionRole;
import com.lenovo.cmplib.base.ISocketListener;
import com.lenovo.cmplib.base.InitInfo;
import com.lenovo.cmplib.base.PackageHeader;
import com.lenovo.cmplib.base.SessionInfo;

public class SlaveSession implements ISessionRole, ISocketListener {
	private String TAG = "SlaveSession";
	private Context mContext;
	private Listeners mListener;
	private Bluetooth mBluetoothObject;
	
	private List<MasterInfo> mMasters;
	private Object mListLock;
	private Thread mCheckServerThread;
	private boolean mRelease;

	public SlaveSession(Context context) {
		mContext = context;
		mListener = new Listeners();

		mBluetoothObject = null;
		mMasters = null;
		mRelease = false;

		mListLock = new Object();
		mCheckServerThread = null;
	}

	@Override
	public void onNotify(int param) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnect(BluetoothSocket socket, String address, int param,
			int errorCode) {
		switch (param) {
		case Constants.CONNECT_APP: {
			if (socket == null) {
				onConnect(Constants.INVALID_SESSIONID, Errors.ERROR_FAIL);
				return;
			}

			if (errorCode != Errors.ERROR_SUCCESS) {
				onConnect(Constants.INVALID_SESSIONID, errorCode);
				break;
			}

			// make sure only one connection
			synchronized (mListLock) {
				if (mMasters.size() > 0) {
					for (MasterInfo master : mMasters) {
						master.forceCloseConnection();
					}
				}

				try {
					MasterInfo masterInfo = new MasterInfo(socket);
					masterInfo.setListener(this);
					masterInfo.setBluetooth(mBluetoothObject);
					masterInfo.init();

					mMasters.add(masterInfo);

					Log.i(TAG, "New connect("
							+ socket.getRemoteDevice().getAddress()
							+ ") is ready...");
					onConnect(masterInfo.getSessionId(), Errors.ERROR_SUCCESS);

				} catch (IOException e) {
					onConnect(Constants.INVALID_SESSIONID, Errors.ERROR_FAIL);
				}
			}

			break;
		}
		case Constants.CONNECT_RECONNECT: {
			MasterInfo master = getMasterInfoByAddress(address);
			if (master != null) {
				if (socket != null) {
					master.refreshSocket(socket);
				} else {
					master.reconnectFail();
				}
			}

			break;
		}
		case Constants.CONNECT_AUTO: {
			if (errorCode == Errors.ERROR_SUCCESS) {
				synchronized (mListLock) {
					try {
						MasterInfo masterInfo = new MasterInfo(socket);
						masterInfo.setListener(this);
						masterInfo.setBluetooth(mBluetoothObject);
						masterInfo.init();

						mMasters.add(masterInfo);

						onConnect(masterInfo.getSessionId(),
								Errors.ERROR_SUCCESS);

					} catch (Exception e) {

					}
				}

			}
			break;
		}
		}
	}

	@Override
	public void onDisconnect(int sessionId, int param, int errorCode) {
		switch (param) {
		case Constants.CONNECT_CLOSE_ACTIVE:
		case Constants.CONNECT_CLOSE_PASSIVE: {
			onDisconnect(sessionId, errorCode);

			if (errorCode == Errors.ERROR_SUCCESS) {
				removeCloseMaster(sessionId);
			}

			break;
		}
		case Constants.CONNECT_CLOSE_BREAK: {
			onDisconnect(sessionId, errorCode);

			if (errorCode == Errors.ERROR_SUCCESS
					|| errorCode == Errors.ERROR_CONNECT_BREAK) {
				removeBreakMaster(sessionId);
			}

			break;
		}
		case Constants.CONNECT_CLOSE_ONE_EXIST: {
			onDisconnect(sessionId, Errors.ERROR_CONNECT_ONE_EXIST);
			removeBreakMaster(sessionId);
			break;
		}
		case Constants.CONNECT_CLOSE_FORCE: {
			synchronized (mListLock) {
				Iterator<MasterInfo> iterator = mMasters.iterator();
				while (iterator.hasNext()) {
					MasterInfo master = iterator.next();
					if (master.getSessionId() == sessionId) {
						mMasters.remove(master);
						return;
					}
				}
			}
			break;
		}
		}

	}

	@Override
	public void onSend(int sessionId, int cmd, int packageId, int param) {
		switch (cmd) {
		case Constants.PACK_HEAD_ACK:
			break;
		case Constants.PACK_HEAD_FINISH: {
			if (param != Errors.ERROR_SUCCESS) {
				onDisconnect(sessionId, param);
			}

			break;
		}
		case Constants.PACK_HEAD_HEARTBEAT:
			break;
		case Constants.PACK_HEAD_DATA: {
			onSend(sessionId, packageId, param);
			break;
		}
		}
	}

	@Override
	public void onRecv(int sessionId, Package pkg, int param) {
		if (pkg == null) {
			return;
		}

		PackageHeader header = pkg.getHeader();
		if (header == null) {
			return;
		}

		switch (header.getCmd()) {
		case Constants.PACK_HEAD_ACK: {
			break;
		}
		case Constants.PACK_HEAD_FINISH: {
			break;
		}
		case Constants.PACK_HEAD_HEARTBEAT: {
			break;
		}
		case Constants.PACK_HEAD_DATA: {
			String message = new String(pkg.getContent());
			onRecv(sessionId, message);
			break;
		}
		}
	}

	@Override
	public int registerListener(ISessionListener listener) {
		return mListener.registerListener(listener);
	}
	
	@Override
	public int unregisterListener(ISessionListener listener) {
		return mListener.unregisterListener(listener);
	}

	@Override
	public int init(InitInfo info) {
		if (info == null
				|| info.getConnector() == InitInfo.EConnectorType.BLUETOOTH) {
			mBluetoothObject = new Bluetooth(mContext);
			if (mBluetoothObject.init() != Errors.ERROR_SUCCESS) {
				return Errors.ERROR_FAIL;
			}

			mBluetoothObject.rigisterListener(this);

			mMasters = new ArrayList<MasterInfo>();

			mRelease = false;
			mCheckServerThread = new Thread(new CheckServerRunnable());
			mCheckServerThread.start();

			return Errors.ERROR_SUCCESS;
		} else {
			return Errors.ERROR_INVALID_PARAMETER;
		}
	}

	@Override
	public int uninit() {		
		mRelease = true;

		synchronized (mListLock) {
			Iterator<MasterInfo> iterator = mMasters.iterator();
			while (iterator.hasNext()) {
				MasterInfo master = iterator.next();
				master.release();

				mMasters.remove(master);
			}
		}

		if (mBluetoothObject != null) {
			mBluetoothObject.release();
		}		

		return Errors.ERROR_SUCCESS;
	}

	@Override
	public int connect(SessionInfo info) {
		if ((info != null)
				&& (info.getBluetoothMacAddr().trim().length() == 17)) {
			Log.i(TAG, "Connect to device:" + info.getBluetoothMacAddr());

			synchronized (mListLock) {
				if (mMasters.size() > 0) {
					for (MasterInfo master : mMasters) {
						if (master.isMatchAddress(info.getBluetoothMacAddr())) {
							onConnect(master.getSessionId(),
									Errors.ERROR_SUCCESS);
							return Errors.ERROR_SUCCESS;
						}
					}
				}
			}

			int ret = mBluetoothObject
					.connectServer(info.getBluetoothMacAddr());
			if (ret != Errors.ERROR_SUCCESS) {
				onConnect(Constants.INVALID_SESSIONID, ret);
			}

			return ret;
		}

		onConnect(Constants.INVALID_SESSIONID, Errors.ERROR_NOT_FOUND);
		return Errors.ERROR_NOT_FOUND;
	}

	@Override
	public int disconnect(int sessionID) {
		if (sessionID <= 0) {
			onDisconnect(sessionID, Errors.ERROR_NOT_FOUND);
			return Errors.ERROR_NOT_FOUND;
		}

		MasterInfo master = getMasterInfoBySessionId(sessionID);
		if (master == null) {
			onDisconnect(sessionID, Errors.ERROR_NOT_FOUND);
			return Errors.ERROR_NOT_FOUND;
		}

		int ret = master.closeConnection();
		if (ret != Errors.ERROR_SUCCESS) {
			onDisconnect(sessionID, ret);
		}

		return ret;
	}

	@Override
	public int send(int sessionID, String message) {
		if (sessionID <= 0) {
			return Errors.ERROR_NOT_FOUND;
		}

		MasterInfo master = getMasterInfoBySessionId(sessionID);
		if (master == null) {
			return Errors.ERROR_NOT_FOUND;
		}

		// Log.i(TAG, "Send message:" + message);

		PackageHeader header = new PackageHeader();
		header.setCmd(Constants.PACK_HEAD_DATA);

		Package pkg = new Package();
		pkg.setHeader(header);
		pkg.setContent(message.getBytes());

		int ret = master.addSendPackage(pkg);
		if (ret == Errors.ERROR_SUCCESS) {
			return pkg.getMsgID();
		}

		return ret;
	}

	// private
	private MasterInfo getMasterInfoByAddress(String address) {
		synchronized (mListLock) {
			for (int i = 0; i < mMasters.size(); i++) {
				if (mMasters.get(i).getSocket().getRemoteDevice().getAddress()
						.equals(address)) {
					return mMasters.get(i);
				}
			}
		}

		return null;
	}

	private MasterInfo getMasterInfoBySessionId(int sessionId) {
		synchronized (mListLock) {
			for (int i = 0; i < mMasters.size(); i++) {
				if (mMasters.get(i).getSessionId() == sessionId) {
					return mMasters.get(i);
				}
			}
		}

		return null;
	}

	private void removeCloseMaster(int sessionId) {
		synchronized (mListLock) {
			Iterator<MasterInfo> iterator = mMasters.iterator();
			while (iterator.hasNext()) {
				MasterInfo master = iterator.next();
				if (master.getSessionId() == sessionId) {
					master.clearAutoSearch();
					master.release();
					mMasters.remove(master);
					break;
				}
			}
		}
	}

	private void removeBreakMaster(int sessionId) {
		synchronized (mListLock) {
			Iterator<MasterInfo> iterator = mMasters.iterator();
			while (iterator.hasNext()) {
				MasterInfo master = iterator.next();
				if (master.getSessionId() == sessionId) {
					master.release();
					master.notifySendMessageFail();
					mMasters.remove(master);
					break;
				}
			}
		}
	}

	// internal class
	class CheckServerRunnable implements Runnable {

		@Override
		public void run() {
			Log.i(TAG, "Check server thread is running...");

			while (!mRelease) {
				try {
					Thread.sleep(Constants.CHECK_SERVER_INTERVAL);
				} catch (Exception e) {
				}

				if (mMasters.size() > 0) {
					continue;
				}

				// make sure only one connection
				mBluetoothObject.autoSearchMaster();
			}
			Log.i(TAG, "Check server thread is stoped...");
		}
	}

	private void onConnect(int sessionID, int errorCode) {
		Message msg = mHandler.obtainMessage();

		msg.what = Constants.OP_ONCONNECT;
		msg.arg1 = sessionID;
		msg.arg2 = errorCode;

		mHandler.sendMessage(msg);
	}

	private void onDisconnect(int sessionID, int errorCode) {
		Message msg = mHandler.obtainMessage();

		msg.what = Constants.OP_ONDISCONNECT;
		msg.arg1 = sessionID;
		msg.arg2 = errorCode;

		mHandler.sendMessage(msg);
	}

	private void onSend(int sessionID, int msgID, int errorCode) {
		Message msg = mHandler.obtainMessage();

		msg.what = Constants.OP_ONSEND;
		msg.arg1 = sessionID;
		msg.arg2 = errorCode;
		msg.obj = "" + msgID;

		mHandler.sendMessage(msg);
	}

	private void onRecv(int sessionID, String message) {
		Message msg = mHandler.obtainMessage();

		msg.what = Constants.OP_ONRECV;
		msg.arg1 = sessionID;
		msg.obj = message;

		mHandler.sendMessage(msg);
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case Constants.OP_ONCONNECT: {
				mListener.onConnect(msg.arg1, msg.arg2);
				break;
			}
			case Constants.OP_ONDISCONNECT: {
				mListener.onDisconnect(msg.arg1, msg.arg2);
				break;
			}
			case Constants.OP_ONSEND: {
				mListener.onSend(msg.arg1, Integer.parseInt((String) msg.obj),
						msg.arg2);
				break;
			}
			case Constants.OP_ONRECV: {
				mListener.onRecv(msg.arg1, (String) msg.obj);
				break;
			}
			}
		};
	};
}
