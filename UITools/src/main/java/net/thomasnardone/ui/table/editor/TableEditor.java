package net.thomasnardone.ui.table.editor;

import static net.thomasnardone.ui.table.TableManager.COLUMNS;
import static net.thomasnardone.ui.table.TableManager.COLUMN_PREFIX;
import static net.thomasnardone.ui.table.TableManager.FILTER_ROWS;
import static net.thomasnardone.ui.table.TableManager.VALUE_QUERY;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileFilter;

import net.thomasnardone.ui.swing.DocumentAdapter;
import net.thomasnardone.ui.swing.DragArrangePanel;
import net.thomasnardone.ui.swing.DragArrangePanel.ArrangeListener;
import net.thomasnardone.ui.swing.MyPanel;
import net.thomasnardone.ui.swing.UndoTextArea;
import net.thomasnardone.ui.swing.UndoTextField;
import net.thomasnardone.ui.table.TableManager;
import net.thomasnardone.ui.table.editor.TableColumnEditor.ColumnNameChangeListener;
import net.thomasnardone.ui.table.editor.ValueQueryEditor.QueryChangeListener;
import net.thomasnardone.ui.util.SortedProperties;

public class TableEditor extends JFrame implements ActionListener, ColumnNameChangeListener, ArrangeListener, QueryChangeListener {

	private static final String			EXIT				= "exit";

	private static final String			NEW					= "new";
	private static final String			OPEN				= "open";
	private static final Preferences	prefs				= Preferences.userNodeForPackage(TableEditor.class);
	private static final String			SAVE				= "save";
	private static final String			SAVE_AS				= "save_as";
	private static final long			serialVersionUID	= 1L;
	private static final String			TITLE				= "Table Editor";

