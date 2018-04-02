package backup;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Message {
	
	public final static byte CR = 0xD;
	public final static byte LF = 0xA;
	public final static String CRLF = new String(new byte[] {CR,LF});
	
	private byte[] message;
	private MessageFields messageFields;
	private byte[] chunk = new byte[Peer.CHUNK_MAX_SIZE];
	
	private String headerString;
	 
			
	public static enum MessageType {
		
		
		PUTCHUNK("PUTCHUNK"),
		STORED("STORED"),
		GETCHUNK("GETCHUNK"),
		CHUNK("CHUNK"),
		DELETE("DELETE"),
		REMOVED("REMOVED"),
		NEWPEER("NEWPEER");
		
		static final ArrayList<String> types = new ArrayList<String>(Arrays.asList("PUTCHUNK","STORED","GETCHUNK","CHUNK","DELETE","REMOVED","NEWPEER")) ;
		
		public String text;
		
		MessageType(String text) {
			this.text = text;
		}
		
		static MessageType type(String text) {
			
			switch(types.indexOf(text)) {
			
				case 0:
					return MessageType.PUTCHUNK;
				case 1:
					return MessageType.STORED;
				case 2:
					return MessageType.GETCHUNK;
				case 3:
					return MessageType.CHUNK;
				case 4:
					return MessageType.DELETE;
				case 5:
					return MessageType.REMOVED;
				case 6:
					return MessageType.NEWPEER;
				default:
					throw new IllegalArgumentException(text);
			
			}
					
		}
				
	}

	public static class ReplicationDegreeOutOfLimitsException extends Exception {

		private static final long serialVersionUID = 1L;

		public ReplicationDegreeOutOfLimitsException() {
			super("Replication Degree must be between 1 to 9");
		}

	}

	public static class ChunkNoException extends Exception {

		private static final long serialVersionUID = 1L;

		public ChunkNoException() {
			super("Number of chunks in a file should be less than 1 000 000 and more than 0");
		}

	}

	public static class MessageFields {

		public MessageType messageType;
		public float protocolVersion;
		public int senderId;
		public String fileId;
		public int chunkNo;
		public int replicationDegree;
		
			
		public MessageFields(MessageType messageType, float protocolVersion, int senderId, String fileId, int chunkNo,
				int replicationDegree) {
			this.messageType = messageType;
			this.protocolVersion = protocolVersion;
			this.senderId = senderId;
			this.fileId = fileId;
			this.chunkNo = chunkNo;
			this.replicationDegree = replicationDegree;
		}
		
		public MessageFields(MessageType messageType, float protocolVersion, int senderId, String fileId, int chunkNo) {
			this.messageType = messageType;
			this.protocolVersion = protocolVersion;
			this.senderId = senderId;
			this.fileId = fileId;
			this.chunkNo = chunkNo;
			this.replicationDegree = -1;
		}
		
		public MessageFields(MessageType messageType, float protocolVersion, int senderId, String fileId) {
			this.messageType = messageType;
			this.protocolVersion = protocolVersion;
			this.senderId = senderId;
			this.fileId = fileId;
			this.chunkNo = -1;
			this.replicationDegree = -1;
		}
		
		public MessageFields(int senderId) {
			this.messageType = MessageType.NEWPEER;
			this.senderId = senderId;
		}

	}


	
	private Message(int senderId) {
		this.messageFields = new MessageFields(senderId);
	}

	private Message(MessageFields messageFields, byte[] chunk) {
		this.messageFields = messageFields;
		this.chunk = chunk; 
	}

	private Message(byte[] message) {
		this.message = message;
	}


	public static Message buildMessage(MessageFields messageFields, byte[] chunk) throws ReplicationDegreeOutOfLimitsException,ChunkNoException {

		Message result = new Message(messageFields, chunk);

	
		result.headerString = messageFields.messageType.text + " " + messageFields.protocolVersion + " " + messageFields.senderId + " " + messageFields.fileId;
		
	
		if(messageFields.messageType != MessageType.DELETE) {
			
			if(messageFields.chunkNo < 0 || messageFields.chunkNo > 1000000)
				throw new ChunkNoException();
			
			result.headerString += (" " + messageFields.chunkNo);
			
			if(messageFields.messageType == MessageType.PUTCHUNK) {
				
				if(messageFields.replicationDegree < 1 || messageFields.replicationDegree > 9)
					throw new ReplicationDegreeOutOfLimitsException();
				
				result.headerString += (" " + messageFields.replicationDegree);
				
			}
				
		}
		
		result.headerString += (" " + CRLF + CRLF); 
		
		byte[] header = result.headerString.getBytes();
		
		if(messageFields.messageType == MessageType.PUTCHUNK || messageFields.messageType == MessageType.CHUNK) {
			
			result.message = new byte[header.length + chunk.length];
			System.arraycopy(header, 0, result.message, 0, header.length);
			System.arraycopy(chunk, 0, result.message, header.length, chunk.length);
			
		}
		
		else {
			
			result.message = new byte[header.length];
			System.arraycopy(header, 0, result.message, 0, header.length);
			
		}
		
		
		return result;

	}
	
	public static Message buildMessage(MessageFields messageFields) throws ReplicationDegreeOutOfLimitsException, ChunkNoException {
		
		return buildMessage(messageFields,null);
				
	}
	
	public static Message buildNewPeerMessage(int senderId, float protocolVersion) {
		
		Message result = new Message(senderId);
		
		result.headerString = result.getMessageFields().messageType.text + " " + protocolVersion + " " + senderId + " " + CRLF + CRLF;
		
		result.message = result.headerString.getBytes();
		
		return result;
		
		
	}

	public static Message processMessage(byte[] message) throws NumberFormatException {

		Message result = new Message(message);		
		byte[][] splitMessage = splitMessage(message);
		
		result.headerString = new String(splitMessage[0]);
				
		ByteArrayInputStream inputStream = new ByteArrayInputStream(splitMessage[0]);
		
		Scanner scanner = new Scanner(inputStream);
		
		ArrayList<String> fields = new ArrayList<>();
		
		
		while(scanner.hasNext()) {
			
			String current = scanner.next();
			
			fields.add(current);
			
		}
		
		scanner.close();
		
		
		MessageType messageType = MessageType.type(fields.get(0));
		
		float protocolVersion = Float.parseFloat(fields.get(1));
		int senderId = Integer.parseInt(fields.get(2));
		
		if(messageType == MessageType.NEWPEER) {
			
			result.messageFields = new MessageFields(senderId);
			return result;
		}
			
		
		String fileId = fields.get(3);
		int chunkNo = -1;
		int replicationDegree = -1;
		
		
		if(messageType != MessageType.DELETE) {
			chunkNo = Integer.parseInt(fields.get(4));
			
			if(messageType == MessageType.PUTCHUNK) {
				replicationDegree = Integer.parseInt(fields.get(5));
			}
			
		}
		
		
		result.messageFields = new MessageFields(messageType, protocolVersion, senderId, fileId, chunkNo, replicationDegree);
		
		if(messageType == MessageType.PUTCHUNK || messageType == MessageType.CHUNK) {

			
			result.chunk = splitMessage[1];
			
						
		}
		
		

		scanner.close();



		return result;
	}



	public byte[] getMessage() {
		return message;
	}

	public MessageFields getMessageFields() {
		return messageFields;
	}

	public byte[] getChunk() {
		return chunk;
	}
	
	

	
	public String getHeaderString() {
		return headerString;
	}

	private static byte[][] splitMessage(byte[] message) {
		
		int index;
		
		int foundIndex = 0;
		
		for(index = 0; index < message.length; index++) {
			
			if(message[index] == CR && message[index+1] == LF && message[index+2] == CR && message[index+3] == LF) {
				foundIndex = index + 3;
				break;
			}
				
			
		}
		
		byte[][] result = new byte[2][];
		result[0] = new byte[message.length];
		
		int chunkSize = message.length - foundIndex-1;
		
		result[1] = new byte[chunkSize];
		
		System.arraycopy(message, 0, result[0],0 , foundIndex);
		System.arraycopy(message, foundIndex+1, result[1], 0, chunkSize);
		
		
		
		return result;
		
		
		
	}
	

}
