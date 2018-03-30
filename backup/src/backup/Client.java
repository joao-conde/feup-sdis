package backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import backup.Protocol.SUB_PROTOCOL_TYPE;

public class Client {
	
	public static final int RMI_CHUNK = 8094;
	
	private Registry registry;
	private Protocol stub;

	public static void main(String[] args) {

		if (args.length < 2) {

			System.out.println("Usage:\n\"<peer_ap> <sub_protocol> <opnd_1> <opnd_2>\n");
			System.out.println("<peer_ap> - //host:port/name");
			System.out.println("<sub_protocol> - BACKUP | RESTORE | DELETE | RECLAIM | STATE");
			System.out.println("<opnd_1> - file_name (backup, restore, delete) | Space to reclaim (reclaim)");
			System.out.println("<opnd_2> - replication degree (for backup sub_protocol)");
			System.exit(1);

		}

			

		new Client(args);

	}

	public Client(String[] args) {


		String ap, protocol;

		ap = args[0];
		protocol = args[1];
				
		String[] apComps = ap.split("/");
				
		String host = apComps[0];
		String remoteObject = apComps[1];
				
		try {
			
			registry = LocateRegistry.getRegistry(host);
			
			stub = (Protocol) registry.lookup(remoteObject);
			
						
			SUB_PROTOCOL_TYPE subProtocol = SUB_PROTOCOL_TYPE.type(protocol); 
			
			
			switch (subProtocol) {
			case BACKUP:
				
				String op1 = args[2];
				String op2 = args[3];
				File file = new File(op1);
				
				this.sendData(op1);
				
				Path path = Paths.get(file.getAbsolutePath());
				
				BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
				
				FileTime lastModifiedTime = attributes.lastModifiedTime();
				
				stub.backup(file.getName(), Integer.parseInt(op2), lastModifiedTime.toString());
								
				break;

			case DELETE:
				
				break;

			case STATE:
				System.out.println(stub.showServiceState());
				break;

			default:
				break;
			}
			
			
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}

	}
	
	private void sendData(String filePath) {
		
		File file = new File(filePath);
		
		try {
			
			FileInputStream input = new FileInputStream(file);
			
			
			byte[] buffer = new byte[RMI_CHUNK];
			
			int bytesRead;

			while((bytesRead = input.read(buffer)) > 0) {
				
				stub.receiveData(file.getName(), bytesRead, buffer);

			}
			



			input.close();
			
						
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	

}
