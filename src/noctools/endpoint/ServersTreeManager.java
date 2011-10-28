/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.endpoint;
import noctools.settings.NocToolsSettings;
import java.util.*;
import java.io.*;
import java.awt.Component;
import javax.swing.JFileChooser;
import java.awt.datatransfer.*;
import java.awt.Toolkit;
/**
 *
 * @author Yuri
 */
public class ServersTreeManager
{
    public static enum WorkMode {CHECKED, SELECTION};

    private static LinkedList<String> _allServers;
    private static TreeMap<String, Server> _checkedServers = new TreeMap<String, Server>();
    private static Server _selectedServer = null;
    private static WorkMode _workMode = ServersTreeManager.WorkMode.SELECTION;
    private static String _currentServersListDir = System.getProperty("user.home");

    public static void removeAllcheckedServers() 
    {
        _checkedServers.clear();
    }

    public static void setSelectedServer(Object node)
    {
        if (node instanceof ComparableMutableTreeNode)
        {
            Object endpoint = ((ComparableMutableTreeNode)node).getUserObject();
            if (endpoint instanceof Server)
            {
                _selectedServer = (Server)endpoint;
                //org.apache.log4j.Logger.getRootLogger().debug("The selected server is " + _selectedServer);
            }
            else
            {
                _selectedServer = null;
                //org.apache.log4j.Logger.getRootLogger().debug("There is no selected server");
            }
        }
        else
            throw new ClassCastException();
    }

    public static Server getSelectedServer()
    {
        return _selectedServer;
    }


    public static void removeCheckedServer(Server server)
    {
        _checkedServers.remove(server.getName());
    }

    public static boolean areThereCheckedServers()
    {
        if (_checkedServers.isEmpty())
            return false;
        else
            return true;
    }

    public static void setSelectionMode()
    {
        _workMode = ServersTreeManager.WorkMode.SELECTION;
        //org.apache.log4j.Logger.getRootLogger().debug("Selection mode activated");
    }

    public static void setCheckedMode()
    {
        _workMode = ServersTreeManager.WorkMode.CHECKED;
        //org.apache.log4j.Logger.getRootLogger().debug("Checked mode activated");
    }

    public static boolean isSelectionMode()
    {
        return (_workMode == ServersTreeManager.WorkMode.SELECTION) ? true : false;
    }
    public static boolean isCheckedMode()
    {
        return (_workMode == ServersTreeManager.WorkMode.CHECKED) ? true : false;
    }

    /**
     * Adds all the children of this object to the _checkedServers list
     * @param root ComparableMutableTreeNode object
     */
    public static void addCheckedPath(Object root)
    {
        if (root instanceof ComparableMutableTreeNode)
        {
            addServerFromTree((ComparableMutableTreeNode)root);
        }
        else
            throw new ClassCastException();
    }

    /**
     * recursive method that adds servers to the _checkedServers tree map
     * @param root
     */
    private static void addServerFromTree(ComparableMutableTreeNode root)
    {
        if (root.isLeaf())
        {
            Object endpoint = root.getUserObject();
            if (endpoint instanceof Server)
            {
                Server tempServer = (Server)endpoint;
                _checkedServers.put(tempServer.getName(), tempServer);
                //org.apache.log4j.Logger.getRootLogger().debug("checked servers list - " + _checkedServers);
            }

        }
        else
        {
            Enumeration children = root.children();
            while (children.hasMoreElements())
            {
                Object child = children.nextElement();
                addServerFromTree((ComparableMutableTreeNode)child);
            }

        }
    }

    public static void removeUncheckedPath(Object root)
    {
        if (root instanceof ComparableMutableTreeNode)
        {
            if (!_checkedServers.isEmpty())
                removeServerFromTree((ComparableMutableTreeNode)root);
        }
        else
            throw new ClassCastException();
    }

    private static void removeServerFromTree(ComparableMutableTreeNode root)
    {
        if (root.isLeaf())
        {
            Object endpoint = root.getUserObject();
            if (endpoint instanceof Server)
            {
                Server tempServer = (Server)endpoint;
                tempServer.setCurrentRdp(0);
                _checkedServers.remove(tempServer.getName());
                //org.apache.log4j.Logger.getRootLogger().debug("Removed from checked servers list - " + tempServer.toString());
            }

        }
        else
        {
            Enumeration children = root.children();
            while (children.hasMoreElements())
            {
                Object child = children.nextElement();
                removeServerFromTree((ComparableMutableTreeNode)child);
            }
        }
    }

    public static int getMaxMiniservers()
    {
        int maxMiniservers = 0;
        // if this is checked mode, we have to find the maximum miniservers among the checked servers
        if (_workMode == ServersTreeManager.WorkMode.CHECKED)
        {
            Collection checkedServers = _checkedServers.values();
            if (checkedServers.size() > 0)
            {
                for (Object serverObj : checkedServers)
                {
                    Server tempServer = (Server)serverObj;
                    maxMiniservers = (tempServer.getMiniservers() > maxMiniservers) ? tempServer.getMiniservers() : maxMiniservers;
                }
            }
            return maxMiniservers;
        }
        else
            return (_selectedServer != null) ?_selectedServer.getMiniservers() : 0;
    }

