package sr.thrift.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sr.gen.thrift.DeviceInfo;
import sr.gen.thrift.InvalidArgs;
import sr.gen.thrift.NotFound;
import sr.gen.thrift.OvenMode;
import sr.gen.thrift.OvenService;
import sr.gen.thrift.OvenState;
import sr.thrift.server.impl.OvenImpl;

import java.util.List;
import java.util.Map;

public class OvenHandler implements OvenService.Iface {

    private static final Logger log = LoggerFactory.getLogger(OvenHandler.class);

    private final Map<String, OvenImpl> devices;

    public OvenHandler(Map<String, OvenImpl> devices) {
        this.devices = devices;
    }

    private OvenImpl get(String id) throws NotFound {
        OvenImpl d = devices.get(id);
        if (d == null) throw new NotFound(id);
        return d;
    }

    @Override
    public List<DeviceInfo> listDevices() {
        log.info("[oven] listDevices() -> {} devices", devices.size());
        return devices.values().stream().map(OvenImpl::info).toList();
    }

    @Override
    public DeviceInfo describe(String id) throws NotFound {
        log.info("[oven] describe({})", id);
        return get(id).info();
    }

    @Override
    public OvenState read(String id) throws NotFound {
        log.info("[oven] read({})", id);
        return get(id).read();
    }

    @Override
    public OvenState setMode(String id, OvenMode m, double tempC, int minutes) throws NotFound, InvalidArgs {
        log.info("[oven] setMode({}, {}, {}, {})", id, m, tempC, minutes);
        return get(id).setMode(m, tempC, minutes);
    }
}
