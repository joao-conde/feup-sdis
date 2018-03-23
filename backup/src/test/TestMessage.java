package test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Matcher;
import org.junit.Assert;
import org.junit.Test;

import backup.Message;
import backup.Message.MessageType;

public class TestMessage {
	
	@Test
	public void testProcessMessagePutChunk() {
		
		//PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
		
		byte[] message = new String("PUTCHUNK 1.0 1 DummyFile.jpg 1 3 " + Message.CRLF + " " + Message.CRLF + "gdfgfhgf4536").getBytes();
		
		Message processedMessage = Message.processMessage(message);
		
		Assert.assertTrue(processedMessage.getMessageFields().messageType == MessageType.PUTCHUNK);
		Assert.assertTrue(processedMessage.getMessageFields().protocolVersion == 1.0f);
		Assert.assertTrue(processedMessage.getMessageFields().senderId == 1);
		Assert.assertTrue(processedMessage.getMessageFields().fileId.equals("DummyFile.jpg"));
		Assert.assertTrue(processedMessage.getMessageFields().chunkNo == 1);
		Assert.assertTrue(processedMessage.getMessageFields().replicationDegree == 3);
		
		
		byte[] chunk = "gdfgfhgf4536".getBytes();
		
		for(int i = 0; i < chunk.length; i++) {
			
			Assert.assertEquals(chunk[i], processedMessage.getChunk()[i]);
			
		}
		
	}
	
	@Test
	public void testProcessMessageStored() {
		
		//STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>

		
		byte[] message = new String("STORED 1.0 1 DummyFile.jpg 1 " + Message.CRLF + " " + Message.CRLF).getBytes();
		
		Message processedMessage = Message.processMessage(message);
		
		Assert.assertTrue(processedMessage.getMessageFields().messageType == MessageType.STORED);
		Assert.assertTrue(processedMessage.getMessageFields().protocolVersion == 1.0f);
		Assert.assertTrue(processedMessage.getMessageFields().senderId == 1);
		Assert.assertTrue(processedMessage.getMessageFields().fileId.equals("DummyFile.jpg"));
		Assert.assertTrue(processedMessage.getMessageFields().chunkNo == 1);

		
					
	}
	
	@Test
	public void testProcessMessageGetchunk() {
		
		//GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>

		byte[] message = new String("GETCHUNK 1.0 1 DummyFile.jpg 1 " + Message.CRLF + " " + Message.CRLF).getBytes();
		
		Message processedMessage = Message.processMessage(message);
		
		Assert.assertTrue(processedMessage.getMessageFields().messageType == MessageType.GETCHUNK);
		Assert.assertTrue(processedMessage.getMessageFields().protocolVersion == 1.0f);
		Assert.assertTrue(processedMessage.getMessageFields().senderId == 1);
		Assert.assertTrue(processedMessage.getMessageFields().fileId.equals("DummyFile.jpg"));
		Assert.assertTrue(processedMessage.getMessageFields().chunkNo == 1);

		
					
	}
	
	@Test
	public void testProcessMessageChunk() {
		
		//CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
		
		byte[] message = new String("CHUNK 1.0 1 DummyFile.jpg 1 3 " + Message.CRLF + " " + Message.CRLF + "gdfgfhgf4536").getBytes();
		
		Message processedMessage = Message.processMessage(message);
		
		Assert.assertTrue(processedMessage.getMessageFields().messageType == MessageType.CHUNK);
		Assert.assertTrue(processedMessage.getMessageFields().protocolVersion == 1.0f);
		Assert.assertTrue(processedMessage.getMessageFields().senderId == 1);
		Assert.assertTrue(processedMessage.getMessageFields().fileId.equals("DummyFile.jpg"));
		Assert.assertTrue(processedMessage.getMessageFields().chunkNo == 1);

		
					
	}
	
	@Test
	public void testProcessMessageDelete() {
		
		//DELETE <Version> <SenderId> <FileId> <CRLF><CRLF>
		
		byte[] message = new String("DELETE 1.0 1 DummyFile.jpg" + Message.CRLF + " " + Message.CRLF).getBytes();
		
		Message processedMessage = Message.processMessage(message);
		
		Assert.assertTrue(processedMessage.getMessageFields().messageType == MessageType.DELETE);
		Assert.assertTrue(processedMessage.getMessageFields().protocolVersion == 1.0f);
		Assert.assertTrue(processedMessage.getMessageFields().senderId == 1);
		Assert.assertTrue(processedMessage.getMessageFields().fileId.equals("DummyFile.jpg"));

		
					
	}
	
	@Test
	public void testProcessMessageRemoved() {
		
		//REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		
		byte[] message = new String("REMOVED 1.0 1 DummyFile.jpg 3" + Message.CRLF + " " + Message.CRLF).getBytes();
		
		Message processedMessage = Message.processMessage(message);
		
		Assert.assertTrue(processedMessage.getMessageFields().messageType == MessageType.REMOVED);
		Assert.assertTrue(processedMessage.getMessageFields().protocolVersion == 1.0f);
		Assert.assertTrue(processedMessage.getMessageFields().senderId == 1);
		Assert.assertTrue(processedMessage.getMessageFields().fileId.equals("DummyFile.jpg"));
		Assert.assertTrue(processedMessage.getMessageFields().chunkNo == 3);

		
					
	}
	
	


}
