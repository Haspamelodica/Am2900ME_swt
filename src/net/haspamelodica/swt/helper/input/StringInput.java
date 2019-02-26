package net.haspamelodica.swt.helper.input;

import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class StringInput extends Input<String> {
	public StringInput(Composite parent) {
		this(parent, SWT.NONE);
	}

	public StringInput(Composite parent, int style) {
		super(parent, style);
		setStringToTMapper(Function.identity());
		setTToStringMapper(Function.identity());
		setValue("");
	}
}