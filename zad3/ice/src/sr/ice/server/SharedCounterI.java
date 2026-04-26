package sr.ice.server;

import Demo.Counter;
import Demo.State;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedCounterI implements Counter {

    private static final Logger log = LoggerFactory.getLogger(SharedCounterI.class);

    private final Map<Identity, State> states = new ConcurrentHashMap<>();

    private State get(Identity id) {
        return states.computeIfAbsent(id, k -> new State("default", 0, 0.0));
    }

    @Override
    public State read(Current current) {
        log.info("[Shared] op=read id={} hash={} states.size={}",
            current.id.name, System.identityHashCode(this), states.size());
        return get(current.id);
    }

    @Override
    public synchronized void inc(long delta, Current current) {
        log.info("[Shared] op=inc id={} delta={} hash={}",
            current.id.name, delta, System.identityHashCode(this));
        get(current.id).counter += delta;
    }

    @Override
    public synchronized void setLabel(String s, Current current) {
        log.info("[Shared] op=setLabel id={} label='{}' hash={}",
            current.id.name, s, System.identityHashCode(this));
        get(current.id).label = s;
    }
}
