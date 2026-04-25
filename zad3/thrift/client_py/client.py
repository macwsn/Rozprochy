import sys
import os
import argparse

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "gen"))

from thrift.transport import TSocket, TTransport
from thrift.protocol import TBinaryProtocol
from thrift.protocol.TMultiplexedProtocol import TMultiplexedProtocol
from thrift.Thrift import TException
from thrift.transport.TSSLSocket import TSSLSocket

from smarthome import FridgeService, OvenService, CameraService
from smarthome.ttypes import (FridgeMode, OvenMode, PtzPosition,NotFound, InvalidArgs, Unsupported)

FRIDGE_MODES = {v: k for k, v in FridgeMode._VALUES_TO_NAMES.items()}
OVEN_MODES = {v: k for k, v in OvenMode._VALUES_TO_NAMES.items()}


class ServerConn:
    def __init__(self, host, port, tls=False, ca=None):
        self.host = host
        self.port = port
        self.tls = tls
        self.ca = ca
        self.transport = None
        self.fridge = None
        self.oven = None
        self.camera = None
        self._connect()

    def _connect(self):
        if self.transport and self.transport.isOpen():
            self.transport.close()
        if self.tls:
            sock = TSSLSocket(self.host, self.port, ca_certs=self.ca, validate=True)
        else:
            sock = TSocket.TSocket(self.host, self.port)
        sock.setTimeout(2000)
        self.transport = TTransport.TBufferedTransport(sock)
        proto = TBinaryProtocol.TBinaryProtocol(self.transport)
        self.fridge = FridgeService.Client(TMultiplexedProtocol(proto, "fridge"))
        self.oven   = OvenService.Client(TMultiplexedProtocol(proto, "oven"))
        self.camera = CameraService.Client(TMultiplexedProtocol(proto, "camera"))
        self.transport.open()

    def close(self):
        if self.transport:
            self.transport.close()


def list_all(servers):
    index = {}
    for s in servers:
        try:
            if not s.transport.isOpen():
                s._connect()
            for d in s.fridge.listDevices(): index[d.id] = (s, "fridge")
            for d in s.oven.listDevices():   index[d.id] = (s, "oven")
            for d in s.camera.listDevices(): index[d.id] = (s, "camera")
        except TException:
            if s.transport.isOpen():
                s.transport.close()
            print(f"  warning: {s.host}:{s.port} unreachable")
    return index


HELP = """\
Commands:
  list
  get <id>
  fridge-mode <id> <OFF|ON|ECO|PARTY>
  fridge-temp <id> <celsius>
  fridge-add  <id> <item>
  fridge-rm   <id> <item>
  oven <id> <OFF|BAKE|GRILL|PREHEAT> <celsius> <minutes>
  recording <id> <on|off>
  ptz <id> <pan> <tilt> <zoom>
  x  — exit"""


def repl(servers):
    print(HELP)
    while True:
        try: line = input("> ").strip()
        except EOFError: break
        if not line: continue
        cmd = line.split()
        if cmd[0] == "x": break

        try:
            index = list_all(servers)

            if cmd[0] == "list":
                for did, (_, kind) in sorted(index.items()):
                    print(f"  {did:<20} {kind}")

            elif cmd[0] == "get":
                s, kind = index[cmd[1]]
                client = getattr(s, kind)
                print(client.read(cmd[1]))

            elif cmd[0] == "fridge-mode":
                did, mode_name = cmd[1], cmd[2].upper()
                m = FridgeMode._NAMES_TO_VALUES[mode_name]
                print(index[did][0].fridge.setMode(did, m))

            elif cmd[0] == "fridge-temp":
                did, t = cmd[1], float(cmd[2])
                print(index[did][0].fridge.setTemp(did, t))

            elif cmd[0] == "fridge-add":
                did, item = cmd[1], cmd[2]
                print(index[did][0].fridge.addItem(did, item))

            elif cmd[0] == "fridge-rm":
                did, item = cmd[1], cmd[2]
                print(index[did][0].fridge.removeItem(did, item))

            elif cmd[0] == "oven":
                did, mode_name = cmd[1], cmd[2].upper()
                t, minutes = float(cmd[3]), int(cmd[4])
                m = OvenMode._NAMES_TO_VALUES[mode_name]
                print(index[did][0].oven.setMode(did, m, t, minutes))

            elif cmd[0] == "recording":
                did, on = cmd[1], cmd[2].lower() == "on"
                print(index[did][0].camera.setRecording(did, on))

            elif cmd[0] == "ptz":
                did = cmd[1]
                pos = PtzPosition(float(cmd[2]), float(cmd[3]), float(cmd[4]))
                print(index[did][0].camera.movePtz(did, pos))

            else: print(f"Unknown command. Type 'list' or see help above.")

        except NotFound as e:
            print(f"! NotFound: {e.id}")
        except InvalidArgs as e:
            print(f"! InvalidArgs: {e.field} — {e.reason}")
        except Unsupported as e:
            print(f"! Unsupported: {e.op} — {e.reason}")
        except (KeyError, IndexError):
            print(f"! Unknown device id or missing argument")
        except TException as e:
            print(f"! Transport error: {e}")
        except ValueError as e:
            print(f"! Bad argument: {e}")


def main():
    ap = argparse.ArgumentParser(description="SmartHome Thrift client")
    ap.add_argument("--servers", default="127.0.0.1:9090,127.0.0.1:9091",  help="comma-separated host:port list")
    ap.add_argument("--tls", action="store_true", help="use TLS")
    ap.add_argument("--ca", default="certs/ca.crt", help="CA certificate path (TLS)")
    args = ap.parse_args()

    servers = []
    for hp in args.servers.split(","):
        host, port = hp.strip().rsplit(":", 1)
        servers.append(ServerConn(host, int(port), tls=args.tls, ca=args.ca))

    try: repl(servers)
    finally:
        for s in servers: s.close()

if __name__ == "__main__": main()