package aaron.examples.livephotos;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.desktop.OpenFilesEvent;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

/**
 * Window used when the program runs as a macOS app. Folders dropped onto the
 * app's icon (delivered by the OS as "open files" events) or onto the window
 * itself are searched, and the results are shown in the window.
 */
final class LivePhotoWindow {
    private static final String HINT = "Drop a folder onto this window or onto the app icon.";
    private static final String THINKING = "Thinking…";

    private final JFrame frame;
    private final JTextArea output;
    private final JLabel status;

    /**
     * Number of {@link #processAll} batches submitted but not yet finished.
     * Only read or written on the Swing thread.
     */
    private int pending;

    /**
     * Runs folder searches off the Swing thread. A single daemon thread keeps
     * results in drop order and does not keep the JVM alive on exit.
     */
    private final ExecutorService searcher = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("live-photo-search").daemon().factory());

    private LivePhotoWindow() {
        output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setTransferHandler(new FolderDropHandler());

        status = new JLabel(HINT);
        status.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        frame = new JFrame("Live Photos");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(status, BorderLayout.NORTH);
        frame.add(new JScrollPane(output), BorderLayout.CENTER);
        frame.setSize(700, 450);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Creates the window and registers for folders dropped onto the app icon.
     * Call this early in {@code main}: macOS queues the launch-time "open"
     * event until a handler is registered.
     */
    static void launch() throws InterruptedException, InvocationTargetException {
        final AtomicReference<LivePhotoWindow> created = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> created.set(new LivePhotoWindow()));
        final LivePhotoWindow window = created.get();

        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
            Desktop.getDesktop().setOpenFileHandler((final OpenFilesEvent event) ->
                    window.processAll(event.getFiles()));
        }
    }

    /**
     * Searches each dropped item that is a folder. Safe to call from any thread:
     * the searching happens on {@link #searcher}, and each result is appended to
     * the window on the Swing thread. The top label shows "Thinking…" until
     * every submitted batch has finished.
     */
    private void processAll(final List<File> files) {
        final List<File> snapshot = List.copyOf(files);
        // Posted before execute(), so it reaches the Swing thread before the task's reset below.
        SwingUtilities.invokeLater(() -> {
            pending++;
            status.setText(THINKING);
        });
        searcher.execute(() -> {
            try {
                for (final File file : snapshot) {
                    final String text = describe(file);
                    SwingUtilities.invokeLater(() -> output.append(text));
                }
            } finally {
                SwingUtilities.invokeLater(() -> {
                    if (--pending == 0) {
                        status.setText(HINT);
                    }
                });
            }
        });
    }

    /** Searches one dropped item and returns the text to show for it. Does file I/O. */
    private static String describe(final File file) {
        if (!file.isDirectory()) {
            return "Not a folder: " + file + "\n\n";
        }
        final Path folder = file.toPath();
        try {
            return formatResults(folder, LivePhotoUtils.findLivePhotoVideos(folder));
        } catch (final IOException | UncheckedIOException e) {
            return "Error reading " + folder + ": " + e.getMessage() + "\n\n";
        }
    }

    /**
     * Returns the text shown in the window for one searched folder: a heading
     * line, each video's file name indented two spaces, then the total count.
     */
    static String formatResults(final Path folder, final List<Path> videos) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Processing ").append(folder).append("…\n");
        for (final Path video : videos) {
            sb.append("  ").append(video.getFileName()).append('\n');
        }
        final int count = videos.size();
        sb.append("Total: ").append(count).append(count == 1 ? " video file" : " video files").append("\n\n");
        return sb.toString();
    }

    /**
     * Accepts files dragged from Finder onto the window, and copies the selected
     * output text to the clipboard. Installing a custom handler replaces the text
     * area's built-in one, so the copy side has to be provided here too.
     */
    private final class FolderDropHandler extends TransferHandler {
        @Override
        public int getSourceActions(final JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(final JComponent c) {
            final String selected = output.getSelectedText();
            // Returning null cancels the transfer when nothing is selected.
            return (selected == null || selected.isEmpty()) ? null : new StringSelection(selected);
        }

        @Override
        public boolean canImport(final TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(final TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                @SuppressWarnings("unchecked")
                final List<File> files = (List<File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                processAll(files);
                return true;
            } catch (final UnsupportedFlavorException | IOException e) {
                return false;
            }
        }
    }
}
