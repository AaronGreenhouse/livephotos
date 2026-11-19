import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Draws the app icon: a photo print in front of a film frame, with the film
 * crossed out, because the app finds the video halves of live photos so they
 * can be removed. Writes every size macOS needs into an .iconset folder;
 * make-icon.sh then packs it into src/main/jpackage/LivePhotos.icns.
 *
 * Run with: java tools/MakeIcon.java <output.iconset>
 */
public final class MakeIcon {
    /** Everything is drawn on a 1024 x 1024 canvas and scaled to each size. */
    private static final double CANVAS = 1024;

    public static void main(final String[] args) throws IOException {
        final Path out = Path.of(args[0]);
        Files.createDirectories(out);
        for (final int size : new int[] {16, 32, 128, 256, 512}) {
            write(size, out.resolve("icon_" + size + "x" + size + ".png"));
            write(size * 2, out.resolve("icon_" + size + "x" + size + "@2x.png"));
        }
    }

    private static void write(final int pixels, final Path file) throws IOException {
        final BufferedImage image = new BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(pixels / CANVAS, pixels / CANVAS);
        draw(g);
        g.dispose();
        ImageIO.write(image, "png", new File(file.toString()));
    }

    private static void draw(final Graphics2D g) {
        // Background tile, using Apple's icon grid: an 824-unit rounded square, centered.
        final Shape tile = new RoundRectangle2D.Double(100, 100, 824, 824, 370, 370);
        shadow(g, tile, 12, 28, 0.03f);
        g.setPaint(new GradientPaint(0, 100, new Color(0x6FD3FF), 0, 924, new Color(0x2F6FE0)));
        g.fill(tile);

        final Graphics2D clip = (Graphics2D) g.create();
        clip.clip(tile);
        drawFilm(clip);
        drawPhoto(clip);
        drawCross(clip);
        clip.dispose();
    }

    /** A dark film frame with sprocket holes and a play symbol, tilted, at the back right. */
    private static void drawFilm(final Graphics2D g0) {
        final Graphics2D g = (Graphics2D) g0.create();
        g.rotate(Math.toRadians(10), 620, 380);
        final Shape strip = new RoundRectangle2D.Double(390, 220, 460, 320, 36, 36);
        shadow(g, strip, 18, 10, 0.035f);
        g.setColor(new Color(0x1F1F24));
        g.fill(strip);

        // Sprocket holes along the top and bottom edges.
        g.setColor(new Color(0xE8E8EE));
        for (int x = 418; x < 830; x += 58) {
            g.fill(new RoundRectangle2D.Double(x, 238, 30, 26, 8, 8));
            g.fill(new RoundRectangle2D.Double(x, 496, 30, 26, 8, 8));
        }

        // The picture area, with a play triangle.
        g.setPaint(new GradientPaint(0, 282, new Color(0x5B5F6E), 0, 478, new Color(0x3A3D48)));
        g.fill(new RoundRectangle2D.Double(418, 282, 404, 196, 16, 16));
        final Path2D play = new Path2D.Double();
        play.moveTo(588, 324);
        play.lineTo(588, 436);
        play.lineTo(680, 380);
        play.closePath();
        g.setColor(new Color(255, 255, 255, 220));
        g.fill(play);
        g.dispose();
    }

    /** A white-bordered photo print of a sunny landscape, tilted, at the front left. */
    private static void drawPhoto(final Graphics2D g0) {
        final Graphics2D g = (Graphics2D) g0.create();
        g.rotate(Math.toRadians(-8), 410, 640);
        final Shape print = new RoundRectangle2D.Double(200, 470, 420, 350, 20, 20);
        shadow(g, print, 22, 14, 0.04f);
        g.setColor(Color.WHITE);
        g.fill(print);

        final Shape picture = new RoundRectangle2D.Double(226, 496, 368, 266, 8, 8);
        final Graphics2D p = (Graphics2D) g.create();
        p.clip(picture);
        p.setPaint(new GradientPaint(0, 496, new Color(0x8ED8FF), 0, 762, new Color(0xD8F2FF)));
        p.fill(picture);
        p.setColor(new Color(0xFFCC33));
        p.fill(new Ellipse2D.Double(480, 524, 76, 76));

        final Path2D farHill = new Path2D.Double();
        farHill.moveTo(226, 700);
        farHill.lineTo(330, 590);
        farHill.lineTo(420, 670);
        farHill.lineTo(500, 610);
        farHill.lineTo(600, 700);
        farHill.lineTo(600, 770);
        farHill.lineTo(226, 770);
        farHill.closePath();
        p.setColor(new Color(0x4CAF6A));
        p.fill(farHill);

        final Path2D nearHill = new Path2D.Double();
        nearHill.moveTo(226, 730);
        nearHill.quadTo(360, 660, 600, 735);
        nearHill.lineTo(600, 770);
        nearHill.lineTo(226, 770);
        nearHill.closePath();
        p.setColor(new Color(0x2E8B4E));
        p.fill(nearHill);
        p.dispose();
        g.dispose();
    }

    /** A red X over the exposed part of the film, with a white outline so it reads on any color. */
    private static void drawCross(final Graphics2D g0) {
        final Graphics2D g = (Graphics2D) g0.create();
        final double cx = 690;
        final double cy = 340;
        final double r = 130;
        final Path2D cross = new Path2D.Double();
        cross.moveTo(cx - r, cy - r);
        cross.lineTo(cx + r, cy + r);
        cross.moveTo(cx + r, cy - r);
        cross.lineTo(cx - r, cy + r);

        g.setStroke(new BasicStroke(92, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(cross);
        g.setStroke(new BasicStroke(60, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xFF3B30));
        g.draw(cross);
        g.dispose();
    }

    /**
     * Draws a soft shadow under {@code shape} by layering widening, faint outlines,
     * shifted down by {@code dropY}.
     */
    private static void shadow(final Graphics2D g0, final Shape shape, final int spread,
            final double dropY, final float alphaPerLayer) {
        final Graphics2D g = (Graphics2D) g0.create();
        g.transform(AffineTransform.getTranslateInstance(0, dropY));
        g.setColor(new Color(0f, 0f, 0f, alphaPerLayer));
        for (int w = spread * 2; w > 0; w -= 2) {
            g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(shape);
        }
        g.fill(shape);
        g.dispose();
    }
}
