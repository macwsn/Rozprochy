package sr.thrift.server.impl;

import sr.gen.thrift.*;

public abstract class CameraImpl {

    protected final String id;
    protected final String room;
    protected final String subtype;
    protected boolean recording = false;
    protected PtzPosition ptz = new PtzPosition(0.0, 0.0, 1.0);

    protected CameraImpl(String id, String subtype, String room) {
        this.id      = id;
        this.subtype = subtype;
        this.room    = room;
    }

    public DeviceInfo info() {
        return new DeviceInfo(id, DeviceKind.CAMERA, subtype, room);
    }

    protected CameraState state() {
        return new CameraState(recording, new PtzPosition(ptz.pan, ptz.tilt, ptz.zoom));
    }

    public abstract CameraState read();
    public abstract CameraState setRecording(boolean on);
    public abstract CameraState movePtz(PtzPosition pos) throws Unsupported, InvalidArgs;
}
