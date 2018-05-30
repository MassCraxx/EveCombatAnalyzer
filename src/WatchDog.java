import java.io.*;
import java.nio.file.*;
import java.util.List;

public class WatchDog implements Runnable {
    private Listener listener;
    private Path path;
    private boolean running = false;
    private boolean stop = false;

    private long lastLineCount = -1;
    private String lastFileChecked = "";

    WatchDog(Path path, Listener listener) {
        this.path = path;
        Main.log(getClass(), "Watchdog created.");
        this.listener = listener;
    }

    @Override
    public void run() {
        // Setup Watchdog
        Main.log(getClass(), "Watching files in: " + path);
        WatchService watchService = null;
        WatchKey wk = null;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            while (!stop) {
                running = true;
                wk = watchService.take();
                List<WatchEvent<?>> events = wk.pollEvents();
                for (WatchEvent<?> event : events) {
                    //we only register "ENTRY_MODIFY" so the context is always a Path.
                    final Path changed = (Path) event.context();
                    String filePath = path + File.separator + changed;
                    Main.log(getClass(), "File changed: " + changed);
                    File file = new File(filePath);
                    onFileChanged(file);
                    listener.onFileChanged(file);
                }
                // reset the key
                boolean valid = wk.reset();
                if (!valid) {
                    Main.log(getClass(), "!!! Key has been unregistered !!!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Main.log(getClass(), "Interrupted!");
        } finally {
            Main.log(getClass(), "Shutdown.");
            running = false;
            if(wk != null){
                wk.cancel();
                wk = null;
            }
            if (watchService != null) {
                try {
                    watchService.close();
                    watchService = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onFileChanged(File file){
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            long lineCounter = 0;
            while ((sCurrentLine = br.readLine()) != null) {
                if (!lastFileChecked.equals(file.getName())) {
                    Main.log(getClass(), "Changed file is new.");
                    // If new file, skip existing lines.
                    long lastLine = 0;
                    while (br.readLine() != null) {
                        lastLine++;
                    }
                    lastLineCount = lastLine;

                    lastFileChecked = file.getName();
                    br = new BufferedReader(new FileReader(file));
                    continue;
                }
                lineCounter++;
                // Only check lines not checked before
                if (lineCounter > lastLineCount) {
                    listener.onNewLine(sCurrentLine);
                }
            }
            // Not sure why this would happen...
            if(lineCounter != 0) {
                lastLineCount = lineCounter;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br!= null) {
                    br.close();
                }
            } catch (IOException ex){
                ex.printStackTrace();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public interface Listener {
        void onFileChanged(File path);
        void onNewLine(String newLine);
    }
}