	public static void main(final String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		final String propFile = args.length > 0 ? args[0] : null;
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				try {
					new TableEditor(propFile).setVisible(true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private final JPanel							columnPanel;

	private boolean									dirty;

	private final Map<String, TableFilterEditor>	filterMap;
	private final DragArrangePanel					filterPanel;
	private UndoTextField							keyFieldsField;
	private final JComponent						mainPanel;
	private File									propFile;
	private final SortedProperties					props;
	private final UndoTextArea						queryField;
	private final JPanel							updatePanel;
	private UndoTextField							updateTableField;

	private final Map<String, ValueQueryEditor>		valueQueryMap;

	private final JPanel							valueQueryPanel;

	public TableEditor(final String propFile) throws IOException {
		super(TITLE);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				exit();
			}
		});

		props = new SortedProperties();

		valueQueryMap = new HashMap<>();
		JPanel queryPanel = new JPanel(new BorderLayout());
		JPanel tableQueryPanel = new JPanel(new BorderLayout());
		tableQueryPanel.add(new JScrollPane(queryField = new UndoTextArea(10, 60)));
		tableQueryPanel.setBorder(BorderFactory.createTitledBorder("Table Query"));
		queryPanel.add(tableQueryPanel, BorderLayout.CENTER);
		JPanel bottomQueryPanel = new JPanel(new BorderLayout());
		valueQueryPanel = new JPanel();
		valueQueryPanel.setLayout(new BoxLayout(valueQueryPanel, BoxLayout.PAGE_AXIS));
		valueQueryPanel.setBorder(BorderFactory.createTitledBorder("Value Queries"));
		bottomQueryPanel.add(valueQueryPanel, BorderLayout.NORTH);

		updatePanel = new JPanel();
		updatePanel.setLayout(new BoxLayout(updatePanel, BoxLayout.LINE_AXIS));
		updatePanel.setBorder(BorderFactory.createTitledBorder("Updates"));
		updatePanel.add(new JLabel("DB Table:"));
		updatePanel.add(updateTableField = new UndoTextField(10));
		updatePanel.add(Box.createHorizontalStrut(10));
		updatePanel.add(new JLabel("Key Fields:"));
		updatePanel.add(keyFieldsField = new UndoTextField(20));
		keyFieldsField.setToolTipText("Comma separated list of key fields in the update table");
		bottomQueryPanel.add(updatePanel, BorderLayout.SOUTH);
		queryPanel.add(bottomQueryPanel, BorderLayout.SOUTH);

		final DocumentAdapter dirtyTextListener = new DocumentAdapter() {
			@Override
			public void insertUpdate(final DocumentEvent e) {
				setDirty();
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				setDirty();
			}
		};
		queryField.getDocument().addDocumentListener(dirtyTextListener);
		updateTableField.getDocument().addDocumentListener(dirtyTextListener);
		keyFieldsField.getDocument().addDocumentListener(dirtyTextListener);

		columnPanel = new JPanel();
		columnPanel.setLayout(new BoxLayout(columnPanel, BoxLayout.PAGE_AXIS));
		final JScrollPane columnScrollPane = new JScrollPane(columnPanel);
		columnScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		columnScrollPane.setBorder(BorderFactory.createTitledBorder("Columns"));

		filterMap = new HashMap<>();
		filterPanel = new DragArrangePanel();
		filterPanel.setBorder(BorderFactory.createTitledBorder("Filters"));
		filterPanel.addArrangeListener(this);

		mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(queryPanel, BorderLayout.NORTH);
		mainPanel.add(new JScrollPane(columnPanel), BorderLayout.CENTER);
		mainPanel.add(filterPanel, BorderLayout.SOUTH);

		setContentPane(mainPanel);

		JMenuBar mb = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.add(menuItem("New", NEW, KeyEvent.VK_N, KeyEvent.VK_N));
		fileMenu.add(menuItem("Open", OPEN, KeyEvent.VK_O, KeyEvent.VK_O));
		fileMenu.add(menuItem("Save", SAVE, KeyEvent.VK_S, KeyEvent.VK_S));
		fileMenu.add(menuItem("Save As", SAVE_AS, KeyEvent.VK_A, KeyEvent.VK_S, true));
		fileMenu.add(menuItem("Exit", EXIT, KeyEvent.VK_X, KeyEvent.VK_Q));
		mb.add(fileMenu);
		setJMenuBar(mb);

		pack();
		setSize(new Dimension(getSize().width, 800));

		setLocationRelativeTo(null);
		startNewFile();
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final String action = e.getActionCommand();
		if (NEW.equals(action)) {
			newFile();
		} else if (OPEN.equals(action)) {
			open();
		} else if (SAVE.equals(action)) {
			save();
		} else if (SAVE_AS.equals(action)) {
			saveAs();
		} else if (EXIT.equals(action)) {
			exit();
		} else if (TableColumnEditor.EDIT_ACTION.equals(action)) {
			setDirty();
		} else if (TableColumnEditor.ADD_ACTION.equals(action)) {
			addColumn((MyPanel) e.getSource());
		} else if (TableColumnEditor.REMOVE_ACTION.equals(action)) {
			removeColumn((TableColumnEditor) e.getSource());
		} else if (TableColumnEditor.UP_ACTION.equals(action)) {
			moveUp((MyPanel) e.getSource());
		} else if (TableColumnEditor.DOWN_ACTION.equals(action)) {
			moveDown((MyPanel) e.getSource());
		} else if (TableColumnEditor.FILTER_ACTION.equals(action)) {
			TableColumnEditor editor = (TableColumnEditor) e.getSource();
			if (editor.isFilterOn()) {
				addFilter(editor.getColumnName(), -1);
			} else {
				removeFilter(editor.getColumnName());
			}
		} else if (TableColumnEditor.VALUE_QUERY_ACTION.equals(action)) {
			TableColumnEditor editor = (TableColumnEditor) e.getSource();
			if (editor.isValueQueryOn()) {
				addValueQuery(editor.getColumnName(), null, -1);
			} else {
				removeValueQuery(editor.getColumnName());
			}
		}
	}

