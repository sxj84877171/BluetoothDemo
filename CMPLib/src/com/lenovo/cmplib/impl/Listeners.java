package com.lenovo.cmplib.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.lenovo.cmplib.base.Errors;
import com.lenovo.cmplib.base.ISessionListener;

public class Listeners {
	private List<ISessionListener> mListeners;
	private Object mObject;

	public Listeners() {
		mListeners = new ArrayList<ISessionListener>();
		mObject = new Object();
	}

	public int registerListener(ISessionListener listener) {
		synchronized (mObject) {
			for (ISessionListener sessionListener : mListeners) {
				if (sessionListener == listener) {
					return Errors.ERROR_SUCCESS;
				}
			}

			mListeners.add(listener);
		}

		return Errors.ERROR_SUCCESS;
	}

	public int unregisterListener(ISessionListener listener) {
		synchronized (mObject) {
			Iterator<ISessionListener> iterator = mListeners.iterator();
			while (iterator.hasNext()) {
				ISessionListener sessionListener = iterator.next();
				if (sessionListener == listener) {
					mListeners.remove(sessionListener);
					return Errors.ERROR_SUCCESS;
				}
			}
		}

		return Errors.ERROR_NOT_FOUND;
	}

	public void onConnect(int sessionID, int errorCode) {
		for (ISessionListener listener : mListeners) {
			listener.onConnect(sessionID, errorCode);
		}
	}

	public void onDisconnect(int sessionID, int errorCode) {
		for (ISessionListener listener : mListeners) {
			listener.onDisconnect(sessionID, errorCode);
		}
	}

	public void onSend(int sessionID, int msgID, int errorCode) {
		for (ISessionListener listener : mListeners) {
			listener.onSend(sessionID, msgID, errorCode);
		}
	}

	public void onRecv(int sessionID, String message) {
		for (ISessionListener listener : mListeners) {
			listener.onRecv(sessionID, message);
		}
	}
}
