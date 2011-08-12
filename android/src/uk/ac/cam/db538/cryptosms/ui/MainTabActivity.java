package uk.ac.cam.db538.cryptosms.ui;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Utils;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.state.State.StateChangeListener;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.TabHost;

public class MainTabActivity extends TabActivity {
	private static final int MENU_MOVE_SESSIONS = Menu.FIRST;

	private Context mContext = this;
	private ErrorOverlay mErrorOverlay;
	
	private View mMainLayout;
	private StateChangeListener mPkiStateListener = new StateChangeListener(){
		@Override
		public void onConnect() {
		}

		@Override
		public void onDisconnect() {
			Log.d(MyApplication.APP_TAG, "Disconnect error overlay");
			mErrorOverlay.modeDisconnected();
			mErrorOverlay.setVisibility(View.VISIBLE);
	        mMainLayout.setVisibility(View.INVISIBLE);
		}

		@Override
		public void onFatalException(Exception ex) {
			Log.d(MyApplication.APP_TAG, "Fatal exception error overlay");
			mErrorOverlay.modeFatalException(ex);
			mErrorOverlay.setVisibility(View.VISIBLE);
	        mMainLayout.setVisibility(View.INVISIBLE);
		}

		@Override
		public void onLogin() {
        	// check for SIM trouble
	        try {
	        	Utils.checkSimPhoneNumberAvailable(mContext);
			} catch (StorageFileException ex) {
				State.fatalException(ex);
				return;
			}

			mErrorOverlay.setVisibility(View.INVISIBLE);
	        mMainLayout.setVisibility(View.VISIBLE);
		}

		@Override
		public void onLogout() {
			Log.d(MyApplication.APP_TAG, "Logout error overlay");
			mErrorOverlay.modeLogin();
			mErrorOverlay.setVisibility(View.VISIBLE);
	        mMainLayout.setVisibility(View.INVISIBLE);
		}

		@Override
		public void onPkiMissing() {
			Log.d(MyApplication.APP_TAG, "PkiMissing error overlay");
			mErrorOverlay.modePkiMissing();
			mErrorOverlay.setVisibility(View.VISIBLE);
	        mMainLayout.setVisibility(View.INVISIBLE);
		}
	};
	
	private TabHost.TabSpec specContacts;
	
	private TabHost.TabSpec specRecent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_main);
	    
	    Resources res = getResources(); 	// Resource object to get Drawables
	    TabHost tabHost = getTabHost();  	// The activity TabHost
	    Intent intent;  					// Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, TabRecent.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    specRecent = tabHost.newTabSpec("recent").setIndicator(res.getString(R.string.tab_recent),
	                      res.getDrawable(R.drawable.tab_recent))
	                  .setContent(intent);
	    tabHost.addTab(specRecent);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, TabContacts.class);
	    specContacts = tabHost.newTabSpec("contacts").setIndicator(res.getString(R.string.tab_contacts),
	                      res.getDrawable(R.drawable.tab_contacts))
	                  .setContent(intent);
	    tabHost.addTab(specContacts);

	    // error overlay
	    mMainLayout = findViewById(R.id.screen_main);
	    mErrorOverlay = (ErrorOverlay) findViewById(R.id.screen_main_error);
        mPkiStateListener.onLogout();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		final Context context = this;
		Resources res = this.getResources();
		
		int idGroup = 0;
		MenuItem menuMoveSessions = menu.add(idGroup, MENU_MOVE_SESSIONS, Menu.NONE, res.getString(R.string.menu_move_sessions));
		menuMoveSessions.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Utils.moveSessionKeys(context);                                 
				return true;
			}
		});
		return true;
	}

	@Override
	protected void onStart() {
        // listen for logins/logouts
        State.addListener(mPkiStateListener);
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(MyApplication.APP_TAG, "Removing listener");
		State.removeListener(mPkiStateListener);
		super.onStop();
	}
}
