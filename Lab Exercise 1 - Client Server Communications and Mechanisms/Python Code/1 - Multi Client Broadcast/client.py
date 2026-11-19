#CH.SC.U4CSE24106
import socket
import threading

HOST = "127.0.0.1"
PORT = 5000

def receive_messages(sock):
    while True:
        try:
            data = sock.recv(4096)
            if not data:
                print("\n[Disconnected from server]")
                break
            print("\n" + data.decode("utf-8", "replace"), end="")
            print("> ", end="", flush=True)
        except OSError:
            break

def main():
    host = input(f"Server IP [{HOST}]: ").strip() or HOST
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((host, PORT))
        print(sock.recv(1024).decode("utf-8", "replace"), end="")
        name = input().strip() or "Guest"
        sock.sendall(name.encode())
        threading.Thread(target=receive_messages, args=(sock,), daemon=True).start()
        print("Connected. Type /quit to leave.")
        while True:
            msg = input("> ")
            sock.sendall(msg.encode())
            if msg.lower() == "/quit":
                break
    except (OSError, ConnectionError) as exc:
        print(f"Connection error: {exc}")
    finally:
        sock.close()

if __name__ == "__main__":
    main()
