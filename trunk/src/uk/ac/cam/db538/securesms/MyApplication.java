package uk.ac.cam.db538.securesms;

import java.io.File;

import uk.ac.cam.db538.securesms.data.DbPendingAdapter;
import uk.ac.cam.db538.securesms.receivers.DataSmsReceiver;
import uk.ac.cam.db538.securesms.storage.Conversation;
import uk.ac.cam.db538.securesms.storage.MessageData;
import uk.ac.cam.db538.securesms.storage.SessionKeys;
import uk.ac.cam.db538.securesms.storage.Storage;
import uk.ac.cam.db538.securesms.storage.SessionKeys.SimNumber;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.ConnectionListener;
import uk.ac.cam.dje38.PKIwrapper.PKIwrapper.PKInotInstalledException;
import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;

public class MyApplication extends Application {
	private static short SMS_PORT; 
	public static final int NOTIFICATION_ID = 1;
	
	private static MyApplication mSingleton;
	
	public static MyApplication getSingleton() {
		return mSingleton;
	}
	
	public static short getSmsPort() {
		return SMS_PORT;
	}

	private Notification mNotification;
	private PKIwrapper mPKI;

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
		
		//TODO: Test whether PKI uses AES-256 by checking these:
		// http://www.inconteam.com/software-development/41-encryption/55-aes-test-vectors#aes-cbc-256
		
		mPKI = new PKIwrapper(context);
		try {
			mPKI.connect(new ConnectionListener() {
				@Override
				public void onConnect() {
					//TODO: Just For Testing!!!
					File file = new File("/data/data/uk.ac.cam.db538.securesms/files/storage.db");
					if (file.exists())
						file.delete();
					file = new File("/data/data/uk.ac.cam.db538.securesms/databases/pending.db");
					if (file.exists())
						file.delete();

					try {
						Storage.initSingleton(context);
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
						keys5.saveToFile();
						Conversation conv2 = Conversation.createConversation();
						MessageData msg2 = MessageData.createMessageData(conv2);
						//msg2.setMessageBody("You're a jerk!");
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
					
				}

				@Override
				public void onConnectionFailed() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onConnectionTimeout() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onDisconnect() {
					// TODO Auto-generated method stub
					
				}
				
			});
		} catch (PKInotInstalledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Notification getNotification() {
		return mNotification;
	}
}
