import socket
import threading
import struct
import sys

class Client:
    def __init__(self, nick):
        self.HOST = "127.0.0.1"
        self.PORT = 9009
        self.MULTICAST_GROUP = "224.1.1.1"
        self.MULTICAST_PORT = 5446
        self.nick = nick
        self.running = True
        self.tcp_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.multicast_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.connect()
        self.run()
    
    def connect(self):
        self.tcp_socket.connect((self.HOST, self.PORT))
        print(f"TCP Polaczono ")
        
        self.udp_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.udp_socket.bind(("", 0))
        self.udp_socket.sendto(b"-", (self.HOST, self.PORT))
        print(f"UDP Zarejestrowano")
        
        self.multicast_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.multicast_socket.bind(("", self.MULTICAST_PORT))
        mreq = struct.pack("4sl", socket.inet_aton(self.MULTICAST_GROUP), socket.INADDR_ANY)
        self.multicast_socket.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)
        self.multicast_socket.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
        print(f"MULTICAST Dolaczono do grupy")
    
    def receive_tcp(self):
        while self.running:
            try:
                data = self.tcp_socket.recv(4096)
                if data:
                    print(f"\nTCP {data.decode('utf-8')}")
                    print(f"{self.nick}> ", end="", flush=True)
                else: break
            except: break
    
    def receive_udp(self):
        while self.running:
            try:
                data, _ = self.udp_socket.recvfrom(65535)
                print(f"\nUDP\n{data.decode('utf-8')}")
                print(f"{self.nick}> ", end="", flush=True)
            except:  break
    
    def receive_multicast(self):
        while self.running:
            try:
                data, _ = self.multicast_socket.recvfrom(65535)
                msg = data.decode('utf-8')
                if not msg.startswith(f"[{self.nick}]"):
                    print(f"\nMULTICAST {msg}")
                    print(f"{self.nick}> ", end="", flush=True)
            except: break
    
    def run(self):
        threading.Thread(target=self.receive_tcp, daemon=True).start()
        threading.Thread(target=self.receive_udp, daemon=True).start()
        threading.Thread(target=self.receive_multicast, daemon=True).start()

        try:
            while self.running:
                msg = input(f"{self.nick}> ")
                
                if not msg: continue
                
                elif msg.upper() == "U":
                    art = """
*****
 ***
  * 
 *** 
*****
"""
                    self.udp_socket.sendto(f"[{self.nick}]\n{art}".encode('utf-8'), (self.HOST, self.PORT))
                elif msg.upper().startswith("M "):
                    multicast_msg = f"[{self.nick}] {msg[2:]}"
                    self.multicast_socket.sendto(multicast_msg.encode('utf-8'), (self.MULTICAST_GROUP, self.MULTICAST_PORT))
                else: self.tcp_socket.sendall(f"[{self.nick}] {msg}".encode('utf-8'))
        
        except KeyboardInterrupt: pass
        finally:
            self.tcp_socket.close()
            self.udp_socket.close()
            self.multicast_socket.close()

# running client
nick = sys.argv[1] if len(sys.argv) > 1 else input("Nick: ").strip() or "User"
Client(nick)