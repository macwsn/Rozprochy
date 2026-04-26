import sys
import os
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "gen"))

from thrift.transport import TSocket, TTransport
from thrift.protocol import TBinaryProtocol
from thrift.protocol.TMultiplexedProtocol import TMultiplexedProtocol
from smarthome import FridgeService

WORKERS = 8
CALLS_PER_WORKER = 50
HOST = "127.0.0.1"
PORT = 9090
DEVICE_ID = "f-kitchen-1"


def hammer(worker_id):
    sock = TSocket.TSocket(HOST, PORT)
    transport = TTransport.TBufferedTransport(sock)
    proto = TBinaryProtocol.TBinaryProtocol(transport)
    client = FridgeService.Client(TMultiplexedProtocol(proto, "fridge"))
    transport.open()
    try:
        for _ in range(CALLS_PER_WORKER): client.read(DEVICE_ID)
    finally: transport.close()
    return worker_id


if __name__ == "__main__":
    print(f"Sending {WORKERS} x {CALLS_PER_WORKER} = {WORKERS * CALLS_PER_WORKER} calls to {HOST}:{PORT} ...")
    start = time.time()
    with ThreadPoolExecutor(max_workers=WORKERS) as ex:
        futures = [ex.submit(hammer, i) for i in range(WORKERS)]
        for f in as_completed(futures): print(f"  worker {f.result()} done")
    elapsed = time.time() - start
    print(f"All done in {elapsed:.2f}s — check server log for pool-1-thread-N entries")
