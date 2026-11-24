#!/bin/sh
# Regenerates the app icon (src/main/jpackage/LivePhotos.icns) and the social preview /
# README banner (docs/social-preview.png) from tools/MakeIcon.java (macOS only).
set -e
cd "$(dirname "$0")/.."
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
java tools/MakeIcon.java "$work/LivePhotos.iconset"
iconutil -c icns "$work/LivePhotos.iconset" -o src/main/jpackage/LivePhotos.icns
echo "Wrote src/main/jpackage/LivePhotos.icns"
java tools/MakeSocialPreview.java docs/social-preview.png
echo "Wrote docs/social-preview.png"
