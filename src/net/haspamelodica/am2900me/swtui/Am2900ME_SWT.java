package net.haspamelodica.am2900me.swtui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.maisikoleni.am2900me.logic.Am2900Machine;

public class Am2900ME_SWT {
	public static final String DESCRIPTOR = "SWT Am2900ME (v0.1.1)";

	private final Am2900Machine machine;
	private final ListenerManager machineStateChangedListenerManager;

	private final List<Resource> toDispose;
	private final Display display;
	private final Image iconImage;
	private final Shell shellMicroinstructions;
	private final Shell shellRegistersStatus;
	private final Shell shellRAM;
	private final Shell shellMappingPROM;

	public Am2900ME_SWT() {
		this.machine = new Am2900Machine();
		this.machineStateChangedListenerManager = new ListenerManager();

		this.toDispose = new ArrayList<>();
		this.display = new Display();
		this.iconImage = registerResource(new Image(display, getClass().getResourceAsStream("/icons/icon128.png")));
		this.shellMicroinstructions = new Shell();
		this.shellRegistersStatus = new Shell();
		this.shellRAM = new Shell();
		this.shellMappingPROM = new Shell();

		initShellMicroinstructions();
		initShellRegistersStatus();
		initShellRAM();
		initShellMappingPROM();
	}

	private void initShellMicroinstructions() {
		shellMicroinstructions.setText(DESCRIPTOR + " - Microinstructions");
		shellMicroinstructions.setImage(iconImage);
		shellMicroinstructions.setLayout(new FillLayout());
		new MicroinstructionsComposite(shellMicroinstructions, machine, machineStateChangedListenerManager);
		shellMicroinstructions.pack();
	}

	private void initShellRegistersStatus() {
		shellRegistersStatus.setText(DESCRIPTOR + " - Registers & Status");
		shellRegistersStatus.setImage(iconImage);
		shellRegistersStatus.setLayout(new FillLayout());
		new RegistersStatusComposite(shellRegistersStatus, machine, machineStateChangedListenerManager);
		shellRegistersStatus.pack();
	}

	private void initShellRAM() {
		shellRAM.setText(DESCRIPTOR + " - Machine RAM");
		shellRAM.setImage(iconImage);
		shellRAM.setLayout(new FillLayout());
		new RAMComposite(shellRAM, machine, machineStateChangedListenerManager);
		shellRAM.pack();
	}

	private void initShellMappingPROM() {
		shellMappingPROM.setText(DESCRIPTOR + " - Mapping PROM");
		shellMappingPROM.setImage(iconImage);
		shellMappingPROM.setLayout(new FillLayout());
		new MappingPROMComposite(shellMappingPROM, machine, machineStateChangedListenerManager);
		shellMappingPROM.pack();
	}

	private <R extends Resource> R registerResource(R r) {
		toDispose.add(r);
		return r;
	}

	public void run() {
		shellMicroinstructions.open();
		shellRegistersStatus.open();
		shellRAM.open();
		shellMappingPROM.open();

		while (!shellMicroinstructions.isDisposed() || !shellRegistersStatus.isDisposed() || !shellRAM.isDisposed()
				|| !shellMappingPROM.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();

		display.dispose();
		for (Resource r : toDispose)
			if (r != null && !r.isDisposed())
				r.dispose();
	}

	public static void main(String[] args) {
		new Am2900ME_SWT().run();
	}
}