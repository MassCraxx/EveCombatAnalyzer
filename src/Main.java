import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Main implements Analyzer.CallBack {
    public static String version = "0.5";
    private final Path defaultPath = FileSystems.getDefault().getPath(System.getProperty("user.home"), "Documents\\EVE\\logs\\Gamelogs");
    private Analyzer analyzer;
    private WatchDog watchDog;

    // UI
    private JPanel mainPanel;
    private JTabbedPane tabbedPane1;
    private JComboBox<String> comboBox1;
    private JTextField prefixText;
    private JLabel timeLabel;
    private JLabel dpsLabel;
    private JLabel averageDanageLabel;
    private JLabel totalHitsLabel;
    private JLabel versionLabel;

    // Buttons
    private JButton resetButton;
    private JButton parseButton;
    private JButton startButton;
    private JButton stopButton;

    // Tables
    private JTable hitTable;
    private HitTableModel hitTableModel;
    private JTable sumTable;
    private JTextField pathText;
    private JLabel statusLabel;
    private JLabel accuracyLabel;
    private SumTableModel sumTableModel;

    // Threads + Timers
    private Thread watchThread;

    private Timer uiUpdateTimer;
    private final int uiUpdateRate = 100;
    private List<Hit> entryQueue = new ArrayList<>();

    private Timer dpsTimer;
    private long dpsLastTimeMillis = 0;
    private long dpsCurrentTimeMillis = 0;
    private final int timeUpdateRate = 250;

    public static void main(String[] args) {
        Main app = new Main();
        JFrame jFrame = new JFrame("EveCombatAnalyzer");
        jFrame.setContentPane(app.mainPanel);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    public Main() {
        init();

        reset();
    }

    private void init() {
        uiUpdateTimer = new Timer(uiUpdateRate, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUI();
            }
        });
        uiUpdateTimer.setRepeats(false);

        analyzer = new Analyzer(this);

        // Settings
        pathText.setText(defaultPath.toString());

        // Buttons
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });

        parseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Path path = getPath();
                if (path == null || !path.toFile().exists()) {
                    log(getClass(), "Could not parse files.");
                    return;
                }
                analyzer.reset();
                setupTables();
                dpsLastTimeMillis = System.currentTimeMillis();

                analyzer.parseAllFiles(path, prefixText.getText());
            }
        });

        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
        for (Analyzer.Filter filter : Analyzer.Filter.values()) {
            comboBoxModel.addElement(filter.toString());
        }
        comboBox1.setModel(comboBoxModel);
        comboBox1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object item = comboBox1.getSelectedItem();
                if (item != null) {
                    String filterString = item.toString().toUpperCase();
                    Analyzer.Filter filter = Analyzer.Filter.valueOf(filterString);

                    analyzer.updateStats(filter);
                    sumTableModel.update(analyzer.getFilteredStats());
                }
            }
        });

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Path path = getPath();
                if (path == null) {
                    log(getClass(), "Listener could not be started.");
                    return;
                }
                watchDog = new WatchDog(path, analyzer);
                watchThread = new Thread(watchDog, "eveLogWatcher");
                watchThread.start();
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                parseButton.setEnabled(false);
                statusLabel.setText("Listening");

                dpsLastTimeMillis = System.currentTimeMillis();

                dpsTimer = new Timer(timeUpdateRate, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        long now = System.currentTimeMillis();
                        dpsCurrentTimeMillis += now - dpsLastTimeMillis;
                        dpsLastTimeMillis = now;
                        float timeDifSeconds = dpsCurrentTimeMillis / 1000F;
                        timeLabel.setText(getPrettyTime(timeDifSeconds));
                        if (timeDifSeconds > 0) {
                            dpsLabel.setText(String.format(Locale.ENGLISH, "%.2f", analyzer.getTotalDamage() / timeDifSeconds));
                        }
                    }
                });
                dpsTimer.start();
            }
        });
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                watchThread.interrupt();
                while (watchDog.isRunning()) {
                    log(getClass(), "WatchDog still running...");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }

                watchThread = null;
                watchDog = null;

                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                parseButton.setEnabled(true);
                statusLabel.setText("Stopped");

                dpsTimer.stop();
                dpsTimer = null;
            }
        });
        versionLabel.setText("v" + version + "    ");
    }

    private void reset() {
        analyzer.reset();
        setupTables();
        dpsCurrentTimeMillis = 0;

        timeLabel.setText("00:00");
        dpsLabel.setText("0.00");
        averageDanageLabel.setText("0");
        accuracyLabel.setText("0 %");
        totalHitsLabel.setText("0 | 0");
    }

    private Path getPath() {
        String path = pathText.getText();
        File file = new File(path);
        if (file.exists()) {
            return file.toPath();
        }

        JOptionPane.showMessageDialog(null, "Path not found! Please adjust settings.");
        log(getClass(), "Path not valid!");
        return null;
    }

    private String getPrettyTime(float secondsDiff) {
        int minutes = (int) Math.abs(secondsDiff / 60);
        int seconds = (int) Math.abs(secondsDiff % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void setupTables() {
        hitTableModel = new HitTableModel();
        hitTable.setModel(hitTableModel);
        hitTable.getColumnModel().getColumn(0).setPreferredWidth(1);
        hitTable.getColumnModel().getColumn(3).setPreferredWidth(10);

        sumTableModel = new SumTableModelExtended();
        sumTable.setModel(sumTableModel);
        sumTable.setAutoCreateRowSorter(true);
        sumTable.getColumnModel().getColumn(0).setPreferredWidth(200);
//        TableRowSorter<SumTableModel> sorter = new TableRowSorter<>(sumTableModel);
//        sumTable.setRowSorter(sorter);

//        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
//        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
//        sorter.setSortKeys(sortKeys);

    }

    @Override
    public void onNewHit(Hit hit) {
        if (uiUpdateTimer.isRunning()) {
            uiUpdateTimer.restart();
        } else {
            uiUpdateTimer.start();
        }
        entryQueue.add(hit);
    }

    private void updateUI() {
        averageDanageLabel.setText(analyzer.getAverageDamagePerShot() + " | " + analyzer.getAverageDamagePerRealHit());
        long hits = analyzer.getTotalHits();
        long misses = analyzer.getTotalMisses();
        accuracyLabel.setText(String.valueOf(analyzer.getAccuracy()));
        totalHitsLabel.setText(String.valueOf(misses) + " | " + String.valueOf(hits));

        for (Hit hit : entryQueue) {
            hitTableModel.addHit(hit);
        }
        log(getClass(), "Added " + entryQueue.size() + " hits to table.");

        sumTableModel.update(analyzer.getFilteredStats());
        entryQueue.clear();
    }

    public static void log(Class clazz, String log) {
        System.out.println("[" + clazz.getName() + "]: " + log);
    }
}