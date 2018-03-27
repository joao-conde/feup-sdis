package backup;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Connection {
	
	private MulticastChannel MC;
	private MulticastChannel MDB;
	private MulticastChannel MDR;
	
	public static class MulticastChannel {
		
		private int port;
		private InetAddress multicastAddress;
		private MulticastSocket multicastSocket;
		private String name;
		
		
		public MulticastChannel(int port, String multicastAddress, String name) {
			
			try {
				
				this.name = name;
				this.port = port;
				this.multicastAddress = InetAddress.getByName(multicastAddress);
				this.multicastSocket = new MulticastSocket(this.port);
				this.multicastSocket.joinGroup(this.multicastAddress);
				
			}
			
			catch(IOException e) {
				e.printStackTrace();
			}
			
		}


		public int getPort() {
			return port;
		}


		public InetAddress getMulticastAddress() {
			return multicastAddress;
		}


		public MulticastSocket getMulticastSocket() {
			return multicastSocket;
		}
		
		
		
		public String getName() {
			return name;
		}


		public void sendMessage(Message message) {
			
			try {
				DatagramPacket messageBuffer = new DatagramPacket(message.getMessage(), message.getMessage().length, this.multicastAddress, this.port);
				this.multicastSocket.send(messageBuffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		public int receiveMessage(byte[] buffer) {
			
			DatagramPacket receivingPacket = new DatagramPacket(buffer, buffer.length);
			
			
			try {
				this.multicastSocket.receive(receivingPacket);
				
								
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return receivingPacket.getLength();

		}
		
		
		
		
	}
		
	public Connection(String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort) {
		
		
		this.MC = new MulticastChannel(mcPort, mcAddress, "Control Channel");
		this.MDB = new MulticastChannel(mdbPort, mdbAddress, "Data Backup Channel");
		this.MDR = new MulticastChannel(mdrPort, mdrAddress, "Data Re");
		
	}

	public MulticastChannel getMC() {
		return MC;
	}

	public MulticastChannel getMDB() {
		return MDB;
	}

	public MulticastChannel getMDR() {
		return MDR;
	}
	
	

	
	
	
	
	

}
