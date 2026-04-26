import sys
import os
import grpc

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "gen"))
import smarthome_pb2 as pb
import smarthome_pb2_grpc as rpc
from google.protobuf.empty_pb2 import Empty

HELP = """\
SmartHome gRPC client (via gateway)
Commands:
  list
  get-fridge  <id>
  fridge-mode <id> <0=OFF|1=ON|2=ECO|3=PARTY>
  get-oven    <id>
  set-oven    <id> <0=OFF|1=BAKE|2=GRILL|3=PREHEAT> <tempC> <minutes>
  get-camera  <id>
  recording   <id> <on|off>
  ptz         <id> <pan> <tilt> <zoom>
  x  — exit"""


def make_channel(host="localhost", port=50444, ca="certs/ca.crt", tls=True):
    if tls:
        with open(ca, "rb") as f:
            creds = grpc.ssl_channel_credentials(root_certificates=f.read())
        return grpc.secure_channel(f"{host}:{port}", creds)
    return grpc.insecure_channel(f"{host}:{port}")


def repl(stub):
    print(HELP)
    while True:
        try:
            line = input("> ").strip()
        except EOFError:
            break
        if not line:
            continue
        cmd = line.split()
        if cmd[0] == "x":
            break
        try:
            if cmd[0] == "list":
                r = stub.ListDevices(Empty())
                for d in r.devices:
                    print(f"  {d.id:<20} {pb.DeviceKind.Name(d.kind):<8} {d.subtype} ({d.room})")

            elif cmd[0] == "get-fridge":
                print(stub.GetFridge(pb.DeviceId(id=cmd[1])))

            elif cmd[0] == "fridge-mode":
                print(stub.SetFridgeMode(pb.SetFridgeModeRequest(id=cmd[1], mode=int(cmd[2]))))

            elif cmd[0] == "get-oven":
                print(stub.GetOven(pb.DeviceId(id=cmd[1])))

            elif cmd[0] == "set-oven":
                print(stub.SetOven(pb.SetOvenRequest(
                    id=cmd[1], mode=int(cmd[2]),
                    temp_c=float(cmd[3]), minutes=int(cmd[4]))))

            elif cmd[0] == "get-camera":
                print(stub.GetCamera(pb.DeviceId(id=cmd[1])))

            elif cmd[0] == "recording":
                print(stub.SetRecording(pb.SetRecordingRequest(id=cmd[1], on=cmd[2].lower() == "on")))

            elif cmd[0] == "ptz":
                pos = pb.PtzPosition(pan=float(cmd[2]), tilt=float(cmd[3]), zoom=float(cmd[4]))
                print(stub.MovePtz(pb.MovePtzRequest(id=cmd[1], ptz=pos)))

            else:
                print(f"unknown command: {cmd[0]}")

        except grpc.RpcError as e:
            print(f"! gRPC {e.code().name}: {e.details()}")
        except (IndexError, ValueError) as e:
            print(f"! bad arguments: {e}")


def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="localhost")
    ap.add_argument("--port", type=int, default=50444)
    ap.add_argument("--ca", default="certs/ca.crt")
    ap.add_argument("--no-tls", action="store_true")
    args = ap.parse_args()

    with make_channel(args.host, args.port, args.ca, not args.no_tls) as ch:
        repl(rpc.SmartHomeStub(ch))


if __name__ == "__main__":
    main()
