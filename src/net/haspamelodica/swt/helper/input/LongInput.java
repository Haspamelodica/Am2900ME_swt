package net.haspamelodica.swt.helper.input;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class LongInput extends NumberInput<Long> {
	public LongInput(Composite parent) {
		this(parent, SWT.NONE);
	}

	public LongInput(Composite parent, int style) {
		super(parent, style);
		setStringToTMapper(Long::valueOf);
	}
}