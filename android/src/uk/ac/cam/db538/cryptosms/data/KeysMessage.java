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
package uk.ac.cam.db538.cryptosms.data;

import java.util.ArrayList;

import org.spongycastle.crypto.digests.SHA256Digest;

import uk.ac.cam.db538.cryptosms.MyApplication;
import uk.ac.cam.db538.cryptosms.SimCard;
import uk.ac.cam.db538.cryptosms.crypto.EllipticCurveDeffieHellman;
import uk.ac.cam.db538.cryptosms.crypto.Encryption;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.EncryptionException;
import uk.ac.cam.db538.cryptosms.crypto.EncryptionInterface.WrongKeyDecryptionException;
import uk.ac.cam.db538.cryptosms.data.PendingParser.ParseResult;
import uk.ac.cam.db538.cryptosms.data.PendingParser.PendingParseResult;
import uk.ac.cam.db538.cryptosms.storage.Conversation;
import uk.ac.cam.db538.cryptosms.storage.MessageData;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys;
import uk.ac.cam.db538.cryptosms.storage.SessionKeys.SessionKeysStatus;
import uk.ac.cam.db538.cryptosms.storage.StorageFileException;
import uk.ac.cam.db538.cryptosms.utils.LowLevel;
import uk.ac.cam.db538.cryptosms.utils.SimNumber;

/*
 * Key negotiation message
 */
public class KeysMessage extends Message {
	
	// Set handshake validity period to 5 days (4 - 6 days with different time zones)
	public static final long HANDSHAKE_VALIDITY_PERIOD = 5L * 24L * 60L * 60L * 1000L;
	// Clock tolerance is 5 mins
	public static final long CLOCK_TOLERANCE = 5L * 60L * 1000L;
	
	protected static final int OFFSET_DATA = OFFSET_HEADER + LENGTH_HEADER;
	protected static final int LENGTH_DATA = MessageData.LENGTH_MESSAGE - OFFSET_DATA;
	
	public static final int LENGTH_TIMESTAMP = 8;
	public static final int OFFSET_PUBLIC_KEY = OFFSET_DATA;
	public static final int OFFSET_TIMESTAMP = OFFSET_PUBLIC_KEY + EllipticCurveDeffieHellman.LENGTH_PUBLIC_KEY;
	public static final int OFFSET_SIGNATURE = OFFSET_TIMESTAMP + LENGTH_TIMESTAMP;
	public static final int LENGTH_CONTENT = OFFSET_SIGNATURE + Encryption.ASYM_SIGNATURE_LENGTH;
	
	private byte[] mPublicKey;
	private byte[] mPrivateKey;
	private long mTimeStamp;

	private byte[] mOtherPublicKey;
	private long mOtherTimeStamp;
	
	private boolean mIsConfirmation;
		
	EllipticCurveDeffieHellman mECDH;
	
	/**
	 * Instantiates new key negotiation message (first request)
	 *
	 * @throws StorageFileException the storage file exception
	 */
	public KeysMessage() throws StorageFileException {
		mIsConfirmation = false;
		
		mECDH = new EllipticCurveDeffieHellman();
		mPublicKey = mECDH.getPublicKey();
		mPrivateKey = mECDH.getPrivateKey();
		mTimeStamp = System.currentTimeMillis();
	}
	
	/**
	 * Instantiates new key negotiation message (from reply)
	 *
	 * @param originalTimeStamp time stamp of the first message
	 * @param privateKey private key of this user
	 * @param otherTimeStamp time stamp from the other user's message
	 * @param otherPublicKey public key from the other user's message
	 */
	public KeysMessage(long originalTimeStamp, byte[] privateKey, long otherTimeStamp, byte[] otherPublicKey) {
		mIsConfirmation = false;
		
		mECDH = new EllipticCurveDeffieHellman(privateKey);
		mPublicKey = mECDH.getPublicKey();
		mPrivateKey = mECDH.getPrivateKey();
		mTimeStamp = originalTimeStamp;
		
		mOtherTimeStamp = otherTimeStamp;
		mOtherPublicKey = otherPublicKey;
	}
	
	/**
	 * Instantiates new key negotiation message (to reply)
	 *
	 * @param otherTimeStamp time stamp from the other user's message
	 * @param otherPublicKey public key from the other user's message
	 * @throws StorageFileException the storage file exception
	 */
	public KeysMessage(long otherTimeStamp, byte[] otherPublicKey) throws StorageFileException {
		mIsConfirmation = true;
		
		mECDH = new EllipticCurveDeffieHellman();
		mPublicKey = mECDH.getPublicKey();
		mPrivateKey = mECDH.getPrivateKey();
		mTimeStamp = System.currentTimeMillis();

		mOtherTimeStamp = otherTimeStamp;
		mOtherPublicKey = otherPublicKey;
	}
	
	public byte[] getPublicKey() {
		return mPublicKey;
	}
	
