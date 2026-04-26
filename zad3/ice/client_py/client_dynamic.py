import sys
import struct
import Ice

_ENCAP_HDR = 6
def _encap(data): return struct.pack('<I', len(data) + _ENCAP_HDR) + bytes([1, 1]) + data
def _enc_size(n):
    if n < 255:  return bytes([n])
    return bytes([255]) + struct.pack('<I', n)

def _enc_string(s):
    b = s.encode('utf-8')
    return _enc_size(len(b)) + b


def _dec_size(data, off):
    n = data[off]; off += 1
    if n == 255:
        n = struct.unpack_from('<I', data, off)[0]; off += 4
    return n, off


def _dec_string(data, off):
    n, off = _dec_size(data, off)
    return data[off:off + n].decode('utf-8'), off + n
def call_read(comm, prx):
    ok, payload = prx.ice_invoke("read", Ice.OperationMode.Normal, b"")
    if not ok: raise RuntimeError("user exception")
    data = bytes(payload)[_ENCAP_HDR:]
    label, off = _dec_string(data, 0)
    counter = struct.unpack_from('<q', data, off)[0]; off += 8
    value = struct.unpack_from('<d', data, off)[0]
    return label, counter, value
def call_inc(comm, prx, delta):prx.ice_invoke("inc", Ice.OperationMode.Normal, _encap(struct.pack('<q', delta)))
def call_set_label(comm, prx, s): prx.ice_invoke("setLabel", Ice.OperationMode.Normal, _encap(_enc_string(s)))

def main():
    with Ice.initialize(sys.argv) as comm:
        prx = comm.stringToProxy("dedicated/c01:tcp -h 127.0.0.1 -p 10010")

        print("BEFORE:", call_read(comm, prx))
        call_inc(comm, prx, 5)
        call_set_label(comm, prx, "set-by-dynamic")
        print("AFTER :", call_read(comm, prx))
if __name__ == "__main__": main()