#!/bin/sh
# Regenerates src/main/jpackage/LivePhotos.icns from tools/MakeIcon.java (macOS only).
set -e
cd "$(dirname "$0")/.."
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
java tools/MakeIcon.java "$work/LivePhotos.iconset"
iconutil -c icns "$work/LivePhotos.iconset" -o src/main/jpackage/LivePhotos.icns
echo "Wrote src/main/jpackage/LivePhotos.icns"
