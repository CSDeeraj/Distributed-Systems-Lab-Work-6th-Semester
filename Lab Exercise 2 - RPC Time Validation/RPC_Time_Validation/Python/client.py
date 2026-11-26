from datetime import datetime, timezone
import time
import xmlrpc.client

SERVER_URL = "http://127.0.0.1:8001"


def current_time_ms():
    return float(time.time() * 1000)


def show_response(label, response):
    print(f"\n{label}")
    print("Accepted:", response.get("accepted"))
    print("Notification:", response.get("notification", ""))
    if "server_time_utc" in response:
        print("Correct server time (UTC):", response["server_time_utc"])
    if "result_text" in response:
        print("Result:", response["result_text"])
    if "error" in response:
        print("Error:", response["error"])


def main():
    dob = input("Enter date of birth (YYYY-MM-DD): ").strip()

    with xmlrpc.client.ServerProxy(SERVER_URL, allow_none=True) as rpc:
        # Deliberately incorrect: set the client's request timestamp 24 hours ahead.
        dummy_timestamp = current_time_ms() + 24 * 60 * 60 * 1000
        print("\nSending first request with an intentionally incorrect future time...")
        response = rpc.handle_request("age", dob, dummy_timestamp)
        show_response("Server response 1:", response)

        if not response.get("accepted"):
            # Update the timestamp used in the RPC request. Do not change the OS clock.
            corrected_timestamp = response["server_time_ms"]
            print("\nClient updated its request timestamp using server time.")
            print("Resending the original service request...")

            response = rpc.handle_request("age", dob, corrected_timestamp)
            show_response("Server response 2:", response)

        if not response.get("accepted"):
            print("\nRequest was rejected again. Check network delay and system clocks.")


if __name__ == "__main__":
    main()
