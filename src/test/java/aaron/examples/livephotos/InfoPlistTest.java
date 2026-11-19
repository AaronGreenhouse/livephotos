package aaron.examples.livephotos;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Checks that the custom {@code src/main/jpackage/Info.plist} still has everything
 * in jpackage's own {@code Info-lite.plist.template}, read from the JDK running the
 * build (the same JDK whose jpackage builds the app). jpackage uses the custom file
 * instead of its template, so anything a newer JDK adds would otherwise be lost.
 */
final class InfoPlistTest {
    private static final String TEMPLATE =
            "/modules/jdk.jpackage/jdk/jpackage/internal/resources/Info-lite.plist.template";
    private static final Path CUSTOM = Path.of("src/main/jpackage/Info.plist");

    /** The app never uses the microphone, so it has no reason to describe that use. */
    private static final Set<String> OMITTED_KEYS = Set.of("NSMicrophoneUsageDescription");

    /**
     * With file associations this expands to a second CFBundleDocumentTypes key,
     * which would clash with the custom file's folder document type.
     */
    private static final Set<String> OMITTED_PLACEHOLDERS = Set.of("DEPLOY_FILE_ASSOCIATIONS");

    private static final Pattern PLACEHOLDER = Pattern.compile("DEPLOY_[A-Z_]+");
    private static final String RESYNC = " Re-sync src/main/jpackage/Info.plist with jpackage's "
            + "Info-lite.plist.template, or list the item as an intended omission in this test.";

    private static String templateText;
    private static String customText;
    private static Element templateDict;
    private static Element customDict;

    @BeforeAll
    static void load() throws Exception {
        final FileSystem jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
        final Path template = jrt.getPath(TEMPLATE);
        assumeTrue(Files.exists(template), "This JDK has no jpackage template: " + TEMPLATE);
        templateText = Files.readString(template);
        customText = Files.readString(CUSTOM);
        templateDict = topDict(templateText);
        customDict = topDict(customText);
    }

    @Test
    void everyTemplateKeyIsPresent() {
        final Set<String> custom = keys(customDict);
        for (final String key : keys(templateDict)) {
            if (!OMITTED_KEYS.contains(key)) {
                assertTrue(custom.contains(key), "Info.plist is missing template key " + key + "." + RESYNC);
            }
        }
    }

    @Test
    void everyTemplatePlaceholderIsPresent() {
        final Matcher m = PLACEHOLDER.matcher(templateText);
        while (m.find()) {
            final String placeholder = m.group();
            if (!OMITTED_PLACEHOLDERS.contains(placeholder)) {
                assertTrue(customText.contains(placeholder),
                        "Info.plist is missing template placeholder " + placeholder + "." + RESYNC);
            }
        }
    }

    @Test
    void minimumSystemVersionNotBelowTemplate() {
        final String template = value(templateDict, "LSMinimumSystemVersion");
        final String custom = value(customDict, "LSMinimumSystemVersion");
        assertTrue(compareVersions(custom, template) >= 0,
                "Info.plist LSMinimumSystemVersion " + custom + " is below the template's " + template + ".");
    }

    @Test
    void declaresFolderDocumentType() {
        assertTrue(customText.contains("<string>public.folder</string>"),
                "Info.plist must declare the public.folder document type so folders can be dropped on the app.");
    }

    /** Parses a plist without fetching its DTD and returns its top-level {@code <dict>}. */
    private static Element topDict(final String text) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Element plist = builder.parse(new InputSource(new StringReader(text))).getDocumentElement();
        for (Node n = plist.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof final Element e && e.getTagName().equals("dict")) {
                return e;
            }
        }
        throw new IOException("No top-level <dict> in plist");
    }

    /** Returns the texts of the {@code <key>} elements directly inside {@code dict}, in order. */
    private static Set<String> keys(final Element dict) {
        final Set<String> keys = new LinkedHashSet<>();
        for (Node n = dict.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof final Element e && e.getTagName().equals("key")) {
                keys.add(e.getTextContent().trim());
            }
        }
        return keys;
    }

    /** Returns the text of the element following {@code <key>name</key>} in {@code dict}. */
    private static String value(final Element dict, final String name) {
        boolean found = false;
        for (Node n = dict.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof final Element e) {
                if (found) {
                    return e.getTextContent().trim();
                }
                found = e.getTagName().equals("key") && e.getTextContent().trim().equals(name);
            }
        }
        throw new AssertionError("No " + name + " in plist");
    }

    /** Compares dotted version numbers part by part; missing parts count as 0. */
    private static int compareVersions(final String a, final String b) {
        final int[] x = Arrays.stream(a.split("\\.")).mapToInt(Integer::parseInt).toArray();
        final int[] y = Arrays.stream(b.split("\\.")).mapToInt(Integer::parseInt).toArray();
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            final int c = Integer.compare(i < x.length ? x[i] : 0, i < y.length ? y[i] : 0);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }
}
