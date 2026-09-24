#!/usr/bin/env bash
set -euo pipefail

mapping="${1:-app/build/outputs/mapping/release/mapping.txt}"
test -s "$mapping" || {
  echo "Missing or empty R8 mapping file: $mapping" >&2
  exit 1
}

awk '
  /^com\.weekssa\.opraeqforuapp\./ {
    original = $1
    obfuscated = $3
    sub(/:$/, "", obfuscated)
    if (obfuscated != "" && original != obfuscated) renamed = 1
  }
  END {
    if (!renamed) {
      print "R8 mapping contains no renamed application class." > "/dev/stderr"
      exit 1
    }
  }
' "$mapping"

echo "R8 mapping verified: at least one application class was renamed."
