package backup;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer {

	public static double BACKUP_PROTOCOL_VERSION = 1.0;
	
	public int id;
	private Connection connection;
	private Dispatcher dispatcher;
	
	ExecutorService threadPool = Executors.newFixedThreadPool(5);
	
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
		dispatcher = new Dispatcher(connection.getMC());
		
		Thread mcDispatcherThread = new Thread(this.dispatcher);
		mcDispatcherThread.start();
		
		
	}
	
	public void putChunk() {
		
		String message = "Test put chunk";
		byte[] buffer = message.getBytes();
		
		System.out.println(message);
		
		try {
			
			System.out.println(this.connection.getMC().getLocalPort());
			
			DatagramPacket packetToSend = new DatagramPacket(
												buffer, buffer.length,
												this.connection.getMC().getLocalAddress(), 
												this.connection.getMC().getLocalPort());
			
			System.out.println("Before Packet sent");
			
			this.connection.getMC().send(packetToSend);
			
			System.out.println("Packet sent");
			
			
		}
		
		catch(Exception e) { e.printStackTrace(); }
	}
	
	
	public class HandlerPutChunk implements Runnable {

		@Override
		public void run() {
			
			System.out.println("Olá a partir da thread do peer " + id);
			
			
		}
		
	}
	
	public class Dispatcher implements Runnable {
		
		public MulticastSocket multicastSocket;
		
		private byte[] buffer = new byte[2048];

		public Dispatcher(MulticastSocket multicastSocket) {
			this.multicastSocket = multicastSocket;
		}

		@Override
		public void run() {
			
			System.out.println("Dispatcher thread created");
			
			while(true) {
				
				try {
					
					DatagramPacket receivingPacket = new DatagramPacket(buffer, buffer.length);
					this.multicastSocket.receive(receivingPacket);
					
					System.out.println("Packet Receieved");
					
					Runnable handler = new HandlerPutChunk();
					
					threadPool.execute(handler);
						
				}
				
				catch(IOException e) {
					
				}
				
			}
			
			
			
		}
		
		
		
		
		
	}
	
	
	
	
}