	public byte[] getPrivateKey() {
		return mPrivateKey;
	}
	
	public long getTimeStamp() {
		return mTimeStamp;
	}
	
	private byte[] getKey(String prefix) {
		return Encryption.getEncryption().getHash(
				(prefix + mECDH.getSharedKey(mOtherPublicKey).toString()).getBytes() 
			);
	}
	
	public byte[] getKeyOut() {
		return getKey(mIsConfirmation ? "0" : "1");
	}
	
	public byte[] getKeyIn() {
		return getKey(mIsConfirmation ? "1" : "0");
	}
	
	public boolean isConfirmation() {
		return mIsConfirmation;
	}

	/**
	 * Returns data ready to be sent via SMS
	 * @return
	 * @throws StorageFileException 
	 * @throws MessageException 
	 * @throws EncryptionException 
	 */
	@Override
	public ArrayList<byte[]> getBytes() throws StorageFileException, MessageException, EncryptionException {
		SHA256Digest hashing = new SHA256Digest(); 
		if (mIsConfirmation) {
			hashing.update(getOtherHeader());
			hashing.update(LowLevel.getBytesLong(mOtherTimeStamp), 0, 8);
			hashing.update(mOtherPublicKey, 0, mOtherPublicKey.length);
		}
		byte[] timeStampBytes = LowLevel.getBytesLong(mTimeStamp);

		hashing.update(getHeader());
		hashing.update(timeStampBytes, 0, 8);
		hashing.update(mPublicKey, 0, mPublicKey.length);

		byte[] hash = new byte[Encryption.HASH_LENGTH];
		hashing.doFinal(hash, 0);
		byte[] signature = Encryption.getEncryption().sign(hash);
		
		byte[] data = new byte[OFFSET_DATA + LENGTH_CONTENT];
		data[OFFSET_HEADER] = getHeader();
		System.arraycopy(mPublicKey, 0, data, OFFSET_PUBLIC_KEY, EllipticCurveDeffieHellman.LENGTH_PUBLIC_KEY);
		System.arraycopy(timeStampBytes, 0, data, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP);
		System.arraycopy(signature, 0, data, OFFSET_SIGNATURE, Encryption.ASYM_SIGNATURE_LENGTH);
		
		ArrayList<byte[]> dataSms = new ArrayList<byte[]>(1);
		dataSms.add(data);
		return dataSms;
	}
	