	@Override
	public void columnNameChanged(final String oldName, final String newName) {
		final TableFilterEditor filter = filterMap.remove(oldName);
		if (filter != null) {
			filter.columnChanged(oldName, newName);
			filterMap.put(newName, filter);
			filterPanel.validate();
			filterPanel.repaint();
		}
		final ValueQueryEditor editor = valueQueryMap.remove(oldName);
		if (editor != null) {
			editor.columnNameChanged(oldName, newName);
			valueQueryMap.put(newName, editor);
			editor.validate();
			editor.repaint();
		}
	}

	@Override
	public void componentMoved() {
		setDirty();
	}

	public List<String> getColumnNames() {
		List<String> names = new ArrayList<>();
		for (int i = 0; i < columnPanel.getComponentCount(); i++) {
			names.add(((TableColumnEditor) columnPanel.getComponent(i)).getColumnName());
		}
		return names;
	}

	@Override
	public void queryChanged() {
		setDirty();
	}

	private void addColumn(final MyPanel source) {
		for (int i = 0; i < columnPanel.getComponentCount(); i++) {
			if (source == columnPanel.getComponent(i)) {
				columnPanel.add(newColumn(null, null), i + 1);
				columnPanel.validate();
				setDirty();
				return;
			}
		}
	}

	private void addFilter(final String columnName, final int row) {
		final TableFilterEditor newFilter = new TableFilterEditor(columnName);
		newFilter.addActionListener(this);
		filterMap.put(columnName, newFilter);
		filterPanel.addComponent(newFilter, row);
		newFilter.revalidate();
		setDirty();
	}

	private void addValueQuery(final String columnName, final String query, final int row) {
		final ValueQueryEditor newEditor = new ValueQueryEditor(columnName, query);
		newEditor.addQueryChangeListener(this);
		valueQueryPanel.add(newEditor, row);
		newEditor.revalidate();
		valueQueryMap.put(columnName, newEditor);
		setDirty();
	}

	private void clearPanel(final JPanel panel) {
		panel.removeAll();
		panel.revalidate();
		panel.repaint();
	}

	private void clearPanels() {
		queryField.setText("");
		updateTableField.setText("");
		keyFieldsField.setText("");
		clearPanel(columnPanel);
		clearPanel(filterPanel);
		clearPanel(valueQueryPanel);
		filterMap.clear();
		valueQueryMap.clear();
	}

	private void exit() {
		int response = saveCheck();
		if (response != JOptionPane.CANCEL_OPTION) {
			dispose();
		}
	}

	private void loadColumns() {
		String[] columns = props.getProperty(TableManager.COLUMNS).split(" ");
		clearPanels();
		for (String column : columns) {
			if (column.trim().length() < 1) {
				continue;
			}
			columnPanel.add(newColumn(column, props));
		}
	}

	private void loadFilters() {
		String rowCountProp = props.getProperty(FILTER_ROWS);
		if (rowCountProp != null) {
			int rowCount = Integer.parseInt(rowCountProp);
			for (int i = 0; i < rowCount; i++) {
				String[] filters = props.getProperty(TableManager.FILTER + "." + TableManager.ROW + i, "").split(" ");
				for (String filter : filters) {
					TableFilterEditor editor = new TableFilterEditor(filter);
					editor.loadFilterProperties(props);
					filterPanel.addComponent(editor, i);
				}
			}
		}
	}

	private void loadUpdateInfo() {
		updateTableField.setText(props.getProperty(TableManager.UPDATE_TABLE, ""));
		keyFieldsField.setText(props.getProperty(TableManager.KEY_FIELDS, ""));
	}

	private void loadValueQueries() {
		String[] columns = props.getProperty(COLUMNS).split(" ");
		int row = 0;
		for (String column : columns) {
			String valueQuery = props.getProperty(COLUMN_PREFIX + column + "." + VALUE_QUERY);
			if (valueQuery != null) {
				addValueQuery(column, valueQuery, row++);
			}
		}
	}

	private JMenuItem menuItem(final String text, final String action, final int mnemonic, final int ctrlKey) {
		return menuItem(text, action, mnemonic, ctrlKey, false);
	}

