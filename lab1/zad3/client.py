import socket

serverIP = "127.0.0.1"
serverPort = 9009
msg_bytes = (300).to_bytes(4, byteorder='little')
clientSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
clientSocket.sendto(msg_bytes, (serverIP, serverPort))
print("Python sent:", 300)
buff, _ = clientSocket.recvfrom(1024)
number = int.from_bytes(buff, byteorder='little')
print("Python received:", number)
clientSocket.close()