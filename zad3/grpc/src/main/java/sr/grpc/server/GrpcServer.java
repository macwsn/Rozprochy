package sr.grpc.server;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class GrpcServer {

    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    public static void main(String[] args) throws Exception {
        Server srv = NettyServerBuilder.forPort(50443)
                .useTransportSecurity(new File("certs/server.crt"), new File("certs/server.key"))
                .addService(new EchoImpl())
                .build()
                .start();
        log.info("gRPC TLS listening on :50443");
        srv.awaitTermination();
    }
}
