package sr.grpc.gateway;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sr.gen.thrift.FridgeService;
import sr.gen.thrift.OvenService;
import sr.gen.thrift.CameraService;
import sr.gen.thrift.NotFound;
import sr.gen.thrift.InvalidArgs;
import sr.gen.thrift.Unsupported;
import sr.grpc.smarthome.*;

public class SmartHomeGatewayImpl extends SmartHomeGrpc.SmartHomeImplBase {

    private static final Logger log = LoggerFactory.getLogger(SmartHomeGatewayImpl.class);

    private final String thriftHost;
    private final int thriftPort;

    public SmartHomeGatewayImpl(String thriftHost, int thriftPort) {
        this.thriftHost = thriftHost;
        this.thriftPort = thriftPort;
    }


    record ThriftClients(FridgeService.Client fridge, OvenService.Client oven, CameraService.Client camera,
                         TTransport transport) implements AutoCloseable {
        @Override public void close() { transport.close(); }
    }

    private ThriftClients connect() throws TException {
        TTransport t = new TSocket(thriftHost, thriftPort);
        t.open();
        var proto  = new TBinaryProtocol(t);
        var fridge = new FridgeService.Client(new TMultiplexedProtocol(proto, "fridge"));
        var oven   = new OvenService.Client(new TMultiplexedProtocol(proto, "oven"));
        var camera = new CameraService.Client(new TMultiplexedProtocol(proto, "camera"));
        return new ThriftClients(fridge, oven, camera, t);
    }

    private static DeviceInfo deviceToProto(sr.gen.thrift.DeviceInfo d) {
        DeviceKind kind = switch (d.kind) {
            case FRIDGE -> DeviceKind.FRIDGE;
            case CAMERA -> DeviceKind.CAMERA;
            case OVEN   -> DeviceKind.OVEN;
        };
        return DeviceInfo.newBuilder()
                .setId(d.id).setKind(kind).setSubtype(d.subtype).setRoom(d.room)
                .build();
    }

    private static sr.grpc.smarthome.FridgeState fridgeToProto(sr.gen.thrift.FridgeState s) {
        return sr.grpc.smarthome.FridgeState.newBuilder()
                .setTempC(s.tempC).setDoorOpen(s.doorOpen)
                .setMode(sr.grpc.smarthome.FridgeMode.forNumber(s.mode.getValue()))
                .addAllContents(s.contents)
                .build();
    }

    private static sr.grpc.smarthome.OvenState ovenToProto(sr.gen.thrift.OvenState s) {
        return sr.grpc.smarthome.OvenState.newBuilder()
                .setTempC(s.tempC)
                .setMode(sr.grpc.smarthome.OvenMode.forNumber(s.mode.getValue()))
                .setMinutesLeft(s.minutesLeft)
                .build();
    }

    private static sr.grpc.smarthome.CameraState cameraToProto(sr.gen.thrift.CameraState s) {
        var b = sr.grpc.smarthome.CameraState.newBuilder().setRecording(s.recording);
        if (s.ptz != null)
            b.setPtz(sr.grpc.smarthome.PtzPosition.newBuilder()
                    .setPan(s.ptz.pan).setTilt(s.ptz.tilt).setZoom(s.ptz.zoom));
        if (s.lastFrameUrl != null) b.setLastFrameUrl(s.lastFrameUrl);
        return b.build();
    }

