/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.util;

import java.io.InputStream;
import java.util.LinkedList;
import noctools.settings.NocToolsSettings;

/**
 * Counts the number of processes on a remove machine. There is an option to
 * use a filter
 * @author Yuri
 */
public class TasklistProcessCounter extends Thread
{
    private boolean _isDone = false;
    private int _processes = 0;
    private String _remoteMachine = null;
    private String _processFilter = null;
    private String _user = null;
    private String _userFilter = null;
    LinkedList<String> _errorsList = new LinkedList<String>();

    public TasklistProcessCounter(String remoteMachine, String user)
    {
        super("Remote process counter on " + remoteMachine);

        // TODO add a check that this is a valid address
        _remoteMachine = new String(remoteMachine);
        _user = new String(user);
    }

    public TasklistProcessCounter(String remoteMachine, String processFilter, String user)
    {
        super("Remote process counter on " + remoteMachine + " (" + processFilter + ")");

        // TODO add a check that this is a valid address
        if (remoteMachine != null)
            _remoteMachine = new String(remoteMachine);

        if (processFilter != null)
            _processFilter = new String(processFilter);
        _user = new String(user);
    }

    public TasklistProcessCounter(String remoteMachine, String processFilter, String userFilter, String user)
    {
        super("Remote process counter on " + remoteMachine + " (" + processFilter + ", " + userFilter + ")");

        // TODO add a check that this is a valid address
        if (remoteMachine != null)
            _remoteMachine = new String(remoteMachine);

        if (processFilter != null)
            _processFilter = new String(processFilter);


        _user = new String(user);

        if (userFilter != null)
            _userFilter = new String(userFilter);
    }

    @Override
    public void run()
    {

        _processes = 0;
        try
        {
            String pass = new String();

            try
            {
                pass = AES.decrypt(Key.getKey(), NocToolsSettings.getCredentials().get(_user));
            }
            catch (Exception e) 
            {
                System.out.println("An error occured while getting the credentials of user " + _user);
                org.apache.log4j.Logger.getRootLogger().error("An error occured while getting the credentials of user " + _user);
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }

            Process pr;

            String commandStr = new String();
            String outputStr = new String();

            commandStr += "cmd /c tasklist /S " + _remoteMachine + " /U " + _user + " /P " + pass;
            outputStr = "Executing \"" + "cmd /c tasklist /S " + _remoteMachine + " /U " + _user + " /P " + "[PASSWORD]";

            if (_processFilter != null)
            {
                commandStr += " /FI \"IMAGENAME eq " + _processFilter + "\"";
                outputStr += " /FI \"IMAGENAME eq " + _processFilter + "\"";
            }

            if (_userFilter != null)
            {
                commandStr += " /FI \"USERNAME eq " + _userFilter + "\"";
                outputStr += " /FI \"USERNAME eq " + _userFilter + "\"";
            }

            commandStr += " /FO table /NH";
            outputStr += " /FO table /NH\'";


            System.out.println(outputStr);
            pr = Runtime.getRuntime().exec(commandStr);
            org.apache.log4j.Logger.getRootLogger().debug(outputStr);

            InputStream is = pr.getInputStream();
            InputStream err = pr.getErrorStream();
            CmdReader cmdReader = new CmdReader(is);
            CmdReader errorReader = new CmdReader(err);
            cmdReader.start();
            errorReader.start();
            try
            {
                System.out.println("Waiting for the tasklist process to finish. Please wait...");
                pr.waitFor();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            LinkedList<String> linesList = cmdReader.getLinesList();
            for (String line : linesList)
            {
                // if there was some kind of tasklist error - print it
                if (line.toLowerCase().contains("error"))
                {
                    org.apache.log4j.Logger.getRootLogger().error("An error has occured while running the tasklist process:\n" + line);
                    System.out.println(line);
                }

                if (_processFilter != null)
                {
                    if (line.toLowerCase().indexOf(_processFilter.toLowerCase()) != -1)
                        _processes++;
                }
                else
                {
                    if (!line.equalsIgnoreCase("") && line.equalsIgnoreCase(NocToolsSettings.ENDL)
                            && !line.contains("No tasks are running which match the specified criteria"))
                        _processes++;
                }
            }

            _errorsList = errorReader.getLinesList();
            for (String line : _errorsList)
            {
                // if there was some kind of tasklist error - print it
                if (line.toLowerCase().contains("error"))
                {
                    org.apache.log4j.Logger.getRootLogger().error("An error has occured while running the tasklist process: \"" + line + "\"");
                }
            }

            is.close();
            err.close();
            _isDone = true;
        }
        catch (Exception e)
        {
            System.out.println("A general error occured while running the tasklist process");

            org.apache.log4j.Logger.getRootLogger().error("A general error occured while running the tasklist process");
            org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isDone() {return _isDone;}

    public LinkedList<String> getErrorsList() {return _errorsList;}

    public int getProcesses() {return _processes;}
}
