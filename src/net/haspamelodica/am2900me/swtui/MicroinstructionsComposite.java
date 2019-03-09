package net.haspamelodica.am2900me.swtui;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.graphics.Color;
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

import net.haspamelodica.swt.helper.InputBox;
import net.maisikoleni.am2900me.logic.Am2900Machine;
import net.maisikoleni.am2900me.logic.MicroprogramMemory;
import net.maisikoleni.am2900me.logic.microinstr.Am2904_Shift;
import net.maisikoleni.am2900me.logic.microinstr.BAR;
import net.maisikoleni.am2900me.logic.microinstr.Interrupt;
import net.maisikoleni.am2900me.logic.microinstr.Konst;
import net.maisikoleni.am2900me.logic.microinstr.MicroInstruction;
import net.maisikoleni.am2900me.logic.microinstr.RA_ADDR;
import net.maisikoleni.am2900me.logic.microinstr.RB_ADDR;
import net.maisikoleni.am2900me.util.HexIntStringConverter;
import net.maisikoleni.am2900me.util.NBitsUInt;

public class MicroinstructionsComposite extends Composite {
	private final static List<InstructionProperty> instructionProperties;

	private static class InstructionProperty {
		private final String name;
		private final int nibbles;
		private final Function<MicroInstruction, String> getVal;
		private final Predicate<String> checkVal;
		private final BiFunction<MicroInstruction, String, MicroInstruction> setVal;
		private final List<String> values;

		private InstructionProperty(String name, int nibbles, Function<MicroInstruction, String> getVal,
				Predicate<String> checkVal, BiFunction<MicroInstruction, String, MicroInstruction> setVal,
				List<String> values) {
			this.name = name;
			this.nibbles = nibbles;
			this.getVal = getVal;
			this.checkVal = checkVal;
			this.setVal = setVal;
			this.values = values;
		}

	}

	private static <E extends Enum<E>> InstructionProperty createIP(String name, Function<MicroInstruction, E> getVal,
			BiFunction<MicroInstruction, E, MicroInstruction> setVal) {
		E defaultVal = getVal.apply(MicroInstruction.DEFAULT);
		E[] values = defaultVal.getDeclaringClass().getEnumConstants();
		List<String> valueNames = Arrays.stream(values).map(Object::toString).collect(Collectors.toList());
		return new InstructionProperty(name, -1, i -> getVal.apply(i).toString(), s -> valueNames.contains(s),
				(i, s) -> setVal.apply(i, values[valueNames.indexOf(s)]), valueNames);
	}

	private static <N extends NBitsUInt> InstructionProperty createIP(String name, int nibbles,
			Function<MicroInstruction, N> getVal, IntFunction<N> createVal,
			BiFunction<MicroInstruction, N, MicroInstruction> setVal) {
		int highestAllowedHighestOneBit = 1 << (4 * nibbles);
		return new InstructionProperty(name, nibbles, i -> getVal.apply(i).toString(), s -> {
			try {
				int decoded = Integer.decode(s);
				int highestOneBit = Integer.highestOneBit(decoded);
				if (highestOneBit < 0)
					highestOneBit = Integer.highestOneBit(~decoded) << 1;
				return highestOneBit < highestAllowedHighestOneBit;
			} catch (NumberFormatException x) {
				return false;
			}
		}, (i, s) -> setVal.apply(i, createVal.apply(Integer.decode(s) & (highestAllowedHighestOneBit - 1))), null);
	}

