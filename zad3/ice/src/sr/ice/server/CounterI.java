package sr.ice.server;

import Demo.Counter;
import Demo.State;
import com.zeroc.Ice.Current;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CounterI implements Counter {

    private static final Logger log = LoggerFactory.getLogger(CounterI.class);

    private String label;
    private long counter;
    private double value;

    public CounterI(State initial) {
        if (initial != null) {
            this.label = initial.label;
            this.counter = initial.counter;
            this.value = initial.value;
        } else {
            this.label = "default";
            this.counter = 0;
            this.value = 0.0;
        }
    }

    @Override
    public synchronized State read(Current current) {
        log.info("[Counter] op=read id={} hash={}", current.id.name, System.identityHashCode(this));
        return new State(label, counter, value);
    }

    @Override
    public synchronized void inc(long delta, Current current) {
        log.info("[Counter] op=inc id={} delta={} hash={}", current.id.name, delta, System.identityHashCode(this));
        counter += delta;
    }

    @Override
    public synchronized void setLabel(String s, Current current) {
        log.info("[Counter] op=setLabel id={} label='{}' hash={}", current.id.name, s, System.identityHashCode(this));
        this.label = s;
    }

    public synchronized State snapshot() {
        return new State(label, counter, value);
    }
}
