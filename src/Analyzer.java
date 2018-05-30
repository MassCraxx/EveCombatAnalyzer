import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO: Other message structures (no weapon type etc)

public class Analyzer implements WatchDog.Listener {
    private CallBack callBack;

    private boolean parseWholeFile = false;

    private ArrayList<Hit> hits = new ArrayList<>();
    private HashMap<String, SummedStats> filteredStats = new HashMap<>();
    private long totalDamage = 0;

    private Pattern hitPattern = Pattern.compile("(\\d+)<.+>(.+)</.+- (\\w+)");
    private Pattern hitPatternWeapon = Pattern.compile("(\\d+)<.+>(.+)</.+- (.+) - (\\w+)");
    private Pattern missPattern = Pattern.compile("Your (.+) misses (.+) c.+");

    private Filter activeFilter = Filter.WEAPON;
    private long totalHits = 0;
    private long totalMisses = 0;

    Analyzer(CallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    public void onNewLine(String newLine) {
        if (!parseWholeFile) {
            checkForHit(newLine);
        }
    }

    @Override
    public void onFileChanged(File file) {
        if (parseWholeFile) {
            parseWholeFile(file);
        }
    }

    public void parseWholeFile(File file) {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                checkForHit(sCurrentLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void parseAllFiles(Path defaultPath, String prefix) {
        Main.log(getClass(), "Starting to parse all log files.");
        long timeStarted = System.currentTimeMillis();
        File dir = defaultPath.toFile();
        if (dir == null) {
            Main.log(getClass(), "Could not parse files: path.toFile was null!");
            return;
        } else {
            File[] files = dir.listFiles();
            if (files == null) {
                Main.log(getClass(), "ERROR: Could not list files!");
                return;
            }
            int counter = 0;
            for (File file : files) {
                if (prefix == null || file.getName().startsWith(prefix)) {
                    parseWholeFile(file);
                    counter++;
                }
            }
            Main.log(getClass(), "Parsed " + counter + " files.");
        }
        String timeTaken = String.valueOf((System.currentTimeMillis() - timeStarted) / 1000);
        Main.log(getClass(), "Parsing took " + timeTaken + "s");
    }

    private void checkForHit(String line) {
        // Combat line
        if (line.contains("(combat)")) {
            // Hit line
            Hit hit = null;
            if (line.contains(">to<")) {
                // Hit detected
                hit = parseHit(line);
                if (hit == null) {
                    Main.log(getClass(), "Hit was null!\n" + line);
                    return;
                }

//            Main.log(getClass(),line);
                totalHits++;
                totalDamage += hit.getDamage();
                hits.add(hit);
                addStat(hit);

            } else if (line.contains("Your")) {
                // Miss detected
                Matcher m = missPattern.matcher(line);
                if (m.find()) {
                    String weapon = m.group(1);
                    String enemy = m.group(2);
                    weapon = weapon.replace("group of ", "");

                    totalMisses++;
                    hit = new Hit(0, enemy, weapon, Hit.Quality.MISS);
                    hits.add(hit);
                    addMiss(weapon, enemy);
                }
            }
            if (hit != null) {
                callBack.onNewHit(hit);
            }
        }
    }

    private void addStat(Hit hit) {
        String key = null;
        // Update SummedStats
        switch (activeFilter) {
            case ENEMY:
                key = hit.getEnemy();
                break;
            case WEAPON:
                key = hit.getWeapon();
                break;
        }
        SummedStats stats = filteredStats.get(key);
        if (stats == null) {
            stats = new SummedStats();
        }
        stats.addHit(hit);
        filteredStats.put(key, stats);
    }

    private void addMiss(String weapon, String enemy) {
        String key = null;
        // Update SummedStats
        switch (activeFilter) {
            case ENEMY:
                key = enemy;
                break;
            case WEAPON:
                key = weapon;
                break;
        }
        SummedStats stats = filteredStats.get(key);
        if (stats == null) {
            stats = new SummedStats();
        }
        stats.addMiss();
        filteredStats.put(key, stats);
    }

    private Hit parseHit(String text) {
        Matcher m = hitPatternWeapon.matcher(text);
        if (m.find()) {
            return new Hit(Integer.valueOf(m.group(1)), m.group(2), m.group(3), Hit.Quality.valueOf(m.group(4).toUpperCase()));
        } else if ((m = hitPattern.matcher(text)).find()) {
            return new Hit(Integer.valueOf(m.group(1)), m.group(2), "N/A", Hit.Quality.valueOf(m.group(3).toUpperCase()));
        } else {
            return null;
        }

    }

    public void updateStats(Filter filter) {
        activeFilter = filter;
        filteredStats.clear();
        for (Hit hit : hits) {
            if (!hit.isMiss()) {
                addStat(hit);
            } else {
                addMiss(hit.getWeapon(), hit.getEnemy());
            }
        }
    }

    public String getAverageDamagePerRealHit() {
        return String.format(Locale.ENGLISH, "%.2f", totalHits > 0 ? totalDamage / (float) totalHits : 0f);
    }

    public String getAverageDamagePerShot() {
        if (totalHits != 0) {
            return String.format("%.2f", totalDamage / (float) hits.size());
        }
        return "0";
    }

    public void reset() {
        totalDamage = 0;
        totalMisses = 0;
        totalHits = 0;
        hits.clear();
        filteredStats.clear();
    }

    public void setParseWholeFile(boolean parseWholeFile) {
        this.parseWholeFile = parseWholeFile;
    }

    public HashMap<String, SummedStats> getFilteredStats() {
        return filteredStats;
    }

    public long getTotalDamage() {
        return totalDamage;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public long getTotalMisses() {
        return totalMisses;
    }

    public interface CallBack {
        void onNewHit(Hit hit);
    }

    public ArrayList<Hit> getHits() {
        return hits;
    }

    public void setActiveFilter(Filter activeFilter) {
        this.activeFilter = activeFilter;
    }

    public String getAccuracy() {
        return String.format("%.0f", Math.abs(totalHits > 0 ? (totalHits / (float) (totalHits + totalMisses) * 100) : 0)) + " %";
    }

    public enum Filter {
        WEAPON,
        //        QUALITY,
        ENEMY;

        @Override
        public String toString() {
            return this.name().charAt(0) + name().substring(1).toLowerCase();
        }
    }
}