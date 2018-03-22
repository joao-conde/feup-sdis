package backup;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import backup.Message.ChunkNoException;
import backup.Message.MessageType;
import backup.Message.ReplicationDegreeOutOfLimitsException;

public class Peer {

	public static float BACKUP_PROTOCOL_VERSION = 1.0f;
	
	public int id;
	private Connection connection;
	private Dispatcher mcDispatcher;
	private Dispatcher mdbDispatcher;
	private Dispatcher mdrDispatcher;
	
	
	
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
		mcDispatcher = new Dispatcher(connection.getMC().getMulticastSocket());
		mdbDispatcher = new Dispatcher(connection.getMDB().getMulticastSocket());
		mdrDispatcher = new Dispatcher(connection.getMDR().getMulticastSocket());
		
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
		
			DatagramPacket packetToSend = new DatagramPacket(message.getMessage(), message.getMessage().length,
					this.connection.getMDB().getMulticastAddress(), 
					this.connection.getMDB().getPort());

			this.connection.getMC().getMulticastSocket().send(packetToSend);
		
		} 
		
		catch (ReplicationDegreeOutOfLimitsException e) {
			e.printStackTrace();
		}
		
		catch (ChunkNoException e) {
			e.printStackTrace();
		}
				
		catch(IOException e) {
			e.printStackTrace(); 
		}
	}
	
	
	public class MessageHandler implements Runnable {

		private byte[] buffer;
		
		public MessageHandler(byte[] buffer) {
			this.buffer = buffer;
		}


		@Override
		public void run() {
			
			System.out.println("Olá a partir da thread do peer " + id + "\nI received the message: " + new String(buffer));
			
			
		}
		
	}
	
	public class Dispatcher implements Runnable {
		
		private ExecutorService threadPool = Executors.newFixedThreadPool(5);
		
		public MulticastSocket multicastSocket;
		
		private byte[] buffer = new byte[2048];

		public Dispatcher(MulticastSocket multicastSocket) {
			this.multicastSocket = multicastSocket;

			
		}

		@Override
		public void run() {
						
			while(true) {
				
				try {
					
					DatagramPacket receivingPacket = new DatagramPacket(buffer, buffer.length);
					this.multicastSocket.receive(receivingPacket);
										
					Runnable handler = new MessageHandler(buffer);
					
					threadPool.execute(handler);
						
				}
				
				catch(IOException e) {
					
				}
				
			}
			
			
			
		}
		
	
	}
	
}
