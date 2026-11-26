import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Instant;
import java.util.Scanner;

public class RPCClient {
    private static long currentTimeMs() {
        return System.currentTimeMillis();
    }

    private static void printResponse(String label, ServiceResponse response) {
        System.out.println("\n" + label);
        System.out.println("Accepted: " + response.accepted);
        if (response.notification != null) {
            System.out.println("Notification: " + response.notification);
        }
        if (response.serverTimeUtc != null) {
            System.out.println("Correct server time (UTC): " + response.serverTimeUtc);
        }
        if (response.result != null) {
            System.out.println("Result: " + response.result);
        }
        if (response.error != null) {
            System.out.println("Error: " + response.error);
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter date of birth (YYYY-MM-DD): ");
            String dob = scanner.nextLine().trim();

            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            RemoteService service =
                    (RemoteService) registry.lookup("TimeValidationService");

            // Deliberately incorrect: set the request timestamp 24 hours in the future.
            long dummyTimestamp = currentTimeMs() + 24L * 60L * 60L * 1000L;
            System.out.println("\nSending first request with an intentionally incorrect future time...");
            ServiceResponse response = service.handleRequest("age", dob, dummyTimestamp);
            printResponse("Server response 1:", response);

            if (!response.accepted) {
                // Update the RPC request timestamp, not the operating-system clock.
                long correctedTimestamp = response.serverTimeMs;
                System.out.println("\nClient updated its request timestamp using server time.");
                System.out.println("Resending the original service request...");

                response = service.handleRequest("age", dob, correctedTimestamp);
                printResponse("Server response 2:", response);
            }

            if (!response.accepted) {
                System.out.println("\nRequest was rejected again. Check network delay and system clocks.");
            }
        } catch (Exception e) {
            System.err.println("RPC client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
