/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.rdp;
import noctools.endpoint.*;
import noctools.settings.NocToolsSettings;
import java.util.*;
import noctools.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import noctools.NocToolsWorker;
/**
 *
 * @author Yuri
 */
public  class RdpManager
{
    // TODO add maximum rdp limitation that will be loaded from the settings file
    private static TreeMap<String, RdpJob> _activeRdps = new TreeMap<String, RdpJob>();
    private static TreeMap<String, RdpJob> _toDeleteRdps = new TreeMap<String, RdpJob>();


    public synchronized static void addRdp(String userName)
    {
        // if we are in selection mode, then we need to create only one rdp job
        if (ServersTreeManager.isSelectionMode() && ServersTreeManager.getSelectedServer() != null)
        {
            createRdpJob(ServersTreeManager.getSelectedServer(), userName);
        }

        // if we are in checked mode, then we have to create many RDPs
        else if (ServersTreeManager.isCheckedMode() && ServersTreeManager.areThereCheckedServers())
        {
            Server[] servers = ServersTreeManager.getCheckedServers();
            for (Server tempServer : servers)
            {
                // TODO - check if there is restriction, and we shouldn't open an rdp for that user
                createRdpJob(tempServer, userName);
            }
        }
        else
            org.apache.log4j.Logger.getRootLogger().warn("There are no checked servers, or no server is selected");
    }


    public synchronized static void addRdp(Server server, String userName)
    {
        createRdpJob(server, userName);
    }

    public static void removeFinishedRdp(Server server)
    {
        _activeRdps.remove(server.getName());
    }

    public synchronized static void removeRdp(Server server)
    {
        _toDeleteRdps.put(server.getName(), _activeRdps.remove(server.getName()));
    }

    public static synchronized void createRdpJob(Server server, String userName)
    {

        // if an RDP job is already exists for this server - stop it
        boolean wait = false;
        if (_activeRdps.containsKey(server.getName()))
        {

            // TODO not working right. Needs some fixing

            org.apache.log4j.Logger.getRootLogger().error("We already have rdp for " + server.getName());
            // move this rdp to the "_toDeleteRdps" treemap
            removeRdp(server);
            wait = true;
        }
        RdpJob tempRdpJob = new RdpJob(server, userName, wait);
        _activeRdps.put(server.getName(), tempRdpJob);

        // if we have to use the Credentials Manager (for newer rdp versions)
        if (NocToolsSettings.getUseCredentialsManager())
        {

            // if we were suppose to use the credential manager, than we have to remove the credentials
            if (NocToolsSettings.getUseCredentialsManager())
            {
                // use DNS only if you are allowed and only if this is an apps server
                if (NocToolsSettings.getUseDns() && server.checkCorrectDns() && server.getMiniservers() > 0)
                    RdpManager.removeCredentialsFromCM(server.getName());
                else
                    RdpManager.removeCredentialsFromCM(server.getIP());
            }
            String pass = null;

            boolean found = false;

            // TODO check if the user exists in the credentials map
            String tempUserName = null;
            if (NocToolsSettings.getCredentials().get(userName) != null)
            {
                tempUserName = userName;
                found = true;
            }
            else
            {
                Set<String> userNames = NocToolsSettings.getCredentials().keySet();
                Iterator <String> userNamesIterator = userNames.iterator();
                while (userNamesIterator.hasNext() && !found)
                {
                    tempUserName = userNamesIterator.next();
                    if (tempUserName.equalsIgnoreCase(userName))
                        found = true;
                }
            }

            if (found && tempUserName != null)
            {
                try
                {
                    pass = AES.decrypt(Key.getKey(), NocToolsSettings.getCredentials().get(tempUserName));
                }
                catch (Exception e)
                {
                    if (e.getMessage()  != null)
                        org.apache.log4j.Logger.getRootLogger().error("Got error while decrypting credentials for user \"" + tempUserName + "\":" + e.getMessage());
                    e.printStackTrace();
                }

                if (pass != null)
                {
                    // use DNS only if you are allowed and only if this is an apps server
                    if (NocToolsSettings.getUseDns() && server.checkCorrectDns() && server.getMiniservers() > 0)
                        addCredentialsToCM(server.getName(), tempUserName, pass);
                    else
                        addCredentialsToCM(server.getIP(), tempUserName, pass);
                }
                pass = null;
                System.gc();
            }
        }

        NocToolsWorker.getInstance().addTask(tempRdpJob);
        org.apache.log4j.Logger.getRootLogger().debug("Executing the rdp job for " + server.getShortName());
    }

    public static void removeAllRdp()
    {
        while (!_activeRdps.isEmpty())
        {
            _activeRdps.remove(_activeRdps.ceilingKey(_activeRdps.firstKey())).finalize();
        }
        while (!_toDeleteRdps.isEmpty())
        {
            _toDeleteRdps.remove(_toDeleteRdps.ceilingKey(_toDeleteRdps.firstKey())).finalize();
        }
    }