	private JMenuItem menuItem(final String text, final String action, final int mnemonic, final int ctrlKey, final boolean shift) {
		JMenuItem item = new JMenuItem(text, mnemonic);
		item.setActionCommand(action);
		if (shift) {
			item.setAccelerator(KeyStroke.getKeyStroke(ctrlKey, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
					| KeyEvent.SHIFT_DOWN_MASK));
		} else {
			item.setAccelerator(KeyStroke.getKeyStroke(ctrlKey, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}
		item.addActionListener(this);
		return item;
	}

	private void moveDown(final MyPanel source) {
		for (int i = 0; i < columnPanel.getComponentCount(); i++) {
			if (source == columnPanel.getComponent(i)) {
				if (i < (columnPanel.getComponentCount() - 1)) {
					columnPanel.remove(source);
					columnPanel.add(source, i + 1);
					columnPanel.validate();
					setDirty();
				}
				return;
			}
		}
	}

	private void moveUp(final MyPanel source) {
		for (int i = 0; i < columnPanel.getComponentCount(); i++) {
			if (source == columnPanel.getComponent(i)) {
				if (i > 0) {
					columnPanel.remove(source);
					columnPanel.add(source, i - 1);
					columnPanel.validate();
					setDirty();
				}
				return;
			}
		}
	}

	private TableColumnEditor newColumn(final String column, final Properties props) {
		final TableColumnEditor newColumn = new TableColumnEditor(column, props);
		newColumn.addActionListener(this);
		newColumn.addColumnNameChangeListener(this);
		return newColumn;
	}

	private void newFile() {
		if (dirty) {
			String message = "Would you like to save your changes first?";
			if (propFile != null) {
				message = "Would you like to save your changes to " + propFile.getName() + " first?";
			}
			final int response = JOptionPane.showConfirmDialog(this, message, "New File", JOptionPane.YES_NO_CANCEL_OPTION);
			switch (response) {
				case JOptionPane.YES_OPTION:
					save();
				case JOptionPane.NO_OPTION:
					startNewFile();
					break;

				case JOptionPane.CANCEL_OPTION:
					break;
			}
		} else {
			startNewFile();
		}
	}

	private void open() {
		int response = saveCheck();
		if (JOptionPane.CANCEL_OPTION == response) {
			return;
		}

		propFile = selectPropFile("Open", "Open");
		if (propFile == null) {
			return;
		}

		try {
			props.load(new FileInputStream(propFile));
			loadColumns();
			loadFilters();
			loadUpdateInfo();
			loadValueQueries();
			queryField.setText(props.getProperty(TableManager.QUERY));
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Exception occurred: " + e.toString(), "Error loading properties",
					JOptionPane.ERROR_MESSAGE);
			clearPanels();
		}
		columnPanel.revalidate();
		filterPanel.revalidate();
		reset();
	}

	private void removeColumn(final TableColumnEditor source) {
		columnPanel.remove(source);
		columnPanel.validate();
		columnPanel.repaint();
		removeFilter(source.getColumnName());
		setDirty();
	}

	private void removeFilter(final String column) {
		final TableFilterEditor filter = filterMap.remove(column);
		if (filter != null) {
			filterPanel.removeComponent(filter);
			setDirty();
		}
	}

	private void removeValueQuery(final String column) {
		final ValueQueryEditor editor = valueQueryMap.remove(column);
		if (editor != null) {
			valueQueryPanel.remove(editor);
			setDirty();
		}

	}

	private void reset() {
		dirty = false;
		if (propFile == null) {
			setTitle(TITLE);
		} else {
			setTitle(TITLE + " - " + propFile.getName());
		}
	}

	private void save() {
		if (propFile == null) {
			saveAs();
		} else {
			props.clear();
			saveColumns();
			saveFilters();
			saveUpdateInfo();
			saveValues();
			props.setProperty(TableManager.QUERY, queryField.getText());
			try {
				final FileOutputStream output = new FileOutputStream(propFile);
				props.store(output, "Created by " + getClass().getName());
				output.flush();
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Unable to Save", "Exception occurred: " + e.toString(),
						JOptionPane.ERROR_MESSAGE);
			}
			reset();
		}
	}

