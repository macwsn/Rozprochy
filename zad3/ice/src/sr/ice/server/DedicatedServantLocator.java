package sr.ice.server;

import com.zeroc.Ice.Current;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ServantLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DedicatedServantLocator implements ServantLocator {

    private static final Logger log = LoggerFactory.getLogger(DedicatedServantLocator.class);

    private final Map<Identity, CounterI> servants = new ConcurrentHashMap<>();

    @Override
    public ServantLocator.LocateResult locate(Current current) {
        boolean created = !servants.containsKey(current.id);
        CounterI servant = servants.computeIfAbsent(current.id, id -> new CounterI(null));
        log.info("[Dedicated] locate({}) created={} hash={}", current.id.name, created, System.identityHashCode(servant));
        return new ServantLocator.LocateResult(servant, null);
    }

    @Override
    public void finished(Current current, com.zeroc.Ice.Object servant, Object cookie) {}

    @Override
    public void deactivate(String category) {}
}
