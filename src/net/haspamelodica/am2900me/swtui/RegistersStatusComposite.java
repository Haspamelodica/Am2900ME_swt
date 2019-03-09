package net.haspamelodica.am2900me.swtui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableCursor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import net.maisikoleni.am2900me.logic.Am2900Machine;
import net.maisikoleni.am2900me.util.HexIntStringConverter;

public class RegistersStatusComposite extends Composite {

	private final Am2900Machine machine;
	private final ListenerManager machineStateChangedListenerManager;

	public RegistersStatusComposite(Composite parent, Am2900Machine machine,
			ListenerManager machineStateChangedListenerManager) {
		super(parent, SWT.NONE);

		this.machine = machine;
		this.machineStateChangedListenerManager = machineStateChangedListenerManager;

		setLayout(new FillLayout());

		SashForm sashFormH = new SashForm(this, SWT.HORIZONTAL | SWT.SMOOTH);
		sashFormH.setSashWidth(2);

		SashForm sashFormV = new SashForm(sashFormH, SWT.VERTICAL | SWT.SMOOTH);
		sashFormV.setSashWidth(2);
		setupMachineRegisterTable(sashFormV);
		setupStatusTable(sashFormV);

		setupGeneralPurposeMachineRegisterTable(sashFormH);

		pack();
	}

	private void setupMachineRegisterTable(Composite parent) {
		Table table = new Table(parent, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);

		TableUtil.createColumn(table, "Register Name", "Register/Counter");
		TableUtil.createColumn(table, "Value", 4);

		List<Supplier<String>> getVals = new ArrayList<>();
		List<Consumer<String>> setVals = new ArrayList<>();
		createIntItem(table, getVals, setVals, "PC", machine.getPc()::getPc, machine.getPc()::setPc, 4);
		createIntItem(table, getVals, setVals, "IR", machine.getIr()::getInstruction, machine.getIr()::setInstruction,
				4);
		createIntItem(table, getVals, setVals, "µPC", machine.getAm2910()::getmuPC, machine.getAm2910()::setmuPC, 3);
		createIntItem(table, getVals, setVals, "Register/Counter", machine.getAm2910()::getRegisterCounter,
				machine.getAm2910()::setRegisterCounter, 3);
		createIntItem(table, getVals, setVals, "Stack Pointer", machine.getAm2910()::getStackPointer,
				machine.getAm2910()::setStackPointer, 0);
		createIntItem(table, getVals, setVals, "Stack [4]", () -> machine.getAm2910().getStack(4),
				v -> machine.getAm2910().setStack(4, v), 3);
		createIntItem(table, getVals, setVals, "Stack [3]", () -> machine.getAm2910().getStack(3),
				v -> machine.getAm2910().setStack(3, v), 3);
		createIntItem(table, getVals, setVals, "Stack [2]", () -> machine.getAm2910().getStack(2),
				v -> machine.getAm2910().setStack(2, v), 3);
		createIntItem(table, getVals, setVals, "Stack [1]", () -> machine.getAm2910().getStack(1),
				v -> machine.getAm2910().setStack(1, v), 3);
		createIntItem(table, getVals, setVals, "Stack [0]", () -> machine.getAm2910().getStack(0),
				v -> machine.getAm2910().setStack(0, v), 3);

		createCursor(table, getVals, setVals);
	}

	private void setupStatusTable(Composite parent) {
		Table table = new Table(parent, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);

		TableUtil.createColumn(table, "Statusbit", "µOVR", "MOVR");
		TableUtil.createColumn(table, "Value", "false");

		List<Supplier<String>> getVals = new ArrayList<>();
		List<Consumer<String>> setVals = new ArrayList<>();
		createStatusItem(table, getVals, setVals, "µC");
		createStatusItem(table, getVals, setVals, "µN");
		createStatusItem(table, getVals, setVals, "µZ");
		createStatusItem(table, getVals, setVals, "µOVR");
		createStatusItem(table, getVals, setVals, "MC");
		createStatusItem(table, getVals, setVals, "MN");
		createStatusItem(table, getVals, setVals, "MZ");
		createStatusItem(table, getVals, setVals, "MOVR");

		createCursor(table, getVals, setVals);
	}

