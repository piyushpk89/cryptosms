/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.db538.cryptosms.ui.list;

import java.util.zip.DataFormatException;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.R;
import uk.ac.cam.db538.cryptosms.data.Contact;
import uk.ac.cam.db538.cryptosms.data.TextMessage;
import uk.ac.cam.db538.cryptosms.state.State;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.utils.CompressedText;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import android.util.AttributeSet;
import android.view.View;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * This class manages the view for given conversation.
 */
public class ListItemConversation extends RelativeLayout {
    private TextView mSubjectView;
    private TextView mFromView;
    private TextView mDateView;
    private View mNoEncryptionView;
    private View mErrorIndicator;
    private QuickContactBadge mAvatarView;

    private Conversation mConversationHeader;

    /**
     * Instantiates a new list item conversation.
     *
     * @param context the context
     */
    public ListItemConversation(Context context) {
        super(context);
    }

    /**
     * Instantiates a new list item conversation.
     *
     * @param context the context
     * @param attrs the attrs
     */
    public ListItemConversation(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFromView = (TextView) findViewById(R.id.from);
        mSubjectView = (TextView) findViewById(R.id.subject);
        mDateView = (TextView) findViewById(R.id.date);
        mNoEncryptionView = findViewById(R.id.no_encryption);
        mErrorIndicator = findViewById(R.id.error);
        mAvatarView = (QuickContactBadge) findViewById(R.id.avatar);
    }

    private void setConversationHeader(Conversation conv) {
    	mConversationHeader = conv;
    }

	public Conversation getConversationHeader() {
    	return mConversationHeader;
    }

	private String getPreview(Conversation conv) {
		MessageData firstMessageData = null;
		try {
			firstMessageData = conv.getFirstMessageData();
		} catch (StorageFileException ex) {
			State.fatalException(ex);
			return new String();
		}
		
		if (firstMessageData != null) {
			TextMessage message = null;
			CompressedText text = null;
			try {
				message = new TextMessage(firstMessageData);
				text = message.getText();
			} catch (StorageFileException ex) {
				State.fatalException(ex);
				return new String();
			} catch (DataFormatException ex) {
				State.fatalException(ex);
				return new String();
			}
			if (text != null)
				return text.getMessage();
		}
		
		return new String();
	}


    /**
     * Bind a conversation to this item 
     *
     * @param conv the conversation
     */
    public final void bind(final Conversation conv) {
    	Context context = this.getContext();
    	Resources res = context.getResources();
        setConversationHeader(conv);
        
        boolean hasError = false; 
        boolean hasNoEncryption = false;
        
        setBackgroundDrawable(
        	(conv.getMarkedUnread()) ?
        		res.getDrawable(R.drawable.conversation_item_background_unread) :
        		res.getDrawable(R.drawable.conversation_item_background_read)
        );

        LayoutParams attachmentLayout = (LayoutParams)mNoEncryptionView.getLayoutParams();
        if (hasError) {
            attachmentLayout.addRule(RelativeLayout.LEFT_OF, R.id.error);
        } else {
            attachmentLayout.addRule(RelativeLayout.LEFT_OF, R.id.date);
        }

        LayoutParams subjectLayout = (LayoutParams)mSubjectView.getLayoutParams();
        subjectLayout.addRule(RelativeLayout.LEFT_OF, hasNoEncryption ? R.id.type :
            (hasError ? R.id.error : R.id.date));

        mErrorIndicator.setVisibility(hasError ? VISIBLE : GONE);

        mNoEncryptionView.setVisibility(hasNoEncryption ? VISIBLE : GONE);
        mDateView.setText(conv.getFormattedTime());

    	Contact contact = Contact.getContact(context, conv.getPhoneNumber());
        mFromView.setText(contact.getName());
        mSubjectView.setText(getPreview(conv));

    	Drawable avatarDrawable = contact.getAvatar(context, MyApplication.getSingleton().getDefaultContactImage());
        if (contact.existsInDatabase()) {
            mAvatarView.assignContactUri(contact.getUri());
        } else {
            mAvatarView.assignContactFromPhone(conv.getPhoneNumber(), true);
        }
        mAvatarView.setImageDrawable(avatarDrawable);
        mAvatarView.setVisibility(View.VISIBLE);
    }
}