    public static boolean jobExists(Server server)
    {
        return (_activeRdps.containsKey(server.getName()) ? true : false);
    }

    public static boolean toBeDeletedRdp(Server server)
    {
        return (_toDeleteRdps.containsKey(server.getName()) ? true : false);
    }

    public static void removeFromToBeDeletedRdp(Server server)
    {
        _activeRdps.remove(server.getName());
    }

    /**
     * Reads the stored RDP credentials in the windows credentials manager and returns it
     * @return all the RDP stored credentials
     */
    public static HashMap<String, HashSet<String>>getStoredCredentialsListFromCM()
    {
        HashMap<String, HashSet<String>> credentials = new HashMap<String, HashSet<String>>();
        try
        {
            Process p = Runtime.getRuntime().exec("cmd /c " + NocToolsSettings.getRdpDir() + "\\cmdkey.exe /list");
            // TODO find out why it stucks - probably because of the BufferedReader
            //p.waitFor();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()/*, "UTF-8"*/));

            boolean finished = false;
            String line;
            while (!finished)
            {
                line = readLine(in);

                if (line != null)
                {
                    if (line.contains("Target: TERMSRV"))
                    {
                        System.out.println(line);

                        String address = line.substring(line.indexOf('/') + 1);
                        String userName = null;

                        // if this address doesn't exists in the credentials map
                        if (!credentials.containsKey(address))
                        {
                            line = readLine(in); // generic
                            line = readLine(in);
                            if (line.contains("User:"))
                            {
                                // extract the user name
                                if (line.contains("\\"))
                                    userName = line.substring(line.indexOf('\\') + 1);
                                else
                                    userName = line.substring(line.indexOf(": ") + 2);

                                HashSet<String> users = new HashSet<String>();
                                users.add(userName);

                                credentials.put(address, users);

                                System.out.println(userName);
                            }
                        }
                        else
                        // this address exists in the credentials map, so we have to add a user to it
                        {
                            HashSet<String >usersSet = credentials.get(address);

                            line = readLine(in); // generic
                            line = readLine(in);

                            // extract the user name
                            if (line.contains("User:"))
                            {
                                if (line.contains("\\"))
                                    userName = line.substring(line.indexOf('\\') + 1);
                                else
                                    userName = line.substring(line.indexOf(": ") + 2);
                            }
                            usersSet.add(userName);
                        }
                    }
                }
                else finished = true;
            }
            in.close();
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
        return credentials;
    }

    /**
     * reads a line from the BufferedReader and removes any "\u0000" chars
     * @param in
     * @return
     */
    private static String readLine(BufferedReader in)
    {
        String newStr = null;
        try
        {
            // read the line from the BufferedReader
            String str = in.readLine();
            newStr = null;

            if (str != null)
            {
                newStr = new String();
                char[] array = str.toCharArray();

                for (int i = 0; i < array.length; i++)
                {
                    if (array[i] != 0)
                        newStr += array[i];
                }
            }
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }

        return newStr;
    }

    /**
     * adds the given credentials to the windows credentials manager
     * @param address
     * @param userName
     * @param password
     */
    public synchronized static void addCredentialsToCM(String address, String userName, String password)
    {
        try
        {
            Process p = Runtime.getRuntime().exec("cmd /c " + NocToolsSettings.getRdpDir() + "cmdkey.exe  /generic:TERMSRV/" + address + " /user:" + userName + " /pass:\"" + password + "\"");
            p.waitFor();
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * removes the credentials for the address from the windows credentials manager
     * @param address
     */
    public synchronized static void removeCredentialsFromCM(String address)
    {
        try
        {
            Process p = Runtime.getRuntime().exec("cmd /c " + NocToolsSettings.getRdpDir() + "cmdkey.exe  /delete:TERMSRV/" + address + "");
            p.waitFor();
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean userExists(String userName)
    {
        boolean exists = false;

        // we have to search all it one by one because the userName is not case sensetive

        if (NocToolsSettings.getCredentials().containsKey(userName))
            exists = true;
        else
        {
            Set<String> credentialsSet = NocToolsSettings.getCredentials().keySet();
            Iterator<String> credentialsIterator = credentialsSet.iterator();
            while (credentialsIterator.hasNext())
                if (credentialsIterator.next().equalsIgnoreCase(userName))
                    exists = true;
        }
        
        return exists;
    }

    /**
     * Returns a sorted list of all the userNames
     * @return If there is no User Names, it returns an empty list
     */
    public static List<String> getUsersList()
    {
        List <String> usersList = new LinkedList<String>();

        if (NocToolsSettings.getCredentials().size() > 0)
        {
            Set <String> usersSet = NocToolsSettings.getCredentials().keySet();
            Iterator<String> i = usersSet.iterator();
            while (i.hasNext())
            {
                usersList.add(i.next());
            }

            Collections.sort(usersList);
        }

        return usersList;
    }
}

