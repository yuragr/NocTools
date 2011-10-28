/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.link;
import javax.swing.table.AbstractTableModel;
import java.util.*;
/**
 *
 * @author Yuri
 */
public class LinksTableModel extends AbstractTableModel
{
    private LinkedList<Vector<Object>> _data;
    private final static int COLUMN_COUNT = 2;

    public LinksTableModel(HashMap<String, String> allLinks)
    {
        super();
        _data = new LinkedList<Vector<Object>>();

        if (allLinks != null)
        {
            if (!allLinks.isEmpty())
            {
                Object [] descriptions = allLinks.keySet().toArray();
                for (Object linkDescriptionObject : descriptions)
                {
                    if (linkDescriptionObject instanceof String)
                    {
                        String tempDescription = (String)linkDescriptionObject;
                        Vector<Object> row = new Vector<Object>(COLUMN_COUNT);
                        row.add(0, tempDescription);
                        row.add(1, allLinks.get(tempDescription));

                        _data.add(row);
                    }
                }
            }
        }
    }

    public Object getValueAt(int rowIndex,int columnIndex)
    {
        if (rowIndex < _data.size() && columnIndex < COLUMN_COUNT)
        {
            Vector<Object> row = _data.get(rowIndex);
            return row.get(columnIndex);
        }
        return null;
    }

    @Override
    public int getRowCount()
    {
        return _data.size();
    }

    public int getColumnCount()
    {
        return COLUMN_COUNT;
    }

    @Override
    public String getColumnName(int index)
    {
        if (index == 0)
            return "Description";
        if (index == 1)
            return "Web Link";

        return new String();
    }

    @Override
    public boolean isCellEditable(int rowIndex,int columnIndex)
    {
        if (rowIndex < _data.size() && columnIndex < COLUMN_COUNT)
            return true;
        else
            return false;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        if (rowIndex < _data.size() && columnIndex < COLUMN_COUNT)
        {
            Vector<Object> row = _data.get(rowIndex);
            row.set(columnIndex, aValue);
        }
    }

    public void addEmptyRow()
    {
        Vector <Object> emptyRow = new Vector <Object>();
        emptyRow.add(new String(""));
        emptyRow.add(new String(""));
        _data.add(emptyRow);
    }

    public void removeRow(int rowIndex)
    {
        if (rowIndex >= 0 && rowIndex < _data.size())
        {
            _data.remove(rowIndex);
        }
    }
}
