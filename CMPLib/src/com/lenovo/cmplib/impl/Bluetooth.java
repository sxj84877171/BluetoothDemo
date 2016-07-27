package com.lenovo.cmplib.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.lenovo.cmplib.base.Constants;
import com.lenovo.cmplib.base.Errors;
import com.lenovo.cmplib.base.ISocketListener;
import com.lenovo.cmplib.base.Utils;

public class Bluetooth {
	private final String TAG = "Bluetooth";
	private UUID mCmpUUID = UUID.nameUUIDFromBytes("Lenovo_AirDrop".getBytes());

	private Context mContext;
	private BluetoothAdapter mAdapter;
	private List<BluetoothDevice> mDeviceList;
	private boolean mBonding;
	private String mBondingAddress;

	private ISocketListener mListener;

	public Bluetooth(Context context) {
		mContext = context;
		mAdapter = null;
	}

	public void autoSearchMaster() {
		for (BluetoothDevice device : mDeviceList) {

			try {
				BluetoothSocket socket = device
						.createRfcommSocketToServiceRecord(mCmpUUID);
				if (socket != null) {
					socket.connect();

					Log.i(TAG, "Auto search, device:(" + device.getName() + ","
							+ device.getAddress() + ") found");

					mListener.onConnect(socket, null, Constants.CONNECT_AUTO,
							Errors.ERROR_SUCCESS);

					// order devices, last connect is top one
					freshDeviceList(device);

					break;
				}
			} catch (Exception e) {
				Log.i(TAG, "Auto search exception...");
			}
		}
	}

	// private
	private List<BluetoothDevice> getBondedPCDevices() {
		Set<BluetoothDevice> set = mAdapter.getBondedDevices();
		if (set.size() == 0) {
			return new ArrayList<BluetoothDevice>();
		}

		List<BluetoothDevice> bonedPC = new ArrayList<BluetoothDevice>();
		for (BluetoothDevice device : set) {
			if (isPCDevice(device.getBluetoothClass())) {
				bonedPC.add(device);
			}
		}

		return filterDeviceListFromCache(bonedPC);
	}

	private boolean isPCDevice(BluetoothClass cls) {
		/*switch (cls.getDeviceClass()) {
		case BluetoothClass.Device.PHONE_SMART:
		case BluetoothClass.Device.PHONE_CELLULAR:
		case BluetoothClass.Device.PHONE_CORDLESS:
		case BluetoothClass.Device.PHONE_ISDN:
		case BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY:
		case BluetoothClass.Device.PHONE_UNCATEGORIZED:
			return false;
		case BluetoothClass.Device.COMPUTER_DESKTOP:
		case BluetoothClass.Device.COMPUTER_LAPTOP:
		case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
		case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
		case BluetoothClass.Device.COMPUTER_SERVER:
		case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:		
		default:
			return true;
		}*/
		
		return true;
	}

	private int pairWithDeviceByAddress(String macAddress) {
		if (macAddress == null) {
			Log.i(TAG, "Address is null");
			return Errors.ERROR_INVALID_PARAMETER;
		}

		// check the MAC address format
		if (!BluetoothAdapter.checkBluetoothAddress(macAddress)) {
			Log.i(TAG, "Address " + macAddress + " is wrong format");
			return Errors.ERROR_INVALID_PARAMETER;
		}

		BluetoothDevice remoteDevice = mAdapter.getRemoteDevice(macAddress);
		if (remoteDevice == null) {
			return Errors.ERROR_NOT_FOUND;
		}

		Log.i(TAG, "Bonding...");
		if (remoteDevice.getBondState() == BluetoothDevice.BOND_NONE) {
			try {
				Method crateBondMethod = BluetoothDevice.class
						.getMethod("createBond");
				Boolean ret = (Boolean) crateBondMethod.invoke(remoteDevice);
				if (ret) {
					return Errors.ERROR_SUCCESS;
				}

				return Errors.ERROR_FAIL;
			} catch (Exception e) {
				return Errors.ERROR_FAIL;
			}
		} else {
			return Errors.ERROR_BLUETOOTH_BONDED;
		}
	}

