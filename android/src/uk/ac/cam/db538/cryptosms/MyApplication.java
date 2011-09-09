package uk.ac.cam.db538.cryptosms;

import java.lang.Thread.UncaughtExceptionHandler;

import roboguice.application.RoboApplication;

import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionPki;
import uk.ac.cam.db538.cryptosms.data.DbPendingAdapter;
import uk.ac.cam.db538.cryptosms.data.PendingParser;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class MyApplication extends RoboApplication {
	private static short SMS_PORT; 
	public static final int NOTIFICATION_ID = 1;
	public static final String APP_TAG = "CRYPTOSMS";
	public static final String STORAGE_FILE_NAME = "storage.db";
	
	public static final String PKI_PACKAGE = "uk.ac.cam.dje38.pki";
	public static final String PKI_CONTACT_PICKER = "uk.ac.cam.dje38.pki.picker";
	public static final String PKI_KEY_PICKER = "uk.ac.cam.dje38.pki.keypicker";
	public static final String PKI_LOGIN = "uk.ac.cam.dje38.pki.login";
	
	public static final String NEWLINE = System.getProperty("line.separator");
	public static final String[] REPORT_EMAILS = new String[] { "db538@cam.ac.uk" }; // TODO: create new email!
	
	private static MyApplication mSingleton;
	
	public static MyApplication getSingleton() {
		return mSingleton;
	}
	
	public static short getSmsPort() {
		return SMS_PORT;
	}

	private Notification mNotification = null;
	private Drawable mDefaultContactImage = null;
	
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
		
		mDefaultContactImage = getResources().getDrawable(R.drawable.ic_contact_picture);
		
		Preferences.initSingleton(context);
		if (Encryption.getEncryption() == null)
			Encryption.setEncryption(new EncryptionPki());
		
		final UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			private void logException(Throwable ex) {
				Log.e(APP_TAG, "Exception: " + ex.getClass().getName());
				Log.e(APP_TAG, "Message: " + ex.getMessage());
				Log.e(APP_TAG, "Stack: ");
				for (StackTraceElement element : ex.getStackTrace())
					Log.e(APP_TAG, element.toString());
				if (ex.getCause() != null) {
					Log.e(APP_TAG, "Cause: ");
					logException(ex.getCause());
				}
			}
			
			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				logException(ex);
				if ((ex instanceof WrongKeyDecryptionException) ||
					(ex instanceof RuntimeException && ex.getCause() instanceof WrongKeyDecryptionException)) {
					// TODO: Handle better
					State.fatalException((WrongKeyDecryptionException) ex);
				}
				else
					defaultHandler.uncaughtException(thread, ex);
			}
		});

		String storageFile = context.getFilesDir().getAbsolutePath() + "/" + MyApplication.STORAGE_FILE_NAME;

		Storage.setFilename(storageFile);
		
		Pki.init(this.getApplicationContext());
		SimCard.init(this.getApplicationContext());
		PendingParser.init(this.getApplicationContext());
	}
	
	public Notification getNotification() {
		return mNotification;
	}
	
	public Drawable getDefaultContactImage() {
		return mDefaultContactImage;
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		Pki.disconnect();
	}
}
