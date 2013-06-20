package net.thomasnardone.ui.table.filter;

import java.util.regex.Pattern;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.thomasnardone.ui.rest.FilterInfo;

@SuppressWarnings("serial")
public class TextFilter extends AbstractFilter {
	private JTextField	field;
	private Pattern		pattern;

	public TextFilter(final FilterInfo filterInfo) {
		super(filterInfo);
	}

	@Override
	public boolean include(final Object value) {
		return (pattern == null) || pattern.matcher((String) value).matches();
	}

	@Override
	protected void setupFilter() {
		add(field = new JTextField(10));
		field.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(final DocumentEvent e) {
				filter();
			}

			@Override
			public void insertUpdate(final DocumentEvent e) {
				filter();
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				filter();
			}
		});
	}

	private void filter() {
		if (field.getText().trim().isEmpty()) {
			pattern = null;
		} else {
			String text = field.getText().replaceAll("\\*", ".*");
			if (!text.endsWith(".*")) {
				text = text + ".*";
			}
			pattern = Pattern.compile(text);
		}
		fireFilterChanged();
	}
}