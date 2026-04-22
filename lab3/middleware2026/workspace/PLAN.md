# Plan implementacji — lab4 (A1 + I1 + I2 + I5)

**Języki:** Java (server) + Python (client) we wszystkich zadaniach.
**Technologie:** Thrift (A1) + Ice (I1, I2) + gRPC, Thrift, Ice (I5).

## Wybór i punktacja

| Zadanie | Technologia | Punkty | Uwagi |
|---|---|---|---|
| A1 — Smart environment | Thrift (Java srv + Python cli) | 12 (+2) | Główna aplikacja |
| I1 — Wywołanie dynamiczne | Ice (Java srv z I2 + Python dynamic cli) | — | Rozszerzenie projektu I2 |
| I2 — Zarządzanie serwantami | Ice (Java srv + Python cli) | 8 (+2) | 3 strategie + LRU evictor |
| I5 — TLS | Ice + Thrift + gRPC | 8 | TLS dorzucany do A1/I2 + mini-projekt gRPC |

Spełnienie wymogu „**gRPC oraz (Ice lub Thrift)**” → przez I5 (Ice + Thrift + gRPC) oraz przez to, że A1 jest w Thrift, a I1/I2 w Ice.

---

## A1 — Smart environment (Thrift, 12 + 2 pkt)

### Model dziedziny
- **Fridge** (2 podtypy: `BasicFridge`, `SmartFridge`)
- **Camera** (2 podtypy: `FixedCam`, `PtzCam`)
- **Oven** (1 typ z trybami BAKE/GRILL/PREHEAT)
- ~10 instancji łącznie, rozproszone na **2 serwerach** (np. „kitchen” port 9090, „garage” port 9091).

### IDL — `smarthome.thrift`
- 3 usługi (`FridgeService`, `OvenService`, `CameraService`) — minimalizacja liczby usług, ale bez ekstremizmu.
- Wszystkie dziedziczą po `DeviceRegistry` (operacje `list()`, `describe(id)`).
- Bogate typy: `struct DeviceInfo`, `PtzPosition`, `FridgeState`, `OvenState`, `CameraState`, `list<>`, `enum`.
- Wyjątki domenowe: `NotFound`, `InvalidArgs`, `Unsupported` (np. PTZ na FixedCam).

```thrift
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

exception NotFound    { 1: string id }
exception InvalidArgs { 1: string field, 2: string reason }
exception Unsupported { 1: string op, 2: string reason }

service DeviceRegistry {
  list<DeviceInfo> list(),
  DeviceInfo describe(1: string id) throws (1: NotFound nf),
}

service FridgeService extends DeviceRegistry {
  FridgeState read(1: string id)                              throws (1: NotFound nf),
  FridgeState setMode(1: string id, 2: FridgeMode m)          throws (1: NotFound nf, 2: InvalidArgs ia),
  FridgeState setTemp(1: string id, 2: double tempC)          throws (1: NotFound nf, 2: InvalidArgs ia),
  FridgeState addItem(1: string id, 2: string item)           throws (1: NotFound nf),
  FridgeState removeItem(1: string id, 2: string item)        throws (1: NotFound nf),
}

service OvenService extends DeviceRegistry {
  OvenState read(1: string id)                                throws (1: NotFound nf),
  OvenState setMode(1: string id, 2: OvenMode m, 3: double tempC, 4: i32 minutes)
                                                              throws (1: NotFound nf, 2: InvalidArgs ia),
}

service CameraService extends DeviceRegistry {
  CameraState read(1: string id)                              throws (1: NotFound nf),
  CameraState record(1: string id, 2: bool on)                throws (1: NotFound nf),
  CameraState movePtz(1: string id, 2: PtzPosition pos)       throws (1: NotFound nf, 2: Unsupported un, 3: InvalidArgs ia),
}
```

### Java server
- `TThreadPoolServer` + `TMultiplexedProcessor` z trzema procesorami pod nazwami `fridge`, `oven`, `camera`.
- Handlery: `FridgeHandler`, `OvenHandler`, `CameraHandler` — każdy trzyma `Map<String, *Impl>` (stan urządzeń).
- Initial state z `devices.json` lub na sztywno w `main`.
- Logowanie każdego wywołania (id, metoda, parametry, wynik).
- 2 procesy serwera startowane z osobnym configiem (port + zestaw urządzeń).
- **+2 pkt:** wielowątkowość — `TThreadPoolServer` z `Args(...).executorService(...)`.

