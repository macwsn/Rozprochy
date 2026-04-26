import sys
import os
import Ice

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "gen"))
import Demo

ENDPOINT = "tcp -h 127.0.0.1 -p 10010"

HELP = """\
Commands:
  read  <cat>/<name>
  inc   <cat>/<name> <delta>
  label <cat>/<name> <text>
  cast  <cat>/<name> checked|unchecked
  x  — exit"""


def proxy_for(comm, ident):
    return comm.stringToProxy(f"{ident}:{ENDPOINT}")


def main():
    with Ice.initialize(sys.argv) as comm:
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
                if cmd[0] == "read":
                    p = Demo.CounterPrx.checkedCast(proxy_for(comm, cmd[1]))
                    print(p.read())

                elif cmd[0] == "inc":
                    p = Demo.CounterPrx.checkedCast(proxy_for(comm, cmd[1]))
                    p.inc(int(cmd[2]))

                elif cmd[0] == "label":
                    p = Demo.CounterPrx.checkedCast(proxy_for(comm, cmd[1]))
                    p.setLabel(" ".join(cmd[2:]))

                elif cmd[0] == "cast":
                    raw = proxy_for(comm, cmd[1])
                    if cmd[2] == "checked":
                        p = Demo.CounterPrx.checkedCast(raw)
                        print(f"checkedCast -> {p is not None}")
                    else:
                        p = Demo.CounterPrx.uncheckedCast(raw)
                        print(f"uncheckedCast -> {p}")

                else:
                    print("Unknown command.")

            except Demo.NotFound as e:
                print(f"! NotFound: {e.what}")
            except Ice.Exception as e:
                print(f"! Ice: {e}")


if __name__ == "__main__":
    main()
