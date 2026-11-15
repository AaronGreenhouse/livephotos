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

- `src/main/java/aaron/examples/livephotos/Main.java` — CLI entry point and `findLivePhotoVideos(Path)`.
- `src/test/java/aaron/examples/livephotos/MainTest.java` — JUnit 5 tests.

## Build and test

- Java 25+ (compiled with `maven.compiler.release` 25), Maven, JUnit Jupiter 5.
- Maven is installed at `~/Applications/apache-maven-3.10.0/bin/mvn` and may not be on `PATH`.
- Run tests: `mvn test`
- Run the program: `mvn compile && java -cp target/classes aaron.examples.livephotos.Main <folder>`

## Working style

- This project uses the **Learning** output style (set via `"outputStyle": "Learning"` in `.claude/settings.json`).
  Explain implementation choices and ask the user to write small, meaningful pieces of code for hands-on practice.

## Java coding style

- Always declare variables `final` when applicable: local variables, method parameters, fields, loop variables, lambda parameters, and catch parameters that are never reassigned.
