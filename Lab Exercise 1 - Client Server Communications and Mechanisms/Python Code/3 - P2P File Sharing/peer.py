#CH.SC.U4CSE24106
import argparse
import os
import socket
import threading
from pathlib import Path

BUFFER = 64 * 1024

def safe_name(name):
    return Path(name).name

class PeerServer(threading.Thread):
    def __init__(self, host, port, folder):
        super().__init__(daemon=True)
        self.host, self.port, self.folder = host, port, Path(folder)
        self.running = True

    def run(self):
        self.folder.mkdir(parents=True, exist_ok=True)
        srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            srv.bind((self.host, self.port))
            srv.listen()
            print(f"[P2P] Sharing {self.folder.resolve()} on port {self.port}")
            while self.running:
                conn, addr = srv.accept()
                threading.Thread(target=self.handle, args=(conn, addr), daemon=True).start()
        except OSError as exc:
            print(f"[P2P server error] {exc}")
        finally:
            srv.close()

    def handle(self, conn, addr):
        try:
            stream = conn.makefile("rwb")
            command = stream.readline().decode().strip()
            if command == "LIST":
                files = sorted(p.name for p in self.folder.glob("*.txt") if p.is_file())
                stream.write(("\n".join(files) + "\n").encode())
                stream.flush()
            elif command.startswith("GET "):
                name = safe_name(command[4:].strip())
                path = self.folder / name
                if not name.lower().endswith(".txt") or not path.is_file():
                    stream.write(b"ERROR\n")
                    stream.flush()
                else:
                    size = path.stat().st_size
                    stream.write(f"OK {size}\n".encode())
                    stream.flush()
                    with open(path, "rb") as f:
                        while True:
                            chunk = f.read(BUFFER)
                            if not chunk:
                                break
                            stream.write(chunk)
                            stream.flush()
            else:
                stream.write(b"ERROR\n")
                stream.flush()
        except (OSError, ConnectionError):
            pass
        finally:
            conn.close()

def request_list(host, port):
    with socket.create_connection((host, port), timeout=8) as sock:
        sock.sendall(b"LIST\n")
        data = b""
        while True:
            part = sock.recv(4096)
            if not part:
                break
            data += part
        return [x for x in data.decode("utf-8", "replace").splitlines() if x]

def download(host, port, name, destination):
    name = safe_name(name)
    if not name.lower().endswith(".txt"):
        raise ValueError("Only .txt files are allowed.")
    os.makedirs(destination, exist_ok=True)
    with socket.create_connection((host, port), timeout=8) as sock:
        sock.sendall(f"GET {name}\n".encode())
        header = b""
        while not header.endswith(b"\n"):
            part = sock.recv(1)
            if not part:
                raise ConnectionError("Peer closed before sending file header.")
            header += part
        fields = header.decode().strip().split()
        if len(fields) != 2 or fields[0] != "OK":
            raise FileNotFoundError("Peer does not have that file.")
        remaining = int(fields[1])
        target = Path(destination) / name
        with open(target, "wb") as f:
            while remaining:
                chunk = sock.recv(min(BUFFER, remaining))
                if not chunk:
                    raise ConnectionError("Transfer interrupted.")
                f.write(chunk)
                remaining -= len(chunk)
    return target

def main():
    parser = argparse.ArgumentParser(description="Terminal P2P .txt file-sharing peer")
    parser.add_argument("--port", type=int, default=6000, help="port to share on")
    parser.add_argument("--share", default="shared", help="folder containing files to share")
    parser.add_argument("--download", default="downloads", help="download destination")
    args = parser.parse_args()
    Path(args.share).mkdir(exist_ok=True)
    server = PeerServer("0.0.0.0", args.port, args.share)
    server.start()
    print("\nCommands: list HOST PORT | get HOST PORT FILENAME | mine | quit")
    print("Other peers can connect to your IP and port", args.port)
    while True:
        try:
            parts = input("p2p> ").strip().split()
            if not parts:
                continue
            if parts[0] == "quit":
                break
            if parts[0] == "mine":
                print("\n".join(sorted(p.name for p in Path(args.share).glob("*.txt"))) or "(no .txt files)")
            elif parts[0] == "list" and len(parts) == 3:
                files = request_list(parts[1], int(parts[2]))
                print("\n".join(files) if files else "(peer has no .txt files)")
            elif parts[0] == "get" and len(parts) == 4:
                path = download(parts[1], int(parts[2]), parts[3], args.download)
                print(f"Downloaded: {path.resolve()}")
            else:
                print("Usage: list HOST PORT | get HOST PORT FILENAME | mine | quit")
        except Exception as exc:
            print(f"Error: {exc}")
    print("Peer client stopped. Server thread ends when this process exits.")

if __name__ == "__main__":
    main()
