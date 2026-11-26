import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public class RemoteServiceImpl extends UnicastRemoteObject implements RemoteService {
    private static final long serialVersionUID = 1L;
    private static final long CLOCK_TOLERANCE_MS = 5000L;

    public RemoteServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public ServiceResponse handleRequest(String serviceName, String dobText,
                                         long clientTimestampMs) throws RemoteException {
        long now = System.currentTimeMillis();
        String serverUtc = Instant.ofEpochMilli(now).toString();

        // Reject future timestamps and timestamps more than five seconds away.
        if (clientTimestampMs > now || Math.abs(now - clientTimestampMs) > CLOCK_TOLERANCE_MS) {
            return new ServiceResponse(
                    false,
                    "System time is incorrect. Please update your request time and resend.",
                    now, serverUtc, null, null
            );
        }

        if (!"age".equalsIgnoreCase(serviceName)) {
            return new ServiceResponse(true, "Time validated.",
                    now, serverUtc, null, "Unknown service. Supported service: age");
        }

        try {
            LocalDate dob = LocalDate.parse(dobText);
            LocalDate today = LocalDate.now();
            if (dob.isAfter(today)) {
                return new ServiceResponse(true, "Time validated.",
                        now, serverUtc, null, "Date of birth cannot be in the future.");
            }

            int age = Period.between(dob, today).getYears();
            return new ServiceResponse(
                    true,
                    "Client time validated. Request executed successfully.",
                    now, serverUtc, "Calculated age: " + age + " years", null
            );
        } catch (DateTimeParseException ex) {
            return new ServiceResponse(true, "Time validated.",
                    now, serverUtc, null, "DOB must use YYYY-MM-DD format.");
        }
    }
}
