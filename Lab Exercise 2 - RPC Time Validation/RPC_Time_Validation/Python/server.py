from datetime import date, datetime, timezone
from socketserver import ThreadingMixIn
from xmlrpc.server import SimpleXMLRPCServer

CLOCK_TOLERANCE_MS = 5000


def server_time_ms():
    return float(datetime.now(timezone.utc).timestamp() * 1000)


def calculate_age(dob_text):
    try:
        dob = date.fromisoformat(dob_text)
    except ValueError:
        raise ValueError("DOB must use YYYY-MM-DD format.")

    today = datetime.now().date()
    if dob > today:
        raise ValueError("Date of birth cannot be in the future.")

    age = today.year - dob.year
    if (today.month, today.day) < (dob.month, dob.day):
        age -= 1
    return age


def handle_request(service_name, dob_text, client_timestamp_ms):
    now_ms = server_time_ms()

    # Reject all future timestamps and timestamps outside the allowed clock skew.
    if client_timestamp_ms > now_ms or abs(now_ms - client_timestamp_ms) > CLOCK_TOLERANCE_MS:
        return {
            "accepted": False,
            "notification": "System time is incorrect. Please update your request time and resend.",
            "server_time_ms": now_ms,
            "server_time_utc": datetime.fromtimestamp(
                now_ms / 1000, tz=timezone.utc
            ).isoformat(),
        }

    try:
        if service_name == "age":
            result = calculate_age(dob_text)
            result_text = f"Calculated age: {result} years"
        else:
            return {
                "accepted": True,
                "server_time_ms": now_ms,
                "error": "Unknown service. Supported service: age",
            }
    except ValueError as exc:
        return {
            "accepted": True,
            "server_time_ms": now_ms,
            "error": str(exc),
        }

    return {
        "accepted": True,
        "notification": "Client time validated. Request executed successfully.",
        "server_time_ms": now_ms,
        "result": result,
        "result_text": result_text,
    }


class ThreadedXMLRPCServer(ThreadingMixIn, SimpleXMLRPCServer):
    daemon_threads = True
    allow_reuse_address = True


if __name__ == "__main__":
    host, port = "127.0.0.1", 8001
    with ThreadedXMLRPCServer((host, port), allow_none=True) as server:
        server.register_function(handle_request, "handle_request")
        print(f"Time-validation RPC server running at http://{host}:{port}")
        print("Press Ctrl+C to stop.")
        server.serve_forever()
