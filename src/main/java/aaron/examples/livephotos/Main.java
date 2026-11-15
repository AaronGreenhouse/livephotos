package aaron.examples.livephotos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Command-line entry point: lists the live-photo video files in a folder.
 *
 * @see LivePhotoUtils#findLivePhotoVideos(Path)
 */
public class Main {
    public static void main(final String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java aaron.examples.livephotos.Main <folder>");
            System.exit(1);
        }

        final Path folder = Path.of(args[0]);
        if (!Files.isDirectory(folder)) {
            System.err.println("Not a directory: " + folder);
            System.exit(1);
        }

        try {
            final List<Path> liveVideos = LivePhotoUtils.findLivePhotoVideos(folder);
            for (final Path p : liveVideos) {
                System.out.println(p);
            }
            System.out.println("Found " + liveVideos.size() + " live photo video file(s).");
        } catch (final IOException e) {
            System.err.println("Error reading folder: " + e.getMessage());
            System.exit(2);
        }
    }
}
