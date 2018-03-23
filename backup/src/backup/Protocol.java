package backup;

import java.rmi.Remote;
import java.rmi.RemoteException;


//Peer must implement this interface and the required methods to be called via RMI by the test client app
public interface Hello extends Remote {
    String backup() throws RemoteException;
    String restore() throws RemoteException;
    String delete() throws RemoteException;
}
