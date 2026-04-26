#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
PASS=changeit
export MSYS_NO_PATHCONV=1

openssl req -x509 -newkey rsa:4096 -nodes -days 365 \
    -keyout ca.key -out ca.crt -subj "/CN=Lab4-CA"

cat > server.ext <<EOF
subjectAltName = DNS:localhost, IP:127.0.0.1
EOF
openssl req -newkey rsa:4096 -nodes -keyout server.key -out server.csr -subj "/CN=localhost"
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
    -out server.crt -days 365 -extfile server.ext

openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12 \
    -name server -CAfile ca.crt -caname ca-root -chain -password pass:$PASS

openssl req -x509 -newkey rsa:4096 -nodes -days 365 \
    -keyout bad-ca.key -out bad-ca.crt -subj "/CN=Bad-CA" 2>/dev/null

rm -f server.csr server.ext
echo "Generated: ca.crt, server.crt, server.key, server.p12, bad-ca.crt"
