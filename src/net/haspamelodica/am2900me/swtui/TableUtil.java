package net.haspamelodica.am2900me.swtui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class TableUtil {
	public static void createColumn(Table table, String label) {
		createColumn(table, label, -1);
	}

	public static void createColumn(Table table, String label, int minHexDigits) {
		TableColumn col = new TableColumn(table, SWT.LEFT);
		if (minHexDigits > 0) {
			col.setText("0x" + new String(new char[minHexDigits]).replace((char) 0, 'D'));
			col.pack();
			int width = col.getWidth();
			col.setText(label);
			col.pack();
			if (col.getWidth() < width)
				col.setWidth(width);
		} else {
			col.setText(label);
			col.pack();
		}
	}
}
