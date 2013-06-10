package net.thomasnardone.ui.table;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;

import net.thomasnardone.ui.swing.DocumentAdapter;

public class MyToolBar extends JToolBar implements ActionListener {
	private static final long			serialVersionUID	= 1L;
	private final Set<ActionListener>	actionListeners		= new LinkedHashSet<>();

	public MyToolBar() {
		super();
	}

	public MyToolBar(final int orientation) {
		super(orientation);
	}

	public MyToolBar(final String name) {
		super(name);
	}

	public MyToolBar(final String name, final int orientation) {
		super(name, orientation);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (!(e.getSource() instanceof DocumentEvent)) {
			e.setSource(this);
		}
		notifyActionListeners(e);
	}

	public void addActionListener(final ActionListener listener) {
		actionListeners.add(listener);
	}

	public void removeActionListener(final ActionListener listener) {
		actionListeners.remove(listener);
	}

	protected JComponent borderPanel(final JComponent component, final String title) {
		JToolBar panel = new JToolBar();
		panel.add(component);
		panel.setBorder(BorderFactory.createTitledBorder(title));
		return panel;
	}

	protected JButton button(final String icon) {
		JButton button = new JButton(loadIcon(icon));
		button.setMargin(new Insets(1, 1, 1, 1));
		button.setFocusable(false);
		button.setAlignmentY(0.25f);
		return button;
	}

	protected void notifyActionListeners(final ActionEvent e) {
		for (ActionListener listener : actionListeners) {
			listener.actionPerformed(e);
		}
	}

	protected void setupAction(final AbstractButton button, final String action) {
		button.setActionCommand(action);
		button.addActionListener(this);
	}

	protected void setupEditAction(final JTextComponent field, final String editAction) {
		field.getDocument().addDocumentListener(new DocumentAdapter() {
			@Override
			public void insertUpdate(final DocumentEvent e) {
				actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, editAction));
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				actionPerformed(new ActionEvent(e, ActionEvent.ACTION_PERFORMED, editAction));
			}

		});
	}

	protected void setupSelectAction(final JComboBox<?> combo, final String editAction) {
		combo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, editAction));
				}
			}
		});
	}

	private ImageIcon loadIcon(final String fileName) {
		try {
			return new ImageIcon(ImageIO.read(getClass().getClassLoader().getResourceAsStream(fileName)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
