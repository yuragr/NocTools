package noctools.rdp;
import noctools.settings.NocToolsSettings;
import noctools.endpoint.Server;
import noctools.*;
import java.io.*;
import javax.swing.*;
import java.util.Date;

/**
 *
 * @author Yuri
 */
public class RdpJob extends Thread
{
    private boolean _initOk;
    private File _rdpFile;
    private String _rdpFilesDir;
    private String _tempRdpFilesDir; // this directory will store the running RDPs
    private Server _server;
    RdpJob _previousJob;
    boolean _wait;
    String _userName; // just for logging

    public RdpJob(Server server, String userName, boolean wait)
    {
        super("RDP job for " + server);
        _rdpFile = null;
        _initOk = false;
        _server = new Server(server);

        // load those varaibles from settings file
        _rdpFilesDir = NocToolsSettings.getRdpDir();
        _tempRdpFilesDir = NocToolsSettings.getTempRdpDir();

        // try to create an RDP file
        _rdpFile = createRdpFile(server, userName);
        _wait = wait; // wait for the previous rdp to end
        _userName = userName;

        if (_rdpFile == null)
        {
            //if the RDP file doesn't exists'
            JOptionPane.showMessageDialog(null, "Please create an RDP template file in " + NocToolsSettings.getRdpDir());
        }
        else
        {
            // if the file exists, the init was OK
            _initOk = true;
        }
    }

