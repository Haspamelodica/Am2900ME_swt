package net.haspamelodica.am2900me.swtui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import net.maisikoleni.am2900me.logic.Am2900Machine;
import net.maisikoleni.am2900me.logic.MappingPROM;
import net.maisikoleni.am2900me.util.HexIntStringConverter;

public class MappingPROMComposite extends Composite {
	private static final String ERROR_NOT_IMPLEMENTED = "Not yet implemented in the SWT version...";

	private static final HexIntStringConverter hexIntConv3 = HexIntStringConverter.forNibbles(3);

	private final MappingPROM mprom;
	private final ListenerManager machineStateChangedListenerManager;

	public MappingPROMComposite(Composite parent, Am2900Machine machine,
			ListenerManager machineStateChangedListenerManager) {
		super(parent, SWT.NONE);
		this.mprom = machine.getmProm();
		this.machineStateChangedListenerManager = machineStateChangedListenerManager;

		setLayout(new GridLayout());

		Composite toolbar = new Composite(this, SWT.NONE);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolbar.setLayout(new RowLayout());
		Button loadFile = new Button(toolbar, SWT.PUSH);
		loadFile.setText("Load from File");
		loadFile.addListener(SWT.Selection, e -> showError(ERROR_NOT_IMPLEMENTED));
		Button saveFile = new Button(toolbar, SWT.PUSH);
		saveFile.setText("Save to File");
		saveFile.addListener(SWT.Selection, e -> showError(ERROR_NOT_IMPLEMENTED));

		setupTable();
	}

	private void setupTable() {
		Table table = new Table(this, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setItemCount(256);

		TableUtil.createColumn(table, "OP-Code", 3);
		TableUtil.createColumn(table, "Mapping Address", 4);
		TableUtil.createColumn(table, "Comment");

		table.addListener(SWT.SetData, e -> {
			TableItem item = (TableItem) e.item;
			int opcode = table.indexOf(item);

			item.setText(0, HexIntStringConverter.forNibbles(2).toString(opcode));
			item.setText(2, "");
			Runnable updateTexts = () -> item.setText(1, hexIntConv3.toString(mprom.get(opcode)));
			updateTexts.run();
			machineStateChangedListenerManager.addListener(updateTexts);
		});
		TableCursor cursor = new TableCursor(table, SWT.NONE);
		ControlEditor editor = new ControlEditor(cursor);
		editor.grabHorizontal = true;
		editor.grabVertical = true;
		cursor.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLUE));
		cursor.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		cursor.addListener(SWT.Selection, e -> table.setSelection(cursor.getRow()));
		Listener editListener = e -> {
			TableItem item = cursor.getRow();
			int opcode = table.indexOf(item);
			switch (cursor.getColumn()) {
			case 0:
				break;
			case 1:
				Text text = new Text(cursor, SWT.NONE);
				text.setText(hexIntConv3.toString(mprom.get(opcode)));
				text.selectAll();
				text.addListener(SWT.FocusOut, e2 -> {
					try {
						int i = Integer.decode(text.getText());
						mprom.set(opcode, i);
						item.setText(1, hexIntConv3.toString(i));
						machineChanged();
					} catch (IllegalArgumentException x) {
					}
					text.dispose();
				});
				text.addListener(SWT.DefaultSelection, e2 -> {
					try {
						int i = Integer.decode(text.getText());
						mprom.set(opcode, i);
						item.setText(opcode, hexIntConv3.toString(i));
						machineChanged();
						text.dispose();
					} catch (IllegalArgumentException x) {
					}
				});
				editor.setEditor(text);
				text.setFocus();
				break;
			case 2:
				text = new Text(cursor, SWT.NONE);
				text.setText(item.getText(2));
				text.selectAll();
				text.addListener(SWT.FocusOut, e2 -> {
					item.setText(2, text.getText());
					text.dispose();
				});
				text.addListener(SWT.DefaultSelection, e2 -> {
					item.setText(2, text.getText());
					text.dispose();
				});
				editor.setEditor(text);
				text.setFocus();
				break;
			}
		};
		cursor.addListener(SWT.DefaultSelection, editListener);
		cursor.addListener(SWT.MouseDown, editListener);
	}

	private void machineChanged() {
		machineStateChangedListenerManager.callAllListeners();
	}

	private void showError(String msg) {
		MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_ERROR);
		msgBox.setMessage(msg);
		msgBox.setText("Error");
		msgBox.open();
	}
}