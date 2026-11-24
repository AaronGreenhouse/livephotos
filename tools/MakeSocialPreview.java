import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.imageio.ImageIO;

/**
 * Draws the project's GitHub social preview and README banner: the app icon
 * (drawn by MakeIcon, so the two never drift apart) next to the project name
 * and a one-line description. The image is 1280 x 640, the size GitHub
 * recommends for social previews.
 *
 * Run with: java tools/MakeSocialPreview.java <output.png>
 * (Java 22+ compiles MakeIcon.java from the same folder automatically.)
 */
public final class MakeSocialPreview {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 640;

    /** Size of the icon on the image, and its left edge. */
    private static final double ICON = 560;
    private static final double ICON_X = 50;

    /** Left edge of the text. The icon tile's art ends about 10% in from its canvas edge. */
    private static final int TEXT_X = 610;

    private static final String TITLE = "Live Photos";
    private static final String[] TAGLINE = {
        "Finds the video halves of iPhone",
        "live photos and moves them",
        "into a “Live Videos” folder.",
    };

    public static void main(final String[] args) throws IOException {
        final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // A deep blue background, so the icon's light blue tile stands out.
        g.setPaint(new GradientPaint(0, 0, new Color(0x1E4A6E), WIDTH, HEIGHT, new Color(0x0E2233)));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        final Graphics2D icon = (Graphics2D) g.create();
        icon.translate(ICON_X, (HEIGHT - ICON) / 2);
        icon.scale(ICON / 1024, ICON / 1024);
        MakeIcon.draw(icon);
        icon.dispose();

        final String family = fontFamily();
        final Font titleFont = new Font(family, Font.BOLD, 104);
        final Font taglineFont = new Font(family, Font.PLAIN, 40);
        final FontMetrics titleMetrics = g.getFontMetrics(titleFont);
        final FontMetrics taglineMetrics = g.getFontMetrics(taglineFont);
        final int lineHeight = (int) Math.round(taglineMetrics.getHeight() * 1.15);
        final int gapAfterTitle = 36;

        // Center the block of text vertically.
        final int blockHeight = titleMetrics.getAscent() + gapAfterTitle + TAGLINE.length * lineHeight;
        int y = (HEIGHT - blockHeight) / 2 + titleMetrics.getAscent();

        g.setColor(Color.WHITE);
        g.setFont(titleFont);
        g.drawString(TITLE, TEXT_X, y);

        y += gapAfterTitle;
        g.setColor(new Color(0xC9E4F7));
        g.setFont(taglineFont);
        for (final String line : TAGLINE) {
            y += lineHeight;
            g.drawString(line, TEXT_X, y - taglineMetrics.getDescent());
        }
        g.dispose();

        ImageIO.write(image, "png", new File(args[0]));
    }

    /** Helvetica Neue if this machine has it (every Mac does), otherwise Java's default sans serif. */
    private static String fontFamily() {
        final Set<String> installed = Set.of(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        return installed.contains("Helvetica Neue") ? "Helvetica Neue" : Font.SANS_SERIF;
    }
}
