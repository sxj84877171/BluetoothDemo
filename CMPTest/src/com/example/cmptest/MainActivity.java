package com.example.cmptest;

import com.lenovo.cmplib.base.Errors;
import com.lenovo.cmplib.base.ISession;
import com.lenovo.cmplib.base.ISessionListener;
import com.lenovo.cmplib.base.InitInfo;
import com.lenovo.cmplib.base.SessionInfo;
import com.lenovo.cmplib.impl.Session;
import com.lenovo.cmplib.impl.SlaveSession;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements ISessionListener {
	private String TAG = "MainActivity";
	
	private Button connectButton;
	private Button sendButton;
	private Button closeButton;
	private Button sendBigDataButton;
	private EditText editAddress;
	private EditText editMessage;
	
	private ISession mSession;
	private int mSessionId;
	private int mMsgId;
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			
			}
		};
	};
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		connectButton = (Button) this.findViewById(R.id.bt_connect);
		sendButton = (Button) this.findViewById(R.id.bt_send);
		closeButton = (Button) this.findViewById(R.id.bt_close);
		sendBigDataButton = (Button) this.findViewById(R.id.bt_sendBigData);
		editAddress = (EditText) this.findViewById(R.id.edit_address);
		editMessage = (EditText) this.findViewById(R.id.edit_message);
		
		mSession = new Session(MainActivity.this);
		mSession.registerListener(this);
		mSession.init(null);
		
		mSessionId = 0;
		
		connectButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String address = editAddress.getText().toString();
				if (address != null) {
					SessionInfo info = new SessionInfo();
					info.setBluetoothMacAddr(address);
					
					Log.i(TAG, "Connect to " + address);
					
					mSession.connect(info);
				}
			}
		});
		
		sendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {	
				String message = editMessage.getText().toString();
				if (message != null) {					
					int ret = mSession.send(mSessionId, message);
					if (ret < 0) {
						Toast.makeText(MainActivity.this, "Send message error: " + ret, 1).show();
					} else {
						mMsgId = ret;
					}
				}
			}
		});
		
		sendBigDataButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				/*for (int i=0; i<20; i++) {
					byte[] data = new byte[1000];
					for (int j=0; j<1000; j++) {
						data[j] = (byte) (65 + i);
					}
					
					String message = new String(data);
					if (message != null) {					
						int ret = mSession.send(mSessionId, message);
						if (ret < 0) {
							Toast.makeText(MainActivity.this, "Send message error(" + i + "): " + ret, 1).show();
						} else {
							mMsgId = ret;
						}
					}
				}*/
				
				String message = getString(R.string.message);
				Log.i(TAG, message);
				if (message != null) {					
					int ret = mSession.send(mSessionId, message);
					if (ret < 0) {
						Toast.makeText(MainActivity.this, "Send message error: " + ret, 1).show();
					} else {
						mMsgId = ret;
					}
				}
				
				message = getString(R.string.message1);
				if (message != null) {					
					int ret = mSession.send(mSessionId, message);
					if (ret < 0) {
						Toast.makeText(MainActivity.this, "Send message error: " + ret, 1).show();
					} else {
						mMsgId = ret;
					}
				}
			}
		});
		
		closeButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mSessionId > 0) {
					mSession.disconnect(mSessionId);
				}
			}
		});
	}
	
	@Override
	protected void onStop() {
		Log.i(TAG, "onStop...");
		
		if (mSession != null) {
			mSession.uninit();
		}
		super.onStop();
	}

	@Override
	public void onConnect(int sessionID, int errorCode) {
		Log.i(TAG, "onConnect...");
		
		if (errorCode == Errors.ERROR_SUCCESS) {
			mSessionId = sessionID;
			
			Toast.makeText(MainActivity.this, "Connect success!", 1).show();
		} else {
			Toast.makeText(MainActivity.this, "Connect fail!, error: " + errorCode, 1).show();
		}
	}


	@Override
	public void onDisconnect(int sessionID, int errorCode) {
		Log.i(TAG, "onDisconnect...");
		
		if (errorCode == Errors.ERROR_SUCCESS) {
			mSessionId = 0;
			Toast.makeText(MainActivity.this, "Disconnect success", 1).show();
		} else if (errorCode == Errors.ERROR_CONNECT_BREAK) {
			mSessionId = 0;
			Toast.makeText(MainActivity.this, "Connection break", 1).show();
		} else if (errorCode == Errors.ERROR_CONNECT_ONE_EXIST) {
			mSessionId = 0;
			Toast.makeText(MainActivity.this, "One connection is exist in server", 1).show();
		} else {
			Toast.makeText(MainActivity.this, "Disconnect error: " + errorCode, 1).show();
		} 
	}


	@Override
	public void onRecv(int sessionID, String message) {
		Log.i(TAG, "onRecv...");
		
		Toast.makeText(MainActivity.this, "Receive message:\n" + message, 0).show();
	}


	@Override
	public void onSend(int sessionID, int msgID, int errorCode) {
		Log.i(TAG, "onSend...");
		
		if (msgID == mMsgId) {
			if (errorCode == Errors.ERROR_SUCCESS) {
				Toast.makeText(MainActivity.this, "Send message success!", 1).show();
			} else {
				Toast.makeText(MainActivity.this, "Send message error: " + errorCode, 1).show();
			}
		}
	}
	
}
