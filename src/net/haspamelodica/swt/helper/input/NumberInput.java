package net.haspamelodica.swt.helper.input;

import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class NumberInput<N extends Number> extends Input<N> {
	private String unitWithLeadingSpace, unit;
	private Function<String, N> stringToNMapper;
	private Function<N, String> nTostringMapper;

	public NumberInput(Composite parent) {
		this(parent, SWT.NONE);
	}

	public NumberInput(Composite parent, int style) {
		super(parent, style);
		super.setStringToTMapper(s -> {
			s = s.trim();
			if (unit != null && s.endsWith(unit))
				s = s.substring(0, s.length() - unit.length()).trim();
			s = s.replace(',', '.');
			try {
				return stringToNMapper.apply(s);
			} catch (NumberFormatException e) {
				return null;// sign for invalid input
			}
		});
		super.setTToStringMapper(n -> nTostringMapper.apply(n) + (unit == null ? "" : unitWithLeadingSpace));
		nTostringMapper = Number::toString;
		setErrorContent("NaN");
	}

	public Input<N> setStringToTMapper(Function<String, N> stringToN) {
		stringToNMapper = stringToN;
		return this;
	}

	public Function<String, N> getStringToTMapper() {
		return stringToNMapper;
	}

	public Input<N> setTToStringMapper(Function<N, String> nToString) {
		nTostringMapper = nToString;
		return this;
	}

	public Function<N, String> getTToStringMapper() {
		return nTostringMapper;
	}

	public NumberInput<N> setUnit(String unit) {
		unit = unit == null ? "" : unit;
		this.unit = unit;
		unitWithLeadingSpace = "".equals(unit) ? "" : ' ' + unit;
		setErrorContent("NaN" + unitWithLeadingSpace);
		setValue(getValue());// update
		return this;
	}
}