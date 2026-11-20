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
 * Draws the app icon: a photo print with a green checkmark, and a three-frame
 * film strip of the same scene crossed out, because the app finds the video
 * halves of live photos so they can be removed. Writes every size macOS needs
 * into an .iconset folder; make-icon.sh then packs it into
 * src/main/jpackage/LivePhotos.icns.
 *
 * Run with: java tools/MakeIcon.java <output.iconset>
 */
public final class MakeIcon {
    /** Everything is drawn on a 1024 x 1024 canvas and scaled to each size. */
    private static final double CANVAS = 1024;

    /**
     * The photo print's bounds before it is tilted, and its tilt in degrees. The
     * photo and the film are kept apart so the kept photo and the deleted film
     * read as two separate things.
     */
    private static final double PHOTO_X = 180;
    private static final double PHOTO_Y = 170;
    private static final double PHOTO_W = 380;
    private static final double PHOTO_H = 320;
    private static final double PHOTO_TILT = -8;

    /** The film strip's bounds before it is tilted, and its tilt in degrees. */
    private static final double FILM_X = 280;
    private static final double FILM_Y = 605;
    private static final double FILM_W = 560;
    private static final double FILM_H = 230;
    private static final double FILM_TILT = 8;

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
        g.setColor(new Color(0xA6D6F5));
        g.fill(tile);

