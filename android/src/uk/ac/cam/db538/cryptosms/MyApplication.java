package uk.ac.cam.db538.cryptosms;

import java.io.File;
import java.util.ArrayList;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionPki;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.data.TextMessage;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys.SimNumber;
import uk.ac.cam.db538.cryptosms.ui.PkiInstallActivity;
import uk.ac.cam.db538.cryptosms.utils.CompressedText;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.ConnectionListener;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.PKInotInstalledException;
import android.app.Application;
import android.app.Notification;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.util.Log;

public class MyApplication extends Application {
	private static short SMS_PORT; 
	public static final int NOTIFICATION_ID = 1;
	public static final String APP_TAG = "CRYPTOSMS";
	private static final String STORAGE_FILE_NAME = "storage.db";
	
	private static MyApplication mSingleton;
	
	public static MyApplication getSingleton() {
		return mSingleton;
	}
	
	public static short getSmsPort() {
		return SMS_PORT;
	}

	private Notification mNotification = null;
	private PKIwrapper mPki = null;
	private ArrayList<ProgressDialog> mPkiWaitingDialogs = new ArrayList<ProgressDialog>();
	
	//private final Context mContext = this.getApplicationContext();
	private ConnectionListener onPkiConnect;

	private void initPki() {
		if (mPki != null) return;
		
		try {
			mPki = new PKIwrapper(this.getApplicationContext());
		} catch (InterruptedException e1) {
			// ignore
		}
		try {
			if (mPki != null) {
				mPki.setTimeout(60);
				mPki.connect(onPkiConnect);
			}
		} catch (PKInotInstalledException e) {
			mPki = null;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mSingleton = this;
		final Context context = this.getApplicationContext();
		Resources res = this.getResources();
		
		SMS_PORT = (short) res.getInteger(R.integer.presets_data_sms_port);
		
		int icon = R.drawable.icon_notification;
		String tickerText = res.getString(R.string.notification_ticker);
		long when = System.currentTimeMillis();
		mNotification = new Notification(icon, tickerText, when);
		
		if (Encryption.getEncryption() == null)
			Encryption.setEncryption(new EncryptionPki());
	
		onPkiConnect = new ConnectionListener() {
				@Override
				public void onConnect() {
					Log.d(APP_TAG, "onConnect");
					for (ProgressDialog pd : mPkiWaitingDialogs)
						pd.cancel();
					mPkiWaitingDialogs.clear();
					
					// Check whether there is already a Master Key
					// and generate a new one if not
					try {
						EncryptionInterface crypto = Encryption.getEncryption();
						crypto.generateMasterKey();
						Log.d(APP_TAG, "Master Key: " + LowLevel.toHex(crypto.getMasterKey()));
						if (!crypto.testEncryption())
							throw new EncryptionException();
					} catch (EncryptionException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}

					//TODO: Just For Testing!!!
					File file = new File("/data/data/uk.ac.cam.db538.securesms/files/storage.db");
					if (file.exists())
						file.delete();
					file = new File("/data/data/uk.ac.cam.db538.securesms/databases/pending.db");
					if (file.exists())
						file.delete();

					try {
						String filename = context.getFilesDir().getAbsolutePath() + "/" + STORAGE_FILE_NAME;
						Storage.initSingleton(filename);
						Preferences.initSingleton(context);
					} catch (Exception e) {
						// TODO: show error dialog
					}
					
					try {
						Conversation conv1 = Conversation.createConversation();
						conv1.setPhoneNumber("+447572306095");
						SessionKeys keys1 = SessionKeys.createSessionKeys(conv1);
						keys1.setSimNumber(new SimNumber("89441000301641313004", true));
						keys1.setKeysSent(true);
						keys1.setKeysConfirmed(false);
						keys1.saveToFile();
						SessionKeys keys4 = SessionKeys.createSessionKeys(conv1);
						keys4.setSimNumber(new SimNumber("07879116797", false));
						keys4.setKeysSent(true);
						keys4.setKeysConfirmed(true);
						keys4.saveToFile();
						SessionKeys keys5 = SessionKeys.createSessionKeys(conv1);
						keys5.setSimNumber(new SimNumber("07572306095", false));
						keys5.setKeysSent(true);
						keys5.setKeysConfirmed(true);
						keys5.setSessionKey_In(keys5.getSessionKey_Out().clone());
						keys5.saveToFile();
						Conversation conv2 = Conversation.createConversation();
						MessageData msg2 = MessageData.createMessageData(conv2);
						TextMessage txtmsg2 = new TextMessage(msg2);
						txtmsg2.setText(CompressedText.createFromString("You're a jerk!"));
						msg2.setUnread(false);
						msg2.saveToFile();
						conv2.setPhoneNumber("+20104544366");
						SessionKeys keys2 = SessionKeys.createSessionKeys(conv2);
						keys2.setSimNumber(new SimNumber("89441000301641313002", true));
						keys2.setKeysSent(false);
						keys2.setKeysConfirmed(true);
						keys2.saveToFile();
						SessionKeys keys3 = SessionKeys.createSessionKeys(conv2);
						keys3.setSimNumber(new SimNumber("07879116797", false));
						keys3.setKeysSent(false);
						keys3.setKeysConfirmed(false);
						keys3.saveToFile();
						Conversation conv3 = Conversation.createConversation();
						conv3.setPhoneNumber("+447879116797");
						SessionKeys keys6 = SessionKeys.createSessionKeys(conv3);
						keys6.setSimNumber(new SimNumber("+447572306095", false));
						keys6.setKeysSent(true);
						keys6.setKeysConfirmed(true);
						keys6.saveToFile();
					} catch (Exception ex) {
					}
				}		

				@Override
				public void onConnectionDeclined() {
					// TODO Auto-generated method stub
					Log.d(APP_TAG, "onConnectionDeclined");
					
				}
				@Override
				public void onConnectionFailed() {
					// TODO Auto-generated method stub
					Log.d(APP_TAG, "onConnectionFailed");
				}
				@Override
				public void onConnectionTimeout() {
					// TODO Auto-generated method stub
					Log.d(APP_TAG, "onConnectionTimeout");
				}
				@Override
				public void onDisconnect() {
					mPki = null;
					Log.d(APP_TAG, "onDisconnect");
				}
		};
		initPki();
	}
	
	public Notification getNotification() {
		return mNotification;
	}
	
	public PKIwrapper getPki() {
		return mPki;
	}
	
	public boolean checkPki() {
		initPki();
		return mPki != null;  
	}
	
	public interface OnPkiAvailableListener {
		public void OnPkiAvailable();
	}
	
	public void waitForPki(Context context, final OnPkiAvailableListener listener) {
	    if (mPki != null) {
	    	if (mPki.isConnected()) {
	    		listener.OnPkiAvailable();
	    	} else {
	    		Resources res = context.getResources();
	    		ProgressDialog pd = new ProgressDialog(context);
	    		pd.setCancelable(false);
	    		pd.setTitle(res.getString(R.string.pki_waiting_title));
	    		pd.setMessage(res.getString(R.string.pki_waiting_details));
	    		pd.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						listener.OnPkiAvailable();
					}
				});
	    		mPkiWaitingDialogs.add(pd);
	    		pd.show();
	    	}
	    } else {
			Intent intent = new Intent(context, PkiInstallActivity.class);
			context.startActivity(intent);
	    }
	}
}