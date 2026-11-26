# RPC Distributed Application: Client Time Validation

This project implements the same time-validation workflow independently in Python and Java.

## Required workflow

1. The client sends a service request with an intentionally incorrect future timestamp.
2. The RPC server compares the supplied timestamp with its current system time.
3. The server rejects the request if the timestamp is in the future or differs from server time by more than 5 seconds.
4. The rejection response contains a notification and the server's current timestamp.
5. The client updates its **request timestamp** using the server-provided time and resends the same service request.
6. The server validates the corrected timestamp, performs the requested computation, and returns the result.

The demo service calculates age from a date of birth (`YYYY-MM-DD`).

> The client updates the timestamp it sends in the RPC request. The program does not change the computer's operating-system clock; changing OS time would require elevated privileges and is not appropriate for this demonstration.

## Python implementation

Requirements: Python 3.9+; standard library only.

Terminal 1:
```bash
cd Python
python server.py
```

Terminal 2:
```bash
cd Python
python client.py
```

Expected sequence:
- First response: `accepted = false`, with an incorrect-time notification and server time.
- Client updates its request timestamp.
- Second response: `accepted = true`, with calculated age.

## Java implementation

Requirements: JDK 11+.

Terminal 1:
```bash
cd Java
javac *.java
java RPCServer
```

Terminal 2:
```bash
cd Java
javac *.java
java RPCClient
```

Java uses Java RMI. The server creates an RMI registry on port 1099.

## Validation rule

The server rejects a request when:
- `clientTimestamp > serverTimestamp` (future timestamp), OR
- absolute clock difference is greater than 5,000 milliseconds.

A corrected timestamp returned by the server is therefore accepted, provided it is resent promptly.

## Notes

- Python uses XML-RPC over HTTP.
- Java uses Java RMI.
- Each implementation is self-contained; Python and Java do not communicate with one another.
- Run the client and server in separate terminals on the same machine for the local demonstration. For two different machines, configure the server host/address in the client and allow the relevant port through the firewall.
