#CH.SC.U4CSE24106
import socket
import threading
from datetime import datetime

HOST = "0.0.0.0"
PORT = 5001
MAX_CLIENTS = 30
clients = set()
lock = threading.Lock()

def reply(message):
    msg = message.lower().strip()
    if any(x in msg for x in ("hello", "hi", "hey")):
        return "Hello! How can I help you?"
    if "your name" in msg or "who are you" in msg:
        return "I'm PyBot, a simple server-side chatbot."
    if "time" in msg:
        return "Server time: " + datetime.now().strftime("%H:%M:%S")
    if "date" in msg:
        return "Server date: " + datetime.now().strftime("%d-%m-%Y")
    if "help" in msg:
        return "Try: hello, your name, time, date, or bye."
    if msg in ("bye", "goodbye"):
        return "Goodbye! You can disconnect with /quit."
    if "thank" in msg:
        return "You're welcome!"
    return "I am a simple rule-based bot. Type 'help' to see what I understand."

def handle_client(conn, addr):
    name = f"{addr[0]}:{addr[1]}"
    try:
        conn.sendall(b"Welcome to PyBot. Enter your name: ")
        raw = conn.recv(1024)
        if not raw:
            return
        name = raw.decode("utf-8", "replace").strip()[:30] or name
        print(f"[+] {name} connected")
        conn.sendall(b"PyBot: Hello! Type 'help' for commands. Use /quit to exit.\n")
        buffer = b""
        while True:
            data = conn.recv(2048)
            if not data:
                break
            buffer += data
            while b"\n" in buffer:
                line, buffer = buffer.split(b"\n", 1)
                msg = line.decode("utf-8", "replace").strip()
                if msg.lower() == "/quit":
                    conn.sendall(b"PyBot: Session ended. Goodbye!\n")
                    return
                if msg:
                    print(f"{name}: {msg}")
                    conn.sendall(("PyBot: " + reply(msg) + "\n").encode())
    except (OSError, ConnectionError):
        pass
    finally:
        with lock:
            clients.discard(conn)
        try:
            conn.close()
        except OSError:
            pass
        print(f"[-] {name} disconnected")

def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((HOST, PORT))
    server.listen()
    print(f"PyBot server listening on port {PORT} (maximum {MAX_CLIENTS} clients)")
    try:
        while True:
            conn, addr = server.accept()
            with lock:
                if len(clients) >= MAX_CLIENTS:
                    conn.sendall(b"Server full. Try again later.\n")
                    conn.close()
                    continue
                clients.add(conn)
            threading.Thread(target=handle_client, args=(conn, addr), daemon=True).start()
    except KeyboardInterrupt:
        print("\nStopping PyBot...")
    finally:
        server.close()

if __name__ == "__main__":
    main()
