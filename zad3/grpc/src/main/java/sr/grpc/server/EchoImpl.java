package sr.grpc.server;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sr.grpc.echo.EchoGrpc;
import sr.grpc.echo.Msg;

public class EchoImpl extends EchoGrpc.EchoImplBase {

    private static final Logger log = LoggerFactory.getLogger(EchoImpl.class);

    @Override
    public void ping(Msg req, StreamObserver<Msg> resp) {
        log.info("[grpc] Ping(\"{}\")", req.getText());
        resp.onNext(Msg.newBuilder().setText(req.getText()).build());
        resp.onCompleted();
    }
}
