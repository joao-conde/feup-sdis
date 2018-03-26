package backup;

import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Random;

import backup.Connection.MulticastChannel;
import backup.Message.ChunkNoException;
import backup.Message.MessageFields;
import backup.Message.MessageType;
import backup.Message.ReplicationDegreeOutOfLimitsException;

public class Peer {

	public static float BACKUP_PROTOCOL_VERSION = 1.0f;
	public final static int CHUNK_MAX_SIZE = 64000;
	public final static int INITIAL_PUTCHUNK_WAIT_TIME = 400;

	public int id;
	private Connection connection;
	private Dispatcher mcDispatcher;
	private Dispatcher mdbDispatcher;
	private Dispatcher mdrDispatcher;
	private Random randomGenerator = new Random(System.currentTimeMillis());
	
	private int putchunkConter = 0;



	public static void main(String[] args) {

		System.setProperty("java.net.preferIPv4Stack" , "true");

		Peer peer = new Peer(Integer.parseInt(args[0]));

		if(peer.id > 1) {

			peer.putChunk();

		}


	}

	public Peer(int id) {

		this.id = id;

		connection = new Connection("224.0.0.1", 2300, "224.0.0.2", 2301, "224.0.0.3", 2302);
		mcDispatcher = new Dispatcher(connection.getMC());
		mdbDispatcher = new Dispatcher(connection.getMDB());
		mdrDispatcher = new Dispatcher(connection.getMDR());

		Thread mcDispatcherThread = new Thread(this.mcDispatcher);
		Thread mdbDispatcherThread = new Thread(this.mdbDispatcher);
		Thread mdrDispatcherThread = new Thread(this.mdrDispatcher);
		mcDispatcherThread.start();
		mdbDispatcherThread.start();
		mdrDispatcherThread.start();


	}

	public void putChunk() {

		try {

			Message message = Message.buildMessage(new Message.MessageFields(MessageType.PUTCHUNK, BACKUP_PROTOCOL_VERSION, this.id, "FileId", 1, 2), new byte[] {0x1,0xb});
			this.connection.getMDB().sendMessage(message);
			
		}


		catch (ReplicationDegreeOutOfLimitsException e) {
			e.printStackTrace();
		}

		catch (ChunkNoException e) {
			e.printStackTrace();
		}

	}


	public void saveChunk(Message msg){
		System.out.println("Save chunk");

		try {
			FileOutputStream stream = new FileOutputStream("chunk" + msg.getMessageFields().chunkNo);
		    stream.write(msg.getChunk());
		    stream.close();
		}catch(Exception e){
			e.printStackTrace();
		}

		System.out.println("Chunk saved");
	}


	public ArrayList<byte[]> chunkFile(byte[] msg){
		ArrayList<byte[]> chunks = new ArrayList<byte[]>();
		//global constant in Peer?
		int chunkSize = 64 * 1024;
		int numberOfChunks = msg.length / chunkSize;
		if(msg.length % chunkSize != 0) numberOfChunks++;

		for(int i = 0; i < numberOfChunks; i++){
			byte[] chunk = new byte[chunkSize];
			System.arraycopy(msg, i*chunkSize, chunk, 0, chunkSize);
			chunks.add(chunk);
		}

		return chunks;
	}

	public class MessageHandler implements Runnable {

		private Message message;
		private byte[] buffer;
		private MulticastChannel channel;

		public MessageHandler(byte[] buffer, MulticastChannel channel) {
			this.buffer = buffer;
			this.channel = channel;
		}
		
		private void sendStoredMessage(String fileId, int chunkNo) {
			
			try {
				Message reply = Message.buildMessage(new MessageFields(MessageType.STORED, BACKUP_PROTOCOL_VERSION, id, fileId,chunkNo));
				Peer.this.connection.getMC().sendMessage(reply);
			} catch (ReplicationDegreeOutOfLimitsException | ChunkNoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}


		@Override
		public void run() {
			
			this.message = Message.processMessage(this.buffer);
			
			if(id == this.message.getMessageFields().senderId)
				return;
			
			System.out.println("\n---------  Message Received at " + this.channel.getName() + " ---------\n\t\t" + new String(message.getMessage()));
					
			switch(message.getMessageFields().messageType){

				case PUTCHUNK:
					//saveChunk(Message.processMessage(buffer));
					
					
					try {
						int delay = randomGenerator.nextInt(INITIAL_PUTCHUNK_WAIT_TIME);
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					this.sendStoredMessage(this.message.getMessageFields().fileId, this.message.getMessageFields().chunkNo);
					
					break;
				case STORED:
					break;
			default:
				break;
			}



		}

	}

	public class Dispatcher implements Runnable {

		private ExecutorService threadPool = Executors.newFixedThreadPool(5);

		public MulticastChannel multicastChannel;


		public Dispatcher(MulticastChannel multicastChannel) {
			this.multicastChannel = multicastChannel;


		}

		@Override
		public void run() {

			while(true) {
				
				byte[] buffer = new byte[2048];
				
				this.multicastChannel.receiveMessage(buffer);

				Runnable handler = new MessageHandler(buffer, this.multicastChannel);

				threadPool.execute(handler);

			}



		}


	}

}
