![Live Photos: finds the video halves of iPhone live photos and moves them into a "Live Videos" folder](docs/social-preview.png)

# Live Photos

Live Photos is a deliberately scoped tool. It finds the movie files of iPhone Live Photos and separates them from the iPhone photographs and from ordinary movie files.

It is a Java application, and Claude wrote 100% of its source code, working from my direction in interactive sessions. The project looks simple, and that was on purpose: with a task this simple, I could judge whether the code Claude generated was sane. Claude also generated the Maven setup that bundles the program as a macOS application, and the icon that application uses.

The project was begun on 12 November 2026.

## How it works

When you copy photos from an iPhone, each Live Photo arrives as two files: a still image, `x.heic`, and a short movie, `x.mov`. In a folder, a `.mov` file counts as a Live Photo movie when a `.heic` file with exactly the same base name is beside it.

- Extensions are matched without regard to case, so `IMG_1.HEIC` pairs with `IMG_1.mov`.
- Only the folder you give is searched, not its subfolders.
- A `.mov` file with no matching `.heic` file is an ordinary movie, and it is left alone.

The Live Photo movies are moved, not deleted, into a `Live Videos` subfolder of that folder, so nothing is lost if one is wanted after all. The subfolder is created only when there is something to move.

## Using it

You need Java 26 or later and Maven.

From the command line:

```sh
mvn package
java -jar target/livephotos-1.0-SNAPSHOT.jar <folder>
```

The program lists the moved files and how many there were.

As a macOS application:

```sh
mvn package -Pmacos-app
```

This builds `target/app/LivePhotos.app`. Drop one or more folders onto the app's icon or onto its window, and the window shows what was moved from each.

## Repository layout

| Path | Contents |
| --- | --- |
| [`src/main/java/aaron/examples/livephotos/`](src/main/java/aaron/examples/livephotos/) | The program. `LivePhotoUtils` finds and moves the Live Photo movies, `Main` is the command-line entry point, and `LivePhotoWindow` is the macOS application's window. |
| [`src/test/java/aaron/examples/livephotos/`](src/test/java/aaron/examples/livephotos/) | JUnit 5 tests: the matching and moving rules, and a check that `Info.plist` stays in step with the JDK's jpackage template. |
| [`src/main/jpackage/`](src/main/jpackage/) | Resources for the macOS application: a custom `Info.plist`, so Finder lets folders be dropped on the app, and the app icon. |
| [`pom.xml`](pom.xml) | The Maven build. Its `macos-app` profile bundles the program as a macOS application with jpackage. |
| [`tools/`](tools/) | Generators for the images. `MakeIcon.java` draws the app icon in Java2D, `MakeSocialPreview.java` draws the banner at the top of this page, and `make-icon.sh` regenerates both. |
| [`docs/`](docs/) | Images: the banner, which is also the repository's GitHub social preview, and a call tree of the window's `processAll` method. |
| [`claude-prompt-log.md`](claude-prompt-log.md) | The log of Claude Code prompts and responses (see [The Claude log](#the-claude-log)). |
| [`.claude/`](.claude/) | Claude Code settings, and the hook that writes the log. |
| [`CLAUDE.md`](CLAUDE.md) | Instructions that Claude Code reads at the start of each session: the matching rules, the layout, how to build, and the coding style. |

## The Claude log

[claude-prompt-log.md](claude-prompt-log.md) records my prompts to Claude, each with a timestamp and Claude's reply. A hook in `.claude/hooks/prompt-log.sh` adds each new entry.

The log is incomplete. It occurred to me only partway through the project to have Claude keep it, so it starts on 19 November 2026 at 6:20 pm EST, with the work on the app's icon. It has no record of the earlier sessions that produced the command-line program, its tests, the macOS window, or the Maven packaging. The Git history covers that period.
