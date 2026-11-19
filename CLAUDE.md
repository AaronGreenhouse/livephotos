# CLAUDE.md

## Project overview

Small Java CLI that finds iPhone "live photo" video files in a folder so they can be removed.
A file `x.mov` is a live photo video when a file `x.heic` exists in the same folder.

Matching rules (covered by tests — keep them passing):
- Extension matching is case-insensitive (`.HEIC`, `.HeIc`, `.MoV`, ... all count).
- Base names must match exactly.
- Only the given folder is searched (not recursive).
- Only `.mov` files are ever returned; never `.heic` files, and never a `.mov` without a matching `.heic`.

The program currently only lists the matching files. Deleting them is planned (see README.md) but not implemented.

## Layout

- `src/main/java/aaron/examples/livephotos/LivePhotoUtils.java` — `findLivePhotoVideos(Path)`, the matching logic.
- `src/main/java/aaron/examples/livephotos/Main.java` — entry point: CLI with a folder argument; with no arguments prints usage and exits 1, unless `-Dlivephotos.gui=true` is set (the macOS app sets it), which opens `LivePhotoWindow`.
- `src/main/java/aaron/examples/livephotos/LivePhotoWindow.java` — Swing window for the macOS app; receives folders dropped on the app icon (`Desktop.setOpenFileHandler`) or the window.
- `src/test/java/aaron/examples/livephotos/LivePhotoUtilsTest.java` — JUnit 5 tests.
- `src/test/java/aaron/examples/livephotos/InfoPlistTest.java` — checks `Info.plist` against the building JDK's jpackage `Info-lite.plist.template` (all keys and `DEPLOY_*` placeholders present, minimum macOS not below the template's).
- `src/main/jpackage/Info.plist` — custom Info.plist for the macOS app (jpackage's template plus a `public.folder` document type so Finder accepts dropped folders). Keep the `DEPLOY_*` placeholders; jpackage fills them in. `LSMinimumSystemVersion` is 11.0 on purpose (the bundled runtime's real minimum; the template says 10.11). The header comment lists the template items left out on purpose.
- `src/main/jpackage/LivePhotos.icns` — the app icon (photo in front of a crossed-out film frame). Generated; edit `tools/MakeIcon.java` (Java2D drawing) and rerun `tools/make-icon.sh` rather than editing the .icns.

## Build and test

- Java 26+ (compiled with `maven.compiler.release` 26), Maven, JUnit Jupiter 5.
- Maven is installed at `~/Applications/apache-maven-3.10.0/bin/mvn` and may not be on `PATH`.
- Run tests: `mvn test`
- Run the program: `mvn package && java -jar target/livephotos-1.0-SNAPSHOT.jar <folder>`
- Build the drag-and-drop macOS app (macOS only): `mvn package -Pmacos-app` → `target/app/LivePhotos.app`.
  The `macos-app` profile uses `org.panteleyev:jpackage-maven-plugin` with `--resource-dir src/main/jpackage`. It also passes `-Dlivephotos.gui=true` via `<javaOptions>` so the app opens the window.

## Java coding style

- Always declare variables `final` when applicable: local variables, method parameters, fields, loop variables, lambda parameters, and catch parameters that are never reassigned.
