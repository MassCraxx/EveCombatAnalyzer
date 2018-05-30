import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.Map;

public class SumTableModel extends DefaultTableModel {

    public SumTableModel() {
        addColumn("Type");
        addColumn("Hits");
        addColumn("Misses");
        addColumn("Accuracy");
        addColumn("AverageDamage");
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
                //Average Damage
                return Integer.class;
            default:
                return String.class;
        }
    }

    void update(HashMap<String, SummedStats> stats) {
        setRowCount(0);
        for (Map.Entry<String, SummedStats> entry : stats.entrySet()) {
            String weapon = entry.getKey();
            SummedStats stat = entry.getValue();
            addRow(buildRow(weapon, stat));
        }
    }

    Object[] buildRow(String weapon, SummedStats stat){
        return new Object[]{weapon, stat.getHitCount(), stat.getMisses(), stat.getAverageDamage()};
    }
}
