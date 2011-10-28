package noctools.endpoint;

import javax.swing.tree.*;
import java.util.*;

/**
 * This class is needed so that the tree nodes will be sortable
 * @author Yuri
 */
public class ComparableMutableTreeNode extends DefaultMutableTreeNode implements Comparable
{
    public ComparableMutableTreeNode()
    {
        super();
    }

    public ComparableMutableTreeNode(Endpoint endpoint)
    {
        super(endpoint);
    }

    public void sortChildren()
    {
        if (children != null)
        {
            Collections.sort(children);
        }
    }

    public int compareTo(Object other)
    {
        if (other instanceof ComparableMutableTreeNode)
        {
            return(userObject.toString().compareToIgnoreCase(((ComparableMutableTreeNode)other).userObject.toString()));
        }
        else
            throw new ClassCastException();
    }
}
