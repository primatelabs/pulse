package com.zutubi.pulse.acceptance.components.table;

import com.zutubi.pulse.acceptance.SeleniumBrowser;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Corresponds to the Zutubi.table.SummaryTable JS component.
 */
public class SummaryTable extends ContentTable
{
    public SummaryTable(SeleniumBrowser browser, String id)
    {
        super(browser, id);
    }

    /**
     * Returns the number of data rows displayed by this table.
     *
     * @return the number of data rows shown
     */
    public long getRowCount()
    {
        return getDataLength();
    }

    /**
     * Returns the data in the given row.  The returned map has keys
     * corresponding to the column names of the table, associated with the
     * values in those columns.  The map is iterable in the order that the
     * columns are displayed.
     *
     * @param index zero-based index of the row to retrieve (note that title
     *        rows are skipped automatically)
     * @return the data in the given row, keyed by column names
     */
    public Map<String, String> getRow(int index)
    {
        String[] columnNames = ((String) browser.evaluateScript("return " + getComponentJS() + ".getColumnNames();")).split(",");
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++)
        {
            result.put(columnNames[columnIndex], getCellContents(index + 2, columnIndex));
        }
        return result;
    }
}