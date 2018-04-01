package backup;

import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
			System.out.println("<peer_ap> - host/peer-n");
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
			
			String op1, op2;
			switch (subProtocol) {
			case BACKUP:
				
				op1 = args[2];
				op2 = args[3];
				
				//this.sendData(op1);
				
						
				System.out.println(stub.backup(op1, Integer.parseInt(op2)));
								
				break;

			case DELETE:
				op1 = args[2];
				File fileToDelete = new File(op1);

				System.out.println(stub.delete(fileToDelete.getName()));

				break;

			case STATE:
				System.out.println(stub.showServiceState());
				break;
				
			case RESTORE:
				
				op1 = args[2];				
				System.out.println(stub.restore(op1));
				
				break;

			case RECLAIM:

				op1 = args[2];
				System.out.println(stub.reclaim(Integer.parseInt(op1)));
				break;

			default:
				break;
			}
			
			
		} catch (RemoteException | NumberFormatException | NotBoundException e) {
			System.out.println(e.getLocalizedMessage());

		}

	}
	
}