	static {
		ArrayList<InstructionProperty> iEsM = new ArrayList<>();
		iEsM.add(0, null);
		iEsM.add(1, createIP("IE", MicroInstruction::getIe, MicroInstruction::withIe));
		iEsM.add(2, createIP("Interrupt", 1, MicroInstruction::getInterrupt, Interrupt::new,
				MicroInstruction::withInterrupt));
		iEsM.add(3, createIP("KMUX", MicroInstruction::getKmux, MicroInstruction::withKmux));
		iEsM.add(4, createIP("Konst", 4, MicroInstruction::getK, Konst::new, MicroInstruction::withK));
		iEsM.add(5, createIP("Am2901_Src", MicroInstruction::getAm2901_Src, MicroInstruction::withAm2901_Src));
		iEsM.add(6, createIP("Am2901_Func", MicroInstruction::getAm2901_Func, MicroInstruction::withAm2901_Func));
		iEsM.add(7, createIP("Am2901_Dest", MicroInstruction::getAm2901_Dest, MicroInstruction::withAm2901_Dest));
		iEsM.add(8, createIP("RA_ADDR", 1, MicroInstruction::getRa_addr, RA_ADDR::new, MicroInstruction::withRa_addr));
		iEsM.add(9, createIP("ASEL", MicroInstruction::getAsel, MicroInstruction::withAsel));
		iEsM.add(10, createIP("RB_ADDR", 1, MicroInstruction::getRb_addr, RB_ADDR::new, MicroInstruction::withRb_addr));
		iEsM.add(11, createIP("BSEL", MicroInstruction::getBsel, MicroInstruction::withBsel));
		iEsM.add(12, createIP("_ABUS", MicroInstruction::getAbus, MicroInstruction::withAbus));
		iEsM.add(13, createIP("_DBUS", MicroInstruction::getDbus, MicroInstruction::withDbus));
		iEsM.add(14, createIP("Am2904_Carry", MicroInstruction::getAm2904_Carry, MicroInstruction::withAm2904_Carry));
		iEsM.add(15, createIP("Am2904_Shift", 1, MicroInstruction::getAm2904_Shift, Am2904_Shift::new,
				MicroInstruction::withAm2904_Shift));
		iEsM.add(16, createIP("_CE_mu", MicroInstruction::getCe_mu, MicroInstruction::withCe_mu));
		iEsM.add(17, createIP("_CE_m", MicroInstruction::getCe_m, MicroInstruction::withCe_m));
		iEsM.add(18, createIP("Am2904_Inst", MicroInstruction::getAm2904_Inst, MicroInstruction::withAm2904_Inst));
		iEsM.add(19, createIP("_CCEN", MicroInstruction::getCcen, MicroInstruction::withCcen));
		iEsM.add(20, createIP("Am2910_Inst", MicroInstruction::getAm2910_Inst, MicroInstruction::withAm2910_Inst));
		iEsM.add(21, createIP("BAR", 3, MicroInstruction::getBar, BAR::new, MicroInstruction::withBar));
		iEsM.add(22, createIP("_BZ_LD", MicroInstruction::getBz_ld, MicroInstruction::withBz_ld));
		iEsM.add(23, createIP("_BZ_ED", MicroInstruction::getBz_ed, MicroInstruction::withBz_ed));
		iEsM.add(24, createIP("_BZ_INC", MicroInstruction::getBz_inc, MicroInstruction::withBz_inc));
		iEsM.add(25, createIP("_BZ_EA", MicroInstruction::getBz_ea, MicroInstruction::withBz_ea));
		iEsM.add(26, createIP("_IR_LD", MicroInstruction::getIr_ld, MicroInstruction::withIr_ld));
		iEsM.add(27, createIP("_MWE", MicroInstruction::getMwe, MicroInstruction::withMwe));

		instructionProperties = Collections.unmodifiableList(iEsM);
	}

	private final Am2900Machine machine;
	private final MicroprogramMemory muProgMem;
	private final ListenerManager machineStateChangedListenerManager;

	private final Button execNext;
	private final Button execNextN;

	private final Table table;

	private final Color unchangedInstrBG;
	private final Color changedInstrUnchangedValueBG;
	private final Color changedValueBG;
	private final Color executingInstrBG;

