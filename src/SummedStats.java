import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SummedStats {
    private long totalDamage = 0;
    private long totalHits = 0;
    private long totalMisses = 0;

    private HashMap<Hit.Quality, List<Integer>> qualityHits = new HashMap<>();

    public SummedStats() {

    }

    public void addHit(Hit hit) {
        Integer dmg = hit.getDamage();
        Hit.Quality quality = hit.getQuality();
        totalDamage += dmg;
        totalHits++;
        List<Integer> qualityHitList = qualityHits.get(quality);
        if (qualityHitList == null) {
            qualityHitList = new ArrayList<>();
        }
        qualityHitList.add(dmg);
        qualityHits.put(quality, qualityHitList);
    }

    public void addMiss() {
        totalMisses++;
    }

    public long getTotalDamage() {
        return totalDamage;
    }

    public int getAverageDamage() {
        return totalHits > 0 ? (int) (totalDamage / totalHits) : 0;
    }

    public long getHitCount() {
        return totalHits;
    }

    public HashMap<Hit.Quality, List<Integer>> getQualityHits() {
        return qualityHits;
    }

    public int getQualityAverageDamage(Hit.Quality quality) {
        List<Integer> list = qualityHits.get(quality);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        long totalQualityDamage = 0;
        for (Integer dmg : list) {
            totalQualityDamage += dmg;
        }
        return (int) (totalQualityDamage / list.size());
    }

    public Object getMisses() {
        return totalMisses;
    }

    public int getAccuracy() {
        return (int) Math.abs(totalHits > 0 ? (totalHits / (float) (totalHits + totalMisses) * 100) : 0);
    }
}
