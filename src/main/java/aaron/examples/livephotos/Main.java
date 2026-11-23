package aaron.examples.livephotos;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Entry point. With a folder argument, moves the live-photo video files in
 * that folder into its {@value LivePhotoUtils#LIVE_VIDEOS_FOLDER} subfolder
 * and lists their new locations on the console. With no arguments, prints a usage message and
 * exits with status 1, unless the program was launched as the macOS app
 * (which sets {@code -Dlivephotos.gui=true}); then it opens a window that
 * folders can be dropped onto.
 *
 * @see LivePhotoUtils#findLivePhotoVideos(Path)
 */
public class Main {
    /** System property set by the macOS app's launcher (see pom.xml) to request the window. */
    private static final String GUI_PROPERTY = "livephotos.gui";

    public static void main(final String[] args) throws Exception {
        if (args.length == 0 && Boolean.getBoolean(GUI_PROPERTY) && !GraphicsEnvironment.isHeadless()) {
            LivePhotoWindow.launch();
            return;
        }

        if (args.length != 1) {
            System.err.println("Usage: java -jar livephotos.jar <folder>");
            System.exit(1);
        }

        final Path folder = Path.of(args[0]);
        if (!Files.isDirectory(folder)) {
            System.err.println("Not a directory: " + folder);
            System.exit(1);
        }

        try {
            final List<Path> moved = LivePhotoUtils.moveLivePhotoVideos(folder);
            for (final Path p : moved) {
                System.out.println(p);
            }
            System.out.println("Moved " + moved.size() + " live photo video file(s) to "
                    + folder.resolve(LivePhotoUtils.LIVE_VIDEOS_FOLDER) + ".");
        } catch (final IOException e) {
            System.err.println("Error moving live photo videos: " + e);
            System.exit(2);
        }
    }
}
