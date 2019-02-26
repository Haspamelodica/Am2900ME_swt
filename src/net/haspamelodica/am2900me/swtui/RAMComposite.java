package net.haspamelodica.am2900me.swtui;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import net.maisikoleni.am2900me.logic.Am2900Machine;
import net.maisikoleni.am2900me.logic.MachineRAM;
import net.maisikoleni.am2900me.util.HexIntStringConverter;

public class RAMComposite extends Composite {
	private static final HexIntStringConverter hexIntConv4 = HexIntStringConverter.forNibbles(4);

	private final MachineRAM ram;
	private final ListenerManager machineStateChangedListenerManager;

	private int viewedPage;
	private int selectedPage;
	private boolean pageLoaded;

	private final Composite ramPageParent;
	private Control ramPageChild;

	private List<Runnable> changeListenersCurrentRamPage;

	public RAMComposite(Composite parent, Am2900Machine machine, ListenerManager machineStateChangedListenerManager) {
		super(parent, SWT.NONE);

		this.ram = machine.getMachineRam();
		this.machineStateChangedListenerManager = machineStateChangedListenerManager;

		viewedPage = -1;

		setLayout(new GridLayout());

		Composite toolbar = new Composite(this, SWT.NONE);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolbar.setLayout(new RowLayout());
		new Label(toolbar, SWT.NONE).setText("RAM page:");
		Spinner spinner = new Spinner(toolbar, SWT.NONE);
		spinner.setMinimum(0);
		spinner.setMaximum(15);
		spinner.addListener(SWT.Selection, e -> {
			selectedPage = spinner.getSelection();
			updateRamPage();
		});
		Button loadFile = new Button(toolbar, SWT.PUSH);
		loadFile.setText("Load from File");
		loadFile.addListener(SWT.Selection, e -> loadCSVFile());
		Button saveFile = new Button(toolbar, SWT.PUSH);
		saveFile.setText("Save to File");
		saveFile.addListener(SWT.Selection, e -> saveCSVFile());

		ramPageParent = new Composite(this, SWT.NONE);
		GridData parentData = new GridData(SWT.FILL, SWT.FILL, true, true);
		ramPageParent.setLayoutData(parentData);
		ramPageParent.setLayout(new FillLayout());
		Runnable updateRamPage = this::updateRamPage;
		machineStateChangedListenerManager.addListener(updateRamPage);
		addDisposeListener(e -> machineStateChangedListenerManager.removeListener(updateRamPage));

		Table getWidthTable = new Table(ramPageParent, SWT.BORDER | SWT.FULL_SELECTION);
		getWidthTable.setHeaderVisible(true);
		getWidthTable.setItemCount(1);

		TableUtil.createColumn(getWidthTable, "Offset", 4);
		for (int i = 0; i < 16; i++)
			TableUtil.createColumn(getWidthTable, HexIntStringConverter.forNibbles(1).toString(i), 4);

		TableItem item = new TableItem(getWidthTable, SWT.NONE);
		for (int i = 0; i < 17; i++)
			item.setText(i, "0xDDDD");
		ramPageParent.pack();
		int width = ramPageParent.getSize().x;
		parentData.widthHint = width;
		getWidthTable.dispose();

		updateRamPage();
	}