        final Graphics2D clip = (Graphics2D) g.create();
        clip.clip(tile);
        drawPhoto(clip);
        drawFilm(clip);
        clip.dispose();
    }

    /** A white-bordered photo print of a sunny landscape, tilted, at the back upper left. */
    private static void drawPhoto(final Graphics2D g0) {
        final Graphics2D g = (Graphics2D) g0.create();
        g.rotate(Math.toRadians(PHOTO_TILT), PHOTO_X + PHOTO_W / 2, PHOTO_Y + PHOTO_H / 2);
        final Shape print = new RoundRectangle2D.Double(PHOTO_X, PHOTO_Y, PHOTO_W, PHOTO_H, 20, 20);
        shadow(g, print, 22, 14, 0.04f);
        g.setColor(Color.WHITE);
        g.fill(print);

        // The picture inside the white border, which is wider at the bottom like a print.
        // It is the same moment as the film's middle frame.
        drawScene(g, PHOTO_X + 24, PHOTO_Y + 24, PHOTO_X + PHOTO_W - 24, PHOTO_Y + PHOTO_H - 54,
                SUN_PATH[1][0], SUN_PATH[1][1]);

        // A green checkmark badge on the top-right corner: this is the one that is kept.
        drawCheckBadge(g, PHOTO_X + PHOTO_W - 30, PHOTO_Y + 30, 58);
        g.dispose();
    }

    /**
     * The sun's center in each of the film's three frames, as fractions of the
     * picture's width and height. The sun rises from frame to frame, so the strip
     * reads as a short video; the photo shows the middle frame, the way a live
     * photo's still image is one moment from its video.
     */
    private static final double[][] SUN_PATH = {{0.18, 0.40}, {0.25, 0.27}, {0.32, 0.16}};

    /**
     * Draws the landscape (sky, sun, two hills) filling the given rectangle, with the
     * sun centered at ({@code sunX}, {@code sunY}) as fractions of the width and height.
     * Everything is placed by fractions, so the scene fits both the photo and the
     * differently shaped film frames.
     */
    private static void drawScene(final Graphics2D g0, final double left, final double top,
            final double right, final double bottom, final double sunX, final double sunY) {
        final double w = right - left;
        final double h = bottom - top;
        final Graphics2D p = (Graphics2D) g0.create();
        final Shape picture = new RoundRectangle2D.Double(left, top, w, h, 0.06 * h, 0.06 * h);
        p.clip(picture);
        p.setPaint(new GradientPaint(0, (float) top, new Color(0x8ED8FF),
                0, (float) bottom, new Color(0xD8F2FF)));
        p.fill(picture);

        final double sun = 0.27 * h;
        p.setColor(new Color(0xFFCC33));
        p.fill(new Ellipse2D.Double(left + sunX * w - sun / 2, top + sunY * h - sun / 2, sun, sun));

        final Path2D farHill = new Path2D.Double();
        farHill.moveTo(left, bottom - 0.25 * h);
        farHill.lineTo(left + 0.28 * w, bottom - 0.70 * h);
        farHill.lineTo(left + 0.52 * w, bottom - 0.37 * h);
        farHill.lineTo(left + 0.74 * w, bottom - 0.62 * h);
        farHill.lineTo(right, bottom - 0.25 * h);
        farHill.lineTo(right, bottom + 1);
        farHill.lineTo(left, bottom + 1);
        farHill.closePath();
        p.setColor(new Color(0x4CAF6A));
        p.fill(farHill);

        final Path2D nearHill = new Path2D.Double();
        nearHill.moveTo(left, bottom - 0.12 * h);
        nearHill.quadTo(left + 0.36 * w, bottom - 0.41 * h, right, bottom - 0.10 * h);
        nearHill.lineTo(right, bottom + 1);
        nearHill.lineTo(left, bottom + 1);
        nearHill.closePath();
        p.setColor(new Color(0x2E8B4E));
        p.fill(nearHill);
        p.dispose();
    }

    /** A green circle with a white checkmark, centered on (cx, cy), outlined in white. */
    private static void drawCheckBadge(final Graphics2D g0, final double cx, final double cy, final double r) {
        final Graphics2D g = (Graphics2D) g0.create();
        final Shape outer = new Ellipse2D.Double(cx - r - 8, cy - r - 8, 2 * (r + 8), 2 * (r + 8));
        shadow(g, outer, 10, 6, 0.04f);
        g.setColor(Color.WHITE);
        g.fill(outer);
        g.setColor(new Color(0x34C759));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));

        final Path2D check = new Path2D.Double();
        check.moveTo(cx - 0.45 * r, cy + 0.02 * r);
        check.lineTo(cx - 0.12 * r, cy + 0.35 * r);
        check.lineTo(cx + 0.48 * r, cy - 0.33 * r);
        g.setStroke(new BasicStroke((float) (0.24 * r), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(check);
        g.dispose();
    }

    /**
     * A dark three-frame film strip with sprocket holes, tilted, at the front lower
     * right, crossed out by a red X centered on the strip.
     */
    private static void drawFilm(final Graphics2D g0) {
        final Graphics2D g = (Graphics2D) g0.create();
        final double cx = FILM_X + FILM_W / 2;
        final double cy = FILM_Y + FILM_H / 2;
        g.rotate(Math.toRadians(FILM_TILT), cx, cy);
        final Shape strip = new RoundRectangle2D.Double(FILM_X, FILM_Y, FILM_W, FILM_H, 36, 36);
        shadow(g, strip, 18, 10, 0.035f);
        g.setColor(new Color(0x1F1F24));
        g.fill(strip);

        // Sprocket holes along the top and bottom edges.
        g.setColor(new Color(0xE8E8EE));
        for (double x = FILM_X + 20; x + 26 <= FILM_X + FILM_W - 16; x += 48) {
            g.fill(new RoundRectangle2D.Double(x, FILM_Y + 14, 26, 24, 7, 7));
            g.fill(new RoundRectangle2D.Double(x, FILM_Y + FILM_H - 38, 26, 24, 7, 7));
        }

        // Three frames side by side, each showing the photo's scene a moment apart.
        final double inset = 20;
        final double gap = 14;
        final double frameTop = FILM_Y + 50;
        final double frameH = FILM_H - 100;
        final double frameW = (FILM_W - 2 * inset - 2 * gap) / 3;
        for (int i = 0; i < 3; i++) {
            final double x = FILM_X + inset + i * (frameW + gap);
            drawScene(g, x, frameTop, x + frameW, frameTop + frameH, SUN_PATH[i][0], SUN_PATH[i][1]);
        }

        // Drawn in the strip's tilted coordinates, so the X stays centered on it.
        drawCross(g, cx, cy, 125);
        g.dispose();
    }

    /** A red X centered on (cx, cy), with a white outline so it reads on any color. */
    private static void drawCross(final Graphics2D g0, final double cx, final double cy, final double r) {
        final Graphics2D g = (Graphics2D) g0.create();
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
