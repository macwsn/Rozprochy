package sr.thrift.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import sr.thrift.server.impl.BasicFridgeImpl;
import sr.thrift.server.impl.CameraImpl;
import sr.thrift.server.impl.FixedCamImpl;
import sr.thrift.server.impl.FridgeImpl;
import sr.thrift.server.impl.OvenImpl;
import sr.thrift.server.impl.PtzCamImpl;
import sr.thrift.server.impl.SmartFridgeImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceLoader {

    public static class Result {
        public final Map<String, FridgeImpl> fridges = new ConcurrentHashMap<>();
        public final Map<String, OvenImpl> ovens = new ConcurrentHashMap<>();
        public final Map<String, CameraImpl> cameras = new ConcurrentHashMap<>();
    }

    private static class DevicesJson {
        public List<FridgeJson> fridges = List.of();
        public List<CameraJson> cameras = List.of();
        public List<OvenJson> ovens = List.of();
    }

    private static class FridgeJson {
        public String id, subtype, room;
        public double tempC = 4.0;
    }

    private static class CameraJson {
        public String id, subtype, room;
    }

    private static class OvenJson {
        public String id, room;
    }

    public static Result load(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        DevicesJson data = mapper.readValue(path.toFile(), DevicesJson.class);
        Result result = new Result();

        for (FridgeJson f : data.fridges) {
            FridgeImpl impl = "smart".equals(f.subtype)
                ? new SmartFridgeImpl(f.id, f.room, f.tempC)
                : new BasicFridgeImpl(f.id, f.room, f.tempC);
            result.fridges.put(f.id, impl);
        }

        for (CameraJson c : data.cameras) {
            CameraImpl impl = "ptz".equals(c.subtype)
                ? new PtzCamImpl(c.id, c.room)
                : new FixedCamImpl(c.id, c.room);
            result.cameras.put(c.id, impl);
        }

        for (OvenJson o : data.ovens) {
            result.ovens.put(o.id, new OvenImpl(o.id, o.room));
        }

        return result;
    }
}