	public MicroinstructionsComposite(Composite parent, Am2900Machine machine,
			ListenerManager machineStateChangedListenerManager) {
		super(parent, SWT.NONE);

		this.machine = machine;
		this.muProgMem = machine.getMpm();
		this.machineStateChangedListenerManager = machineStateChangedListenerManager;

		this.unchangedInstrBG = getDisplay().getSystemColor(SWT.COLOR_GRAY);
		this.changedInstrUnchangedValueBG = getDisplay().getSystemColor(SWT.COLOR_WHITE);
		this.changedValueBG = getDisplay().getSystemColor(SWT.COLOR_CYAN);
		this.executingInstrBG = getDisplay().getSystemColor(SWT.COLOR_GREEN);

		setLayout(new GridLayout());

		Composite toolbar = new Composite(this, SWT.NONE);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolbar.setLayout(new RowLayout());
		execNext = new Button(toolbar, SWT.PUSH);
		execNext.addListener(SWT.Selection, e -> executeNextN(1));
		execNextN = new Button(toolbar, SWT.PUSH);
		execNextN.addListener(SWT.Selection, e -> {
			Integer input = InputBox.intInput(getShell(), "Execute Next N",
					"Please enter the number of microinstruction cycles you want to execute\n"
							+ "(expect 5 mio. to take ~ 1 sec.)",
					1);
			if (input != null)
				if (input > 0)
					executeNextN(input);
				else
					showError("Not a valid n: " + input);
		});
		Button loadFile = new Button(toolbar, SWT.PUSH);
		loadFile.setText("Load from File");
		loadFile.addListener(SWT.Selection, e -> loadCSVFile());
		Button saveFile = new Button(toolbar, SWT.PUSH);
		saveFile.setText("Save to File");
		saveFile.addListener(SWT.Selection, e -> saveCSVFile());
		Button reset = new Button(toolbar, SWT.PUSH);
		reset.setText("Reset Machine State");
		reset.addListener(SWT.Selection, e -> {
			int oldMI = machine.getCurrentMicroInstruction();
			machine.reset();
			updateItemColor(oldMI);
			machineChanged();
		});
		updateButtonLabels();
		Runnable updateButtonLabels = this::updateButtonLabels;
		machineStateChangedListenerManager.addListener(updateButtonLabels);
		addDisposeListener(e -> machineStateChangedListenerManager.removeListener(updateButtonLabels));

		table = new Table(this, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
		setupTable();

		pack();
	}

	private void setupTable() {
		for (int i = 0; i < muProgMem.size(); i++)
			muProgMem.setInstruction(i, MicroInstruction.DEFAULT);
		machineChanged();

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		table.setBackground(unchangedInstrBG);
//		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setItemCount(muProgMem.size());
		TableUtil.createColumn(table, "Address", 3);
		for (int i = 1; i < instructionProperties.size(); i++)
			TableUtil.createColumn(table, instructionProperties.get(i).name, instructionProperties.get(i).nibbles);
		table.addListener(SWT.SetData, e -> {
			TableItem item = (TableItem) e.item;
			int address = table.indexOf(item);

			item.setText(0, HexIntStringConverter.forNibbles(3).toString(address));
			Runnable updateItem = () -> {
				MicroInstruction instr = muProgMem.getInstruction(address);
				for (int i = 1; i < instructionProperties.size(); i++)
					item.setText(i, instructionProperties.get(i).getVal.apply(instr));
				updateItemColor(item, address);
			};
			updateItem.run();
			machineStateChangedListenerManager.addListener(updateItem);
			item.addDisposeListener(v -> machineStateChangedListenerManager.removeListener(updateItem));
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
			int address = table.indexOf(item);
			int column = cursor.getColumn();
			InstructionProperty instrPropEditor = instructionProperties.get(column);
			if (instrPropEditor != null)
				if (instrPropEditor.values == null) {
					MicroInstruction instr = muProgMem.getInstruction(address);
					Text text = new Text(cursor, SWT.NONE);
					text.setText(instrPropEditor.getVal.apply(instr).toString());
					text.selectAll();
					text.addListener(SWT.FocusOut, e2 -> {
						if (instrPropEditor.checkVal.test(text.getText()))
							updateInstr(address, column, item, text.getText());
						text.dispose();
					});
					text.addListener(SWT.DefaultSelection, e2 -> {
						if (instrPropEditor.checkVal.test(text.getText())) {
							updateInstr(address, column, item, text.getText());
							text.dispose();
						}
					});
					editor.setEditor(text);
					text.setFocus();
				} else {
					CCombo combo = new CCombo(cursor, SWT.NONE);
					combo.setItems(instrPropEditor.values.stream().toArray(String[]::new));
					combo.select(instrPropEditor.values.indexOf(item.getText(column)));
					combo.addListener(SWT.FocusOut, e2 -> {
						if (instrPropEditor.checkVal.test(combo.getText()))
							updateInstr(address, column, item, combo.getText());
						combo.dispose();
					});
					combo.addListener(SWT.DefaultSelection, e2 -> {
						if (instrPropEditor.checkVal.test(combo.getText())) {
							updateInstr(address, column, item, combo.getText());
							combo.dispose();
						}
					});
					editor.setEditor(combo);
					combo.setListVisible(true);
					combo.setFocus();
				}
		};
		cursor.addListener(SWT.DefaultSelection, editListener);
		cursor.addListener(SWT.MouseDown, editListener);
		machineStateChangedListenerManager.addListener(cursor::redraw);
	}

	private void updateItemColor(int address) {
		if (address >= 0)
			updateItemColor(table.getItem(address), address);
	}

	private void updateItemColor(TableItem item, int address) {
		if (item != null) {
			MicroInstruction instr = muProgMem.getInstruction(address);
			boolean executing = machine.getCurrentMicroInstruction() == address;
			if (executing)
				item.setBackground(executingInstrBG);
			else if (instr.equals(MicroInstruction.DEFAULT))
				item.setBackground(unchangedInstrBG);
			else
				item.setBackground(changedInstrUnchangedValueBG);
			for (int col = 0; col < instructionProperties.size(); col++)
				if (!isInstrColumnUnchanged(instr, col))
					item.setBackground(col, changedValueBG);
				else
					item.setBackground(col, null);
		}
	}

	private boolean isInstrColumnUnchanged(MicroInstruction instr, int column) {
		if (column == 0)
			return true;
		Function<MicroInstruction, ?> getVal = instructionProperties.get(column).getVal;
		return getVal.apply(instr).equals(getVal.apply(MicroInstruction.DEFAULT));
	}

	private void updateInstr(int address, int column, TableItem item, String newVal) {
		MicroInstruction instr = muProgMem.getInstruction(address);
		muProgMem.setInstruction(address, instructionProperties.get(column).setVal.apply(instr, newVal));
		machineChanged();
	}

	private void executeNextN(int n) {
		try {
			for (int i = 0; i < n; i++)
				machine.executeNext();
		} catch (Exception ex) {
			showError("An error occured during execution:\n" + ex);
		}
		machineChanged();
		table.showItem(table.getItem(machine.getCurrentMicroInstruction()));
	}

	private void updateButtonLabels() {
		if (machine.getCurrentMicroInstruction() == -1) {
			execNext.setText("Startup machine");
			execNextN.setText("Execute Next N (inkl. Startup)");
		} else {
			execNext.setText("Execute Next");
			execNextN.setText("Execute Next N");
		}
	}

	private void machineChanged() {
		machineStateChangedListenerManager.callAllListeners();
	}

	private void saveCSVFile() {
		String filename = openFileDialog(SWT.SAVE);
		if (filename != null)
			try (PrintWriter out = new PrintWriter(filename)) {
				for (int addr = 0; addr < muProgMem.size(); addr++) {
					MicroInstruction instr = muProgMem.getInstruction(addr);
					out.print(HexIntStringConverter.INT_12.toString(addr));
					for (int col = 1; col < instructionProperties.size(); col++) {
						out.print(',');
						out.print(instructionProperties.get(instructionProperties.size() - col).getVal.apply(instr));
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
						if (properties.length < instructionProperties.size()) {
							errorMessages.add("Not enough properties");
							errorLines.add(lineIndex);
						} else {
							try {
								boolean addrParsed;
								int addr = -1;
								try {
									addr = Integer.decode(properties[0]);
									addrParsed = true;
								} catch (NumberFormatException e) {
									errorMessages.add("Couldn't parse target address");
									errorLines.add(lineIndex);
									addrParsed = false;
								}
								if (addrParsed) {
									MicroInstruction instr = MicroInstruction.DEFAULT;
									for (int col = 1; col < instructionProperties.size(); col++) {
										String propStr = properties[col];
										InstructionProperty propDef = instructionProperties
												.get(instructionProperties.size() - col);
										if (propDef.checkVal.test(propStr))
											instr = propDef.setVal.apply(instr, propStr);
										else {
											errorMessages.add("Illegal property for col " + col + " (" + propDef.name
													+ "): \"" + propStr + "\". Using default");
											errorLines.add(lineIndex);
										}
									}
									muProgMem.setInstruction(addr, instr);
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