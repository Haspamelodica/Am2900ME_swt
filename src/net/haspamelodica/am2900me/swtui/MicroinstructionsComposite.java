package net.haspamelodica.am2900me.swtui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

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

	private final static List<InstructionEditorE<?>> instructionEnumEditors;
	private final static List<InstructionEditorN<?>> instructionNBitsUIntEditors;

	private static class InstructionEditorE<E extends Enum<E>> {
		private final Function<MicroInstruction, E> getVal;
		private final BiFunction<MicroInstruction, E, MicroInstruction> setVal;

		private InstructionEditorE(Function<MicroInstruction, E> getVal,
				BiFunction<MicroInstruction, E, MicroInstruction> setVal) {
			this.getVal = getVal;
			this.setVal = setVal;
		}
	}

	private static class InstructionEditorN<E extends NBitsUInt> {
		private final Function<MicroInstruction, E> getVal;
		private final BiFunction<MicroInstruction, E, MicroInstruction> setVal;
		private final IntFunction<E> createVal;

		private InstructionEditorN(Function<MicroInstruction, E> getVal,
				BiFunction<MicroInstruction, E, MicroInstruction> setVal, IntFunction<E> createVal) {
			this.getVal = getVal;
			this.setVal = setVal;
			this.createVal = createVal;
		}
	}

	static {
		ArrayList<InstructionEditorE<?>> iEsM = new ArrayList<>();
		iEsM.add(null);
		iEsM.add(1, new InstructionEditorE<>(MicroInstruction::getIe, MicroInstruction::withIe));
		iEsM.add(2, null);
		iEsM.add(3, new InstructionEditorE<>(MicroInstruction::getKmux, MicroInstruction::withKmux));
		iEsM.add(4, null);
		iEsM.add(5, new InstructionEditorE<>(MicroInstruction::getAm2901_Src, MicroInstruction::withAm2901_Src));
		iEsM.add(6, new InstructionEditorE<>(MicroInstruction::getAm2901_Func, MicroInstruction::withAm2901_Func));
		iEsM.add(7, new InstructionEditorE<>(MicroInstruction::getAm2901_Dest, MicroInstruction::withAm2901_Dest));
		iEsM.add(8, null);
		iEsM.add(9, new InstructionEditorE<>(MicroInstruction::getAsel, MicroInstruction::withAsel));
		iEsM.add(10, null);
		iEsM.add(11, new InstructionEditorE<>(MicroInstruction::getBsel, MicroInstruction::withBsel));
		iEsM.add(12, new InstructionEditorE<>(MicroInstruction::getAbus, MicroInstruction::withAbus));
		iEsM.add(13, new InstructionEditorE<>(MicroInstruction::getDbus, MicroInstruction::withDbus));
		iEsM.add(14, new InstructionEditorE<>(MicroInstruction::getAm2904_Carry, MicroInstruction::withAm2904_Carry));
		iEsM.add(15, null);
		iEsM.add(16, new InstructionEditorE<>(MicroInstruction::getCe_mu, MicroInstruction::withCe_mu));
		iEsM.add(17, new InstructionEditorE<>(MicroInstruction::getCe_m, MicroInstruction::withCe_m));
		iEsM.add(18, new InstructionEditorE<>(MicroInstruction::getAm2904_Inst, MicroInstruction::withAm2904_Inst));
		iEsM.add(19, new InstructionEditorE<>(MicroInstruction::getCcen, MicroInstruction::withCcen));
		iEsM.add(20, new InstructionEditorE<>(MicroInstruction::getAm2910_Inst, MicroInstruction::withAm2910_Inst));
		iEsM.add(21, null);
		iEsM.add(22, new InstructionEditorE<>(MicroInstruction::getBz_ld, MicroInstruction::withBz_ld));
		iEsM.add(23, new InstructionEditorE<>(MicroInstruction::getBz_ed, MicroInstruction::withBz_ed));
		iEsM.add(24, new InstructionEditorE<>(MicroInstruction::getBz_inc, MicroInstruction::withBz_inc));
		iEsM.add(25, new InstructionEditorE<>(MicroInstruction::getBz_ea, MicroInstruction::withBz_ea));
		iEsM.add(26, new InstructionEditorE<>(MicroInstruction::getIr_ld, MicroInstruction::withIr_ld));
		iEsM.add(27, new InstructionEditorE<>(MicroInstruction::getMwe, MicroInstruction::withMwe));
		instructionEnumEditors = Collections.unmodifiableList(iEsM);
	}

	static {
		ArrayList<InstructionEditorN<?>> iEsM = new ArrayList<>();
		while (iEsM.size() < 2)
			iEsM.add(null);
		iEsM.add(2, new InstructionEditorN<>(MicroInstruction::getInterrupt, MicroInstruction::withInterrupt,
				Interrupt::new));
		while (iEsM.size() < 4)
			iEsM.add(null);
		iEsM.add(4, new InstructionEditorN<>(MicroInstruction::getK, MicroInstruction::withK, Konst::new));
		while (iEsM.size() < 8)
			iEsM.add(null);
		iEsM.add(8,
				new InstructionEditorN<>(MicroInstruction::getRa_addr, MicroInstruction::withRa_addr, RA_ADDR::new));
		while (iEsM.size() < 10)
			iEsM.add(null);
		iEsM.add(10,
				new InstructionEditorN<>(MicroInstruction::getRb_addr, MicroInstruction::withRb_addr, RB_ADDR::new));
		while (iEsM.size() < 15)
			iEsM.add(null);
		iEsM.add(15, new InstructionEditorN<>(MicroInstruction::getAm2904_Shift, MicroInstruction::withAm2904_Shift,
				Am2904_Shift::new));
		while (iEsM.size() < 21)
			iEsM.add(null);
		iEsM.add(21, new InstructionEditorN<>(MicroInstruction::getBar, MicroInstruction::withBar, BAR::new));
		instructionNBitsUIntEditors = Collections.unmodifiableList(iEsM);
	}

	private final Am2900Machine machine;
	private final MicroprogramMemory muProgMem;
	private final List<Runnable> machineStateChangedListeners;

	private final Button execNext;
	private final Button execNextN;

	private final Table table;

	private final Color unchangedInstrBG;
	private final Color changedInstrUnchangedValueBG;
	private final Color changedValueBG;
	private final Color executingInstrBG;

	public MicroinstructionsComposite(Composite parent, Am2900Machine machine,
			List<Runnable> machineStateChangedListeners) {
		super(parent, SWT.NONE);

		this.machine = machine;
		this.muProgMem = machine.getMpm();
		this.machineStateChangedListeners = machineStateChangedListeners;

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
		machineStateChangedListeners.add(this::updateButtonLabels);

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

			String valAddress;
			String valIE;
			String valInterrupt;
			String valKMUX;
			String valKonst;
			String valAm2901_Src;
			String valAm2901_Func;
			String valAm2901_Dest;
			String valRA_ADDR;
			String valASEL;
			String valRB_ADDR;
			String valBSEL;
			String val_ABUS;
			String val_DBUS;
			String valAm2904_Carry;
			String valAm2904_Shift;
			String val_CE_mu;
			String val_CE_m;
			String valAm2904_Inst;
			String val_CCEN;
			String valAm2910_Inst;
			String valBAR;
			String val_BZ_LD;
			String val_BZ_ED;
			String val_BZ_INC;
			String val_BZ_EA;
			String val_IR_LD;
			String val_MWE;

			valAddress = HexIntStringConverter.forNibbles(3).toString(address);
			valIE = instr.getIe().toString();
			valInterrupt = instr.getInterrupt().toString();
			valKMUX = instr.getKmux().toString();
			valKonst = instr.getK().toString();
			valAm2901_Src = instr.getAm2901_Src().toString();
			valAm2901_Func = instr.getAm2901_Func().toString();
			valAm2901_Dest = instr.getAm2901_Dest().toString();
			valRA_ADDR = instr.getRa_addr().toString();
			valASEL = instr.getAsel().toString();
			valRB_ADDR = instr.getRb_addr().toString();
			valBSEL = instr.getBsel().toString();
			val_ABUS = instr.getAbus().toString();
			val_DBUS = instr.getDbus().toString();
			valAm2904_Carry = instr.getAm2904_Carry().toString();
			valAm2904_Shift = instr.getAm2904_Shift().toString();
			val_CE_mu = instr.getCe_mu().toString();
			val_CE_m = instr.getCe_m().toString();
			valAm2904_Inst = instr.getAm2904_Inst().toString();
			val_CCEN = instr.getCcen().toString();
			valAm2910_Inst = instr.getAm2910_Inst().toString();
			valBAR = instr.getBar().toString();
			val_BZ_LD = instr.getBz_ld().toString();
			val_BZ_ED = instr.getBz_ed().toString();
			val_BZ_INC = instr.getBz_inc().toString();
			val_BZ_EA = instr.getBz_ea().toString();
			val_IR_LD = instr.getIr_ld().toString();
			val_MWE = instr.getMwe().toString();

			item.setText(new String[] { valAddress, valIE, valInterrupt, valKMUX, valKonst, valAm2901_Src,
					valAm2901_Func, valAm2901_Dest, valRA_ADDR, valASEL, valRB_ADDR, valBSEL, val_ABUS, val_DBUS,
					valAm2904_Carry, valAm2904_Shift, val_CE_mu, val_CE_m, valAm2904_Inst, val_CCEN, valAm2910_Inst,
					valBAR, val_BZ_LD, val_BZ_ED, val_BZ_INC, val_BZ_EA, val_IR_LD, val_MWE });

			updateItemColor(item, address);
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
			switch (column) {
			case 0:
				break;
			case 2:
			case 4:
			case 8:
			case 10:
			case 15:
			case 21:
				MicroInstruction instr = muProgMem.getInstruction(address);
				Text text = new Text(cursor, SWT.NONE);
				@SuppressWarnings("unchecked")
				InstructionEditorN<NBitsUInt> instrEditor = (InstructionEditorN<NBitsUInt>) instructionNBitsUIntEditors
						.get(column);
				text.setText(instrEditor.getVal.apply(instr).toString());
				text.selectAll();
				text.addListener(SWT.FocusOut, e2 -> {
					try {
						int i = Integer.decode(text.getText());
						MicroInstruction newInstr = instrEditor.setVal.apply(instr, instrEditor.createVal.apply(i));
						muProgMem.setInstruction(address, newInstr);
						item.setText(column, instrEditor.getVal.apply(newInstr).toString());
						updateItemColor(item, address);
						machineChanged();
					} catch (IllegalArgumentException x) {
					}
					text.dispose();
				});
				text.addListener(SWT.DefaultSelection, e2 -> {
					try {
						int i = Integer.decode(text.getText());
						MicroInstruction newInstr = instrEditor.setVal.apply(instr, instrEditor.createVal.apply(i));
						muProgMem.setInstruction(address, newInstr);
						item.setText(column, instrEditor.getVal.apply(newInstr).toString());
						updateItemColor(item, address);
						machineChanged();
						text.dispose();
					} catch (IllegalArgumentException x) {
					}
				});
				editor.setEditor(text);
				text.setFocus();
				break;
			default:
				CCombo combo;
				combo = initEditInstrCombo(cursor, item, address);
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
		Function<MicroInstruction, ?> getVal;
		switch (column) {
		case 0:
			return true;
		case 2:
		case 4:
		case 8:
		case 10:
		case 15:
		case 21:
			getVal = instructionNBitsUIntEditors.get(column).getVal;
			break;
		default:
			getVal = instructionEnumEditors.get(column).getVal;
			break;
		}
		return getVal.apply(instr).equals(getVal.apply(MicroInstruction.DEFAULT));
	}

	private <E extends Enum<E>> CCombo initEditInstrCombo(TableCursor cursor, TableItem item, int address) {
		MicroInstruction instr = muProgMem.getInstruction(address);
		@SuppressWarnings("unchecked")
		InstructionEditorE<E> editor = (InstructionEditorE<E>) instructionEnumEditors.get(cursor.getColumn());
		return initShortLivedEnumCCombo(cursor, editor.getVal.apply(instr),
				updateInstr(address, cursor.getColumn(), item, editor.setVal));
	}

	private <E extends Enum<E>> Consumer<E> updateInstr(int address, int column, TableItem item,
			BiFunction<MicroInstruction, E, MicroInstruction> setVal) {
		return e -> {
			MicroInstruction instr = muProgMem.getInstruction(address);
			muProgMem.setInstruction(address, setVal.apply(instr, e));
			updateItemColor(item, address);
			item.setText(column, e.toString());
			machineChanged();
		};
	}

	private <E extends Enum<E>> CCombo initShortLivedEnumCCombo(TableCursor cursor, E currentVal, Consumer<E> setVal) {
		E[] values = currentVal.getDeclaringClass().getEnumConstants();
		CCombo combo = new CCombo(cursor, SWT.NONE);
		combo.setItems(Arrays.stream(values).map(Enum::toString).toArray(String[]::new));
		combo.select(currentVal.ordinal());
		combo.addListener(SWT.FocusOut, e2 -> {
			String text = combo.getText();
			for (E e : values)
				if (e.toString().equals(text)) {
					setVal.accept(e);
					break;
				}
			combo.dispose();
		});
		combo.addListener(SWT.DefaultSelection, e2 -> {
			String text = combo.getText();
			for (E e : values)
				if (e.toString().equals(text)) {
					setVal.accept(e);
					combo.dispose();
					break;
				}
		});
		return combo;
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
		machineStateChangedListeners.forEach(Runnable::run);
	}
}