package net.haspamelodica.swt.helper.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class Input<T> extends Composite {
	private Label label;
	private Text text;
	private GridData textData;

	private Function<String, T> stringToT;
	private Function<T, String> tToString;
	private List<Consumer<T>> inputChangedListeners;
	private List<Consumer<T>> manualConfirmListeners;
	private String errorContent;

	private String currentInput;
	private T currentValue;

	public Input(Composite parent) {
		this(parent, SWT.NONE);
	}

	public Input(Composite parent, int style) {
		super(parent, style);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);

		label = new Label(this, SWT.NONE);
		text = new Text(this, SWT.BORDER);

		GridData labelData = new GridData();
		labelData.verticalAlignment = SWT.CENTER;
		label.setLayoutData(labelData);

		textData = new GridData();
		textData.horizontalAlignment = SWT.FILL;
		textData.grabExcessHorizontalSpace = true;
		textData.widthHint = 100;
		text.setLayoutData(textData);

		text.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				updateTextAndCallListeners(true);
			}
		});
		text.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				updateTextAndCallListeners(false);
			}
		});

		inputChangedListeners = new ArrayList<>();
		manualConfirmListeners = new ArrayList<>();
	}

	public Input<T> setLabel(String label) {
		this.label.setText(label);
		return this;
	}

	public Input<T> setValue(T t) {
		currentValue = t;
		inputChanged(false);
		return this;
	}

	public Input<T> setText(String text) {
		setRawInput(text);
		return this;
	}

	public Input<T> setTextWHint(int wHint) {
		textData.widthHint = wHint;
		return this;
	}

	public Input<T> setStringToTMapper(Function<String, T> stringToT) {
		this.stringToT = stringToT;
		return this;
	}

	public Function<String, T> getStringToTMapper() {
		return stringToT;
	}

	public Input<T> setTToStringMapper(Function<T, String> tToString) {
		this.tToString = tToString;
		return this;
	}

	public Function<T, String> getTToStringMapper() {
		return tToString;
	}

	public Input<T> addChangeListener(Consumer<T> inputChanged) {
		if (inputChanged != null)
			inputChangedListeners.add(inputChanged);
		return this;
	}

	public Input<T> removeChangeListener(Consumer<T> inputChanged) {
		inputChangedListeners.remove(inputChanged);
		return this;
	}

	public Input<T> addManualConfirmListener(Consumer<T> manualConfirm) {
		if (manualConfirm != null)
			manualConfirmListeners.add(manualConfirm);
		return this;
	}

	public Input<T> removeManualConfirmListener(Consumer<T> manualConfirm) {
		manualConfirmListeners.remove(manualConfirm);
		return this;
	}

	public Input<T> setErrorContent(String errorContent) {
		this.errorContent = errorContent;
		return this;
	}

	public T getValue() {
		return currentValue;
	}

	private void updateTextAndCallListeners(boolean manualConfirm) {
		String newInput = text.getText();
		boolean inputChanged = !Objects.equals(newInput, currentInput);
		currentInput = newInput;
		userInputChanged(inputChanged, manualConfirm);
	}

	private void userInputChanged(boolean inputChanged, boolean manualConfirm) {
		if (stringToT != null) {
			if (inputChanged) {
				T val = stringToT.apply(currentInput);
				if (val == null)
					setRawInput(errorContent);
				else {
					boolean valueChanged = !Objects.equals(val, currentValue);
					currentValue = val;
					inputChanged(valueChanged);
				}
			}
			if (manualConfirm)
				manualConfirm();
		}
	}

	private void inputChanged(boolean callValueChangedListeners) {
		if (tToString != null)
			setRawInput(tToString.apply(currentValue));
		if (callValueChangedListeners)
			inputChangedListeners.forEach(c -> c.accept(currentValue));
	}

	private void manualConfirm() {
		manualConfirmListeners.forEach(c -> c.accept(currentValue));
	}

	private void setRawInput(String text) {
		currentInput = text;
		this.text.setText(currentInput);
	}
}