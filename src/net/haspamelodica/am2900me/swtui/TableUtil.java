package net.haspamelodica.am2900me.swtui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class TableUtil {
	private static final String[] HEX_DIGITS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
			"A", "B", "C", "D", "E", "F" };

	public static void createColumn(Table table, String label, int hexDigits) {
		List<String> possibleLongestHexStrings = Arrays
				.stream(HEX_DIGITS)
				.map(s -> "0x" + repeat(s, hexDigits))
				.collect(Collectors.toList());
		createColumn(table, label, possibleLongestHexStrings);
	}

	private static String repeat(String s, int n) {
		StringBuilder result = new StringBuilder(s.length() * n);
		for (int i = 0; i < n; i++)
			result.append(s);
		return result.toString();
	}

	public static void createColumn(Table table, String label, String... possibleLongestContents) {
		createColumn(table, label, Arrays.asList(possibleLongestContents));
	}

	public static void createColumn(Table table, String label, List<String> possibleLongestContents) {
		TableColumn col = new TableColumn(table, SWT.LEFT);
		col.setText(label);
		col.pack();
		int maxWidth = col.getWidth();
		for (String possibleLongestContent : possibleLongestContents) {
			col.setText(possibleLongestContent);
			col.pack();
			int width = col.getWidth();
			if (width > maxWidth)
				maxWidth = width;
		}
		col.setText(label);
		col.setWidth(maxWidth);
	}
}
