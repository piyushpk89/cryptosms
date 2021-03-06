/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.state;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.BadInputException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.ConnectionListener;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.DeclinedException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.NotConnectedException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.PKIErrorException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.PKInotInstalledException;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.TimeoutException;

/*
 * Static class that takes care of PKI login session
 */
public class Pki {
	private static final String INTENT_PKI_LOGIN = "uk.ac.cam.dje38.pki.login";
	private static final String INTENT_PKI_LOGOUT = "uk.ac.cam.dje38.pki.logout";
	
	private static final int TIMEOUT_DEFAULT = 60;
	private static final int TIMEOUT_LOGIN_CHECK = 5;

	private static PKIwrapper mPki = null;
	private static Context mContext = null;
	
	private static byte[] mMasterKey = null;
	private static final String KEY_STORAGE = "CRYPTOSMS_MASTER_KEY";

	private static boolean mMissing = false;
	private static boolean mLoggedIn = false;
	
	public static class PkiNotReadyException extends Exception {
		private static final long serialVersionUID = 5247215307724480706L;
		
		public PkiNotReadyException() {
			super("PKI is not available (either not connected or logged out)");
		}
	}

	/**
	 * Initializes all PKI related stuff and calls connect()
	 *
	 * @param context the context
	 */
	public static void init(Context context) {
		mContext = context;
		if (mPki != null) return;

		IntentFilter filterLogin = new IntentFilter();
		filterLogin.addAction(INTENT_PKI_LOGIN);
		filterLogin.addAction(INTENT_PKI_LOGOUT);
		context.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(INTENT_PKI_LOGIN)) {
					mPki.setTimeout(TIMEOUT_LOGIN_CHECK);
					try {
						Pki.getMasterKey(true); // calls authorize
						Pki.setLoggedIn(true);
					} catch (PkiNotReadyException e) {
						// if this happens then setLoggedIn didn't
					}
					mPki.setTimeout(TIMEOUT_DEFAULT);
				} else if (intent.getAction().equals(INTENT_PKI_LOGOUT))
					Pki.setLoggedIn(false);
			}
		}, filterLogin);
		
		IntentFilter filterInstall = new IntentFilter();
		filterInstall.addAction(Intent.ACTION_PACKAGE_ADDED);
		filterInstall.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filterInstall.addCategory("android.intent.category.DEFAULT");
		filterInstall.addDataScheme("package");
		filterInstall.addDataPath("uk.ac.cam.dje38.pki", 0);
		context.registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
					if (intent.getDataString().equals("package:" + MyApplication.PKI_PACKAGE))
						Pki.setMissing();
				} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
					if (intent.getDataString().equals("package:" + MyApplication.PKI_PACKAGE))
						Pki.connect();
				}
			}
		}, filterInstall);
		
		try {
			mPki = new PKIwrapper(mContext);
		} catch (InterruptedException e1) {
			// ignore
		}

		connect();
	}
	
	public static PKIwrapper getPkiWrapper() {
		return mPki;
	}
	
	/**
	 * Connects to PKIwrapper
	 */
	public static void connect() {
		try {
			if (mPki != null) {
				mPki.setTimeout(TIMEOUT_DEFAULT);
				mPki.connect(new ConnectionListener() {
					
					@Override
					public void onConnectionDeclined() {
						Log.d(MyApplication.APP_TAG, "onConnectionDeclined");
					}
					
					@Override
					public void onConnectionFailed() {
						Log.d(MyApplication.APP_TAG, "onConnectionFailed");
					}
					
					@Override
					public void onConnectionTimeout() {
						Log.d(MyApplication.APP_TAG, "onConnectionTimeout");
					}
					
					@Override
					public void onDisconnect() {
						Log.d(MyApplication.APP_TAG, "onDisconnect");
						if (!State.isInFatalState()) {
							setLoggedIn(false);
							State.notifyDisconnect();
						}
					}
					
					@Override
					public void onConnect() {
						Log.d(MyApplication.APP_TAG, "onConnect");
						State.notifyConnect();
						setLoggedIn(false, true);
						login(false);
					}
				});
				mMissing = false;
			}
		} catch (PKInotInstalledException e) {
			mMissing = true;
			State.notifyPkiMissing();
		}
	}

	public static boolean isConnected() {
		return !State.isInFatalState() && mPki != null && mPki.isConnected();
	}
	
	/**
	 * Call login on PKI
	 *
	 * @param force login synchronously
	 */
	public static void login(boolean force) {
		Log.d(MyApplication.APP_TAG, "Login: " + (isConnected() ? "connected" : "disconnected") + ", " + (isLoggedIn() ? "logged in" : "logged out") + ", " + (force ? "forced" : "unforced") );
		if (isConnected() && !isLoggedIn()) {
			if (force) {
				try {
					mPki.setTimeout(TIMEOUT_DEFAULT);
					setLoggedIn(mPki.authorise());
				} catch (TimeoutException e) {
				} catch (PKIErrorException e) {
				} catch (NotConnectedException e) {
				}
			} else {
				setLoggedIn(false);
				Intent intent = new Intent(MyApplication.PKI_LOGIN);
				mContext.startService(intent);
			}
		} else if (isLoggedIn())
			State.notifyLogin();
	}

	/**
	 * Disconnect from PKI
	 */
	public static void disconnect() {
		if (mPki != null)
			try {
				mPki.disconnect();
			} catch (TimeoutException e) {
			} catch (PKIErrorException e) {
		}
	}
	
	/**
	 * PKI is missing
	 */
	static void setMissing() {
		Log.d(MyApplication.APP_TAG, "PKI missing");
		mMissing = true;
		State.notifyPkiMissing();
	}

	/*
	 * Is PKI missing
	 */
	static boolean isMissing() {
		return mMissing;
	}

	public static boolean isLoggedIn() {
		return isConnected() && mLoggedIn;
	}
	
	static void setLoggedIn(boolean value) {
		setLoggedIn(value, false);
	}
	
	/**
	 * Sets whether the user is logged in
	 *
	 * @param value
	 * @param forceNotify send notification to listeners
	 */
	static void setLoggedIn(boolean value, boolean forceNotify) {
		if (mLoggedIn != value || forceNotify) {
			mLoggedIn = value;
			if (mLoggedIn) {
				State.notifyLogin();
			} else {
//				mMasterKey = null; // forget the master key
				State.notifyLogout();
			}
		}
	}
	
	/**
	 * Gets the master key to storage file.
	 *
	 * @param forceLogIn
	 * @return the master key
	 * @throws PkiNotReadyException
	 */
	public static byte[] getMasterKey(boolean forceLogIn) throws PkiNotReadyException {
		return getMasterKey(forceLogIn, true);
	}
	
	/**
	 * Gets the master key to storage file.
	 *
	 * @param forceLogIn
	 * @param generateAllow if the key is not stored, allow it to be generated
	 * @return the master key
	 * @throws PkiNotReadyException
	 */
	public static byte[] getMasterKey(boolean forceLogIn, boolean generateAllow) throws PkiNotReadyException {
		if (mMasterKey != null)
			return mMasterKey;
		else if (isLoggedIn()) {
			try {
				if (mPki.hasDataStore(KEY_STORAGE)) {
					mMasterKey = mPki.getDataStore(KEY_STORAGE);
					if (mMasterKey == null)
						throw new PkiNotReadyException();
					else
						return mMasterKey;
				} else if (generateAllow) {
					mPki.setDataStore(KEY_STORAGE, Encryption.getEncryption().generateRandomData(Encryption.SYM_KEY_LENGTH));
					return getMasterKey(forceLogIn, false);
				} else
					throw new PkiNotReadyException();
			} catch (NotConnectedException e) {
				throw new PkiNotReadyException();
			} catch (TimeoutException e) {
				throw new PkiNotReadyException();
			} catch (DeclinedException e) {
				throw new PkiNotReadyException();
			} catch (PKIErrorException e) {
				throw new PkiNotReadyException();
			} catch (BadInputException e) {
				throw new PkiNotReadyException();
			}
		} else if (isConnected() && forceLogIn) {
			Pki.login(true);
			// no need to alter our own login information - broadcast will be sent by PKI
			return getMasterKey(false, generateAllow);
		} else
			throw new PkiNotReadyException();
	}
}