	private void saveAs() {
		File newFile = selectPropFile("Save As", "Save");
		if (newFile == null) {
			return;
		}

		while (newFile.exists() && ((propFile == null) || !newFile.equals(propFile))) {
			final int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to overwrite " + newFile.getName()
					+ "?", "Overwrite " + newFile.getName() + "?", JOptionPane.YES_NO_CANCEL_OPTION);
			if (response == JOptionPane.YES_OPTION) {
				break;
			} else if (response == JOptionPane.CANCEL_OPTION) {
				return;
			} else {
				newFile = selectPropFile("Save As", "Save");
			}
		}
		propFile = newFile;
		if (propFile != null) {
			save();
		}
	}

	private int saveCheck() {
		if (dirty) {
			final int response = JOptionPane.showConfirmDialog(this, "Would you like to save your changes?", "Exit",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (JOptionPane.YES_OPTION == response) {
				save();
			}
			return response;
		}
		return JOptionPane.YES_OPTION;
	}

	private void saveColumns() {
		StringBuilder columns = new StringBuilder();
		for (int i = 0; i < columnPanel.getComponentCount(); i++) {
			TableColumnEditor column = (TableColumnEditor) columnPanel.getComponent(i);
			columns.append(column.getColumnName());
			if (i < (columnPanel.getComponentCount() - 1)) {
				columns.append(" ");
			}
			column.saveColumnProperties(props);
		}
		props.setProperty(TableManager.COLUMNS, columns.toString());
	}

	private void saveFilters() {
		final int rowCount = filterPanel.getRowCount();
		props.setProperty(TableManager.FILTER_ROWS, Integer.toString(rowCount));
		for (int i = 0; i < rowCount; i++) {
			final Component[] components = filterPanel.getRowComponents(i);
			StringBuilder filterList = new StringBuilder();
			for (Component component2 : components) {
				TableFilterEditor filter = (TableFilterEditor) component2;
				filterList.append(filter.getColumnName());
				if (i < (components.length - 1)) {
					filterList.append(" ");
				}
				filter.saveFilterProperties(props);
			}
			props.setProperty(TableManager.FILTER + "." + TableManager.ROW + i, filterList.toString());
		}
	}

	private void saveUpdateInfo() {
		String updateTable = updateTableField.getText().trim();
		String keyFields = keyFieldsField.getText().trim();
		if (!(updateTable.isEmpty() || keyFields.isEmpty())) {
			props.setProperty(TableManager.UPDATE_TABLE, updateTable);
			props.setProperty(TableManager.KEY_FIELDS, keyFields);
		}
	}

	private void saveValues() {
		for (ValueQueryEditor editor : valueQueryMap.values()) {
			editor.saveQuery(props);
		}
	}

	private File selectPropFile(final String title, final String actionText) {
		JFileChooser cf = new JFileChooser(prefs.get("lastDir", null));
		cf.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(final File f) {
				final String filename = f.getName().toLowerCase();
				return f.isDirectory() || filename.endsWith("properties") || filename.endsWith("props");
			}

			@Override
			public String getDescription() {
				return "Properties Files (*.props, *.properties)";
			}
		});
		cf.setDialogTitle(title);
		final int option = cf.showDialog(this, actionText);
		if (JFileChooser.APPROVE_OPTION != option) {
			return null;
		}
		final File file = cf.getSelectedFile();
		if (file != null) {
			prefs.put("lastDir", file.getParent());
		}
		return file;
	}

	private void setDirty() {
		if (!dirty) {
			dirty = true;
			setTitle(getTitle() + "*");
		}
	}

	private void startNewFile() {
		propFile = null;
		clearPanels();
		columnPanel.add(newColumn(null, null));
		columnPanel.validate();
		reset();
	}
}
