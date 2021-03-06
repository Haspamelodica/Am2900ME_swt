package net.haspamelodica.am2900me.swtui;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import net.maisikoleni.am2900me.logic.Am2900Machine;
import net.maisikoleni.am2900me.logic.MappingPROM;
import net.maisikoleni.am2900me.util.HexIntStringConverter;

public class MappingPROMComposite extends Composite {
	private static final HexIntStringConverter hexIntConv3 = HexIntStringConverter.forNibbles(3);

	private final MappingPROM mprom;
	private final ListenerManager machineStateChangedListenerManager;

	private final String[] comments;

	public MappingPROMComposite(Composite parent, Am2900Machine machine,
			ListenerManager machineStateChangedListenerManager) {
		super(parent, SWT.NONE);
		this.mprom = machine.getmProm();
		this.machineStateChangedListenerManager = machineStateChangedListenerManager;

		this.comments = new String[mprom.size()];
		Arrays.fill(comments, "");

		setLayout(new GridLayout());

		Composite toolbar = new Composite(this, SWT.NONE);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolbar.setLayout(new RowLayout());
		Button loadFile = new Button(toolbar, SWT.PUSH);
		loadFile.setText("Load from File");
		loadFile.addListener(SWT.Selection, e -> loadCSVFile());
		Button saveFile = new Button(toolbar, SWT.PUSH);
		saveFile.setText("Save to File");
		saveFile.addListener(SWT.Selection, e -> saveCSVFile());

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
			Runnable updateTexts = () -> {
				item.setText(1, hexIntConv3.toString(mprom.get(opcode)));
				item.setText(2, comments[opcode]);
			};
			updateTexts.run();
			machineStateChangedListenerManager.addListener(updateTexts);
			item.addDisposeListener(v -> machineStateChangedListenerManager.removeListener(updateTexts));
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
						machineChanged();
					} catch (IllegalArgumentException x) {
					}
					text.dispose();
				});
				text.addListener(SWT.DefaultSelection, e2 -> {
					try {
						int i = Integer.decode(text.getText());
						mprom.set(opcode, i);
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
					String comment = text.getText();
					item.setText(2, comment);
					comments[opcode] = comment;
					text.dispose();
				});
				text.addListener(SWT.DefaultSelection, e2 -> {
					String comment = text.getText();
					item.setText(2, comment);
					comments[opcode] = comment;
					text.dispose();
				});
				editor.setEditor(text);
				text.setFocus();
				break;
			}
		};
		cursor.addListener(SWT.DefaultSelection, editListener);
		cursor.addListener(SWT.MouseDown, editListener);
		Runnable cursorRedrawListener = cursor::redraw;
		machineStateChangedListenerManager.addListener(cursorRedrawListener);
		cursor.addDisposeListener(e -> machineStateChangedListenerManager.removeListener(cursorRedrawListener));
	}

	private void machineChanged() {
		machineStateChangedListenerManager.callAllListeners();
	}

	private void saveCSVFile() {
		String filename = openFileDialog(SWT.SAVE);
		if (filename != null)
			try (PrintWriter out = new PrintWriter(filename)) {
				for (int opCode = 0; opCode < mprom.size(); opCode++) {
					int addr = mprom.get(opCode);
					out.print(HexIntStringConverter.INT_8.toString(opCode));
					out.print(',');
					out.print(HexIntStringConverter.INT_12.toString(addr));
					out.print(',');
					out.println(comments[opCode]);
				}
			} catch (IOException e) {
				showError("Unexpected IO error: " + e);
			}
	}

	private void loadCSVFile() {
		String filename = openFileDialog(SWT.OPEN);
		if (filename != null) {
			List<String> errorMessages = new ArrayList<>();
			List<Integer> errorLines = new ArrayList<>();
			try (Scanner in = new Scanner(new FileInputStream(filename))) {
				int lineIndex = 0;
				while (in.hasNextLine()) {
					lineIndex++;
					String line = in.nextLine().trim();
					if (!line.equals("")) {
						int opcodeEnd = findNextCSVDelim(line, 0);
						if (opcodeEnd == -1) {
							errorMessages.add("Not enough properties");
							errorLines.add(lineIndex);
						} else {
							int addrEnd = findNextCSVDelim(line, opcodeEnd + 1);
							try {
								String comment = addrEnd < 0 ? "" : line.substring(addrEnd + 1);
								boolean opcodeParsed;
								int opcode = -1;
								try {
									opcode = Integer.decode(line.substring(0, opcodeEnd));
									opcodeParsed = true;
								} catch (NumberFormatException e) {
									errorMessages.add("Couldn't parse opcode");
									errorLines.add(lineIndex);
									opcodeParsed = false;
								}
								if (opcodeParsed)
									try {
										int addr = Integer.decode(line.substring(opcodeEnd + 1, addrEnd));
										mprom.set(opcode, addr);
										comments[opcode] = comment;
									} catch (NumberFormatException e) {
										errorMessages.add("Couldn't parse address");
										errorLines.add(lineIndex);
									}
							} catch (Exception e) {
								errorMessages.add("Unexpected error: " + e);
								errorLines.add(lineIndex);
							}
						}
					}
				}
			} catch (IOException e) {
				showError("Unexpected IO error: " + e);
			}
			if (!errorLines.isEmpty()) {
				showError("Errors occurred during CSV parse.\n" + "First error: #" + errorLines.get(0) + ": "
						+ errorMessages.get(0));
			}
		}
		machineChanged();
	}

	private int findNextCSVDelim(String line, int start) {
		int commaI = line.indexOf(',', start);
		int semicolonI = line.indexOf(';', start);
		return commaI == -1 ? semicolonI : semicolonI == -1 ? commaI : Math.min(commaI, semicolonI);
	}

	private String openFileDialog(int style) {
		FileDialog fd = new FileDialog(getShell(), style);
		fd.setFilterExtensions(new String[] { "*.csv", "*.*" });
		fd.setFilterNames(new String[] { "CSV files (*.csv)", "All files" });
		if ((style & SWT.SAVE) != 0)
			fd.setOverwrite(true);
		String filename = fd.open();
		return filename;
	}

	private void showError(String msg) {
		MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_ERROR);
		msgBox.setMessage(msg);
		msgBox.setText("Error");
		msgBox.open();
	}
}