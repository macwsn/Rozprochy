namespace java sr.gen.thrift
namespace py smarthome

enum DeviceKind { FRIDGE=1, CAMERA=2, OVEN=3 }
enum FridgeMode { OFF=0, ON=1, ECO=2, PARTY=3 }
enum OvenMode   { OFF=0, BAKE=1, GRILL=2, PREHEAT=3 }

struct DeviceInfo  { 1: string id, 2: DeviceKind kind, 3: string subtype, 4: string room }
struct PtzPosition { 1: double pan, 2: double tilt, 3: double zoom }
struct FridgeState { 1: double tempC, 2: bool doorOpen, 3: FridgeMode mode, 4: list<string> contents }
struct OvenState   { 1: double tempC, 2: OvenMode mode, 3: i32 minutesLeft }
struct CameraState { 1: bool recording, 2: PtzPosition ptz, 3: optional string lastFrameUrl }

exception NotFound { 1: string id }
exception InvalidArgs { 1: string field, 2: string reason }
exception Unsupported { 1: string op, 2: string reason }

service DeviceRegistry {
  list<DeviceInfo> listDevices(),
  DeviceInfo describe(1: string id) throws (1: NotFound nf),
}

service FridgeService extends DeviceRegistry {
  FridgeState read(1: string id) throws (1: NotFound nf),
  FridgeState setMode(1: string id, 2: FridgeMode m) throws (1: NotFound nf, 2: InvalidArgs ia),
  FridgeState setTemp(1: string id, 2: double tempC) throws (1: NotFound nf, 2: InvalidArgs ia),
  FridgeState addItem(1: string id, 2: string item) throws (1: NotFound nf, 2: Unsupported un),
  FridgeState removeItem(1: string id, 2: string item) throws (1: NotFound nf, 2: Unsupported un),
}

service OvenService extends DeviceRegistry {
  OvenState read(1: string id)  throws (1: NotFound nf),
  OvenState setMode(1: string id, 2: OvenMode m, 3: double tempC, 4: i32 minutes) throws (1: NotFound nf, 2: InvalidArgs ia),
}

service CameraService extends DeviceRegistry {
  CameraState read(1: string id) throws (1: NotFound nf),
  CameraState setRecording(1: string id, 2: bool on) throws (1: NotFound nf),
  CameraState movePtz(1: string id, 2: PtzPosition pos) throws (1: NotFound nf, 2: Unsupported un, 3: InvalidArgs ia),
}