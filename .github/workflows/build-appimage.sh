#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-LibreLauncher}"
ARCH="${ARCH:-x86_64}"
DIST_DIR="${DIST_DIR:-target/dist}"
WORK_DIR="${WORK_DIR:-target/appimage}"

APP_IMAGE_DIR="$DIST_DIR/$APP_NAME"
APPDIR="$WORK_DIR/$APP_NAME.AppDir"
OUTPUT="$DIST_DIR/$APP_NAME-$ARCH.AppImage"
ICON_OVERRIDE=".github/workflows/$APP_NAME.png"
APPIMAGETOOL_URL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage"

if [ ! -x "$APP_IMAGE_DIR/bin/$APP_NAME" ]; then
  echo "error: $APP_IMAGE_DIR/bin/$APP_NAME not found - run 'mvn package -Papp-image' first" >&2
  exit 1
fi

echo "==> Assembling AppDir"
rm -rf "$APPDIR"
mkdir -p "$APPDIR/usr"
cp -a "$APP_IMAGE_DIR/." "$APPDIR/usr/"

if [ -f "$ICON_OVERRIDE" ]; then
  icon_src="$ICON_OVERRIDE"
else
  icon_src="$(find "$APPDIR/usr" -maxdepth 3 -type f -name '*.png' -print -quit)"
fi
if [ -z "${icon_src:-}" ] || [ ! -f "$icon_src" ]; then
  echo "error: no PNG icon found (add one at $ICON_OVERRIDE)" >&2
  exit 1
fi
cp "$icon_src" "$APPDIR/$APP_NAME.png"
ln -sf "$APP_NAME.png" "$APPDIR/.DirIcon"

cat > "$APPDIR/$APP_NAME.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=$APP_NAME
Exec=$APP_NAME
Icon=$APP_NAME
Categories=Utility;
Terminal=false
EOF

cat > "$APPDIR/AppRun" <<EOF
#!/bin/sh
HERE="\$(dirname "\$(readlink -f "\$0")")"
exec "\$HERE/usr/bin/$APP_NAME" "\$@"
EOF
chmod +x "$APPDIR/AppRun"

mkdir -p "$WORK_DIR"
TOOL="$WORK_DIR/appimagetool-x86_64.AppImage"
if [ ! -x "$TOOL" ]; then
  echo "==> Downloading appimagetool"
  curl -fL --retry 3 -o "$TOOL" "$APPIMAGETOOL_URL"
  chmod +x "$TOOL"
fi

echo "==> Building $OUTPUT"
ARCH="$ARCH" APPIMAGE_EXTRACT_AND_RUN=1 "$TOOL" -n "$APPDIR" "$OUTPUT"
chmod +x "$OUTPUT"

ls -lah "$OUTPUT"
echo "Done: $OUTPUT"
