#CH.SC.U4CSE24106
import socket
import threading

HOST = "127.0.0.1"
PORT = 5001

def receive(sock):
    try:
        while True:
            data = sock.recv(4096)
            if not data:
                print("\n[Server closed the connection]")
                break
            print(data.decode("utf-8", "replace"), end="", flush=True)
    except OSError:
        pass

def main():
    host = input(f"Chatbot server IP [{HOST}]: ").strip() or HOST
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((host, PORT))
        print(sock.recv(1024).decode("utf-8", "replace"), end="")
        sock.sendall((input().strip() or "Guest").encode())
        threading.Thread(target=receive, args=(sock,), daemon=True).start()
        print("Type messages. Use /quit to disconnect.")
        while True:
            msg = input()
            sock.sendall((msg + "\n").encode())
            if msg.lower() == "/quit":
                break
    except OSError as exc:
        print(f"Connection error: {exc}")
    finally:
        sock.close()

if __name__ == "__main__":
    main()