    @Override
    public void run()
    {
        // TODO rdp file creation should be inside the doInBackground() method
        // run the thread only if the init was OK
        try
        {
            if (_initOk)
            {
                if (_wait)
                    Thread.sleep(150);

                try
                {
                    Thread.sleep(100);
                    Runtime rt = Runtime.getRuntime();
                    if (!_rdpFile.exists())
                        org.apache.log4j.Logger.getRootLogger().error("The file " + _rdpFile + " doesn't exist!!!");
                    else
                    {
                        Process pr = rt.exec("cmd /c mstsc \"" + _rdpFile + "\"");
                        pr.waitFor();
                    }
                }
                catch (Exception e)
                {
                    if (e.getMessage()  != null)
                        org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                    e.printStackTrace();
                }

                org.apache.log4j.Logger.getRootLogger().debug(_server + " - RDP Job finished");
                if (_rdpFile.exists())
                    _rdpFile.delete();
                if (RdpManager.toBeDeletedRdp(_server))
                    RdpManager.removeFromToBeDeletedRdp(_server);
                else
                    RdpManager.removeFinishedRdp(_server);
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
     * Chooses what kind of RDP file is needed to be created
     * @param server The server info
     * @param user User info (might be null)
     * @return The File instance of the created RDP file. Returns null if failed
     */
    private File createRdpFile(Server server, String user)
    {
        // TODO add a check if this connection already exists in the temporary RDP folder
        // check if the file exists
        Date date = new Date();
        File serverRdpFile = new File(_rdpFilesDir + server.removeSpecialChars(server.getName()) + "_" + (date.getTime() % 1000) + ".rdp");

        if (user != null)
        {
            File userRdpFile = new File(_rdpFilesDir + user+  ".rdp");
            // find the user's RDP file and create a copy of it with the wanted server
            if (userRdpFile.exists())
                return createTempRdpFromUserRdp(userRdpFile, server);
            else
                // the user RDP file doesn't exists, so just use the servers address to create RDP file without userName
                return createStandartTempRdpFile(server);
        }
        else if (serverRdpFile.exists())
        {
            // find the server's rdp and create a copy of it
            return createTempRdpFile(serverRdpFile);
        }
        else
            // the user's and the server's RDP files do not exists.
            org.apache.log4j.Logger.getRootLogger().warn("The user's and the server's RDP files do not exist");
        return null;
    }

    /**
     * Creates a copy of a "user RDP file" and inserts the server address where needed
     * @param userRdpFile The "user RDP file"
     * @param server The server info that will be added to the RDP file
     * @return new temp RDP file in the temp RDP files folder
     */
    private File createTempRdpFromUserRdp(File userRdpFile, Server server)
    {
        File tempRdpFile = new File(_tempRdpFilesDir + server.removeSpecialChars(server.getName()) + "_" + (Math.abs((new java.util.Random()).nextInt() % 1000)) + "_" + userRdpFile.getName());
        try
        {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempRdpFile)/*, "UTF-16"*/));

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(userRdpFile), "UTF-16"));
            String str;
            while ((str = reader.readLine()) != null)
            {
                if (str.equals("full address:s:"))
                // we have found the place where we should insert the server's address
                {
                    // use DNS only if you are allowed and only if this is an apps server
                    if (NocToolsSettings.getUseDns() && server.checkCorrectDns() && server.getMiniservers() > 0)
                        str += server.getName();
                    else
                        str+= server.getIP();
                }
                writer.append(str);
                writer.append(NocToolsSettings.ENDL);
            }
            reader.close();
            writer.close();

        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
        return tempRdpFile;
    }

    /**
     * Build RDP file from scratch for a given server
     * @param server Server information
     * @return the new RDP file
     */
    private File createStandartTempRdpFile(Server server)
    {
        Date date = new Date();
        File tempRdpFile = new File(_tempRdpFilesDir + server.removeSpecialChars(server.getName()) + "_" + (date.getTime() % 1000) + ".rdp");
        if (tempRdpFile.exists())
        {
            org.apache.log4j.Logger.getRootLogger().warn("The RDP file with the name " + server.getName() +  " already exists");
            return null;
        }
        else
        {
            try
            {
                FileWriter writer = new FileWriter(tempRdpFile);
                writer.append("screen mode id:i:");
                writer.append(Integer.toString(NocToolsSettings.getScreenModeId()));
                writer.append(NocToolsSettings.ENDL);

                writer.append("desktopwidth:i:");
                writer.append(Integer.toString(NocToolsSettings.getDesktopWidth()));
                writer.append(NocToolsSettings.ENDL);

                writer.append("desktopheight:i:");
                writer.append(Integer.toString(NocToolsSettings.getDesktopHeight()));
                writer.append(NocToolsSettings.ENDL);

                writer.append("session bpp:i:16");
                writer.append(NocToolsSettings.ENDL);

                writer.append("winposstr:s:0,1,612,0,1152,559");
                writer.append(NocToolsSettings.ENDL);

                writer.append("full address:s:");
                writer.append(server.getName());
                writer.append(NocToolsSettings.ENDL);

                writer.append("compression:i:1");
                writer.append(NocToolsSettings.ENDL);

                writer.append("keyboardhook:i:2");
                writer.append(NocToolsSettings.ENDL);

                writer.append("audiomode:i:1");
                writer.append(NocToolsSettings.ENDL);

                writer.append("redirectdrives:i:1");
                writer.append(NocToolsSettings.ENDL);

                writer.append("redirectprinters:i:0");
                writer.append(NocToolsSettings.ENDL);

                writer.append("redirectcomports:i:0");
                writer.append(NocToolsSettings.ENDL);

                writer.append("redirectsmartcards:i:1");
                writer.append(NocToolsSettings.ENDL);

                writer.append("displayconnectionbar:i:1");
                writer.append(NocToolsSettings.ENDL);

                writer.append("autoreconnection enabled:i:1");
                writer.append(NocToolsSettings.ENDL);

                writer.append("username:s:");
                writer.append(NocToolsSettings.ENDL);

                writer.append("domain:s:");
                writer.append(NocToolsSettings.ENDL);

                writer.append("alternate shell:s:");
                writer.append(NocToolsSettings.ENDL);

                writer.append("shell working directory:s:");
                writer.append(NocToolsSettings.ENDL);

                writer.append("disable wallpaper:i:0");
                writer.append(NocToolsSettings.ENDL);

                writer.append("disable full window drag:i:0");
                writer.append(NocToolsSettings.ENDL);

                writer.append("disable menu anims:i:0");
                writer.append(NocToolsSettings.ENDL);

                writer.append("disable themes:i:0");
                writer.append(NocToolsSettings.ENDL);

                writer.append("disable cursor setting:i:0");
                writer.append(NocToolsSettings.ENDL);

                writer.append("bitmapcachepersistenable:i:1");
                writer.append(NocToolsSettings.ENDL);
                writer.close();
                return tempRdpFile;
            }
            catch (IOException e)
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Create a copy of an existingFile in the temp RDP folder
     * @param existingFile The file of which you want to make a copy
     * @return the new copy in the temp RDP folder
     */
    private File createTempRdpFile(File existingFile)
    {
        File tempRdpFile = new File(_tempRdpFilesDir + existingFile.getName());
        try
        {

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempRdpFile)));

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(existingFile), "UTF-16"));
            String str;
            while ((str = reader.readLine()) != null)
            {
                writer.append(str);
                writer.append(NocToolsSettings.ENDL);
            }
            reader.close();
            writer.close();
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
        return tempRdpFile;
    }

    @Override
    public void finalize()
    {
        // remove the temporary rdp file
        if (_rdpFile.exists())
            _rdpFile.delete();
    }
}
