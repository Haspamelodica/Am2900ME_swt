package net.haspamelodica.swt.helper.input;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class DoubleInput extends NumberInput<Double> {
	public DoubleInput(Composite parent) {
		this(parent, SWT.NONE);
	}

	public DoubleInput(Composite parent, int style) {
		super(parent, style);
		setStringToTMapper(Double::valueOf);
	}
}