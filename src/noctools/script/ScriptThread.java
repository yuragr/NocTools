/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.script;
import noctools.settings.NocToolsSettings;
import java.util.*;
import noctools.endpoint.*;
import noctools.*;
import javax.swing.*;

/**
 *
 * @author YuriG
 */
public class ScriptThread extends Thread
{
    private String _scriptFile;
    private String _commandStr;
    private Server _server;
    private Script _script;
  
    public ScriptThread(Script script, Server server)
    {
        super("Script Thread running  \"" + script.getDescription() + "\"");
        _script = script;
        _server = (server != null) ? new Server(server) : null;

        _commandStr = new String();
        _scriptFile = NocToolsSettings.getScriptsDir() + script.getScriptFile();

        // if this is a .vbs file, we must run it via "wscript" command
        if (_script.getScriptFileExtention().equalsIgnoreCase(".vbs"))
            _commandStr += "wscript ";
        else
            _commandStr += "cmd /c start ";

        _commandStr += _scriptFile;
        
        LinkedList<String> argumentsList = script.getArgumentsList();
        if (argumentsList != null)
        {
            // add the user arguments to the command line
            for(String tempParameter : argumentsList)
            {
                _commandStr += " ";

                // remove the "[]"
                if (tempParameter.charAt(0) == '[' && tempParameter.charAt(tempParameter.length() - 1) == ']')
                    tempParameter = tempParameter.substring(1, tempParameter.length() - 1);

                // get server IP parameter
                while (tempParameter.toLowerCase().indexOf("%ip") >= 0)
                {
                    int paramIndex = tempParameter.toLowerCase().indexOf("%ip");
                    String target = new String();
                    target += tempParameter.charAt(paramIndex);
                    target += tempParameter.charAt(paramIndex + 1);
                    target += tempParameter.charAt(paramIndex + 2);
                    tempParameter = tempParameter.replace(target, server.getIP());
                }

                // get server name parameter
                while (tempParameter.toLowerCase().indexOf("%s") >= 0)
                {
                    int paramIndex = tempParameter.toLowerCase().indexOf("%s");
                    String target = new String();
                    target += tempParameter.charAt(paramIndex);
                    target += tempParameter.charAt(paramIndex + 1);
                    tempParameter = tempParameter.replace(target, server.getName());
                }

                // get short server name parameter
                while (tempParameter.toLowerCase().indexOf("%r") >= 0)
                {
                    int paramIndex = tempParameter.toLowerCase().indexOf("%r");
                    String target = new String();
                    target += tempParameter.charAt(paramIndex);
                    target += tempParameter.charAt(paramIndex + 1);
                    tempParameter = tempParameter.replace(target, server.getShortName());
                }

                // get DNS server name parameter
                while (tempParameter.toLowerCase().indexOf("%d") >= 0)
                {
                    int paramIndex = tempParameter.toLowerCase().indexOf("%d");
                    String target = new String();
                    target += tempParameter.charAt(paramIndex);
                    target += tempParameter.charAt(paramIndex + 1);
                    tempParameter = tempParameter.replace(target, server.getDnsName());
                }

                _commandStr += tempParameter;
            }
            org.apache.log4j.Logger.getRootLogger().debug("Arguments for \"" + _script.getDescription() + "\" are: " + argumentsList.toString());
        }
    }


    @Override
    public void run()
    {
        if (_script.getAllowExecution())
        {
            org.apache.log4j.Logger.getRootLogger().info("Executing \"" + _commandStr + "\"");
            try
            {
                Runtime rt = Runtime.getRuntime();
                Process pr = rt.exec(_commandStr);
                pr.waitFor();
            }
            catch (Exception e)
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
            finally
            {
                org.apache.log4j.Logger.getRootLogger().debug("Finished executing \"" + _commandStr + "\"");
            }
        }
        else
            org.apache.log4j.Logger.getRootLogger().info("The script \"" + _script.getDescription() + "\" wasn't executed because the user didn't entered one of the requested parameters");
    }

    public Script getScript()
    {
        return _script;
    }
}
