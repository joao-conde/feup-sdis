package backup;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import backup.Protocol.SUB_PROTOCOL_TYPE;

public class Client {

	public static void main(String[] args) {

		if (args.length < 3) {

			System.out.println("Usage:\n\"<peer_ap> <sub_protocol> <opnd_1> <opnd_2>\n");
			System.out.println("<peer_ap> - //host:port/name");
			System.out.println("<sub_protocol> - BACKUP | RESTORE | DELETE | RECLAIM | STATE");
			System.out.println("<opnd_1> - file_name (backup, restore, delete) | Space to reclaim (reclaim)");
			System.out.println("<opnd_2> - replication degree (for backup sub_protocol)");
			System.exit(1);

		}

		String accessPoint = args[0];
		String protocol = args[1];
		String op1 = args[2];
		String op2 = args[3];

		new Client(accessPoint, protocol, op1, op2);

	}

	public Client(String ap, String protocol, String op1, String op2) {

		String[] apComps = ap.split("/");
				
		String host = apComps[0];
		String remoteObject = apComps[1];
		
		System.out.println(host);
		System.out.println(remoteObject);
		System.out.println(op1);
		System.out.println(op2);
		
		
		try {
			
			Registry registry = LocateRegistry.getRegistry(host);
			
			Protocol stub = (Protocol) registry.lookup(remoteObject);
			
						
			SUB_PROTOCOL_TYPE subProtocol = SUB_PROTOCOL_TYPE.type(protocol); 
			
			
			switch (subProtocol) {
			case BACKUP:
				
				stub.backup(op1, Integer.parseInt(op2));
				
				break;

			default:
				break;
			}
			
			
			


		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}

	}

}
