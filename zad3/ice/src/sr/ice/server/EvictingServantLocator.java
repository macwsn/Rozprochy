package sr.ice.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ServantLocator;
import Demo.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class EvictingServantLocator implements ServantLocator {

    private static final Logger log = LoggerFactory.getLogger(EvictingServantLocator.class);

    private final int maxN;
    private final Path stateDir = Paths.get("ice", "state");
    private final ObjectMapper json = new ObjectMapper();

    private final LinkedHashMap<Identity, CounterI> cache;

    public EvictingServantLocator(int maxN) {
        this.maxN = maxN;
        this.cache = new LinkedHashMap<>(maxN, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Identity, CounterI> eldest) {
                if (size() > maxN) {
                    persist(eldest.getKey(), eldest.getValue());
                    log.info("[Evict] evicting {} -> file", eldest.getKey().name);
                    return true;
                }
                return false;
            }
        };
        try {
            Files.createDirectories(stateDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path file(Identity id) {
        return stateDir.resolve(id.category + "-" + id.name + ".json");
    }

    private void persist(Identity id, CounterI servant) {
        try {
            json.writeValue(file(id).toFile(), servant.snapshot());
        } catch (IOException e) {
            log.warn("[Evict] failed to persist {}: {}", id.name, e.getMessage());
        }
    }

    private State restore(Identity id) {
        Path p = file(id);
        if (!Files.exists(p)) return null;
        try {
            return json.readValue(p.toFile(), State.class);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public synchronized ServantLocator.LocateResult locate(Current current) {
        CounterI servant = cache.get(current.id);
        if (servant != null) {
            log.info("[Evict] locate({}) cache-hit hash={}", current.id.name, System.identityHashCode(servant));
            return new ServantLocator.LocateResult(servant, null);
        }
        State initial = restore(current.id);
        servant = new CounterI(initial);
        cache.put(current.id, servant);
        log.info("[Evict] locate({}) cache-miss restored={} hash={}",
            current.id.name, initial != null, System.identityHashCode(servant));
        return new ServantLocator.LocateResult(servant, null);
    }

    @Override
    public void finished(Current current, com.zeroc.Ice.Object servant, Object cookie) {}

    @Override
    public synchronized void deactivate(String category) {
        log.info("[Evict] deactivate({}) flushing {} servants", category, cache.size());
        cache.forEach(this::persist);
    }
}
