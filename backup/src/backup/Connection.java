package backup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Connection {
	
	private MulticastSocket MC;
	private MulticastSocket MDB;
	private MulticastSocket MDR;
	
	private String mcAddress;
	private int mcPort;
	
	public Connection(String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort) {
		
		
		this.mcAddress = mcAddress;
		this.mcPort = mcPort;
		
		try {
			
			
			
			this.MC = new MulticastSocket(mcPort);
			System.out.println(this.MC.getLocalPort());
			this.MDB = new MulticastSocket(mdbPort);
			this.MDR = new MulticastSocket(mdrPort);
			
			this.MC.joinGroup(InetAddress.getByName(mcAddress));
			this.MDB.joinGroup(InetAddress.getByName(mdbAddress));
			this.MDR.joinGroup(InetAddress.getByName(mdrAddress));
			
			System.out.println(this.MC.getPort());
			System.out.println(this.MC.getRemoteSocketAddress());
			System.out.println(this.MC.getLocalSocketAddress());
			
			System.out.println(InetAddress.getByName(mcAddress));
			
			System.out.println(mcAddress);
			System.out.println(mcPort);


			
		
		} catch (IOException e) {
			
			e.printStackTrace();
		}		
	}

	public MulticastSocket getMC() {
		return this.MC;
	}
	
	

	public String getMcAddress() {
		return mcAddress;
	}

	public void setMcAddress(String mcAddress) {
		this.mcAddress = mcAddress;
	}

	public int getMcPort() {
		return mcPort;
	}

	public void setMcPort(int mcPort) {
		this.mcPort = mcPort;
	}

	public MulticastSocket getMDB() {
		return MDB;
	}

	public MulticastSocket getMDR() {
		return MDR;
	}
	
	
	
	

}
