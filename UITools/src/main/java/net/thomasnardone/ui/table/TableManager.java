package net.thomasnardone.ui.table;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.thomasnardone.ui.DataType;
import net.thomasnardone.ui.EditType;
import net.thomasnardone.ui.FilterType;
import net.thomasnardone.ui.rest.ColumnInfo;
import net.thomasnardone.ui.rest.FilterInfo;

/**
 * Manages conversion of column properties to Java entities.
 * 
 * @author Thomas Nardone
 */
public class TableManager {
	public static final String				COLUMN_PREFIX	= "column.";
	public static final String				COLUMNS			= "columns";
	public static final String				DATA_TYPE		= "dataType";
	public static final String				DISPLAY_NAME	= "displayName";
	public static final String				EDIT_TYPE		= "editType";
	public static final String				FILTER			= "filter";
	public static final String				FILTER_ROWS		= FILTER + ".rows";
	public static final String				KEY_FIELDS		= "keyFields";
	public static final String				QUERY			= "query";
	public static final String				ROW				= "row";
	public static final String				TYPE			= "type";
	public static final String				UPDATE_TABLE	= "updateTable";
	public static final String				VALUE_QUERY		= "valueQuery";

	private final Map<String, ColumnInfo>	columnMap;
	private final List<ColumnInfo>			columns;
	private final List<FilterInfo>			filters;
	private String[]						keyFields;
	private String							query;
	private String							updateTable;

	/**
	 * Create an empty {@link TableManager} and load the properties from <tt>input</tt>.
	 * 
	 * @see Properties#load(InputStream)
	 */
	public TableManager(final InputStream input) throws IOException {
		this();
		Properties props = new Properties();
		props.load(input);
		loadProperties(props);
	}

	/**
	 * Create a {@link TableManager} from <tt>props</tt>.
	 * 
	 * @param props
	 */
	public TableManager(final Properties props) {
		this();
		loadProperties(props);
	}

	/**
	 * Create an empty {@link TableManager} and load the properties from <tt>reader</tt>.
	 * 
	 * @see Properties#load(Reader)
	 */
	public TableManager(final Reader reader) throws IOException {
		this();
		Properties props = new Properties();
		props.load(reader);
		loadProperties(props);
	}

	private TableManager() {
		columns = new ArrayList<>();
		columnMap = new HashMap<>();
		filters = new ArrayList<>();
	}

	public ColumnInfo getColumn(final String name) {
		return columnMap.get(name);
	}

	public List<ColumnInfo> getColumns() {
		return columns;
	}

	public List<FilterInfo> getFilters() {
		return filters;
	}

	public String[] getKeyFields() {
		return keyFields;
	}

	public String getQuery() {
		return query;
	}

	public String getUpdateTable() {
		return updateTable;
	}

	public void setQuery(final String query) {
		this.query = query;
	}

	private void loadProperties(final Properties props) {
		String[] columnSplit = props.getProperty(COLUMNS).split(" ");
		for (String column : columnSplit) {
			ColumnInfo info = new ColumnInfo();
			info.setName(column);
			info.setDisplayName(props.getProperty(COLUMN_PREFIX + column + "." + DISPLAY_NAME));
			info.setDataType(DataType.valueOf(props.getProperty(COLUMN_PREFIX + column + "." + DATA_TYPE)));
			info.setEditType(EditType.valueOf(props.getProperty(COLUMN_PREFIX + column + "." + EDIT_TYPE)));
			info.setValueQuery(props.getProperty(COLUMN_PREFIX + column + "." + VALUE_QUERY));
			columns.add(info);
			columnMap.put(column, info);
		}

		String filterRowsProp = props.getProperty(FILTER_ROWS);
		if (filterRowsProp != null) {
			int rowCount = Integer.parseInt(filterRowsProp);
			for (int i = 0; i < rowCount; i++) {
				String[] filtersInRow = props.getProperty(FILTER + "." + ROW + i, "").split(" ");
				for (int j = 0; j < filtersInRow.length; j++) {
					String filter = filtersInRow[j];
					FilterInfo info = new FilterInfo();
					info.setColumnName(filter);
					info.setDisplayName(props.getProperty(COLUMN_PREFIX + filter + "." + DISPLAY_NAME, filter));
					info.setRow(i);
					info.setColumn(j);
					final String type = props.getProperty(FILTER + "." + filter + "." + TYPE);
					try {
						info.setType(FilterType.valueOf(type));
					} catch (IllegalArgumentException | NullPointerException e) {
						System.out.println("Invalid filter type for " + filter + ": " + type);
						info.setType(FilterType.Text);
					}
					filters.add(info);
				}
			}
		}
		query = props.getProperty(QUERY);
		updateTable = props.getProperty(UPDATE_TABLE);
		keyFields = props.getProperty(KEY_FIELDS, "").split(",");
	}
}
