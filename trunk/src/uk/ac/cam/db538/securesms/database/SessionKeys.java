package uk.ac.cam.db538.securesms.database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import uk.ac.cam.db538.securesms.encryption.Encryption;

/**
 * 
 * Class representing a session keys entry in the secure storage file.
 * 
 * @author David Brazdil
 *
 */
public class SessionKeys {
	// FILE FORMAT
	private static final int LENGTH_FLAGS = 1;
	private static final int LENGTH_SIMNUMBER = 32;
	private static final int LENGTH_SESSIONKEY = Encryption.KEY_LENGTH;
	private static final int LENGTH_LASTID = 1;

	private static final int OFFSET_FLAGS = 0;
	private static final int OFFSET_SIMNUMBER = OFFSET_FLAGS + LENGTH_FLAGS;
	private static final int OFFSET_SESSIONKEY_OUTGOING = OFFSET_SIMNUMBER + LENGTH_SIMNUMBER;
	private static final int OFFSET_LASTID_OUTGOING = OFFSET_SESSIONKEY_OUTGOING + LENGTH_SESSIONKEY;
	private static final int OFFSET_SESSIONKEY_INCOMING = OFFSET_LASTID_OUTGOING + LENGTH_LASTID;
	private static final int OFFSET_LASTID_INCOMING = OFFSET_SESSIONKEY_INCOMING + LENGTH_SESSIONKEY;
	
	private static final int OFFSET_RANDOMDATA = OFFSET_LASTID_INCOMING + LENGTH_LASTID;

	private static final int OFFSET_NEXTINDEX = Database.ENCRYPTED_ENTRY_SIZE - 4;
	private static final int OFFSET_PREVINDEX = OFFSET_NEXTINDEX - 4;
	
	private static final int LENGTH_RANDOMDATA = OFFSET_PREVINDEX - OFFSET_RANDOMDATA;	
	
	// STATIC
	
	private static ArrayList<SessionKeys> cacheSessionKeys = new ArrayList<SessionKeys>();
	
	/**
	 * Removes all instances from the list of cached objects.
	 * Be sure you don't use the instances afterwards.
	 */
	public static void forceClearCache() {
		cacheSessionKeys = new ArrayList<SessionKeys>();
	}

	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 */
	static SessionKeys createSessionKeys() throws DatabaseFileException, IOException {
		return createSessionKeys(true);
	}
	
	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 * @param lock		File lock allow
	 */
	static SessionKeys createSessionKeys(boolean lockAllow) throws DatabaseFileException, IOException {
		return new SessionKeys(Empty.getEmptyIndex(lockAllow), true, lockAllow);
	}

	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 */
	static SessionKeys getSessionKeys(long index) throws DatabaseFileException, IOException {
		return getSessionKeys(index, true);
	}
	
	/**
	 * Returns an instance of Empty class with given index in file.
	 * @param index		Index in file
	 * @param lock		File lock allow
	 */
	static SessionKeys getSessionKeys(long index, boolean lockAllow) throws DatabaseFileException, IOException {
		if (index <= 0L)
			return null;
		
		// try looking it up
		for (SessionKeys keys: cacheSessionKeys)
			if (keys.getEntryIndex() == index)
				return keys; 
		
		// create a new one
		return new SessionKeys(index, true, lockAllow);
	}

	// INTERNAL FIELDS
	private long mEntryIndex; // READ ONLY
	private boolean mKeysSent;
	private boolean mKeysConfirmed;
	private String mSimNumber;
	private byte[] mSessionKey_Out;
	private byte mLastID_Out;
	private byte[] mSessionKey_In;
	private byte mLastID_In;
	private long mIndexPrev;
	private long mIndexNext;
	
	// CONSTRUCTORS
	
	private SessionKeys(long index, boolean readFromFile) throws DatabaseFileException, IOException {
		this(index, readFromFile, true);
	}
	
	private SessionKeys(long index, boolean readFromFile, boolean lockAllow) throws DatabaseFileException, IOException {
		mEntryIndex = index;
		
		if (readFromFile) {
			byte[] dataEncrypted = Database.getDatabase().getEntry(index, lockAllow);
			byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());

			byte flags = dataPlain[OFFSET_FLAGS];
			boolean keysSent = ((flags & (1 << 7)) == 0) ? false : true;
			boolean keysConfirmed = ((flags & (1 << 6)) == 0) ? false : true;

			byte[] dataSessionKey_Out = new byte[LENGTH_SESSIONKEY];
			System.arraycopy(dataPlain, OFFSET_SESSIONKEY_OUTGOING, dataSessionKey_Out, 0, LENGTH_SESSIONKEY);
			byte[] dataSessionKey_In = new byte[LENGTH_SESSIONKEY];
			System.arraycopy(dataPlain, OFFSET_SESSIONKEY_INCOMING, dataSessionKey_In, 0, LENGTH_SESSIONKEY);
			
			setKeysSent(keysSent);
			setKeysConfirmed(keysConfirmed);
			setSimNumber(Database.fromLatin(dataPlain, OFFSET_SIMNUMBER, LENGTH_SIMNUMBER));
			setSessionKey_Out(dataSessionKey_Out);
			setLastID_Out(dataPlain[OFFSET_LASTID_OUTGOING]);
			setSessionKey_In(dataSessionKey_In);
			setLastID_In(dataPlain[OFFSET_LASTID_INCOMING]);
			setIndexPrev(Database.getInt(dataPlain, OFFSET_PREVINDEX));
			setIndexNext(Database.getInt(dataPlain, OFFSET_NEXTINDEX));
		}
		else {
			// default values
			setKeysSent(false);
			setKeysConfirmed(false);
			setSimNumber("");
			setSessionKey_Out(Encryption.generateRandomData(Encryption.KEY_LENGTH));
			setLastID_Out((byte) 0x00);
			setSessionKey_In(Encryption.generateRandomData(Encryption.KEY_LENGTH));
			setLastID_In((byte) 0x00);
			setIndexPrev(0L);
			setIndexNext(0L);
			
			saveToFile(lockAllow);
		}

