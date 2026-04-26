package sr.thrift.server.impl;

import sr.gen.thrift.*;

public class PtzCamImpl extends CameraImpl {

    private static final double PAN_MIN = -180.0, PAN_MAX = 180.0;
    private static final double TILT_MIN = -90.0, TILT_MAX = 90.0;
    private static final double ZOOM_MIN = 1.0, ZOOM_MAX = 8.0;

    public PtzCamImpl(String id, String room) {
        super(id, "ptz", room);
    }

    @Override public synchronized CameraState read() {
        return state();
    }

    @Override public synchronized CameraState setRecording(boolean on) {
        this.recording = on;
        return state();
    }

    @Override public synchronized CameraState movePtz(PtzPosition pos) throws InvalidArgs {
        validateRange("pan", pos.pan, PAN_MIN, PAN_MAX);
        validateRange("tilt", pos.tilt, TILT_MIN, TILT_MAX);
        validateRange("zoom", pos.zoom, ZOOM_MIN, ZOOM_MAX);
        this.ptz = new PtzPosition(pos.pan, pos.tilt, pos.zoom);
        return state();
    }

    private static void validateRange(String field, double v, double min, double max) throws InvalidArgs {
        if (v < min || v > max) {
            throw new InvalidArgs(field,
                field + " must be in [" + min + ", " + max + "], got " + v);
        }
    }
}
