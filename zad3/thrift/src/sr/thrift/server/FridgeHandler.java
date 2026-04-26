package sr.thrift.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sr.gen.thrift.DeviceInfo;
import sr.gen.thrift.FridgeMode;
import sr.gen.thrift.FridgeService;
import sr.gen.thrift.FridgeState;
import sr.gen.thrift.InvalidArgs;
import sr.gen.thrift.NotFound;
import sr.gen.thrift.Unsupported;
import sr.thrift.server.impl.FridgeImpl;

import java.util.List;
import java.util.Map;

public class FridgeHandler implements FridgeService.Iface {

    private static final Logger log = LoggerFactory.getLogger(FridgeHandler.class);

    private final Map<String, FridgeImpl> devices;

    public FridgeHandler(Map<String, FridgeImpl> devices) {
        this.devices = devices;
    }

    private FridgeImpl get(String id) throws NotFound {
        FridgeImpl d = devices.get(id);
        if (d == null) throw new NotFound(id);
        return d;
    }

    @Override
    public List<DeviceInfo> listDevices() {
        log.info("[fridge] listDevices() -> {} devices", devices.size());
        return devices.values().stream().map(FridgeImpl::info).toList();
    }

    @Override
    public DeviceInfo describe(String id) throws NotFound {
        log.info("[fridge] describe({})", id);
        return get(id).info();
    }

    @Override
    public FridgeState read(String id) throws NotFound {
        log.info("[fridge] read({})", id);
        return get(id).read();
    }

    @Override
    public FridgeState setMode(String id, FridgeMode m) throws NotFound, InvalidArgs {
        log.info("[fridge] setMode({}, {})", id, m);
        return get(id).setMode(m);
    }

    @Override
    public FridgeState setTemp(String id, double tempC) throws NotFound, InvalidArgs {
        log.info("[fridge] setTemp({}, {})", id, tempC);
        return get(id).setTemp(tempC);
    }

    @Override
    public FridgeState addItem(String id, String item) throws NotFound, Unsupported {
        log.info("[fridge] addItem({}, '{}')", id, item);
        return get(id).addItem(item);
    }

    @Override
    public FridgeState removeItem(String id, String item) throws NotFound, Unsupported {
        log.info("[fridge] removeItem({}, '{}')", id, item);
        return get(id).removeItem(item);
    }
}
