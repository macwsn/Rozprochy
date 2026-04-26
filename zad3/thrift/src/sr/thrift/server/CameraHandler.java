package sr.thrift.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sr.gen.thrift.CameraService;
import sr.gen.thrift.CameraState;
import sr.gen.thrift.DeviceInfo;
import sr.gen.thrift.InvalidArgs;
import sr.gen.thrift.NotFound;
import sr.gen.thrift.PtzPosition;
import sr.gen.thrift.Unsupported;
import sr.thrift.server.impl.CameraImpl;

import java.util.List;
import java.util.Map;

public class CameraHandler implements CameraService.Iface {

    private static final Logger log = LoggerFactory.getLogger(CameraHandler.class);

    private final Map<String, CameraImpl> devices;

    public CameraHandler(Map<String, CameraImpl> devices) {
        this.devices = devices;
    }

    private CameraImpl get(String id) throws NotFound {
        CameraImpl d = devices.get(id);
        if (d == null) throw new NotFound(id);
        return d;
    }

    @Override
    public List<DeviceInfo> listDevices() {
        log.info("[camera] listDevices() -> {} devices", devices.size());
        return devices.values().stream().map(CameraImpl::info).toList();
    }

    @Override
    public DeviceInfo describe(String id) throws NotFound {
        log.info("[camera] describe({})", id);
        return get(id).info();
    }

    @Override
    public CameraState read(String id) throws NotFound {
        log.info("[camera] read({})", id);
        return get(id).read();
    }

    @Override
    public CameraState setRecording(String id, boolean on) throws NotFound {
        log.info("[camera] setRecording({}, {})", id, on);
        return get(id).setRecording(on);
    }

    @Override
    public CameraState movePtz(String id, PtzPosition pos) throws NotFound, Unsupported, InvalidArgs {
        log.info("[camera] movePtz({}, pan={} tilt={} zoom={})", id, pos.pan, pos.tilt, pos.zoom);
        return get(id).movePtz(pos);
    }
}
