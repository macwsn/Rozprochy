package sr.ice.server;

import com.zeroc.Ice.Current;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ServantLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DedicatedServantLocator implements ServantLocator {

    private static final Logger log = LoggerFactory.getLogger(DedicatedServantLocator.class);

    @Override
    public ServantLocator.LocateResult locate(Current current) {
        CounterI servant = new CounterI(null);
        current.adapter.add(servant, current.id);
        log.info("[Dedicated] locate({}) created hash={} -> added to ASM",
            current.id.name, System.identityHashCode(servant));
        return new ServantLocator.LocateResult(servant, null);
    }

    @Override
    public void finished(Current current, com.zeroc.Ice.Object servant, Object cookie) {}

    @Override
    public void deactivate(String category) {}
}
