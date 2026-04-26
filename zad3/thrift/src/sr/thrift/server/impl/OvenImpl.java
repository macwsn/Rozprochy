package sr.thrift.server.impl;

import sr.gen.thrift.*;

public class OvenImpl {

    private static final double TEMP_MIN = 50.0, TEMP_MAX = 300.0;
    private static final int MIN_MIN = 0, MIN_MAX = 600;

    private final String id;
    private final String room;
    private double tempC = 0.0;
    private OvenMode mode = OvenMode.OFF;
    private int minutesLeft = 0;

    public OvenImpl(String id, String room) {
        this.id   = id;
        this.room = room;
    }

    public DeviceInfo info() {
        return new DeviceInfo(id, DeviceKind.OVEN, "standard", room);
    }

    public synchronized OvenState read() {
        return state();
    }

    public synchronized OvenState setMode(OvenMode m, double t, int minutes) throws InvalidArgs {
        if (m != OvenMode.OFF) {
            if (t < TEMP_MIN || t > TEMP_MAX) {
                throw new InvalidArgs("tempC",
                    "temperature must be in [" + TEMP_MIN + ", " + TEMP_MAX + "] °C, got " + t);
            }
            if (minutes < MIN_MIN || minutes > MIN_MAX) {
                throw new InvalidArgs("minutes",
                    "minutes must be in [" + MIN_MIN + ", " + MIN_MAX + "], got " + minutes);
            }
        }
        this.mode = m;
        this.tempC = (m == OvenMode.OFF) ? 0.0 : t;
        this.minutesLeft = (m == OvenMode.OFF) ? 0 : minutes;
        return state();
    }

    private OvenState state() {
        return new OvenState(tempC, mode, minutesLeft);
    }
}
