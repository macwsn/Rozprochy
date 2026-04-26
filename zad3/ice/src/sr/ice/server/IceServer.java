package sr.ice.server;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IceServer {

    private static final Logger log = LoggerFactory.getLogger(IceServer.class);

    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args, "ice/server.config")) {
            ObjectAdapter adapter = communicator.createObjectAdapter("Adapter1");
            adapter.addServantLocator(new DedicatedServantLocator(), "dedicated");
            adapter.addServantLocator(new SharedServantLocator(),    "shared");
            adapter.addServantLocator(new EvictingServantLocator(5), "evict");
            adapter.activate();
            log.info("Adapter1 activated on port 10010");
            communicator.waitForShutdown();
        }
    }
}