	/**
	 * Parses the message
	 *
	 * @param idGroup the id group
	 * @return the parses the result
	 */
	public static ParseResult parseKeysMessage(ArrayList<Pending> idGroup) {
		try {
			// check the sender
			Contact contact = Contact.getContact(MyApplication.getSingleton().getApplicationContext(), idGroup.get(0).getSender());
			if (!contact.existsInDatabase())
				return new ParseResult(idGroup, PendingParseResult.UNKNOWN_SENDER, null);

			if (idGroup.size() != 1)
				return new ParseResult(idGroup, PendingParseResult.REDUNDANT_PARTS, null);
			
			byte[] dataAll = idGroup.get(0).getData();
			String sender = idGroup.get(0).getSender();
			byte header = getMessageHeader(dataAll);
			MessageType type = getMessageType(dataAll);
			Conversation conv = Conversation.getConversation(sender);
			SessionKeys keys = null;
			if (conv != null)
				keys = conv.getSessionKeys(SimCard.getSingleton().getNumber());
	
			byte[] publicKey = LowLevel.cutData(dataAll, OFFSET_PUBLIC_KEY, EllipticCurveDeffieHellman.LENGTH_PUBLIC_KEY);
			byte[] timeStampBytes = LowLevel.cutData(dataAll, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP);
			byte[] signature = LowLevel.cutData(dataAll, OFFSET_SIGNATURE, Encryption.ASYM_SIGNATURE_LENGTH);
			
			long timeStamp = LowLevel.getLong(timeStampBytes);
			
			// check the time stamp isn't too old or in the future
			long now = System.currentTimeMillis();
			if (timeStamp > now + CLOCK_TOLERANCE)
				return new ParseResult(idGroup, PendingParseResult.TIMESTAMP_IN_FUTURE, null);
			else if (now - timeStamp > HANDSHAKE_VALIDITY_PERIOD) 
				return new ParseResult(idGroup, PendingParseResult.TIMESTAMP_OLD, null);
			// chechk that it isn't a replay
			if (keys != null && 
				keys.getStatus() == SessionKeysStatus.KEYS_EXCHANGED && 
				timeStamp <= keys.getTimeStamp())
					return new ParseResult(idGroup, PendingParseResult.TIMESTAMP_OLD, null);

			if (type == MessageType.HANDSHAKE) {
				SHA256Digest hashing = new SHA256Digest(); 
				hashing.update(header);
				hashing.update(timeStampBytes, 0, 8);
				hashing.update(publicKey, 0, publicKey.length);
				
				byte[] hash = new byte[Encryption.HASH_LENGTH];
				hashing.doFinal(hash, 0);
				
				// check the signature
				boolean signatureVerified = false;
				try {
					signatureVerified = Encryption.getEncryption().verify(hash, signature, contact.getId());
				} catch (EncryptionException e) {
				} catch (WrongKeyDecryptionException e) {
				}
				if (!signatureVerified)
					return new ParseResult(idGroup, PendingParseResult.COULD_NOT_VERIFY, null);
				
				// all seems to be fine, so just retrieve the keys and return the result
				return new ParseResult(idGroup, 
				                            PendingParseResult.OK_HANDSHAKE_MESSAGE, 
				                            new KeysMessage(
				                            	timeStamp,
				                            	publicKey
				                            ));
			} else if (type == MessageType.CONFIRM) {
				// find the session keys for this person
				if (keys == null || keys.getStatus() != SessionKeysStatus.WAITING_FOR_REPLY)
					// unexpected
					return new ParseResult(idGroup, PendingParseResult.COULD_NOT_VERIFY, null);
				
				byte[] prevPublicKey = new EllipticCurveDeffieHellman(keys.getPrivateKey()).getPublicKey();
				
				SHA256Digest hashing = new SHA256Digest(); 
				hashing.update(HEADER_HANDSHAKE);
				hashing.update(LowLevel.getBytesLong(keys.getTimeStamp()), 0, 8);
				hashing.update(prevPublicKey, 0, prevPublicKey.length);
				hashing.update(header);
				hashing.update(timeStampBytes, 0, 8);
				hashing.update(publicKey, 0, publicKey.length);

				byte[] hash = new byte[Encryption.HASH_LENGTH];
				hashing.doFinal(hash, 0);
				
				// check the signature
				boolean signatureVerified = false;
				try {
					signatureVerified = Encryption.getEncryption().verify(hash, signature, contact.getId());
				} catch (EncryptionException e) {
				} catch (WrongKeyDecryptionException e) {
				}
				if (!signatureVerified)
					return new ParseResult(idGroup, PendingParseResult.COULD_NOT_VERIFY, null);
				
				// all seems to be fine, so save the result
                KeysMessage keysMsg = new KeysMessage(
                    	keys.getTimeStamp(),
                    	keys.getPrivateKey(),
                    	timeStamp,
                    	publicKey
                    );
                
                keys.setSessionKey_Out(keysMsg.getKeyOut());
                keys.setSessionKey_In(keysMsg.getKeyIn());
                keys.setKeysConfirmed(true);
                keys.setPrivateKey(Encryption.getEncryption().generateRandomData(EllipticCurveDeffieHellman.LENGTH_PRIVATE_KEY));
                keys.setTimeStamp(timeStamp);
                keys.saveToFile();
				
				return new ParseResult(idGroup, 
				                       PendingParseResult.OK_CONFIRM_MESSAGE,
				                       keysMsg);
			} else
				return new ParseResult(idGroup, PendingParseResult.COULD_NOT_DECRYPT, null);
		} catch (StorageFileException e) {
			return new ParseResult(idGroup, PendingParseResult.INTERNAL_ERROR, null);
		}
	}
	
	private byte getHeader() {
		if (mIsConfirmation)
			return HEADER_CONFIRM;
		else
			return HEADER_HANDSHAKE;
	}

	private byte getOtherHeader() {
		if (mIsConfirmation)
			return HEADER_HANDSHAKE;
		else
			return HEADER_CONFIRM;
	}

	protected static long getMessageTimeStamp(byte[] data) {
		return LowLevel.getLong(LowLevel.cutData(data, OFFSET_TIMESTAMP, LENGTH_TIMESTAMP));
	}

	@Override
	protected void onMessageSent(String phoneNumber) throws StorageFileException {
		Conversation conv = Conversation.getConversation(phoneNumber);
		if (conv == null) {
			conv = Conversation.createConversation();
			conv.setPhoneNumber(phoneNumber);
			// will get saved while session keys 
			// are attached
		}
		SimNumber simNumber = SimCard.getSingleton().getNumber();
		conv.deleteSessionKeys(simNumber);

		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		keys.setSimNumber(simNumber);

		if (mIsConfirmation) {
	        keys.setKeysSent(true);
			keys.setKeysConfirmed(true);
			keys.setSessionKey_Out(this.getKeyOut());
			keys.setSessionKey_In(this.getKeyIn());
			keys.setTimeStamp(this.getTimeStamp());
		} else {
			keys.setKeysSent(true);
			keys.setKeysConfirmed(false);
			keys.setPrivateKey(getPrivateKey());
			keys.setTimeStamp(getTimeStamp());
			keys.saveToFile();
		}
		
		keys.saveToFile();
	}

	@Override
	protected void onPartSent(String phoneNumber, int index)
			throws StorageFileException {
	}
}