	private void connectDevice(BluetoothDevice device, int param) {
		new Thread(new ConnectRunnable(device, param)).start();
	}

	private void freshDeviceList(BluetoothDevice device) {
		if (device != null) {
			String address = device.getAddress();

			Iterator<BluetoothDevice> iterator = mDeviceList.iterator();
			while (iterator.hasNext()) {
				BluetoothDevice btDevice = iterator.next();
				if (btDevice.getAddress().equals(address)) {
					mDeviceList.remove(btDevice);
					break;
				}
			}

			mDeviceList.add(0, device);
		}

		saveDeviceList2Cache(mDeviceList);
	}

	public void removeFromDeviceList(BluetoothDevice device) {
		if (device != null) {
			String address = device.getAddress();

			Iterator<BluetoothDevice> iterator = mDeviceList.iterator();
			while (iterator.hasNext()) {
				BluetoothDevice btDevice = iterator.next();
				if (btDevice.getAddress().equals(address)) {
					mDeviceList.remove(btDevice);

					saveDeviceList2Cache(mDeviceList);

					break;
				}
			}
		}
	}

	private List<BluetoothDevice> filterDeviceListFromCache(
			List<BluetoothDevice> list) {
		List<String> oldList = new ArrayList<String>();

		SharedPreferences sp = mContext.getSharedPreferences("deviceInfo",
				Context.MODE_PRIVATE);
		for (int i = 0; i < 100; i++) {
			if (sp.contains("mac" + i)) {
				String addres = sp.getString("mac" + i, "");

				if (BluetoothAdapter.checkBluetoothAddress(addres)) {
					oldList.add(addres);
				}
			} else {
				break;
			}
		}

		List<BluetoothDevice> newList = new ArrayList<BluetoothDevice>();

		Iterator<BluetoothDevice> iterator = list.iterator();
		while (iterator.hasNext()) {
			BluetoothDevice device = iterator.next();
			if (oldList.contains(device.getAddress())) {
				newList.add(device);
			}
		}

		return newList;
	}

	private int saveDeviceList2Cache(List<BluetoothDevice> list) {
		SharedPreferences sp = mContext.getSharedPreferences("deviceInfo",
				Context.MODE_PRIVATE);

		Editor editor = sp.edit();

		for (int i = 0; i < list.size(); i++) {
			editor.putString("mac" + i, list.get(i).getAddress());
		}

		editor.commit();
		return Errors.ERROR_SUCCESS;
	}

	// override
	public int init() {
		Log.i(TAG, "Bluetooth init...");

		mAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mAdapter == null) {
			Log.e(TAG, "Not support bluetooth");
			return Errors.ERROR_BLUETOOTH_NOTSUPPORT;
		}

		// bluetooth not open, open it
		if (mAdapter.isEnabled() != true) {
			boolean ret = mAdapter.enable();
			if (ret != true) {
				Log.e(TAG, "Bluetooth can not open");
				return Errors.ERROR_BLUETOOTH_NOTSUPPORT;
			}
		}

		mDeviceList = getBondedPCDevices();

		// register receiver
		IntentFilter filter = new IntentFilter(
				BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		mContext.registerReceiver(mReceiver, filter);

		Log.i(TAG, "Bluetooth init finish...");
		return Errors.ERROR_SUCCESS;
	}

	public int release() {
		mContext.unregisterReceiver(mReceiver);
		return Errors.ERROR_SUCCESS;
	}

	public int startServer() {
		return Errors.ERROR_NOT_IMPLEMENT;
	}