    /**
     * Return an array of all the checked servers from the tree
     * @return Array of the checked servers
     */
    public static noctools.endpoint.Server[] getCheckedServers()
    {
        Server [] servers = new Server[_checkedServers.size()];
        Collection serversCollection = _checkedServers.values();
        int i = 0;
        for (Object tempServer : serversCollection)
        {
            servers[i] = (Server)tempServer;
            i++;
        }
        return servers;
    }

    public static LinkedList<String> getServersList(ComparableMutableTreeNode root)
    {
        return _allServers == null ? new LinkedList<String>() : _allServers;
    }

    public static void createServersList(ComparableMutableTreeNode root)
    {
        _allServers = new LinkedList<String>();
        createServersListRecursive(root);
    }
    
    /**
     * Creates the list of all servers (recursive)
     * @param root
     */
    private static void createServersListRecursive(ComparableMutableTreeNode root)
    {
        if (root.isLeaf())
        {
            Object endpoint = root.getUserObject();
            if (endpoint instanceof Server)
            {
                Server tempServer = new Server((Server)endpoint);
                _allServers.add(tempServer.getName());
            }
        }
        else
        {
            Enumeration children = root.children();
            while (children.hasMoreElements())
            {
                Object child = children.nextElement();
                createServersListRecursive((ComparableMutableTreeNode)child);
            }
        }
    }

    /**
     * Saves the checked servers to a text file
     * @param component The component is needed for the JFileChooser dialog
     */
    public static boolean exportCheckedServersList(Component component)
    {
        LinkedList<String> tempServers =  new LinkedList<String>();
        for (String tempServer : _allServers)
        {
            if (_checkedServers.containsKey(tempServer))
                tempServers.add(tempServer);
        }

        // show the JFileChooser dialog, and add the ".txt" extention if the user didn't enter it by himself
        JFileChooser fc = new JFileChooser(_currentServersListDir);
        int returnVal = fc.showDialog(component, "Save List");
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            _currentServersListDir = fc.getCurrentDirectory().toString();
            File file = fc.getSelectedFile();
            String txtExtention = ".txt";
            int nameLength = file.toString().length();
            if (file.toString().lastIndexOf(txtExtention) != nameLength - txtExtention.length())
                writeServersListToFile(tempServers, new File(file.toString() + ".txt"));
            else
                writeServersListToFile(tempServers, new File(file.toString()));

            return true; // so that we know that we have to clear the checked servers
        }
        return false; // so that we know that we don't have to clear the selected servers
    }

    private static void writeServersListToFile(LinkedList<String> tempServersList, File serversFile)
    {

        try
        {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(serversFile)));
            for (String tempServer : tempServersList)
            {
                writer.append(tempServer);
                writer.append(NocToolsSettings.ENDL);
            }
            writer.close();

            // open the new file vie editor
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            try
            {
                desktop.open(serversFile);
            }
            catch (Exception e)
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Copies the checked servers to clipboard
     */
    public static void CopySelectedServersToClipboard()
    {
        if (areThereCheckedServers() || _selectedServer != null)
        {
            // get the system clipboard
            Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            
            String serversToClipboard = new String();

            if (areThereCheckedServers())
            {
                Set <String> keys = _checkedServers.keySet();
                Iterator keysIterator = keys.iterator();
                while (keysIterator.hasNext())
                {
                    Endpoint checkedServer = _checkedServers.get(keysIterator.next().toString());
                    serversToClipboard += checkedServer.getName() + "\n";
                }
            }
            else if (_selectedServer != null)
                serversToClipboard += _selectedServer.getName() + "\n";

            Transferable transferableText = new StringSelection(new String(serversToClipboard));
            systemClipboard.setContents(transferableText, null);
        }
    }

    /**
     * Copies the checked servers ip to clipboard
     */
    public static void CopySelectedServersIpToClipboard()
    {
        if (areThereCheckedServers() || _selectedServer != null)
        {
            // get the system clipboard
            Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            String serversToClipboard = new String();

            if (areThereCheckedServers())
            {
                Set <String> keys = _checkedServers.keySet();
                Iterator keysIterator = keys.iterator();
                while (keysIterator.hasNext())
                {
                    Server checkedServer = _checkedServers.get(keysIterator.next().toString());
                    serversToClipboard += checkedServer.getIP() + "\n";
                }
            }
            else if (_selectedServer != null)
                serversToClipboard += _selectedServer.getIP() + "\n";

            Transferable transferableText = new StringSelection(new String(serversToClipboard));
            systemClipboard.setContents(transferableText, null);
        }
    }
}
