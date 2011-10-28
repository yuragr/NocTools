/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.script;
import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 *
 * @author Yuri
 */
public class ScriptsTableModel extends AbstractTableModel
{
    private LinkedList<Vector<Object>> _data;
    private final static int COLUMN_COUNT = 3;

    public ScriptsTableModel(TreeMap<String, Script> allScripts)
    {
        _data = new LinkedList<Vector<Object>>();
        if (!allScripts.isEmpty())
        {
            Object [] scripts = allScripts.values().toArray();
            for (Object script : scripts)
            {
                if (script instanceof Script)
                {
                    Script tempScript = (Script)script;
                    Vector<Object> row = new Vector<Object>(COLUMN_COUNT);
                    row.add(0, tempScript.getDescription());
                    row.add(1, tempScript.getScriptFile());
                    row.add(2, tempScript.getType());
                    _data.add(row);
                }
            }
        }
        //else
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
            return "Script Description";
        if (index == 1)
            return "Script File";
        if (index == 2)
            return "Type";

        return new String();
    }
}