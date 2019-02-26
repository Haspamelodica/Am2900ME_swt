package net.haspamelodica.am2900me.swtui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
	private static final String ERROR_NOT_IMPLEMENTED = "Not yet implemented in the SWT version...";

	private final static List<InstructionPropertyEditor> instructionEditors;

	private static class InstructionPropertyEditor {
		private final Function<MicroInstruction, String> getVal;
		private final Predicate<String> checkVal;
		private final BiFunction<MicroInstruction, String, MicroInstruction> setVal;
		private final List<String> values;

		private InstructionPropertyEditor(Function<MicroInstruction, String> getVal, Predicate<String> checkVal,
				BiFunction<MicroInstruction, String, MicroInstruction> setVal, List<String> values) {
			this.getVal = getVal;
			this.checkVal = checkVal;
			this.setVal = setVal;
			this.values = values;
		}

	}

	private static <E extends Enum<E>> InstructionPropertyEditor createIPE(Function<MicroInstruction, E> getVal,
			BiFunction<MicroInstruction, E, MicroInstruction> setVal) {
		E defaultVal = getVal.apply(MicroInstruction.DEFAULT);
		E[] values = defaultVal.getDeclaringClass().getEnumConstants();
		List<String> valueNames = Arrays.stream(values).map(Object::toString).collect(Collectors.toList());
		return new InstructionPropertyEditor(i -> getVal.apply(i).toString(), s -> valueNames.contains(s),
				(i, s) -> setVal.apply(i, values[valueNames.indexOf(s)]), valueNames);
	}

	private static <N extends NBitsUInt> InstructionPropertyEditor createIPE(Function<MicroInstruction, N> getVal,
			IntFunction<N> createVal, BiFunction<MicroInstruction, N, MicroInstruction> setVal) {
		return new InstructionPropertyEditor(i -> getVal.apply(i).toString(), s -> {
			try {
				Integer.decode(s);
			} catch (NumberFormatException x) {
				return false;
			}
			return true;
		}, (i, s) -> setVal.apply(i, createVal.apply(Integer.decode(s))), null);
	}

	static {
		ArrayList<InstructionPropertyEditor> iEsM = new ArrayList<>();
		iEsM.add(0, null);
		iEsM.add(1, createIPE(MicroInstruction::getIe, MicroInstruction::withIe));
		iEsM.add(2, createIPE(MicroInstruction::getInterrupt, Interrupt::new, MicroInstruction::withInterrupt));
		iEsM.add(3, createIPE(MicroInstruction::getKmux, MicroInstruction::withKmux));
		iEsM.add(4, createIPE(MicroInstruction::getK, Konst::new, MicroInstruction::withK));
		iEsM.add(5, createIPE(MicroInstruction::getAm2901_Src, MicroInstruction::withAm2901_Src));
		iEsM.add(6, createIPE(MicroInstruction::getAm2901_Func, MicroInstruction::withAm2901_Func));
		iEsM.add(7, createIPE(MicroInstruction::getAm2901_Dest, MicroInstruction::withAm2901_Dest));
		iEsM.add(8, createIPE(MicroInstruction::getRa_addr, RA_ADDR::new, MicroInstruction::withRa_addr));
		iEsM.add(9, createIPE(MicroInstruction::getAsel, MicroInstruction::withAsel));
		iEsM.add(10, createIPE(MicroInstruction::getRb_addr, RB_ADDR::new, MicroInstruction::withRb_addr));
		iEsM.add(11, createIPE(MicroInstruction::getBsel, MicroInstruction::withBsel));
		iEsM.add(12, createIPE(MicroInstruction::getAbus, MicroInstruction::withAbus));
		iEsM.add(13, createIPE(MicroInstruction::getDbus, MicroInstruction::withDbus));
		iEsM.add(14, createIPE(MicroInstruction::getAm2904_Carry, MicroInstruction::withAm2904_Carry));
		iEsM.add(15,
				createIPE(MicroInstruction::getAm2904_Shift, Am2904_Shift::new, MicroInstruction::withAm2904_Shift));
		iEsM.add(16, createIPE(MicroInstruction::getCe_mu, MicroInstruction::withCe_mu));
		iEsM.add(17, createIPE(MicroInstruction::getCe_m, MicroInstruction::withCe_m));
		iEsM.add(18, createIPE(MicroInstruction::getAm2904_Inst, MicroInstruction::withAm2904_Inst));
		iEsM.add(19, createIPE(MicroInstruction::getCcen, MicroInstruction::withCcen));
		iEsM.add(20, createIPE(MicroInstruction::getAm2910_Inst, MicroInstruction::withAm2910_Inst));
		iEsM.add(21, createIPE(MicroInstruction::getBar, BAR::new, MicroInstruction::withBar));
		iEsM.add(22, createIPE(MicroInstruction::getBz_ld, MicroInstruction::withBz_ld));
		iEsM.add(23, createIPE(MicroInstruction::getBz_ed, MicroInstruction::withBz_ed));
		iEsM.add(24, createIPE(MicroInstruction::getBz_inc, MicroInstruction::withBz_inc));
		iEsM.add(25, createIPE(MicroInstruction::getBz_ea, MicroInstruction::withBz_ea));
		iEsM.add(26, createIPE(MicroInstruction::getIr_ld, MicroInstruction::withIr_ld));
		iEsM.add(27, createIPE(MicroInstruction::getMwe, MicroInstruction::withMwe));

		instructionEditors = Collections.unmodifiableList(iEsM);
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
			if (input != null && input > 0)
				executeNextN(input);
			else
				showError("Not a valid n: " + input);
		});
		Button loadFile = new Button(toolbar, SWT.PUSH);
		loadFile.setText("Load from File");
		loadFile.addListener(SWT.Selection, e -> showError(ERROR_NOT_IMPLEMENTED));
		Button saveFile = new Button(toolbar, SWT.PUSH);
		saveFile.setText("Save to File");
		saveFile.addListener(SWT.Selection, e -> showError(ERROR_NOT_IMPLEMENTED));
		Button reset = new Button(toolbar, SWT.PUSH);
		reset.setText("Reset Machine State");
		reset.addListener(SWT.Selection, e -> {
			int oldMI = machine.getCurrentMicroInstruction();
			machine.reset();
			updateItemColor(oldMI);
			machineChanged();
		});
		updateButtonLabels();
		machineStateChangedListenerManager.addListener(this::updateButtonLabels);

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
		TableUtil.createColumn(table, "IE");
		TableUtil.createColumn(table, "Interrupt");
		TableUtil.createColumn(table, "KMUX");
		TableUtil.createColumn(table, "Konst", 4);
		TableUtil.createColumn(table, "Am2901_Src");
		TableUtil.createColumn(table, "Am2901_Func");
		TableUtil.createColumn(table, "Am2901_Dest");
		TableUtil.createColumn(table, "RA_ADDR", 1);
		TableUtil.createColumn(table, "ASEL");
		TableUtil.createColumn(table, "RB_ADDR", 1);
		TableUtil.createColumn(table, "BSEL");
		TableUtil.createColumn(table, "_ABUS");
		TableUtil.createColumn(table, "_DBUS");
		TableUtil.createColumn(table, "Am2904_Carry");
		TableUtil.createColumn(table, "Am2904_Shift", 1);
		TableUtil.createColumn(table, "_CE_mu");
		TableUtil.createColumn(table, "_CE_m");
		TableUtil.createColumn(table, "Am2904_Inst");
		TableUtil.createColumn(table, "_CCEN");
		TableUtil.createColumn(table, "Am2910_Inst");
		TableUtil.createColumn(table, "BAR", 3);
		TableUtil.createColumn(table, "_BZ_LD");
		TableUtil.createColumn(table, "_BZ_ED");
		TableUtil.createColumn(table, "_BZ_INC");
		TableUtil.createColumn(table, "_BZ_EA");
		TableUtil.createColumn(table, "_IR_LD");
		TableUtil.createColumn(table, "_MWE");
		table.addListener(SWT.SetData, e -> {
			TableItem item = (TableItem) e.item;
			int address = table.indexOf(item);
			MicroInstruction instr = muProgMem.getInstruction(address);

			item.setText(0, HexIntStringConverter.forNibbles(3).toString(address));
			Runnable updateItem = () -> {
				for (int i = 1; i < 28; i++)
					item.setText(i, instructionEditors.get(i).getVal.apply(instr));
				updateItemColor(item, address);
			};
			updateItem.run();
			machineStateChangedListenerManager.addListener(updateItem);
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
			InstructionPropertyEditor instrPropEditor = instructionEditors.get(column);
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
			for (int col = 0; col < 28; col++)
				if (!isInstrColumnUnchanged(instr, col))
					item.setBackground(col, changedValueBG);
				else
					item.setBackground(col, null);
		}
	}

	private boolean isInstrColumnUnchanged(MicroInstruction instr, int column) {
		if (column == 0)
			return true;
		Function<MicroInstruction, ?> getVal = instructionEditors.get(column).getVal;
		return getVal.apply(instr).equals(getVal.apply(MicroInstruction.DEFAULT));
	}

	private void updateInstr(int address, int column, TableItem item, String newVal) {
		MicroInstruction instr = muProgMem.getInstruction(address);
		muProgMem.setInstruction(address, instructionEditors.get(column).setVal.apply(instr, newVal));
		updateItemColor(item, address);
		item.setText(column, newVal);
		machineChanged();
	}

	private void executeNextN(int n) {
		int oldMI = machine.getCurrentMicroInstruction();
		try {
			for (int i = 0; i < n; i++) {
				machine.executeNext();
			}
		} catch (Exception ex) {
			showError("An error occured during execution:\n" + ex);
		}
		updateItemColor(oldMI);
		updateItemColor(machine.getCurrentMicroInstruction());
		machineChanged();
	}

	private void showError(String msg) {
		MessageBox msgBox = new MessageBox(getShell(), SWT.ICON_ERROR);
		msgBox.setMessage(msg);
		msgBox.setText("Error");
		msgBox.open();
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
}