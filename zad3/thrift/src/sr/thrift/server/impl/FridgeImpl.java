package sr.thrift.server.impl;

import sr.gen.thrift.*;
import java.util.Collections;
import java.util.List;

public abstract class FridgeImpl {

    protected final String id;
    protected final String room;
    protected final String subtype;
    protected double tempC;
    protected boolean doorOpen = false;
    protected FridgeMode mode = FridgeMode.ON;

    protected FridgeImpl(String id, String subtype, String room, double tempC) {
        this.id = id;
        this.subtype = subtype;
        this.room = room;
        this.tempC = tempC;
    }

    public DeviceInfo info() {
        return new DeviceInfo(id, DeviceKind.FRIDGE, subtype, room);
    }

    protected FridgeState state() {
        return new FridgeState(tempC, doorOpen, mode, contents());
    }

    protected List<String> contents() {
        return Collections.emptyList();
    }

    public abstract FridgeState read();
    public abstract FridgeState setMode(FridgeMode m) throws InvalidArgs;
    public abstract FridgeState setTemp(double t) throws InvalidArgs;
    public abstract FridgeState addItem(String item) throws Unsupported;
    public abstract FridgeState removeItem(String item) throws Unsupported;
}
