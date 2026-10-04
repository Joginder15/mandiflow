#!/usr/bin/env bash
cd "$(dirname "$0")/.."
for f in logs/*.pid; do
  [ -e "$f" ] || continue
  kill "$(cat "$f")" 2>/dev/null && echo "Stopped $(basename "$f" .pid)"
  rm -f "$f"
done
