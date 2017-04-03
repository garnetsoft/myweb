import java.lang.reflect.Array;

import javax.swing.table.AbstractTableModel;

import kx.c;
import kx.c.Flip;

public class KxDataModel extends AbstractTableModel {
	
        private c.Flip flip;
        public KxDataModel(Flip f) {
        	this.flip = f;
        }
        public void setFlip(c.Flip data) {
            this.flip = data;
        }

        public int getRowCount() {
            return Array.getLength(flip.y[0]);
        }

        public int getColumnCount() {
            return flip.y.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return c.at(flip.y[columnIndex], rowIndex);
        }

        public String getColumnName(int columnIndex) {
            return flip.x[columnIndex];
        }
}