### Python client
- `pip install thrift`. Generowanie stubów: `thrift -r --gen py -out client_py/gen smarthome.thrift`.
- Jeden `TSocket` per serwer + `TMultiplexedProtocol` per usługa.
- Klient pyta oba serwery przez `DeviceRegistry.list()`, mergeuje listę → sterowanie wszystkimi urządzeniami **bez restartu**.
- Menu interaktywne: `list / get <id> / fridge-mode <id> ECO / fridge-temp <id> 4.5 / ptz <id> 30 10 2.0 / record <id> on / oven <id> BAKE 180 30 / x`.

### Decyzje projektowe (do obrony)
- **Mało usług, bogate operacje** — 1 usługa per typ urządzenia.
- **Dziedziczenie `extends DeviceRegistry`** — każda usługa wie, jak się wylistować.
- **Wyjątki domenowe** zamiast kodów błędów; PTZ na FixedCam → `Unsupported`.
- **Stan w handlerach** — Thrift ma jeden handler na processor, więc obiekt rozróżniamy przez `id` w argumentach.

---

## I1 — Wywołanie dynamiczne (Ice, rozszerzenie I2)

### Pomysł
Wykorzystujemy serwer z I2. Drugi klient Python pisany jest z **Ice Dynamic Invocation API** — bez stubów wygenerowanych ze Slice po stronie klienta.

### Po stronie klienta (Python)
- `Ice.ObjectPrx.ice_invoke(operation, mode, in_params)` — wywołanie po nazwie operacji.
- Argumenty kodowane przez `Ice.OutputStream` (`writeString`, `writeLong`, `writeStruct`...).
- Wynik dekodowany przez `Ice.InputStream`.
- ≥3 różne operacje, ≥1 z nietrywialną strukturą (`State` z I2 — string + long + double).

### Operacje do zademonstrowania
1. `read()` na `dedicated/c01` — zwraca `State` (struct).
2. `inc(delta)` na `shared/c02` — modyfikacja stanu.
3. `setLabel(s)` na `evict/c03` — argument string.
4. (Opcjonalnie) wywołanie z parametrami sczytywanymi z konsoli.

### Po stronie serwera
Bez zmian — ten sam server z I2.

### Demonstracja
- Pokazanie kodu klienta — żaden `import` z generated/.
- Dyskusja przydatności: narzędzia administracyjne, eksploracja, brak typowania w czasie kompilacji, narzut serializacji.

---

## I2 — Effective servant management (Ice, 8 + 2 pkt)

### IDL — `objects.ice`
```ice
module Demo {
  exception NotFound { string what; };
  struct State { string label; long counter; double value; };
  interface Counter {
    State read();
    void inc(long delta);
    void setLabel(string s);
  };
};
```

### 3 strategie zarządzania (różne `category` w `Identity`)

#### 1. `dedicated/<name>` — dedykowany serwant + ASM (lazy)
- `ServantLocator.locate()` przy pierwszym żądaniu tworzy `CounterI` z initial state, dodaje wpis do **ASM** (`adapter.add(servant, id)`) i zwraca.
- Serwant pozostaje w pamięci do końca procesu.

#### 2. `shared/<name>` — jeden współdzielony serwant
- `addServantLocator(sharedLocator, "shared")` — lokator zawsze zwraca tę samą instancję `SharedCounterI`.
- `SharedCounterI` w każdej metodzie czyta `current.id` i sięga do `Map<Identity, State>` w sobie.

#### 3. `evict/<name>` — LRU evictor (+2 pkt)
- Własny `EvictingServantLocator` z LRU cache (max N=5).
- `deactivate(...)` zrzuca stan serwanta do `state/<id>.json`.
- `locate(...)` przy braku w cache czyta z pliku (lub initial state) i tworzy nowego serwanta.
- Po przekroczeniu N — najstarszy serwant evictowany do pliku.

