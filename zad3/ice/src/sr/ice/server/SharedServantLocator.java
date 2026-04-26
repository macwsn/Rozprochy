package sr.ice.server;

import com.zeroc.Ice.Current;
import com.zeroc.Ice.ServantLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedServantLocator implements ServantLocator {

    private static final Logger log = LoggerFactory.getLogger(SharedServantLocator.class);

    private final SharedCounterI shared = new SharedCounterI();

    @Override
    public ServantLocator.LocateResult locate(Current current) {
        log.info("[Shared] locate({}) hash={}", current.id.name, System.identityHashCode(shared));
        return new ServantLocator.LocateResult(shared, null);
    }

    @Override
    public void finished(Current current, com.zeroc.Ice.Object servant, Object cookie) {}

    @Override
    public void deactivate(String category) {}
}
