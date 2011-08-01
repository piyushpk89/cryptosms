package uk.ac.cam.db538.securesms.storage;

import java.io.IOException;
import android.text.format.Time;
import uk.ac.cam.db538.securesms.Charset;
import uk.ac.cam.db538.securesms.encryption.Encryption;
import uk.ac.cam.db538.securesms.storage.Conversation;
import uk.ac.cam.db538.securesms.storage.Storage;
import uk.ac.cam.db538.securesms.storage.StorageFileException;
import uk.ac.cam.db538.securesms.storage.Message;
import uk.ac.cam.db538.securesms.storage.Message.MessageType;
import junit.framework.TestCase;

public class Message_Test extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
		Common.clearStorageFile();
		
		timeStamp.setToNow();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	private boolean deliveredPart = true;
	private boolean deliveredAll = true;
	private MessageType messageType = MessageType.OUTGOING;
	private boolean unread = true;
	private Time timeStamp = new Time(); 
	private String messageBody = "Testing body";
	private long indexParent = 127L;
	private long indexMessageParts = 120L;
	private long indexPrev = 225L;
	private long indexNext = 12L;
	
	private void setData(Message msg) {
		msg.setDeliveredPart(deliveredPart);
		msg.setDeliveredAll(deliveredAll);
		msg.setMessageType(messageType);
		msg.setUnread(unread);
		msg.setTimeStamp(timeStamp);
		msg.setMessageBody(messageBody);
		msg.setIndexParent(indexParent);
		msg.setIndexMessageParts(indexMessageParts);
		msg.setIndexPrev(indexPrev);
		msg.setIndexNext(indexNext);
	}
	
	private void checkData(Message msg) {
		assertEquals(msg.getDeliveredPart(), deliveredPart);
		assertEquals(msg.getDeliveredAll(), deliveredAll);
		assertEquals(msg.getMessageType(), messageType);
		assertEquals(msg.getUnread(), unread);
		assertEquals(Time.compare(msg.getTimeStamp(), timeStamp), 0);
		assertEquals(msg.getMessageBody(), messageBody);
		assertEquals(msg.getIndexParent(), indexParent);
		assertEquals(msg.getIndexMessageParts(), indexMessageParts);
		assertEquals(msg.getIndexPrev(), indexPrev);
		assertEquals(msg.getIndexNext(), indexNext);
	}
	
	public void testConstruction() throws StorageFileException, IOException {
		// create a Message entry
		Conversation conv = Conversation.createConversation();
		Message msg = Message.createMessage(conv);

		// check structure
		assertTrue(Common.checkStructure());
		
		setData(msg);
		msg.saveToFile();
		long index = msg.getEntryIndex();
		
		// for it to be re-read
		Message.forceClearCache();
		msg = Message.getMessage(index);
		
		checkData(msg);
	}
	
	public void testIndices() throws StorageFileException, IOException {
		// INDICES OUT OF BOUNDS
		Conversation conv = Conversation.createConversation();
		Message msg = Message.createMessage(conv);
	
		// indexMessageParts
		try {
			msg.setIndexMessageParts(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
		
		try {
			msg.setIndexMessageParts(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	
		// indexPrev
		try {
			msg.setIndexPrev(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	
		try {
			msg.setIndexPrev(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	
		// indexNext
		try {
			msg.setIndexNext(0x0100000000L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	
		try {
			msg.setIndexNext(-1L);
			assertTrue(false);
		} catch (IndexOutOfBoundsException ex) {
		}
	}
	
	public void testCreateData() throws StorageFileException, IOException {
		// set data
		Conversation conv = Conversation.createConversation();
		Message msg = Message.createMessage(conv);
		setData(msg);
		msg.saveToFile();
		
		// compute expected values
		byte flags = 0;
		flags |= (deliveredPart) ? 0x80 : 0x00;
		flags |= (deliveredAll) ? 0x40 : 0x00;
		flags |= (messageType == MessageType.OUTGOING) ? 0x20 : 0x00;
		flags |= (unread) ? 0x10 : 0x00;
		
		// get the generated data
		byte[] dataEncrypted = Storage.getDatabase().getEntry(msg.getEntryIndex());
		
		// chunk length
		assertEquals(dataEncrypted.length, Storage.CHUNK_SIZE);
		
		// decrypt the encoded part
		byte[] dataPlain = Encryption.decryptSymmetric(dataEncrypted, Encryption.retreiveEncryptionKey());
		
		// check the data
		assertEquals(dataPlain[0], flags);
		Time time = new Time(); time.parse3339(Charset.fromLatin(dataPlain, 1, 29));
		assertEquals(Time.compare(time, timeStamp), 0);
		assertEquals(Charset.fromLatin(dataPlain, 30, 140), messageBody);
		assertEquals(Storage.getInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 16), indexParent);
		assertEquals(Storage.getInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 12), indexMessageParts);
		assertEquals(Storage.getInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 8), indexPrev);
		assertEquals(Storage.getInt(dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4), indexNext);
	}
	
	public void testParseData() throws StorageFileException, IOException {
		Conversation conv = Conversation.createConversation();
		Message msg = Message.createMessage(conv);
		long index = msg.getEntryIndex();
		
		// prepare stuff
		byte flags = 0;
		flags |= (deliveredPart) ? 0x80 : 0x00;
		flags |= (deliveredAll) ? 0x40 : 0x00;
		flags |= (messageType == MessageType.OUTGOING) ? 0x20 : 0x00;
		flags |= (unread) ? 0x10 : 0x00;
	
		// create plain data
		byte[] dataPlain = new byte[Storage.ENCRYPTED_ENTRY_SIZE];
		dataPlain[0] = flags;
		System.arraycopy(Charset.toLatin(timeStamp.format3339(false), 29), 0, dataPlain, 1, 29);
		System.arraycopy(Charset.toLatin(messageBody, 140), 0, dataPlain, 30, 140);
		System.arraycopy(Storage.getBytes(indexParent), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 16, 4);
		System.arraycopy(Storage.getBytes(indexMessageParts), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 12, 4);
		System.arraycopy(Storage.getBytes(indexPrev), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 8, 4);
		System.arraycopy(Storage.getBytes(indexNext), 0, dataPlain, Storage.ENCRYPTED_ENTRY_SIZE - 4, 4);
		
		// encrypt it
		byte[] dataEncrypted = Encryption.encryptSymmetric(dataPlain, Encryption.retreiveEncryptionKey());
	
		// inject it in the file
		Storage.getDatabase().setEntry(index, dataEncrypted);

		// have it parsed
		Message.forceClearCache();
		msg = Message.getMessage(index);
		
		// check the indices
		assertEquals(deliveredPart, msg.getDeliveredPart());
		assertEquals(deliveredAll, msg.getDeliveredAll());
		assertEquals(messageType, msg.getMessageType());
		assertEquals(unread, msg.getUnread());
		assertEquals(Time.compare(timeStamp, msg.getTimeStamp()), 0);
		assertEquals(messageBody, msg.getMessageBody());
		assertEquals(indexParent, msg.getIndexParent());
		assertEquals(indexMessageParts, msg.getIndexMessageParts());
		assertEquals(indexPrev, msg.getIndexPrev());
		assertEquals(indexNext, msg.getIndexNext());
	}
}
