package backup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Connection {
	
	private MulticastSocket MC;
	private MulticastSocket MDB;
	private MulticastSocket MDR;
	
	public Connection(String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort) {
		
		try {
			this.MC = new MulticastSocket(mcPort);
			this.MDB = new MulticastSocket(mdbPort);
			this.MDR = new MulticastSocket(mdrPort);
			
			this.MC.joinGroup(InetAddress.getByName(mcAddress));
			this.MDB.joinGroup(InetAddress.getByName(mdbAddress));
			this.MDR.joinGroup(InetAddress.getByName(mdrAddress));
			
		
		} catch (IOException e) {
			
			e.printStackTrace();
		}		
	}

	public MulticastSocket getMC() {
		return MC;
	}

	public MulticastSocket getMDB() {
		return MDB;
	}

	public MulticastSocket getMDR() {
		return MDR;
	}
	
	
	
	

}