	private void setupGeneralPurposeMachineRegisterTable(Composite parent) {
		Table table = new Table(parent, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);

		TableUtil.createColumn(table, "Register", 1);
		TableUtil.createColumn(table, "Value", 4);

		List<Supplier<String>> getVals = new ArrayList<>();
		List<Consumer<String>> setVals = new ArrayList<>();
		createIntItem(table, getVals, setVals, "Q", machine.getAm2904_01x4()::getQ, machine.getAm2904_01x4()::setQ, 4);
		for (int i = 0; i < 16; i++) {
			int iFinal = i;
			createIntItem(table, getVals, setVals, HexIntStringConverter.forNibbles(1).toString(i),
					() -> machine.getAm2904_01x4().getRegisters4bit(iFinal),
					v -> machine.getAm2904_01x4().setRegisters4bit(iFinal, v), 4);
		}

		createCursor(table, getVals, setVals);
	}

	private void createIntItem(Table table, List<Supplier<String>> getVals, List<Consumer<String>> setVals,
			String label, IntSupplier getValue, IntConsumer setValue, int hexDigits) {
		TableItem item = new TableItem(table, SWT.NONE);
		item.setText(0, label);
		IntFunction<String> valueStringSupplier;
		if (hexDigits > 0)
			valueStringSupplier = HexIntStringConverter.forNibbles(hexDigits)::toString;
		else
			valueStringSupplier = String::valueOf;
		getVals.add(table.indexOf(item), () -> valueStringSupplier.apply(getValue.getAsInt()));
		setVals.add(table.indexOf(item), s -> {
			int i = Integer.decode(s);
			setValue.accept(i);
			item.setText(1, valueStringSupplier.apply(i));
			machineChanged();
		});
		Runnable updateText = () -> item.setText(1, valueStringSupplier.apply(getValue.getAsInt()));
		updateText.run();
		machineStateChangedListenerManager.addListener(updateText);
		item.addDisposeListener(e -> machineStateChangedListenerManager.removeListener(updateText));
	}

	private void createStatusItem(Table table, List<Supplier<String>> getVals, List<Consumer<String>> setVals,
			String status) {
		TableItem item = new TableItem(table, SWT.NONE);
		item.setText(0, status);
		getVals.add(table.indexOf(item), () -> String.valueOf(machine.getAm2904_01x4().isStatusSet(status)));
		setVals.add(table.indexOf(item), s -> {
			boolean b = Boolean.valueOf(s);
			machine.getAm2904_01x4().setStatus(status, b);
			item.setText(1, String.valueOf(b));
			machineChanged();
		});
		Runnable updateText = () -> item.setText(1, String.valueOf(machine.getAm2904_01x4().isStatusSet(status)));
		updateText.run();
		machineStateChangedListenerManager.addListener(updateText);
		item.addDisposeListener(e -> machineStateChangedListenerManager.removeListener(updateText));
	}

	private void createCursor(Table table, List<Supplier<String>> getVals, List<Consumer<String>> setVals) {
		TableCursor cursor = new TableCursor(table, SWT.NONE);
		ControlEditor editor = new ControlEditor(cursor);
		editor.grabHorizontal = true;
		editor.grabVertical = true;
		cursor.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLUE));
		cursor.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		cursor.addListener(SWT.Selection, e -> table.setSelection(cursor.getRow()));
		Listener editListener = e -> {
			TableItem item = cursor.getRow();
			int row = table.indexOf(item);
			switch (cursor.getColumn()) {
			case 0:
				break;
			case 1:
				Text text = new Text(cursor, SWT.NONE);
				text.setText(getVals.get(row).get());
				text.selectAll();
				text.addListener(SWT.FocusOut, e2 -> {
					try {
						setVals.get(row).accept(text.getText());
					} catch (IllegalArgumentException x) {
					}
					text.dispose();
				});
				text.addListener(SWT.DefaultSelection, e2 -> {
					try {
						setVals.get(row).accept(text.getText());
						text.dispose();
					} catch (IllegalArgumentException x) {
					}
				});
				editor.setEditor(text);
				text.setFocus();
				break;
			}
		};
		cursor.addListener(SWT.DefaultSelection, editListener);
		cursor.addListener(SWT.MouseDown, editListener);
		machineStateChangedListenerManager.addListener(cursor::redraw);
	}

	private void machineChanged() {
		machineStateChangedListenerManager.callAllListeners();
	}
}