    @Override
    public void listDevices(Empty req, StreamObserver<DeviceList> resp) {
        log.info("[gateway] listDevices");
        try (var c = connect()) {
            var b = DeviceList.newBuilder();
            c.fridge().listDevices().forEach(d -> b.addDevices(deviceToProto(d)));
            c.oven().listDevices().forEach(d -> b.addDevices(deviceToProto(d)));
            c.camera().listDevices().forEach(d -> b.addDevices(deviceToProto(d)));
            resp.onNext(b.build()); resp.onCompleted();
        } catch (TException e) {
            resp.onError(Status.UNAVAILABLE.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void getFridge(DeviceId req, StreamObserver<FridgeState> resp) {
        log.info("[gateway] getFridge({})", req.getId());
        try (var c = connect()) {
            resp.onNext(fridgeToProto(c.fridge().read(req.getId()))); resp.onCompleted();
        } catch (NotFound e)   { resp.onError(Status.NOT_FOUND.withDescription(e.id).asException()); }
          catch (TException e) { resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asException()); }
    }

    @Override
    public void setFridgeMode(SetFridgeModeRequest req, StreamObserver<FridgeState> resp) {
        log.info("[gateway] setFridgeMode({}, {})", req.getId(), req.getMode());
        if (req.getMode() == sr.grpc.smarthome.FridgeMode.UNRECOGNIZED) { resp.onError(Status.INVALID_ARGUMENT.withDescription("mode: unknown enum value").asException()); return; }
        var mode = sr.gen.thrift.FridgeMode.findByValue(req.getMode().getNumber());
        if (mode == null) { resp.onError(Status.INVALID_ARGUMENT.withDescription("mode: unknown value " + req.getMode().getNumber()).asException()); return; }
        try (var c = connect()) {
            resp.onNext(fridgeToProto(c.fridge().setMode(req.getId(), mode))); resp.onCompleted();
        } catch (NotFound e)    { resp.onError(Status.NOT_FOUND.withDescription(e.id).asException()); }
          catch (InvalidArgs e) { resp.onError(Status.INVALID_ARGUMENT.withDescription(e.field + ": " + e.reason).asException()); }
          catch (TException e)  { resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asException()); }
    }

    @Override
    public void getOven(DeviceId req, StreamObserver<OvenState> resp) {
        log.info("[gateway] getOven({})", req.getId());
        try (var c = connect()) {
            resp.onNext(ovenToProto(c.oven().read(req.getId()))); resp.onCompleted();
        } catch (NotFound e)   { resp.onError(Status.NOT_FOUND.withDescription(e.id).asException()); }
          catch (TException e) { resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asException()); }
    }

    @Override
    public void setOven(SetOvenRequest req, StreamObserver<OvenState> resp) {
        log.info("[gateway] setOven({}, {})", req.getId(), req.getMode());
        if (req.getMode() == sr.grpc.smarthome.OvenMode.UNRECOGNIZED) { resp.onError(Status.INVALID_ARGUMENT.withDescription("mode: unknown enum value").asException()); return; }
        var mode = sr.gen.thrift.OvenMode.findByValue(req.getMode().getNumber());
        if (mode == null) { resp.onError(Status.INVALID_ARGUMENT.withDescription("mode: unknown value " + req.getMode().getNumber()).asException()); return; }
        try (var c = connect()) {
            resp.onNext(ovenToProto(c.oven().setMode(req.getId(), mode, req.getTempC(), req.getMinutes())));
            resp.onCompleted();
        } catch (NotFound e)    { resp.onError(Status.NOT_FOUND.withDescription(e.id).asException()); }
          catch (InvalidArgs e) { resp.onError(Status.INVALID_ARGUMENT.withDescription(e.field + ": " + e.reason).asException()); }
          catch (TException e)  { resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asException()); }
    }

    @Override
    public void getCamera(DeviceId req, StreamObserver<CameraState> resp) {
        log.info("[gateway] getCamera({})", req.getId());
        try (var c = connect()) {
            resp.onNext(cameraToProto(c.camera().read(req.getId()))); resp.onCompleted();
        } catch (NotFound e)   { resp.onError(Status.NOT_FOUND.withDescription(e.id).asException()); }
          catch (TException e) { resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asException()); }
    }

    @Override
    public void setRecording(SetRecordingRequest req, StreamObserver<CameraState> resp) {
        log.info("[gateway] setRecording({}, {})", req.getId(), req.getOn());
        try (var c = connect()) {
            resp.onNext(cameraToProto(c.camera().setRecording(req.getId(), req.getOn()))); resp.onCompleted();
        } catch (NotFound e)   { resp.onError(Status.NOT_FOUND.withDescription(e.id).asException()); }
          catch (TException e) { resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asException()); }
    }

    @Override
    public void movePtz(MovePtzRequest req, StreamObserver<CameraState> resp) {
        log.info("[gateway] movePtz({})", req.getId());
        try (var c = connect()) {
            var pos = new sr.gen.thrift.PtzPosition(
                    req.getPtz().getPan(), req.getPtz().getTilt(), req.getPtz().getZoom());
            resp.onNext(cameraToProto(c.camera().movePtz(req.getId(), pos))); resp.onCompleted();
        } catch (NotFound e)    { resp.onError(Status.NOT_FOUND.withDescription(e.id).asException()); }
          catch (Unsupported e) { resp.onError(Status.UNIMPLEMENTED.withDescription(e.op + ": " + e.reason).asException()); }
          catch (InvalidArgs e) { resp.onError(Status.INVALID_ARGUMENT.withDescription(e.field + ": " + e.reason).asException()); }
          catch (TException e)  { resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asException()); }
    }
}
