import threading
import socket


class Server:
    def __init__(self):
        self.HOST = "127.0.0.1"
        self.PORT = 9009
        self.tcp_clients = {}
        self.lock = threading.Lock()
        tcp_thread = threading.Thread(target=self.handle_tcp_connections, daemon=True)
        tcp_thread.start()
        self.running = True

        try:
            self.handle_udp()
        except KeyboardInterrupt:
            print("\n[SERWER] Wylaczono..")
            self.running = False
    
    def handle_tcp_connections(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as tcp_socket:
            tcp_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            tcp_socket.bind((self.HOST, self.PORT))
            tcp_socket.listen()
            print(f"TCP Serwer jest na {self.HOST}:{self.PORT}")
            
            while self.running:
                try:
                    tcp_socket.settimeout(1.0)
                    conn, addr = tcp_socket.accept()
                    with self.lock: self.tcp_clients[addr] = conn
                    print(f"TCP Polaczono {addr}")
                    
                    client_thread = threading.Thread(target=self.handle_tcp_client, args=(conn, addr), daemon=True)
                    client_thread.start()
                except socket.timeout: continue
                except: break
    
    def handle_tcp_client(self, conn, addr):
        try:
            while self.running:
                data = conn.recv(4096)
                if not data: break
                with self.lock:
                    for client_addr, client_conn in self.tcp_clients.items():
                        if client_addr != addr:
                            try: client_conn.sendall(data)
                            except: pass
        except: pass
        finally:
            with self.lock:
                if addr in self.tcp_clients: del self.tcp_clients[addr]
            conn.close()
            print(f"TCP Rozlaczono {addr}")
    
    def handle_udp(self):
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as udp_socket:
            udp_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            udp_socket.bind((self.HOST, self.PORT))
            udp_socket.settimeout(1.0)
            print(f"UDP Serwer czeka na {self.HOST}:{self.PORT}")
            udp_clients = set()
            
            while self.running:
                try:
                    data, addr = udp_socket.recvfrom(65535)
                    if data == b"-":
                        udp_clients.add(addr)
                        print(f"UDP Dodano: {addr}")
                    else:
                        for client_addr in udp_clients:
                            if client_addr != addr:
                                try: udp_socket.sendto(data, client_addr)
                                except: pass
                except socket.timeout: continue
                except: break
# running server
Server()