package sr.thrift.server;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sr.gen.thrift.CameraService;
import sr.gen.thrift.FridgeService;
import sr.gen.thrift.OvenService;

import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class ThriftServer {

    private static final Logger log = LoggerFactory.getLogger(ThriftServer.class);

    public static void main(String[] args) throws Exception {
        int port = 9090;
        String devicesFile = "thrift/devices-kitchen.json";
        boolean tls = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--devices" -> devicesFile = args[++i];
                case "--tls" -> tls = true;
            }
        }

        DeviceLoader.Result data = DeviceLoader.load(Paths.get(devicesFile));
        log.info("Loaded {} fridges, {} ovens, {} cameras from {}",
            data.fridges.size(), data.ovens.size(), data.cameras.size(), devicesFile);

        FridgeHandler fridgeHandler = new FridgeHandler(data.fridges);
        OvenHandler ovenHandler = new OvenHandler(data.ovens);
        CameraHandler cameraHandler = new CameraHandler(data.cameras);

        TMultiplexedProcessor mux = new TMultiplexedProcessor();
        mux.registerProcessor("fridge", new FridgeService.Processor<>(fridgeHandler));
        mux.registerProcessor("oven",   new OvenService.Processor<>(ovenHandler));
        mux.registerProcessor("camera", new CameraService.Processor<>(cameraHandler));

        TServerTransport transport = tls ? buildTlsTransport(port) : new TServerSocket(port);

        TThreadPoolServer server = new TThreadPoolServer(
            new TThreadPoolServer.Args(transport)
                .processor(mux)
                .protocolFactory(new TBinaryProtocol.Factory())
                .executorService(Executors.newFixedThreadPool(16)));

        log.info("Listening on :{} (tls={})", port, tls);
        server.serve();
    }

    private static TServerTransport buildTlsTransport(int port) throws TTransportException {
        TSSLTransportParameters params = new TSSLTransportParameters();
        params.setKeyStore("certs/server.p12", "changeit", "SunX509", "PKCS12");
        return TSSLTransportFactory.getServerSocket(port, 0, null, params);
    }
}
