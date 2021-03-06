package backup;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

public interface Protocol extends Remote {
	
	public static final String PROTOCOL = "peer";
	public static enum SUB_PROTOCOL_TYPE {
		
		BACKUP("BACKUP"),
		RESTORE("RESTORE"),
		DELETE("DELETE"),
		RECLAIM("RECLAIM"),
		STATE("STATE");
		
		public String name;
		
		private SUB_PROTOCOL_TYPE(String name) {
			this.name = name;
		}
		
		static final ArrayList<String> types = new ArrayList<String>(Arrays.asList("BACKUP","RESTORE","DELETE","RECLAIM","STATE"));
		
		static SUB_PROTOCOL_TYPE type(String text) {
			
			switch(types.indexOf(text)) {
			
				case 0:
					return SUB_PROTOCOL_TYPE.BACKUP;
				case 1:
					return SUB_PROTOCOL_TYPE.RESTORE;
				case 2:
					return SUB_PROTOCOL_TYPE.DELETE;
				case 3:
					return SUB_PROTOCOL_TYPE.RECLAIM;
				case 4:
					return SUB_PROTOCOL_TYPE.STATE;
				default:
					throw new IllegalArgumentException(text);
			
			}
					
		}
		
	}
	
    String backup(String filePath, int replicationDegree) throws RemoteException;
	String delete(String fileName) throws RemoteException;
	String showServiceState() throws RemoteException;
	String restore(String fileName) throws RemoteException;
	String reclaim(int maximumDiskSpace) throws RemoteException;
}
