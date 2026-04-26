import struct
import sys
import Ice
_ENCAPS_HDR_LEN = 6
_ENCODING_1_1 = bytes([1, 1])


def _encode_size(n: int) -> bytes:
    if n < 255:
        return bytes([n])
    return bytes([255]) + struct.pack("<I", n)


def _decode_size(buf: bytes, off: int) -> tuple[int, int]:
    n = buf[off]; off += 1
    if n == 255:
        n = struct.unpack_from("<I", buf, off)[0]; off += 4
    return n, off


def _encode_string(s: str) -> bytes:
    b = s.encode("utf-8")
    return _encode_size(len(b)) + b


def _decode_string(buf: bytes, off: int) -> tuple[str, int]:
    n, off = _decode_size(buf, off)
    return buf[off:off + n].decode("utf-8"), off + n


def _wrap_encaps(payload: bytes) -> bytes:
    return struct.pack("<I", len(payload) + _ENCAPS_HDR_LEN) + _ENCODING_1_1 + payload


def _unwrap_encaps(payload: bytes) -> bytes: return bytes(payload)[_ENCAPS_HDR_LEN:]


def call_read(prx) -> dict:
    """`State read()` — no args, returns struct {string label, long counter, double value}."""
    ok, payload = prx.ice_invoke("read", Ice.OperationMode.Normal, _wrap_encaps(b""))
    if not ok:
        raise RuntimeError("user exception from read()")
    body = _unwrap_encaps(payload)
    label, off = _decode_string(body, 0)
    counter = struct.unpack_from("<q", body, off)[0]; off += 8
    value = struct.unpack_from("<d", body, off)[0]
    return {"label": label, "counter": counter, "value": value}


def call_inc(prx, delta: int) -> None:
    """`void inc(long delta)` — long arg, no return."""
    payload = _wrap_encaps(struct.pack("<q", delta))
    ok, _ = prx.ice_invoke("inc", Ice.OperationMode.Normal, payload)
    if not ok:
        raise RuntimeError("user exception from inc()")


def call_set_label(prx, s: str) -> None:
    """`void setLabel(string s)` — string arg, no return."""
    payload = _wrap_encaps(_encode_string(s))
    ok, _ = prx.ice_invoke("setLabel", Ice.OperationMode.Normal, payload)
    if not ok:
        raise RuntimeError("user exception from setLabel()")


# ---------- driver ----------

ENDPOINT = "tcp -h 127.0.0.1 -p 10010"

HELP = """\
Dynamic-invocation REPL (no slice2py stubs).
Commands:
  read  <cat>/<name>
  inc   <cat>/<name> <delta>
  label <cat>/<name> <text>
  demo  <cat>/<name>            — runs read/inc/setLabel/read in sequence
  x  — exit"""


def _proxy(comm, ident: str):
    return comm.stringToProxy(f"{ident}:{ENDPOINT}")


def repl(comm) -> None:
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
                print(call_read(_proxy(comm, cmd[1])))
            elif cmd[0] == "inc":
                call_inc(_proxy(comm, cmd[1]), int(cmd[2]))
                print("ok")
            elif cmd[0] == "label":
                call_set_label(_proxy(comm, cmd[1]), " ".join(cmd[2:]))
                print("ok")
            elif cmd[0] == "demo":
                prx = _proxy(comm, cmd[1])
                print("BEFORE:", call_read(prx))
                call_inc(prx, 5)
                call_set_label(prx, "set-by-dynamic")
                print("AFTER :", call_read(prx))
            else:
                print(f"unknown command: {cmd[0]}")
        except (IndexError, ValueError) as e:
            print(f"! bad arguments: {e}")
        except Ice.Exception as e:
            print(f"! Ice: {e}")


def main():
    with Ice.initialize(sys.argv) as comm:
        if "--demo" in sys.argv:
            prx = _proxy(comm, "dedicated/c01")
            print("BEFORE:", call_read(prx))
            call_inc(prx, 5)
            call_set_label(prx, "set-by-dynamic")
            print("AFTER :", call_read(prx))
        else:
            repl(comm)


if __name__ == "__main__": main()