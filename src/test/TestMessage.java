package test;

import org.junit.Assert;
import org.junit.Test;

import backup.Message;
import backup.Peer;
import backup.Message.ChunkNoException;
import backup.Message.MessageFields;
import backup.Message.MessageType;
import backup.Message.ReplicationDegreeOutOfLimitsException;

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
	public void testBuildMessagePutChunk() {
		
		//PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
		
		try {
			
			
			Message message = Message.buildMessage(new MessageFields(MessageType.PUTCHUNK, Peer.BACKUP_PROTOCOL_VERSION, 1, "DummyFile.jpg", 2, 3), "123456".getBytes());
			Assert.assertEquals("PUTCHUNK " + Peer.BACKUP_PROTOCOL_VERSION + " 1 DummyFile.jpg 2 3 " + Message.CRLF + Message.CRLF + "123456", new String(message.getMessage()));
		
		
		} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	public void testBuildMessageStored() {
		
		//STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		
		try {
			
			
			Message message = Message.buildMessage(new MessageFields(MessageType.STORED, Peer.BACKUP_PROTOCOL_VERSION, 1, "DummyFile.jpg", 2));
			Assert.assertEquals("STORED " + Peer.BACKUP_PROTOCOL_VERSION + " 1 DummyFile.jpg 2 " + Message.CRLF + Message.CRLF, new String(message.getMessage()));
		
		
		} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	
	@Test
	public void testProcessMessageGetChunk() {
		
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
	public void testBuildMessageGetChunk() {
		
		//GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		
		try {
			
			
			Message message = Message.buildMessage(new MessageFields(MessageType.GETCHUNK, Peer.BACKUP_PROTOCOL_VERSION, 1, "DummyFile.jpg", 2));
			Assert.assertEquals("GETCHUNK " + Peer.BACKUP_PROTOCOL_VERSION + " 1 DummyFile.jpg 2 " + Message.CRLF + Message.CRLF, new String(message.getMessage()));
		
		
		} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
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
	public void testBuildMessageChunk() {
		
		//CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
		
		try {
			
			
			Message message = Message.buildMessage(new MessageFields(MessageType.CHUNK, Peer.BACKUP_PROTOCOL_VERSION, 1, "DummyFile.jpg", 2), "123456".getBytes());
			Assert.assertEquals("CHUNK " + Peer.BACKUP_PROTOCOL_VERSION + " 1 DummyFile.jpg 2 " + Message.CRLF + Message.CRLF + "123456", new String(message.getMessage()));
		
		
		} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	@Test
	public void testProcessMessageDelete() {
		
		//DELETE <Version> <SenderId> <FileId> <CRLF><CRLF>
		
		byte[] message = new String("DELETE   1.0 1 DummyFile.jpg" + Message.CRLF + "   " + Message.CRLF).getBytes();
		
		Message processedMessage = Message.processMessage(message);
		
		Assert.assertTrue(processedMessage.getMessageFields().messageType == MessageType.DELETE);
		Assert.assertTrue(processedMessage.getMessageFields().protocolVersion == 1.0f);
		Assert.assertTrue(processedMessage.getMessageFields().senderId == 1);
		Assert.assertTrue(processedMessage.getMessageFields().fileId.equals("DummyFile.jpg"));

		
					
	}
	
	@Test
	public void testBuildMessageDelete() {
		
		//DELETE <Version> <SenderId> <FileId> <CRLF><CRLF>
		
		try {
			
			
			Message message = Message.buildMessage(new MessageFields(MessageType.DELETE, Peer.BACKUP_PROTOCOL_VERSION, 1, "DummyFile.jpg"));
			Assert.assertEquals("DELETE " + Peer.BACKUP_PROTOCOL_VERSION + " 1 DummyFile.jpg " + Message.CRLF + Message.CRLF, new String(message.getMessage()));
		
		
		} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
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
	
	@Test
	public void testBuildMessageRemoved() {
		
		//REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		
		try {
			
			Message message = Message.buildMessage(new MessageFields(MessageType.REMOVED, Peer.BACKUP_PROTOCOL_VERSION, 1, "DummyFile.jpg", 17), "123456".getBytes());
			Assert.assertEquals("REMOVED " + Peer.BACKUP_PROTOCOL_VERSION + " 1 DummyFile.jpg 17 " + Message.CRLF + Message.CRLF, new String(message.getMessage()));
		
		
		} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	
	
	


}
