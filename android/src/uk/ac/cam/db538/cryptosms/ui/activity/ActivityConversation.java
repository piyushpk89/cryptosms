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
package uk.ac.cam.db538.cryptosms.ui.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import roboguice.inject.InjectView;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.SimCard;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.TextMessage;
import uk.ac.cam.db538.cryptosms.data.Message.MessageException;
import uk.ac.cam.db538.cryptosms.data.Message.MessageSendingListener;
import uk.ac.cam.db538.cryptosms.state.Pki;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys.SessionKeysStatus;
import uk.ac.cam.db538.cryptosms.storage.Storage;
import uk.ac.cam.db538.cryptosms.storage.Storage.StorageChangeListener;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.ui.DummyOnClickListener;
import uk.ac.cam.db538.cryptosms.ui.UtilsContactBadge;
import uk.ac.cam.db538.cryptosms.ui.UtilsSimIssues;
import uk.ac.cam.db538.cryptosms.ui.DialogManager.DialogBuilder;
import uk.ac.cam.db538.cryptosms.ui.adapter.AdapterMessages;
import uk.ac.cam.db538.cryptosms.ui.list.ListViewMessage;
import uk.ac.cam.db538.cryptosms.utils.CompressedText;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/*
 * Activity showing conversation thread and allowing messages to be sent
 */
public class ActivityConversation extends ActivityAppState {
	public static final String OPTION_PHONE_NUMBER = "PHONE_NUMBER";
	public static final String OPTION_OFFER_KEYS_SETUP = "KEYS_SETUP";
	
	private static final String DIALOG_NO_SESSION_KEYS = "DIALOG_NO_SESSION_KEYS";
	private static final String DIALOG_SESSION_KEY_EXPIRED = "DIALOG_SESSION_KEY_EXPIRED";
	
	private Contact mContact;
	private Conversation mConversation;
	
	@InjectView(R.id.send)
    private Button mSendButton;
	@InjectView(R.id.text_editor)
    private EditText mTextEditor;
	@InjectView(R.id.bytes_counter)
    private TextView mBytesCounterView;
	@InjectView(R.id.history)
	private ListViewMessage mListMessageHistory;
	
	private AdapterMessages mAdapterMessageHistory;
    
	private Context mContext = this;
    private boolean mErrorNoKeysShow;

