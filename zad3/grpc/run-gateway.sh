#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/target/grpc-gateway-1.0.0.jar"
THRIFT_JAR="$SCRIPT_DIR/../thrift/target/thrift-server-1.0.0.jar"

exec java -cp "$JAR:$THRIFT_JAR" sr.grpc.gateway.SmartHomeGateway "$@"
