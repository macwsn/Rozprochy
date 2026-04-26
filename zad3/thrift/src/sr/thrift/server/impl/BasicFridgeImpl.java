package sr.thrift.server.impl;

import sr.gen.thrift.*;

public class BasicFridgeImpl extends FridgeImpl {

    public BasicFridgeImpl(String id, String room, double tempC) {
        super(id, "basic", room, tempC);
    }

    @Override public synchronized FridgeState read() {
        return state();
    }

    @Override public synchronized FridgeState setMode(FridgeMode m) throws InvalidArgs {
        if (m == FridgeMode.PARTY) {
            throw new InvalidArgs("mode", "BasicFridge does not support PARTY mode");
        }
        this.mode = m;
        return state();
    }

    @Override public synchronized FridgeState setTemp(double t) throws InvalidArgs {
        if (t < -5.0 || t > 10.0) {
            throw new InvalidArgs("tempC", "BasicFridge temperature must be in [-5, 10] °C, got " + t);
        }
        this.tempC = t;
        return state();
    }

    @Override public FridgeState addItem(String item) throws Unsupported {
        throw new Unsupported("addItem", "BasicFridge has no item tracking");
    }

    @Override public FridgeState removeItem(String item) throws Unsupported {
        throw new Unsupported("removeItem", "BasicFridge has no item tracking");
    }
}