	/* (non-Javadoc)
	 * @see roboguice.activity.RoboActivity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.screen_conversation);
	    
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

	    final Resources res = getResources();
	    
	    Bundle bundle = getIntent().getExtras();
	    String phoneNumber = bundle.getString(OPTION_PHONE_NUMBER);
	    mErrorNoKeysShow = bundle.getBoolean(OPTION_OFFER_KEYS_SETUP, true);
	    
	    // check that we got arguments
	    if (phoneNumber == null)
	    	this.finish();
	    
	    mContact = Contact.getContact(this, phoneNumber);
		try {
			mConversation = Conversation.getConversation(mContact.getPhoneNumber());
		} catch (StorageFileException ex) {
			State.fatalException(ex);
			return;
		}

	    UtilsContactBadge.setBadge(mContact, getMainView());
        
        mTextEditor.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				String text = s.toString();
				CompressedText msg = CompressedText.fromString(text);
				mBytesCounterView.setText(TextMessage.getRemainingBytes(msg.getDataLength()) + " (" + TextMessage.getMessagePartCount(msg.getDataLength()) + ")");
				mSendButton.setEnabled(true);
				mBytesCounterView.setVisibility(View.VISIBLE);
			}
		});

        mSendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mConversation != null) {
					try {
						// start the progress bar
						// TODO: get rid of this!!!
						final ProgressDialog pd = new ProgressDialog(ActivityConversation.this);
						pd.setMessage(res.getString(R.string.sending));
						pd.show();
						
						// create message
						final TextMessage msg = new TextMessage(MessageData.createMessageData(mConversation));
						msg.setText(CompressedText.fromString(mTextEditor.getText().toString()));
						// send it
						msg.sendSMS(mContact.getPhoneNumber(), ActivityConversation.this, new MessageSendingListener() {
							@Override
							public void onMessageSent() {
								pd.cancel();
								mTextEditor.setText("");
								mBytesCounterView.setVisibility(View.GONE);
								updateMessageHistory();
								onSimState();
							}
							
							@Override
							public void onError(Exception ex) {
								pd.cancel();
								// TODO: get rid of this!!!
								new AlertDialog.Builder(ActivityConversation.this)
								.setTitle(res.getString(R.string.error_sms_service))
								.setMessage(res.getString(R.string.error_sms_service_details) + "\nError: " + ex.getMessage())
								.setNeutralButton(res.getString(R.string.ok), new DummyOnClickListener())
								.show();
								updateMessageHistory();
								onSimState();
							}
							
							@Override
							public void onPartSent(int index) {
							}
						});
					} catch (StorageFileException ex) {
						State.fatalException(ex);
						return;
					} catch (MessageException ex) {
						State.fatalException(ex);
						return;
					} catch (EncryptionException ex) {
						State.fatalException(ex);
						return;
					}
				}
			}
        });
        
        // set appearance of list view
	    mListMessageHistory.setFastScrollEnabled(true);
    	// create the adapter
	    mAdapterMessageHistory = new AdapterMessages(getLayoutInflater(), mListMessageHistory);
		// prepare for context menus
		registerForContextMenu(mListMessageHistory);
	
        // prepare dialogs
        getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				return new AlertDialog.Builder(mContext)
				       .setTitle(res.getString(R.string.conversation_no_keys))
				       .setMessage(res.getString(R.string.conversation_no_keys_details))
				       .setPositiveButton(res.getString(R.string.read_only), new DummyOnClickListener())
				       .setNegativeButton(res.getString(R.string.setup), new OnClickListener() {
				    	   @Override
				    	   public void onClick(DialogInterface dialog, int which) {
				    			Intent intent = new Intent(ActivityConversation.this, ActivityExchangeMethod.class);
				    			intent.putExtra(ActivityExchangeMethod.OPTION_PHONE_NUMBER, mConversation.getPhoneNumber());
				    			startActivity(intent);
				    	   }
				       })
				       .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_NO_SESSION_KEYS;
			}
		});
        getDialogManager().addBuilder(new DialogBuilder() {
			@Override
			public Dialog onBuild(Bundle params) {
				return new AlertDialog.Builder(mContext)
				       .setTitle(res.getString(R.string.conversation_key_expired))
				       .setMessage(res.getString(R.string.conversation_key_expired_details))
				       .setPositiveButton(res.getString(R.string.read_only), new DummyOnClickListener())
				       .setNegativeButton(res.getString(R.string.setup), new OnClickListener() {
				    	   @Override
				    	   public void onClick(DialogInterface dialog, int which) {
				    			Intent intent = new Intent(ActivityConversation.this, ActivityExchangeMethod.class);
				    			intent.putExtra(ActivityExchangeMethod.OPTION_PHONE_NUMBER, mConversation.getPhoneNumber());
				    			startActivity(intent);
				    	   }
				       })
				       .create();
			}
			
			@Override
			public String getId() {
				return DIALOG_SESSION_KEY_EXPIRED;
			}
		});
	}
	
	private void modeEnabled(boolean value) {
		Resources res = getResources();
		mSendButton.setEnabled(value);
		mTextEditor.setEnabled(value);
		mTextEditor.setHint((value) ? res.getString(R.string.conversation_type_to_compose) : null);
		mTextEditor.setFocusable(value);
		mTextEditor.setFocusableInTouchMode(value);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		modeEnabled(false);
		Pki.login(false);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.ui.activity.ActivityAppState#onPkiLogin()
	 */
	@Override
	public void onPkiLogin() {
		super.onPkiLogin();
		updateMessageHistory();
		mListMessageHistory.setAdapter(mAdapterMessageHistory);
		Storage.addListener(mStorageChangeListener);
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.ui.activity.ActivityAppState#onPkiLogout()
	 */
	@Override
	public void onPkiLogout() {
		Storage.removeListener(mStorageChangeListener);
		mListMessageHistory.setAdapter(null);
		modeEnabled(false);
		super.onPkiLogout();
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.ui.activity.ActivityAppState#onSimState()
	 */
	@Override
	public void onSimState() {
		super.onSimState();
		
		modeEnabled(false);
		UtilsSimIssues.handleSimIssues(mContext, getDialogManager());
		
		// check for SIM availability
	    try {
			if (SimCard.getSingleton().isNumberAvailable()) {
				// check keys availability
				SessionKeys keys = mConversation.getSessionKeys(SimCard.getSingleton().getNumber());
		    	if (keys != null && keys.getStatus() == SessionKeysStatus.KEYS_EXCHANGED) 
		    		modeEnabled(true);
		    	else if (keys != null && keys.getStatus() == SessionKeysStatus.KEYS_EXPIRED) {
		    		if (mErrorNoKeysShow) {
						// outgoing session key has expired 
						mErrorNoKeysShow = false;
						getDialogManager().showDialog(DIALOG_SESSION_KEY_EXPIRED, null);
		    		}
				} else {
		    		if (mErrorNoKeysShow) {
						// secure connection has not been successfully established yet
						mErrorNoKeysShow = false;
						getDialogManager().showDialog(DIALOG_NO_SESSION_KEYS, null);
		    		}
				}
			}
		} catch (StorageFileException ex) {
			State.fatalException(ex);
			return;
		}
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.cam.db538.cryptosms.ui.activity.ActivityAppState#onEventParsingFinished()
	 */
	@Override
	public void onEventParsingFinished() {
		super.onEventParsingFinished();
		onSimState();
	}

	private void updateMessageHistory() {
		synchronized(mAdapterMessageHistory) {
			new MessageHistoryUpdateTask().execute();
		}
	}

	private class MessageHistoryUpdateTask extends AsyncTask<Void, Void, Void> {

		private ArrayList<TextMessage> mTextMessages = null;
		private Exception mException = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				ArrayList<MessageData> allMessageData = mConversation.getMessages();
				Collections.sort(allMessageData, new Comparator<MessageData>() {
					@Override
					public int compare(MessageData arg0, MessageData arg1) {
						return arg0.getTimeStamp().compareTo(arg1.getTimeStamp());
					}
				});
				mTextMessages = new ArrayList<TextMessage>(allMessageData.size());
				for (MessageData storage : allMessageData)
					mTextMessages.add(new TextMessage(storage));
			} catch (StorageFileException ex) {
				mException = ex;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if (mException != null) {
				State.fatalException(mException);
				return;
			}

			mAdapterMessageHistory.setList(mTextMessages);
			mAdapterMessageHistory.notifyDataSetChanged();
		}
	}
	
	private StorageChangeListener mStorageChangeListener = new StorageChangeListener() {
		
		@Override
		public void onUpdate() {
			updateMessageHistory();
		}
	};
}