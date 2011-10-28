/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.startup;


import java.io.File;
import noctools.endpoint.Server;
import noctools.rdp.RdpManager;
import noctools.settings.NocToolsSettings;

/**
 *
 * @author Yuri
 */
public class StartupTask extends Thread
{
    
    private Server _server;
    private String _accountName;
    private int _taskNum;
    private boolean _wasExecuted;

    public StartupTask(Server server, String accountName, int taskNum)
    {
        super("Startup Task");
        _taskNum = taskNum;
        _server = server;
        _accountName = new String(accountName);
        _wasExecuted = false;
    }

    @Override
    public void run()
    {
        _wasExecuted = true;
        // check if there is no open rdp for the server
        // TODO find a better solution
        while (RdpManager.jobExists(_server))
        {
            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException e)
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("Starting rdp to " +_server.getShortName() + " in order to start the remote there");
        
        // create rdp
        RdpManager.addRdp(_server, _accountName);

        try {
            // wait for the RDP to start and to login
            System.out.println("Sleeping " + NocToolsSettings.getStartupScriptDelay() + " miliseconds");
            Thread.sleep(NocToolsSettings.getStartupScriptDelay());

        }
        catch (InterruptedException e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }

        // run the script
        System.out.println("Running the script");

        Process pr;
        try
        {
            File LastStartupScriptFile = new File(NocToolsSettings.getScriptsDir() + NocToolsSettings.getLastStartupScriptFile());

            if (_taskNum * NocToolsSettings.getMiniserversPerRemote() == _server.getMiniservers() && LastStartupScriptFile.exists())
            {
                System.out.println("Runnung: " + NocToolsSettings.getScriptsDir() + NocToolsSettings.getLastStartupScriptFile() + " /DNSName=" + _server.getName() + " /ServerName=" + _server.getDnsName() + " /param=" + _taskNum);
                pr = Runtime.getRuntime().exec(NocToolsSettings.getScriptsDir() + NocToolsSettings.getLastStartupScriptFile() + " /DNSName=" + _server.getName() + " /ServerName=" + _server.getDnsName() + " /param=" + _taskNum);
            }
            else
            {
    //            System.out.println("running: cmd /c start " + NocToolsSettings.getScriptsDir() + NocToolsSettings.getStartupScriptFile() + " /DNSName=" + _server.getName() + " /ServerName=" + _server.getDnsName() + " /param=" + _taskNum);
                System.out.println("Runnung: " + NocToolsSettings.getScriptsDir() + NocToolsSettings.getStartupScriptFile() + " /DNSName=" + _server.getName() + " /ServerName=" + _server.getDnsName() + " /param=" + _taskNum);
    //            pr = Runtime.getRuntime().exec("cmd /c start " + NocToolsSettings.getScriptsDir() + NocToolsSettings.getStartupScriptFile() + " /DNSName=" + _server.getName() + " /ServerName=" + _server.getDnsName() + " /param=" + _taskNum);
                pr = Runtime.getRuntime().exec(NocToolsSettings.getScriptsDir() + NocToolsSettings.getStartupScriptFile() + " /DNSName=" + _server.getName() + " /ServerName=" + _server.getDnsName() + " /param=" + _taskNum);
            }
            pr.waitFor();
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
//            e.printStackTrace();
        }
    }

    public int getTaskNumber() {return _taskNum;}

    @Override
    public String toString()
    {
        return "Start " + _accountName;
    }

    public boolean wasExecuted() {return _wasExecuted;}
    public void setWasExecuted(boolean executed) {_wasExecuted = executed;}

}
