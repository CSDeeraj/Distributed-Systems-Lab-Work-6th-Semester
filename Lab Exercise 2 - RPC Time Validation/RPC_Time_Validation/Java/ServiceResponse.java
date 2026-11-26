import java.io.Serializable;

public class ServiceResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    public final boolean accepted;
    public final String notification;
    public final long serverTimeMs;
    public final String serverTimeUtc;
    public final String result;
    public final String error;

    public ServiceResponse(boolean accepted, String notification, long serverTimeMs,
                           String serverTimeUtc, String result, String error) {
        this.accepted = accepted;
        this.notification = notification;
        this.serverTimeMs = serverTimeMs;
        this.serverTimeUtc = serverTimeUtc;
        this.result = result;
        this.error = error;
    }
}
