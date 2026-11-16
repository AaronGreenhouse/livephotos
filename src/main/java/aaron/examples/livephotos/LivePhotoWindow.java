package aaron.examples.livephotos;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.desktop.OpenFilesEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
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
    private final JFrame frame;
    private final JTextArea output;

    private LivePhotoWindow() {
        output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setTransferHandler(new FolderDropHandler());

        final JLabel hint = new JLabel("Drop a folder onto this window or onto the app icon.");
        hint.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        frame = new JFrame("Live Photos");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(hint, BorderLayout.NORTH);
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
                    SwingUtilities.invokeLater(() -> window.processAll(event.getFiles())));
        }
    }

    /** Searches each dropped item that is a folder. Runs on the Swing thread. */
    private void processAll(final List<File> files) {
        for (final File file : files) {
            if (!file.isDirectory()) {
                output.append("Not a folder: " + file + "\n\n");
                continue;
            }
            final Path folder = file.toPath();
            try {
                final List<Path> videos = LivePhotoUtils.findLivePhotoVideos(folder);
                output.append(formatResults(folder, videos));
            } catch (final IOException e) {
                output.append("Error reading " + folder + ": " + e.getMessage() + "\n\n");
            }
        }
    }

    /**
     * Returns the text shown in the window for one searched folder.
     */
    static String formatResults(final Path folder, final List<Path> videos) {
        // TODO(human): build the text to display for this folder's results.
        return folder + ": " + videos.size() + "\n";
    }

    /** Accepts files dragged from Finder onto the window. */
    private final class FolderDropHandler extends TransferHandler {
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