### Java server
- Adapter rejestruje 3 lokatory (`addServantLocator(loc, category)`).
- Logowanie w `locate()`, `deactivate()`, oraz w każdej metodzie biznesowej (`current.id.name`, `current.id.category`, hash serwanta).
- Pokazanie różnicy `checkedCast` vs `uncheckedCast`:
  - `checkedCast` woła `ice_isA` → już wchodzi do `locate()` → serwant powstaje.
  - `uncheckedCast` nie tworzy serwanta dopóki nie wywołasz operacji biznesowej.

### Python client
- `pip install zeroc-ice`.
- Komendy: `read <category>/<name> | inc <category>/<name> <delta> | set-label <category>/<name> <text> | cast <category>/<name> checked|unchecked`.
- Tworzenie proxy: `communicator.stringToProxy("dedicated/c01:tcp -h 127.0.0.1 -p 10010")`.
- Argument `<category>/<name>` parametryzowany w czasie wykonania.

### Demonstracja
- Konsola serwera pokazuje moment instancjonowania, na którym obiekcie i którym serwancie żądanie.
- Test `dedicated/c01` 5x → 1x `locate` + 5x `read` (ten sam serwant).
- Test `shared/c01` ... `shared/c10` → 1 instancja serwanta, 10 stanów w mapie.
- Test `evict/...` z N+2 obiektami → eviction do pliku, restore przy ponownym żądaniu.

---

## I5 — TLS w Ice + Thrift + gRPC (8 pkt)

### Wspólne — generowanie certyfikatów
Skrypt `certs/make-certs.sh` (+ wariant `.ps1`):
1. CA self-signed: `ca.key`, `ca.crt`.
2. Server cert z SAN `127.0.0.1, localhost` podpisany przez CA → `server.crt`, `server.key`.
3. (Opcjonalnie) `client.crt` dla mTLS.
4. Konwersja dla Javy: `server.p12` (PKCS12), `truststore.jks`.

```sh
openssl req -x509 -newkey rsa:4096 -keyout ca.key -out ca.crt -days 365 -nodes -subj "/CN=Lab4-CA"
openssl req -newkey rsa:4096 -keyout server.key -out server.csr -nodes -subj "/CN=localhost"
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 365 \
  -extfile <(printf "subjectAltName=DNS:localhost,IP:127.0.0.1")
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12 -name server -CAfile ca.crt -caname ca-root -chain -password pass:changeit
```

### gRPC — mini-projekt „secure echo” (Java srv + Python cli)
- `echo.proto`:
  ```proto
  syntax = "proto3";
  package echo;
  message Msg { string text = 1; }
  service Echo { rpc Ping(Msg) returns (Msg); }
  ```
- Java: `NettyServerBuilder.forPort(50443).useTransportSecurity(certFile, keyFile).addService(new EchoImpl()).build().start();`
- Python: `creds = grpc.ssl_channel_credentials(open('certs/ca.crt','rb').read()); grpc.secure_channel('localhost:50443', creds)`.
- Spełnia wymóg „w przynajmniej jednej technologii dwa różne języki” (Java + Python).

### Thrift — TLS dorzucone do A1
- Drugi tryb startu serwera (`server-tls.config`):
  - `TSSLTransportFactory.getServerSocket(port, 0, null, params)`,
  - `params.setKeyStore("certs/server.p12", "changeit", "SunX509", "PKCS12")`.
- Python klient:
  ```python
  from thrift.transport.TSSLSocket import TSSLSocket
  transport = TSSLSocket(host, port, ca_certs="certs/ca.crt", validate=True)
  ```

### Ice — TLS dorzucone do I2
- W `server.config` linie aktywujące IceSSL:
  ```
  Ice.Plugin.IceSSL=IceSSL.PluginFactory
  IceSSL.DefaultDir=certs
  IceSSL.Keystore=server.p12
  IceSSL.Password=changeit
  IceSSL.VerifyPeer=1
  Adapter1.Endpoints=tcp -h 0.0.0.0 -p 10010 : ssl -h 0.0.0.0 -p 10443
  IceSSL.Trace.Security=1
  ```
- Python klient: parametry `--Ice.Plugin.IceSSL=IceSSL:createIceSSL --IceSSL.DefaultDir=certs --IceSSL.CAs=ca.crt`.

