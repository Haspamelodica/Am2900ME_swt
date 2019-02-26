package net.haspamelodica.swt.helper;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.haspamelodica.swt.helper.input.FloatInput;
import net.haspamelodica.swt.helper.input.Input;
import net.haspamelodica.swt.helper.input.IntegerInput;
import net.haspamelodica.swt.helper.input.StringInput;

public abstract class InputBox<O> {
	public final static InputBox<String> textSingleInput;
	public final static InputBox<String> textMultiInput;
	public final static InputBox<Integer> intInput;
	public final static InputBox<Float> floatInput;
	static {
		textSingleInput = createInputBased(StringInput::new);
		textMultiInput = create((parent, hint) -> {
			parent.setLayout(new GridLayout());
			Text input = new Text(parent, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
			input.setMessage(hint);
			GridData inputData = new GridData(SWT.FILL, SWT.FILL, true, true);
			inputData.widthHint = 200;
			inputData.heightHint = 100;
			input.setLayoutData(inputData);
			return input::getText;
		});
		intInput = createInputBased(IntegerInput::new);
		floatInput = createInputBased(FloatInput::new);
	}

	public static String textSingleInput(Shell parent, String title, String message, String hint) {
		return textSingleInput.input(parent, title, message, hint);
	}

	public static String textMultiInput(Shell parent, String title, String message, String hint) {
		return textMultiInput.input(parent, title, message, hint);
	}

	public static Integer intInput(Shell parent, String title, String message, Integer hint) {
		return intInput.input(parent, title, message, hint);
	}

	public static Float floatInput(Shell parent, String title, String message, Float hint) {
		return floatInput.input(parent, title, message, hint);
	}

	public static <O> InputBox<O> create(BiFunction<Composite, O, Supplier<O>> initInput) {
		return new InputBox<O>() {
			protected Supplier<O> initInput(Composite parent, O hint, Consumer<O> confirm) {
				return initInput.apply(parent, hint);
			}
		};
	}

	public static <O> InputBox<O> createInputBased(Function<Composite, Input<O>> initInput) {
		return new InputBox<O>() {
			protected Supplier<O> initInput(Composite parent, O hint, Consumer<O> confirm) {
				parent.setLayout(new GridLayout());
				Input<O> input = initInput.apply(parent);
				input.setValue(hint);
				input.addManualConfirmListener(confirm);
				GridData inputData = new GridData(SWT.FILL, SWT.FILL, true, true);
				inputData.widthHint = 200;
				input.setLayoutData(inputData);
				return input::getValue;
			}
		};
	}

	public O input(Shell parent, String title, String message, O hint) {
		@SuppressWarnings("unchecked")
		O[] userInputArr = (O[]) new Object[1];
		Shell dialogShell = initInputShell(title, message, hint, parent, userInputArr);
		openAsMessageBox(parent, dialogShell);
		return userInputArr[0];
	}

	private Shell initInputShell(String title, String message, O hint, Shell parentShell, O[] userInputArr) {
		Shell dialogShell = new Shell(parentShell, SWT.BORDER | SWT.CLOSE | SWT.RESIZE);
		dialogShell.setText(title);
		initInputShellContents(message, hint, userInputArr, dialogShell);
		dialogShell.pack();
		moveDialogShellCenterToParentShellCenterClipped(parentShell, dialogShell);
		return dialogShell;
	}

	private void initInputShellContents(String message, O hint, O[] userInputArr, Composite parent) {
		parent.setLayout(new GridLayout(2, false));
		Label msgLabel = new Label(parent, SWT.NONE);
		Composite inputParent = new Composite(parent, SWT.NONE);
		inputParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		Consumer<O> confirm = o -> {
			userInputArr[0] = o;
			parent.dispose();
		};
		Supplier<O> input = initInput(inputParent, hint, confirm);
		Button ok = new Button(parent, SWT.PUSH);
		Button cancel = new Button(parent, SWT.PUSH);
		msgLabel.setText(message);
		ok.setText("OK");
		ok.addListener(SWT.Selection, e -> confirm.accept(input.get()));
		cancel.setText("Cancel");
		cancel.addListener(SWT.Selection, e -> parent.dispose());
		msgLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
	}

	protected abstract Supplier<O> initInput(Composite parent, O hint, Consumer<O> confirm);

	private static void openAsMessageBox(Shell parent, Shell dialogShell) {
		Display display = parent.getDisplay();
		parent.setEnabled(false);
		dialogShell.open();
		while (!dialogShell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
		parent.setEnabled(true);
	}

	private static void moveDialogShellCenterToParentShellCenterClipped(Shell parentShell, Shell dialogShell) {
		Rectangle displayBounds = parentShell.getDisplay().getBounds();
		Rectangle parentBounds = parentShell.getBounds();
		Point dialogSize = dialogShell.getSize();
		int displayW = displayBounds.width;
		int displayH = displayBounds.height;
		int parentX = parentBounds.x;
		int parentW = parentBounds.width;
		int parentY = parentBounds.y;
		int parentH = parentBounds.height;
		int dialogW = dialogSize.x;
		int dialogH = dialogSize.y;
		int dialogX = parentX + parentW / 2 - dialogW / 2;
		int dialogY = parentY + parentH / 2 - dialogH / 2;
		dialogX = Math.min(dialogX, displayW - dialogW);
		dialogY = Math.min(dialogY, displayH - dialogH);
		dialogX = Math.max(dialogX, 0);
		dialogY = Math.max(dialogY, 0);
		dialogShell.setLocation(dialogX, dialogY);
	}
}