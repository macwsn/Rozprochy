package sr.thrift.server.impl;

import sr.gen.thrift.*;
import java.util.ArrayList;
import java.util.List;

public class SmartFridgeImpl extends FridgeImpl {

    private static final int MAX_ITEMS = 30;
    private final List<String> items = new ArrayList<>();

    public SmartFridgeImpl(String id, String room, double tempC) {
        super(id, "smart", room, tempC);
    }

    @Override protected List<String> contents() {
        return new ArrayList<>(items);
    }

    @Override public synchronized FridgeState read() {
        return state();
    }

    @Override public synchronized FridgeState setMode(FridgeMode m) throws InvalidArgs {
        this.mode = m;
        return state();
    }

    @Override public synchronized FridgeState setTemp(double t) throws InvalidArgs {
        if (t < -10.0 || t > 10.0) {
            throw new InvalidArgs("tempC", "temperature must be in [-10, 10] °C, got " + t);
        }
        this.tempC = t;
        return state();
    }

    @Override public synchronized FridgeState addItem(String item) throws Unsupported {
        if (item == null || item.isBlank()) {
            throw new Unsupported("addItem", "item name cannot be empty");
        }
        if (items.size() >= MAX_ITEMS) {
            throw new Unsupported("addItem", "fridge is full (max " + MAX_ITEMS + " items)");
        }
        items.add(item);
        return state();
    }

    @Override public synchronized FridgeState removeItem(String item) throws Unsupported {
        if (!items.remove(item)) {
            throw new Unsupported("removeItem", "item '" + item + "' not found in fridge");
        }
        return state();
    }
}