	public int connectServer(String address) {
		int result = Errors.ERROR_FAIL;
		mBonding = false;

		address = address.toUpperCase();
		int ret = pairWithDeviceByAddress(address);
		if (ret == Errors.ERROR_SUCCESS) {
			mBonding = true;
			mBondingAddress = address;
			return Errors.ERROR_SUCCESS; // callback in the receive

		} else if (ret == Errors.ERROR_BLUETOOTH_BONDED) {
			BluetoothDevice remoteDevice = mAdapter.getRemoteDevice(address);
			if (remoteDevice == null) {
				result = Errors.ERROR_NOT_FOUND;
			}

			Log.i(TAG, "Device(" + remoteDevice.getAddress()
					+ ") is bonded, connecting it...");

			connectDevice(remoteDevice, Constants.CONNECT_APP);
			result = Errors.ERROR_SUCCESS;
		}

		Log.i(TAG, "Pair result:" + ret);

		return result;
	}

	public int reconnectServer(String address) {
		BluetoothDevice remoteDevice = mAdapter.getRemoteDevice(address);
		if (remoteDevice == null) {
			return Errors.ERROR_NOT_FOUND;
		}

		connectDevice(remoteDevice, Constants.CONNECT_RECONNECT);

		return Errors.ERROR_SUCCESS;
	}

	public int disconnect(BluetoothSocket socket) throws IOException {
		if (socket == null) {
			return Errors.ERROR_INVALID_PARAMETER;
		}

		socket.close();

		return Errors.ERROR_SUCCESS;
	}

	public int send(OutputStream stream, byte[] buffer) throws IOException {
		if ((stream == null) || (buffer == null)) {
			return Errors.ERROR_INVALID_PARAMETER;
		}

		Log.i(TAG, "Send :" + Utils.getBufferString(buffer, buffer.length));

		stream.write(buffer);
		stream.flush();

		return Errors.ERROR_SUCCESS;
	}

	public byte[] receive(InputStream stream) throws IOException {
		if (stream == null) {
			return null;
		}

		int len = -1;
		byte[] buffer = new byte[Constants.RECEIVE_BUFFER_SIZE];

		len = stream.read(buffer);

		if (len == -1) {
			return null;
		}

		byte[] result = new byte[len];
		System.arraycopy(buffer, 0, result, 0, len);

		return result;
	}

	public void rigisterListener(ISocketListener listener) {
		this.mListener = listener;
	}

	class ConnectRunnable implements Runnable {
		BluetoothDevice mDevice;
		int mParam;

		public ConnectRunnable(BluetoothDevice device, int param) {
			mDevice = device;
			mParam = param;
		}

		@Override
		public void run() {
			BluetoothSocket socket = null;

			try {
				socket = mDevice.createRfcommSocketToServiceRecord(mCmpUUID);
				if (socket != null) {
					socket.connect();

					Log.i(TAG,
							"Device:(" + mDevice.getName() + ","
									+ mDevice.getAddress() + ") connected");

					mListener.onConnect(socket, mDevice.getAddress(), mParam,
							Errors.ERROR_SUCCESS);

					// order devices, last connect is top one
					freshDeviceList(mDevice);
				} else {
					mListener.onConnect(socket, mDevice.getAddress(), mParam,
							Errors.ERROR_FAIL);
				}
			} catch (IOException e) {
				if (mParam == Constants.CONNECT_RECONNECT) {
					mListener.onConnect(null, mDevice.getAddress(),
							Constants.CONNECT_RECONNECT, Errors.ERROR_FAIL);
				} else if (mParam == Constants.CONNECT_APP) {
					mListener.onConnect(null, null, Constants.CONNECT_APP,
							Errors.ERROR_SERVER_NOT_FOUND);
				}
			}
		}

	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (action.trim().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				int bondedState = intent.getIntExtra(
						BluetoothDevice.EXTRA_BOND_STATE, -1);

				if (bondedState == BluetoothDevice.BOND_NONE) {
					removeFromDeviceList(device);
				} else if (mBonding && (device != null)
						&& (bondedState == BluetoothDevice.BOND_BONDED)
						&& (device.getAddress().equals(mBondingAddress))) {

					connectDevice(device, Constants.CONNECT_APP);

					mBonding = false; // if during pairing, cancel it锟斤拷how to change mBonding to false?
				}
			}
		}

	};

}
