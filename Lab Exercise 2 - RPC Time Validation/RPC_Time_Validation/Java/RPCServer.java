import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RPCServer {
    public static void main(String[] args) {
        try {
            RemoteService service = new RemoteServiceImpl();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("TimeValidationService", service);

            System.out.println("Java RMI time-validation server running on port 1099.");
            System.out.println("Press Ctrl+C to stop.");
        } catch (Exception e) {
            System.err.println("Server startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
