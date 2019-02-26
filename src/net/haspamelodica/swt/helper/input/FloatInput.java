package net.haspamelodica.swt.helper.input;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class FloatInput extends NumberInput<Float> {
	public FloatInput(Composite parent) {
		this(parent, SWT.NONE);
	}

	public FloatInput(Composite parent, int style) {
		super(parent, style);
		setStringToTMapper(Float::valueOf);
	}
}