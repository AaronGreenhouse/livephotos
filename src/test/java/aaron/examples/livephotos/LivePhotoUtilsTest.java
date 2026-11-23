package aaron.examples.livephotos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LivePhotoUtilsTest {
    private static final List<String> FILES = List.of(
            "a.HEIC", "a.mov",
            "b.HeIc", "b.MOV",
            "c.heic", "c.MoV",
            "d.heic", "e.heic", "f.heic", "g.heic",
            "h.heic", "h.mov",
            "k.MoV",
            "z.mov");

    @TempDir
    Path folder;

    private Set<String> found;

    @BeforeEach
    void setUp() throws IOException {
        for (final String name : FILES) {
            Files.createFile(folder.resolve(name));
        }
        final List<Path> result = LivePhotoUtils.findLivePhotoVideos(folder);
        found = result.stream()
                .map((final Path p) -> p.getFileName().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void findsExactlyTheMatchingMovFiles() {
        assertEquals(Set.of("a.mov", "b.MOV", "c.MoV", "h.mov"), found);
    }

    @Test
    void neverIncludesMovWithoutHeic() {
        assertFalse(found.contains("k.MoV"));
        assertFalse(found.contains("z.mov"));
    }

    @Test
    void neverIncludesHeicFiles() {
        for (final String name : found) {
            assertFalse(name.toLowerCase(Locale.ROOT).endsWith(".heic"), name);
        }
    }

    @Test
    void moveLeavesOnlyNonMatchingFilesBehind() throws IOException {
        LivePhotoUtils.moveLivePhotoVideos(folder);
        final Set<String> remaining = namesIn(folder);
        final Set<String> expected = FILES.stream()
                .filter((final String name) -> !found.contains(name))
                .collect(Collectors.toSet());
        expected.add(LivePhotoUtils.LIVE_VIDEOS_FOLDER);
        assertEquals(expected, remaining);
    }

    @Test
    void moveReturnsTheNewLocations() throws IOException {
        final Path target = folder.resolve(LivePhotoUtils.LIVE_VIDEOS_FOLDER);
        final List<Path> moved = LivePhotoUtils.moveLivePhotoVideos(folder);
        for (final Path p : moved) {
            assertEquals(target, p.getParent());
            assertTrue(Files.isRegularFile(p), p.toString());
        }
        assertEquals(found, namesIn(target));
    }

    @Test
    void secondMoveFindsNothing() throws IOException {
        LivePhotoUtils.moveLivePhotoVideos(folder);
        assertEquals(List.of(), LivePhotoUtils.moveLivePhotoVideos(folder));
    }

    @Test
    void moveCreatesNoFolderWhenNothingMatches(@TempDir final Path empty) throws IOException {
        Files.createFile(empty.resolve("k.mov"));
        assertEquals(List.of(), LivePhotoUtils.moveLivePhotoVideos(empty));
        assertFalse(Files.exists(empty.resolve(LivePhotoUtils.LIVE_VIDEOS_FOLDER)));
    }

    private static Set<String> namesIn(final Path dir) throws IOException {
        try (final Stream<Path> entries = Files.list(dir)) {
            return entries.map((final Path p) -> p.getFileName().toString())
                    .collect(Collectors.toSet());
        }
    }
}
