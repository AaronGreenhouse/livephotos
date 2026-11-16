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
- `src/main/java/aaron/examples/livephotos/Main.java` — entry point: CLI with a folder argument, `LivePhotoWindow` with no arguments.
- `src/main/java/aaron/examples/livephotos/LivePhotoWindow.java` — Swing window for the macOS app; receives folders dropped on the app icon (`Desktop.setOpenFileHandler`) or the window.
- `src/test/java/aaron/examples/livephotos/LivePhotoUtilsTest.java` — JUnit 5 tests.
- `src/main/jpackage/Info.plist` — custom Info.plist for the macOS app (jpackage's template plus a `public.folder` document type so Finder accepts dropped folders). Keep the `DEPLOY_*` placeholders; jpackage fills them in.

## Build and test

- Java 25+ (compiled with `maven.compiler.release` 25), Maven, JUnit Jupiter 5.
- Maven is installed at `~/Applications/apache-maven-3.10.0/bin/mvn` and may not be on `PATH`.
- Run tests: `mvn test`
- Run the program: `mvn package && java -jar target/livephotos-1.0-SNAPSHOT.jar <folder>`
- Build the drag-and-drop macOS app (macOS only): `mvn package -Pmacos-app` → `target/app/LivePhotos.app`.
  The `macos-app` profile uses `org.panteleyev:jpackage-maven-plugin` with `--resource-dir src/main/jpackage`.

## Working style

- This project uses the **Learning** output style (set via `"outputStyle": "Learning"` in `.claude/settings.json`).
  Explain implementation choices and ask the user to write small, meaningful pieces of code for hands-on practice.

## Java coding style

- Always declare variables `final` when applicable: local variables, method parameters, fields, loop variables, lambda parameters, and catch parameters that are never reassigned.
