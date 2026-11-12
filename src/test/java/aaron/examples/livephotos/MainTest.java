package aaron.examples.livephotos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
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
        final List<Path> result = Main.findLivePhotoVideos(folder);
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
}
