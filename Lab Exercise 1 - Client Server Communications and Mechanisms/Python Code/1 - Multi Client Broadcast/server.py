#CH.SC.U4CSE24106
import socket
import threading

HOST = "0.0.0.0"
PORT = 5000
clients = {}
lock = threading.Lock()

def broadcast(message, sender=None):
    dead = []
    with lock:
        for conn in list(clients):
            if conn is not sender:
                try:
                    conn.sendall(message)
                except OSError:
                    dead.append(conn)
        for conn in dead:
            clients.pop(conn, None)
            try:
                conn.close()
            except OSError:
                pass

def handle_client(conn, addr):
    name = f"{addr[0]}:{addr[1]}"
    try:
        conn.sendall(b"Enter your name: ")
        data = conn.recv(1024)
        if not data:
            return
        name = data.decode("utf-8", "replace").strip()[:30] or name
        with lock:
            clients[conn] = name
        print(f"[+] {name} connected ({addr})")
        broadcast(f"[SERVER] {name} joined the chat.\n".encode(), conn)
        while True:
            data = conn.recv(4096)
            if not data:
                break
            msg = data.decode("utf-8", "replace").strip()
            if msg.lower() == "/quit":
                break
            if msg:
                print(f"{name}: {msg}")
                broadcast(f"{name}: {msg}\n".encode(), conn)
    except (ConnectionError, OSError):
        pass
    finally:
        with lock:
            clients.pop(conn, None)
        try:
            conn.close()
        except OSError:
            pass
        print(f"[-] {name} disconnected")
        broadcast(f"[SERVER] {name} left the chat.\n".encode())

def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((HOST, PORT))
    server.listen()
    print(f"Broadcast chat server listening on port {PORT}")
    try:
        while True:
            conn, addr = server.accept()
            threading.Thread(target=handle_client, args=(conn, addr), daemon=True).start()
    except KeyboardInterrupt:
        print("\nShutting down...")
    finally:
        server.close()

if __name__ == "__main__":
    main()
