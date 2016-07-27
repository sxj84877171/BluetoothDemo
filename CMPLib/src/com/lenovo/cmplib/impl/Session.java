package com.lenovo.cmplib.impl;

import android.content.Context;

import com.lenovo.cmplib.base.Errors;
import com.lenovo.cmplib.base.ISession;
import com.lenovo.cmplib.base.ISessionListener;
import com.lenovo.cmplib.base.ISessionRole;
import com.lenovo.cmplib.base.InitInfo;
import com.lenovo.cmplib.base.SessionInfo;

public class Session implements ISession {
	private ISessionRole mSessionRole;
	private boolean mIsInit;

	public Session(Context context) {
		mSessionRole = new SlaveSession(context);
		mIsInit = false;
	}

	@Override
	public int registerListener(ISessionListener listener) {
		if (listener != null) {
			return mSessionRole.registerListener(listener);
		}

		return Errors.ERROR_FAIL;
	}

	@Override
	public int unregisterListener(ISessionListener listener) {
		if (listener != null) {
			return mSessionRole.unregisterListener(listener);
		}

		return Errors.ERROR_SUCCESS;
	}

	@Override
	public int init(InitInfo info) {
		if (mIsInit) {
			return Errors.ERROR_SUCCESS;
		}

		return mSessionRole.init(info);
	}
	
	@Override
	public int uninit() {		
		return mSessionRole.uninit();
	}

	@Override
	public int connect(SessionInfo info) {
		return mSessionRole.connect(info);
	}

	@Override
	public int disconnect(int sessionID) {
		return mSessionRole.disconnect(sessionID);
	}

	@Override
	public int send(int sessionID, String message) {
		return mSessionRole.send(sessionID, message);
	}

}
