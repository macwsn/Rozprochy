package sr.thrift.server.impl;

import sr.gen.thrift.*;

public class FixedCamImpl extends CameraImpl {

    public FixedCamImpl(String id, String room) {
        super(id, "fixed", room);
    }

    @Override public synchronized CameraState read() {
        return state();
    }

    @Override public synchronized CameraState setRecording(boolean on) {
        this.recording = on;
        return state();
    }

    @Override public CameraState movePtz(PtzPosition pos) throws Unsupported {
        throw new Unsupported("movePtz", "FixedCam does not support PTZ");
    }
}
