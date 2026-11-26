# Demonstration checklist

For each language, start the server in one terminal and the client in another.

1. Enter a valid DOB such as `2004-06-15`.
2. Confirm the client sends a timestamp 24 hours in the future.
3. Confirm the server rejects the first request.
4. Confirm the rejection includes the server's correct UTC time / epoch timestamp.
5. Confirm the client updates its request timestamp and resends.
6. Confirm the second request is accepted and the age result is returned.

The server also rejects stale timestamps more than 5 seconds away from its current time.
The client corrects its RPC request timestamp; it does not alter the machine clock.
