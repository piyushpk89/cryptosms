package uk.ac.cam.db538.securesms.database;

import java.io.IOException;

import uk.ac.cam.db538.securesms.CustomAsserts;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import junit.framework.TestCase;

public class SessionKeys_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		Common.clearFile();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private boolean keysSent = true;
	private boolean keysConfirmed = true;
	private boolean simSerial = true;
	private String simNumber = "+123456789012";
	private byte[] sessionKey_Out = Encryption.generateRandomData(Encryption.KEY_LENGTH);
	private byte lastID_Out = 0x12;
	private byte[] sessionKey_In = Encryption.generateRandomData(Encryption.KEY_LENGTH);
	private byte lastID_In = 0x18;
	private long indexParent = 246L;
	private long indexPrev = 247L;
	private long indexNext = 248L;
	private String simNumberLong = "+1234567890126549873sdfsat6ewrt987wet3df1g3s2g1e6r5t46wert4dfsgdfsg";
	private String simNumberResult = "+1234567890126549873sdfsat6ewrt9";
	private byte flags = (byte) 0xE0;

	private void setData(SessionKeys keys, boolean longer) {
		keys.setKeysSent(keysSent);
		keys.setKeysConfirmed(keysConfirmed);
		keys.setSimSerial(simSerial);
		keys.setSimNumber((longer) ? simNumberLong : simNumber);
		keys.setSessionKey_Out(sessionKey_Out);
		keys.setLastID_Out(lastID_Out);
		keys.setSessionKey_In(sessionKey_In);
		keys.setLastID_In(lastID_In);
		keys.setIndexParent(indexParent);
		keys.setIndexPrev(indexPrev);
		keys.setIndexNext(indexNext);
	}
	
	private void checkData(SessionKeys keys, boolean longer) {
		assertEquals(keysSent, keys.getKeysSent());
		assertEquals(keysConfirmed, keys.getKeysConfirmed());
		assertEquals(simSerial, keys.usesSimSerial());
		assertEquals((longer) ? simNumberResult : simNumber, keys.getSimNumber());
		CustomAsserts.assertArrayEquals(keys.getSessionKey_Out(), sessionKey_Out);
		assertEquals(lastID_Out, keys.getLastID_Out());
		CustomAsserts.assertArrayEquals(keys.getSessionKey_In(), sessionKey_In);
		assertEquals(lastID_In, keys.getLastID_In());
		assertEquals(indexParent, keys.getIndexParent());
		assertEquals(indexPrev, keys.getIndexPrev());
		assertEquals(indexNext, keys.getIndexNext());
	}
	
	public void testConstruction() throws DatabaseFileException, IOException {
		Conversation conv = Conversation.createConversation();
		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		
		assertTrue(Common.checkStructure());

		setData(keys, false);
		keys.saveToFile();
		long index = keys.getEntryIndex();

		// force to be re-read
		SessionKeys.forceClearCache();
		keys = SessionKeys.getSessionKeys(index);
		
		checkData(keys, false);
	}
			
	public void testIndices() throws DatabaseFileException, IOException {
		// INDICES OUT OF BOUNDS
		Conversation conv = Conversation.createConversation();
		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		// indexNext
		try {
			keys.setIndexNext(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}

		try {
			keys.setIndexNext(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}

	public void testCreateData() throws DatabaseFileException, IOException {
		Conversation conv = Conversation.createConversation();
		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		setData(keys, true);
		keys.saveToFile();
		
		// get the generated data
		byte[] dataEncrypted = Database.getDatabase().getEntry(keys.getEntryIndex());
		
		// chunk length
		assertEquals(dataEncrypted.length, Database.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the data
		assertEquals(flags, dataPlain[0]);
		assertEquals(Database.fromLatin(dataPlain, 1, 32), simNumberResult);
		CustomAsserts.assertArrayEquals(dataPlain, 33, sessionKey_Out, 0, 32);
		assertEquals(lastID_Out, dataPlain[65]);
		CustomAsserts.assertArrayEquals(dataPlain, 66, sessionKey_In, 0, 32);
		assertEquals(lastID_In, dataPlain[98]);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 12), indexParent);
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 8), indexPrev);		
		assertEquals(Database.getInt(dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}

	public void testParseData() throws DatabaseFileException, IOException {
		Conversation conv = Conversation.createConversation();
		SessionKeys keys = SessionKeys.createSessionKeys(conv);
		long index = keys.getEntryIndex();
		
		// create plain data
		byte[] dataPlain = new byte[Database.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Database.toLatin(simNumber, 32), 0, dataPlain, 1, 32);
		System.arraycopy(sessionKey_Out, 0, dataPlain, 33, 32);
		dataPlain[65] = lastID_Out;
		System.arraycopy(sessionKey_In, 0, dataPlain, 66, 32);
		dataPlain[98] = lastID_In;
		System.arraycopy(Database.getBytes(indexParent), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 12, 4);
		System.arraycopy(Database.getBytes(indexPrev), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 8, 4);
		System.arraycopy(Database.getBytes(indexNext), 0, dataPlain, Database.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());

		// inject it into the file
		Database.getDatabase().setEntry(index, dataEncrypted);
		
		// have it parsed
		SessionKeys.forceClearCache();
		keys = SessionKeys.getSessionKeys(index);
		
		// check the indices
		checkData(keys, false);
	}
}