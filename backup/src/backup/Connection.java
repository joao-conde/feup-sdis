package backup;

import java.io.IOException;
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
		
		
		public MulticastChannel(int port, String multicastAddress) {
			
			try {
				
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
		
		
		
		
	}
		
	public Connection(String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort) {
		
		
		this.MC = new MulticastChannel(mcPort, mcAddress);
		this.MDB = new MulticastChannel(mdbPort, mdbAddress);
		this.MDR = new MulticastChannel(mdrPort, mdrAddress);
		
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
