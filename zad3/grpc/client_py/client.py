import sys
import os
import grpc

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "gen"))
import echo_pb2
import echo_pb2_grpc


def main():
    with open("certs/ca.crt", "rb") as f:
        ca = f.read()
    creds = grpc.ssl_channel_credentials(root_certificates=ca)
    #creds = grpc.ssl_channel_credentials(root_certificates=open("certs/bad-ca.crt","rb").read())
    with grpc.secure_channel("localhost:50443", creds) as ch:
        stub = echo_pb2_grpc.EchoStub(ch)
        for msg in ["hello", "świat", "TLS!"]:
            r = stub.Ping(echo_pb2.Msg(text=msg))
            print(f"server -> {r.text!r}")


if __name__ == "__main__":
    main()