### Demonstracja bezpieczeństwa
- **Wireshark** porównanie portów plain vs TLS — w drugim widać wyłącznie „Application Data” + handshake.
- `openssl s_client -connect localhost:50443 -showcerts` → certyfikat + łańcuch CA.
- `openssl x509 -in server.crt -text -noout` → zawartość certyfikatu.
- Demo nieudanego połączenia bez `ca.crt` lub z niepasującym CN.

---

## Struktura katalogów `lab4/`

```
lab4/
├── thrift/                       # A1 + część I5
│   ├── pom.xml
│   ├── smarthome.thrift
│   ├── certs/                    # symlink lub kopia z lab4/certs
│   ├── gen-java/                 # Maven target build
│   ├── src/sr/thrift/server/
│   │   ├── ThriftServer.java
│   │   ├── FridgeHandler.java
│   │   ├── OvenHandler.java
│   │   ├── CameraHandler.java
│   │   └── impl/{Fridge,Oven,Camera}Impl.java
│   ├── client_py/
│   │   ├── requirements.txt
│   │   ├── gen/
│   │   └── client.py
│   ├── server-plain.config
│   ├── server-tls.config
│   └── devices-{kitchen,garage}.json
├── ice/                          # I1 + I2 + część I5
│   ├── pom.xml
│   ├── slice/objects.ice
│   ├── certs/
│   ├── generated/
│   ├── server.config             # zawiera linie SSL
│   ├── client.config
│   ├── src/sr/ice/server/
│   │   ├── IceServer.java
│   │   ├── CounterI.java
│   │   ├── SharedCounterI.java
│   │   ├── DedicatedServantLocator.java
│   │   ├── SharedServantLocator.java
│   │   └── EvictingServantLocator.java
│   └── client_py/
│       ├── requirements.txt
│       ├── client.py             # zwykły klient (I2)
│       └── client_dynamic.py     # I1: ice_invoke, bez stubów
├── grpc/                         # tylko mini-projekt I5 (echo TLS)
│   ├── pom.xml
│   ├── echo.proto
│   ├── certs/
│   ├── src/sr/grpc/server/{grpcServer,EchoImpl}.java
│   └── client_py/{requirements.txt, gen/, client.py}
├── certs/                        # wspólne CA + skrypt
│   ├── make-certs.sh
│   ├── make-certs.ps1
│   ├── ca.{key,crt}
│   ├── server.{key,crt,p12}
│   └── README.md
└── README.md
```

---

## Kolejność prac

1. **A1 (Thrift)** — IDL → Java server (3 handlery, multiplex, thread pool) → Python client → 2 procesy, 10 urządzeń, demo wszystkich operacji + wyjątków.
2. **I2 (Ice)** — IDL → 3 lokatory (dedicated/shared/evict) → Python client → demo strategii, `checkedCast` vs `uncheckedCast`.
3. **I1 (Ice)** — `client_dynamic.py` używający `ice_invoke` na serwerze z I2 (≥3 operacje, ≥1 ze strukturą).
4. **I5 cz.1** — `make-certs` + mini-projekt gRPC `Echo` z TLS.
5. **I5 cz.2** — TLS w A1 (Thrift) i I2 (Ice) — bez przebudowy logiki, tylko transport.
6. **README globalne** + nagrywki Wiresharka + screeny `openssl s_client`.

---

## Wymagania ogólne (checklista)

- [ ] Pliki generowane (stub/skeleton) w osobnym katalogu od kodu źródłowego klienta i serwera.
- [ ] Pliki wynikowe kompilacji w osobnym katalogu (Maven `target/`, Python `__pycache__`).
- [ ] Logowanie aktywności na konsoli (id obiektu, metoda, parametry).
- [ ] Klient tekstowy, interaktywny.
- [ ] Wszystkie urządzenia/obiekty osiągalne bez restartu klienta.
- [ ] Wyjątki/błędy zadeklarowane i zgłaszane tam, gdzie to ma sens.
- [ ] Dziedziczenie interfejsów IDL wykorzystane (Thrift `extends DeviceRegistry`, Slice `interface` w Ice — opcjonalnie).
- [ ] Demo działa na 1 maszynie, ale aplikacja poprawna po rozproszeniu.
- [ ] Każdy projekt ma `pom.xml` lub odpowiednik + skrypt budowania.
