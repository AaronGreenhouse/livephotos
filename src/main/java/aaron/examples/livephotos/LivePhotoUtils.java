package aaron.examples.livephotos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Utility methods for finding live-photo video files.
 * <p>
 * For every file {@code x.heic} in a folder, if a file {@code x.mov} also
 * exists in the same folder, that {@code .mov} file is a live photo video.
 * Extension matching is case-insensitive (e.g. {@code IMG_1.HEIC} pairs
 * with {@code IMG_1.mov}); the base name must match exactly.
 */
public final class LivePhotoUtils {
    private static final String HEIC = "heic";
    private static final String MOV = "mov";

    /** Name of the subfolder that {@link #moveLivePhotoVideos} moves videos into. */
    public static final String LIVE_VIDEOS_FOLDER = "Live Videos";

    private LivePhotoUtils() {
        // Private constructor prevents instantiation.
    }

    /**
     * Returns the {@code .mov} files in {@code folder} that have a matching
     * {@code .heic} file with the same base name. Not recursive.
     */
    public static List<Path> findLivePhotoVideos(final Path folder) throws IOException {
        final List<String> heicBaseNames = new ArrayList<>();
        final Map<String, Path> movByBaseName = new HashMap<>();

        try (final Stream<Path> entries = Files.list(folder)) {
            entries.filter(Files::isRegularFile).forEach((final Path file) -> {
                final String name = file.getFileName().toString();
                final int dot = name.lastIndexOf('.');
                if (dot <= 0) {
                    return; // no extension, or a dotfile like ".heic"
                }
                final String base = name.substring(0, dot);
                final String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ext.equals(HEIC)) {
                    heicBaseNames.add(base);
                } else if (ext.equals(MOV)) {
                    movByBaseName.put(base, file);
                }
            });
        }

        final List<Path> result = new ArrayList<>();
        for (final String base : heicBaseNames) {
            final Path mov = movByBaseName.get(base);
            if (mov != null) {
                result.add(mov);
            }
        }
        result.sort(null);
        return result;
    }

    /**
     * Moves the live photo videos found by {@link #findLivePhotoVideos} into the
     * {@value #LIVE_VIDEOS_FOLDER} subfolder of {@code folder}, creating it if
     * needed. The subfolder is not created when there is nothing to move. Because
     * the search is not recursive, videos already moved are not found again.
     * <p>
     * Stops at the first file that cannot be moved and throws; files moved
     * before that stay in the subfolder.
     *
     * @return the new locations of the moved files, in the order they were moved
     */
    public static List<Path> moveLivePhotoVideos(final Path folder) throws IOException {
        final List<Path> videos = findLivePhotoVideos(folder);
        if (videos.isEmpty()) {
            return List.of();
        }
        final Path targetDir = Files.createDirectories(folder.resolve(LIVE_VIDEOS_FOLDER));
        final List<Path> moved = new ArrayList<>();
        for (final Path video : videos) {
            // Without REPLACE_EXISTING, Files.move throws rather than overwrite a file.
            moved.add(Files.move(video, destinationFor(targetDir, video)));
        }
        return moved;
    }

    /**
     * Returns where {@code video} should be moved to inside {@code targetDir}.
     */
    static Path destinationFor(final Path targetDir, final Path video) {
        final Path fileName = video.getFileName();
        // TODO(human): handle a file with this name already being in targetDir.
        return targetDir.resolve(fileName);
    }
}
