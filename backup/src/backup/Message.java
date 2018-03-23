package backup;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

public class Message {

	private final static byte CR = 0xD;
	private final static byte LF = 0xA;
	private final static String CRLF = new String(new byte[] {CR,LF});

	public static enum MessageType {

		PUTCHUNK("PUTCHUNK"),
		STORED("STORED");

		public String text;

		private MessageType(String text) {
			this.text = text;
		}

		public MessageType getType(String text) {

			if(text.equals(this.text))
				return this;
			return null;

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
		String fileId;
		int chunkNo;
		int replicationDegree;


		public MessageFields(MessageType messageType, float protocolVersion, int senderId, String fileId, int chunkNo,
				int replicationDegree) {
			this.messageType = messageType;
			this.protocolVersion = protocolVersion;
			this.senderId = senderId;
			this.fileId = fileId;
			this.chunkNo = chunkNo;
			this.replicationDegree = replicationDegree;
		}

	}


	private byte[] message;
	private MessageFields messageFields;
	private byte[] chunk;

	private Message(MessageFields messageFields, byte[] chunk) {
		this.messageFields = messageFields;
		this.chunk = chunk; 
	}

	private Message(byte[] message) {
		this.message = message;
	}


	public static Message buildMessage(MessageFields messageFields, byte[] chunk) throws ReplicationDegreeOutOfLimitsException,ChunkNoException {

		Message result = new Message(messageFields, chunk);

		if(messageFields.replicationDegree < 1 || messageFields.replicationDegree > 9)
			throw new ReplicationDegreeOutOfLimitsException();

		if(messageFields.chunkNo < 0 || messageFields.chunkNo > 1000000)
			throw new ChunkNoException();

		String headerString = messageFields.messageType.text + " " + messageFields.protocolVersion + " " + messageFields.senderId + " " + messageFields.fileId;

		switch (messageFields.messageType) {

			case PUTCHUNK:

				headerString += (" " + messageFields.chunkNo + " " + messageFields.replicationDegree);

				byte[] header = headerString.getBytes();

				byte[] finalHeader = new byte[header.length + 2];

				System.arraycopy(header, 0, finalHeader, 0, header.length);
				finalHeader[finalHeader.length-2] = CR;
				finalHeader[finalHeader.length-1] = LF;

				result.message = new byte[finalHeader.length + chunk.length];
				System.arraycopy(finalHeader, 0, result.message, 0, finalHeader.length);
				System.arraycopy(chunk, 0, result.message, header.length, chunk.length);

				break;

			default:
				break;
		}

		return result;

	}

	public static Message processMessage(byte[] message) {

		Message result = new Message(message);

		Scanner scanner = new Scanner(new ByteArrayInputStream(message));


		while(!scanner.hasNext(CRLF + " *" + CRLF)) {

			//MessageType messageType = MessageType. scanner.next()




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





}
