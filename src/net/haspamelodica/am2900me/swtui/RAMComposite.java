package net.haspamelodica.am2900me.swtui;

import java.util.ArrayList;
import java.util.List;

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

	private static final String ERROR_NOT_IMPLEMENTED = "Not yet implemented in the SWT version...";

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
		loadFile.addListener(SWT.Selection, e -> showError(ERROR_NOT_IMPLEMENTED));
		Button saveFile = new Button(toolbar, SWT.PUSH);
		saveFile.setText("Save to File");
		saveFile.addListener(SWT.Selection, e -> showError(ERROR_NOT_IMPLEMENTED));

		ramPageParent = new Composite(this, SWT.NONE);
		GridData parentData = new GridData(SWT.FILL, SWT.FILL, true, true);
		ramPageParent.setLayoutData(parentData);
		ramPageParent.setLayout(new FillLayout());
		machineStateChangedListenerManager.addListener(this::updateRamPage);

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
				ramPageChildT.setItemCount(256);

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

	private void showError(String msg) {
		MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_ERROR);
		msgBox.setMessage(msg);
		msgBox.setText("Error");
		msgBox.open();
	}
}