import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteService extends Remote {
    ServiceResponse handleRequest(String serviceName, String dob, long clientTimestampMs)
            throws RemoteException;
}
