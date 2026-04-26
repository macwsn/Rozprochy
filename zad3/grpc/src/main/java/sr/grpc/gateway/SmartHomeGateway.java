package sr.grpc.gateway;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class SmartHomeGateway {

    private static final Logger log = LoggerFactory.getLogger(SmartHomeGateway.class);

    public static void main(String[] args) throws Exception {
        String thriftHost = "127.0.0.1";
        int    thriftPort = 9090;
        int    grpcPort   = 50444;
        boolean tls       = true;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--thrift-host" -> thriftHost = args[++i];
                case "--thrift-port" -> thriftPort = Integer.parseInt(args[++i]);
                case "--port"        -> grpcPort   = Integer.parseInt(args[++i]);
                case "--no-tls"      -> tls        = false;
            }
        }

        var impl = new SmartHomeGatewayImpl(thriftHost, thriftPort);

        NettyServerBuilder builder = NettyServerBuilder.forPort(grpcPort).addService(impl);
        if (tls) {
            builder.useTransportSecurity(new File("certs/server.crt"), new File("certs/server.key"));
            log.info("[gateway] TLS enabled");
        }

        Server srv = builder.build().start();
        log.info("[gateway] SmartHome gRPC gateway on :{} -> Thrift {}:{}", grpcPort, thriftHost, thriftPort);
        srv.awaitTermination();
    }
}
