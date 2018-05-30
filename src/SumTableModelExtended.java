import java.util.HashMap;
import java.util.Map;

public class SumTableModelExtended extends SumTableModel {

    public SumTableModelExtended() {
        super();
        for (Hit.Quality quality : Hit.Quality.values()) {
            if (quality != Hit.Quality.MISS) {
                addColumn(quality.toString());
            }
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                // Weapon
                return String.class;
            case 1:
                // Hits
                return Integer.class;
            case 2:
                // Misses
                return Integer.class;
            case 3:
                //Average Damage
                return Integer.class;
            case 4:
                // Accuracy
                return Integer.class;
            case 5:
                //WRECKS Damage
                return Integer.class;
            case 6:
                //SMASHES Damage
                return Integer.class;
            case 7:
                //PENETRATES Damage
                return Integer.class;
            case 8:
                //HITS Damage
                return Integer.class;
            case 9:
                //GLANCES Damage
                return Integer.class;
            case 10:
                //GRAZES Damage
                return Integer.class;
            default:
                return String.class;
        }
    }

    @Override
    Object[] buildRow(String weapon, SummedStats stat) {
        return new Object[]{
                weapon,
                stat.getHitCount(),
                stat.getMisses(),
                stat.getAccuracy(),
                stat.getAverageDamage(),
                stat.getQualityAverageDamage(Hit.Quality.WRECKS),
                stat.getQualityAverageDamage(Hit.Quality.SMASHES),
                stat.getQualityAverageDamage(Hit.Quality.PENETRATES),
                stat.getQualityAverageDamage(Hit.Quality.HITS),
                stat.getQualityAverageDamage(Hit.Quality.GLANCES),
                stat.getQualityAverageDamage(Hit.Quality.GRAZES),
        };
    }
}
