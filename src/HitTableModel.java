import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class HitTableModel extends DefaultTableModel {

    private boolean addHitsBottom = false;

    public HitTableModel() {
        addColumn("Damage");
        addColumn("Enemy");
        addColumn("Weapon");
        addColumn("Quality");
    }

    void addHit(Hit hit) {
        addRow(new Object[]{hit.getDamage(), hit.getEnemy(), hit.getWeapon(), hit.getQuality()});
    }

    public void setAddHitsBottom(boolean addHitsBottom) {
        this.addHitsBottom = addHitsBottom;
    }

    @Override
    public void addRow(Vector rowData) {
        super.insertRow(addHitsBottom ? getRowCount() : 0, rowData);
    }
}