	private void updateRamPage() {
		if ((ram.isPageInUse(selectedPage) != pageLoaded) || (viewedPage != selectedPage)) {
			if (ramPageChild != null)
				ramPageChild.dispose();
			if (changeListenersCurrentRamPage != null) {
				changeListenersCurrentRamPage.forEach(machineStateChangedListenerManager::removeListener);
				changeListenersCurrentRamPage.clear();
			} else
				changeListenersCurrentRamPage = new ArrayList<>();
			viewedPage = selectedPage;
			pageLoaded = ram.isPageInUse(selectedPage);
			if (pageLoaded) {
				Table ramPageChildT = new Table(ramPageParent, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
				ramPageChild = ramPageChildT;
				ramPageChildT.setHeaderVisible(true);
				ramPageChildT.setItemCount(ram.cellCount() / 16);

				TableUtil.createColumn(ramPageChildT, "Offset", 4);
				for (int i = 0; i < 16; i++)
					TableUtil.createColumn(ramPageChildT, HexIntStringConverter.forNibbles(1).toString(i), 4);

				ramPageChildT.addListener(SWT.SetData, e -> {
					TableItem item = (TableItem) e.item;
					int addressOffset = (selectedPage << 12) + (ramPageChildT.indexOf(item) << 4);

					String[] texts = new String[17];
					texts[0] = hexIntConv4.toString(addressOffset);
					Runnable updateTexts = () -> {
						for (int i = 0; i < 16; i++)
							texts[i + 1] = hexIntConv4.toString((int) ram.get(addressOffset + i));
						item.setText(texts);
					};
					updateTexts.run();
					machineStateChangedListenerManager.addListener(updateTexts);
					item.addDisposeListener(v -> machineStateChangedListenerManager.removeListener(updateTexts));
					changeListenersCurrentRamPage.add(updateTexts);
				});
				TableCursor cursor = new TableCursor(ramPageChildT, SWT.NONE);
				ControlEditor editor = new ControlEditor(cursor);
				editor.grabHorizontal = true;
				editor.grabVertical = true;
				cursor.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLUE));
				cursor.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
				cursor.addListener(SWT.Selection, e -> ramPageChildT.setSelection(cursor.getRow()));
				Listener editListener = e -> {
					TableItem item = cursor.getRow();
					int addressOffset = (selectedPage << 12) + (ramPageChildT.indexOf(item) << 4);
					int column = cursor.getColumn();
					if (column > 0) {
						int address = addressOffset + column - 1;
						Text text = new Text(cursor, SWT.NONE);
						text.setText(hexIntConv4.toString((int) ram.get(address)));
						text.selectAll();
						text.addListener(SWT.FocusOut, e2 -> {
							try {
								int i = Integer.decode(text.getText());
								ram.set(address, i);
								item.setText(column, hexIntConv4.toString(i));
								machineChanged();
							} catch (IllegalArgumentException x) {
							}
							text.dispose();
						});
						text.addListener(SWT.DefaultSelection, e2 -> {
							try {
								int i = Integer.decode(text.getText());
								ram.set(address, i);
								item.setText(column, hexIntConv4.toString(i));
								machineChanged();
								text.dispose();
							} catch (IllegalArgumentException x) {
							}
						});
						editor.setEditor(text);
						text.setFocus();
					}
				};
				cursor.addListener(SWT.DefaultSelection, editListener);
				cursor.addListener(SWT.MouseDown, editListener);
			} else {
				Composite ramPageChildC = new Composite(ramPageParent, SWT.NONE);
				ramPageChild = ramPageChildC;
				ramPageChildC.setLayout(new GridLayout());
				Composite centered = new Composite(ramPageChildC, SWT.NONE);
				centered.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
				centered.setLayout(new FillLayout(SWT.VERTICAL));
				new Label(centered, SWT.NONE).setText("RAM page currently not used");
				Button alloc = new Button(centered, SWT.NONE);
				alloc.setText("Allocate the page to edit it");
				alloc.addListener(SWT.Selection, e -> {
					ram.allocatePage(selectedPage);
					updateRamPage();
				});
			}
			ramPageParent.layout();
		}
	}

	private void machineChanged() {
		machineStateChangedListenerManager.callAllListeners();
	}

	private void saveCSVFile() {
		String filename = openFileDialog(SWT.SAVE);
		if (filename != null)
			try (PrintWriter out = new PrintWriter(filename)) {
				for (int page = 0; page < ram.pageCount(); page++)
					if (ram.isPageInUse(page))
						for (int base = 0; base < ram.cellCount() / 16; base++) {
							out.print(HexIntStringConverter.INT_16.toString(page * ram.cellCount() + base * 16));
							for (int off = 0; off < 16; off++) {
								out.print(',');
								out.print(HexIntStringConverter.INT_16
										.toString((int) ram.get(page * ram.cellCount() + base * 16 + off)));
							}
							out.println();
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
						String[] properties = line.split("[,;]");
						if (properties.length < 17) {
							errorMessages.add("Not enough columns");
							errorLines.add(lineIndex);
						} else {
							try {
								boolean baseParsed;
								int base = -1;
								try {
									base = Integer.decode(properties[0]);
									baseParsed = true;
								} catch (NumberFormatException e) {
									errorMessages.add("Couldn't parse address base");
									errorLines.add(lineIndex);
									baseParsed = false;
								}
								if (baseParsed)
									for (int off = 0; off < 16; off++) {
										try {
											int val = Integer.decode(properties[1 + off]);
											if (val > 0xFFFF || val < 0) {
												errorMessages.add("Out-of-bounds value; no change");
												errorLines.add(lineIndex);
											} else {
												ram.set(base + off, (short) val);
											}
										} catch (NumberFormatException e) {
											errorMessages.add("Couldn't parse value; no change");
											errorLines.add(lineIndex);
										}
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