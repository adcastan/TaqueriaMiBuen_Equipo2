#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
echo 'Mi Buen · abre http://127.0.0.1:8787 cuando aparezca la estación.'
echo 'Deja esta consola abierta. Para detener usa Ctrl+C.'
if command -v python3 >/dev/null 2>&1; then
  exec python3 server.py
elif command -v python >/dev/null 2>&1; then
  exec python server.py
else
  echo 'Instala Python 3.10 o posterior y vuelve a iniciar.' >&2
  exit 1
fi