		cacheSessionKeys.add(this);
	}

	// FUNCTIONS
	
	public void saveToFile() throws DatabaseFileException, IOException {
		saveToFile(true);
	}
	
	public void saveToFile(boolean lock) throws DatabaseFileException, IOException {
		ByteBuffer keysBuffer = ByteBuffer.allocate(Database.ENCRYPTED_ENTRY_SIZE);
		
		// flags
		byte flags = 0;
		if (this.mKeysSent)
			flags |= (byte) ((1 << 7) & 0xFF);
		if (this.mKeysConfirmed)
			flags |= (byte) ((1 << 6) & 0xFF);
		keysBuffer.put(flags);
		
		// phone number
		keysBuffer.put(Database.toLatin(this.mSimNumber, LENGTH_SIMNUMBER));
		
		// session keys and last IDs
		keysBuffer.put(this.mSessionKey_Out);
		keysBuffer.put((byte) this.mLastID_Out);
		keysBuffer.put(this.mSessionKey_In);
		keysBuffer.put((byte) this.mLastID_In);
		
		// random data
		keysBuffer.put(Encryption.generateRandomData(LENGTH_RANDOMDATA));
		
		// indices
		keysBuffer.put(Database.getBytes(this.mIndexPrev));
		keysBuffer.put(Database.getBytes(this.mIndexNext));
		
		byte[] dataEncrypted = Encryption.encryptSymmetric(keysBuffer.array(), Encryption.retreiveEncryptionKey());
		Database.getDatabase().setEntry(mEntryIndex, dataEncrypted, lock);
	}

	public SessionKeys getPreviousSessionKeys() throws DatabaseFileException, IOException {
		return getPreviousSessionKeys(true);
	}
	
	public SessionKeys getPreviousSessionKeys(boolean lockAllow) throws DatabaseFileException, IOException {
		return getSessionKeys(mIndexPrev, lockAllow);
	}

	public SessionKeys getNextSessionKeys() throws DatabaseFileException, IOException {
		return getNextSessionKeys(true);
	}
	
	public SessionKeys getNextSessionKeys(boolean lockAllow) throws DatabaseFileException, IOException {
		return getSessionKeys(mIndexNext, lockAllow);
	}
	
	// GETTERS / SETTERS
	
	long getEntryIndex() {
		return mEntryIndex;
	}
	
	boolean getKeysSent() {
		return mKeysSent;
	}

	void setKeysSent(boolean keysSent) {
		mKeysSent = keysSent;
	}

	boolean getKeysConfirmed() {
		return mKeysConfirmed;
	}

	void setKeysConfirmed(boolean keysConfirmed) {
		mKeysConfirmed = keysConfirmed;
	}

	String getSimNumber() {
		return mSimNumber;
	}

	void setSimNumber(String simNumber) {
		this.mSimNumber = simNumber;
	}

	byte[] getSessionKey_Out() {
		return mSessionKey_Out;
	}

	void setSessionKey_Out(byte[] sessionKeyOut) {
		mSessionKey_Out = sessionKeyOut;
	}

	byte[] getSessionKey_In() {
		return mSessionKey_In;
	}

	void setSessionKey_In(byte[] sessionKeyIn) {
		mSessionKey_In = sessionKeyIn;
	}
	
	byte getLastID_Out() {
		return mLastID_Out;
	}
	
	void setLastID_Out(byte lastID_Out) {
		mLastID_Out = lastID_Out;
	}

	byte getLastID_In() {
		return mLastID_In;
	}
	
	void setLastID_In(byte lastID_In) {
		mLastID_In = lastID_In;
	}

	long getIndexPrev() {
		return mIndexPrev;
	}

	void setIndexPrev(long indexPrev) {
	    if (indexPrev > 0xFFFFFFFFL || indexPrev < 0L)
	    	throw new IndexOutOfBoundsException();
		
		this.mIndexPrev = indexPrev;
	}

	long getIndexNext() {
		return mIndexNext;
	}

	void setIndexNext(long indexNext) {
	    if (indexNext > 0xFFFFFFFFL || indexNext < 0L)
	    	throw new IndexOutOfBoundsException();
		
		this.mIndexNext = indexNext;
	}
}
