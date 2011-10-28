/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.util;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Scanner;
import noctools.settings.NocToolsSettings;

/**
 *
 * @author Yuri
 */
public class PsexecProcessCounter extends Thread
{
    private File _logFile;
    private String _remoteMachine;
    private String _userFilter;
    private String _user;
    private int _processes;
    boolean _isDone = false;
    private boolean _psexecSucceded = false;
    public PsexecProcessCounter(String remoteMachine, String userFilter, String user, String logFile)
    {
        _logFile = new File(logFile);
        _remoteMachine = new String(remoteMachine);
        _userFilter = new String(userFilter);
        _user = new String(user);
        _processes = 0;
    }

    @Override
    public void run()
    {
        _psexecSucceded = false;
        _processes = 0;
        _isDone = false;

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
                org.apache.log4j.Logger.getRootLogger().error("An error occured while getting the credentials of user " + _user + " : " + e.getMessage());
                e.printStackTrace();
            }

            Process pr;

            String commandStr = new String();
            String outputStr = new String();

            commandStr += NocToolsSettings.getScriptsDir() + NocToolsSettings.getSkypeCounterScriptFile() + " /DNSName=" + _remoteMachine + " /UserHMS=" + _userFilter + " /logpath=" + _logFile.getAbsolutePath() + " /RemUser=" + _user + " /RemPass=" + pass;
            outputStr += NocToolsSettings.getScriptsDir() + NocToolsSettings.getSkypeCounterScriptFile() + " /DNSName=" + _remoteMachine + " /UserHMS=" + _userFilter + " /logpath=" + _logFile.getAbsolutePath() + " /RemUser=" + _user + " /RemPass=" + pass == null ? "[NULL PASSWORD!!!]" : "[PASSWORD]" ;

            org.apache.log4j.Logger.getRootLogger().debug("Executing : " + outputStr);
            System.out.println("Executing : " + outputStr);
            Process script = Runtime.getRuntime().exec(commandStr);
            Thread.sleep(1500);

            // we have to look at the local tasks and see if the psexec has finished running
            boolean localTasklistCompleted = false;
//            String tasksCommand = "cmd /c wmic PROCESS LIST STATUS";
            String tasksCommand = "tasklist /FI \"IMAGENAME eq psexec.exe\" /FO TABLE /NH";

            while (!localTasklistCompleted)
            {
                System.out.println("Checking if the tasklist has finished");
                System.out.println("Executing : " + tasksCommand);
                Process tasksProcess = Runtime.getRuntime().exec(tasksCommand);
                InputStream is = tasksProcess.getInputStream();
                InputStream err = tasksProcess.getErrorStream();
                CmdReader cmdReader = new CmdReader(is);
                CmdReader errorReader = new CmdReader(err);
                try
                {
                    cmdReader.start();
                    errorReader.start();
                    tasksProcess.waitFor();

                    LinkedList<String> tasksList = cmdReader.getLinesList();
                    localTasklistCompleted = true;
                    for (String line : tasksList)
                    {
                        if (localTasklistCompleted &&line.toLowerCase().contains("psexec"))
                            localTasklistCompleted = false;
                    }
                    if (!localTasklistCompleted)
                    {
                        Thread.sleep(2000);
                        System.out.println("Sleeping 2000 ms");
                    }
                }
                finally
                {
                    is.close();
                    err.close();
                }
            }

            // the psexec script was completed and we should have the log file with the result

            if (_logFile.exists())
            {
                org.apache.log4j.Logger.getRootLogger().debug("pstasklist log was created for " + _remoteMachine + ". Its size is " +_logFile.length() + " bytes");
                System.out.println("We have a log file!");
                Scanner logReader = new Scanner(_logFile);
                while (logReader.hasNextLine())
                {
                    String line = logReader.nextLine();
                    if (line != null)
                    {
                        System.out.println(line);
                        org.apache.log4j.Logger.getRootLogger().debug(_logFile + ": \"" + line + "\"");
                    }

                    if (line.toLowerCase().contains("skype"))
                    {
                        org.apache.log4j.Logger.getRootLogger().debug("Skype was found in the log of " + _remoteMachine);
                        System.out.println("Skype was found in the log of " + _remoteMachine);
                        _processes++;
                        _psexecSucceded = true;
                    }
                    else if (line.toLowerCase().contains("no tasks"))
                    {
                        //org.apache.log4j.Logger.getRootLogger().debug("No skypes were found in the log of  " + _remoteMachine);
                        System.out.println("No skypes were found in the log of  " + _remoteMachine);
                        _psexecSucceded = true;
                    }
                }
                logReader.close();
                _logFile.delete();

                if (!_psexecSucceded)
                {
                    org.apache.log4j.Logger.getRootLogger().error("pstasklist had failed for " + _remoteMachine);
                    System.out.println("pstasklist had failed for " + _remoteMachine);
                }
            }
            else
            {
                System.out.println("pstasklist log wasn't created for " + _remoteMachine);
                org.apache.log4j.Logger.getRootLogger().error("pstasklist log wasn't created for " + _remoteMachine);
            }

        }
        catch (Exception e)
        {
            System.out.println("An error occured while counting the processes via psexec");
            org.apache.log4j.Logger.getRootLogger().error("An error occured while counting the processes via psexec on " + _remoteMachine);
            org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }

        _isDone = true;
    }

    public boolean isDone() {return _isDone;}
    
    public boolean isPsexecSuceeded() {return _psexecSucceded;}

    public int getProcesses() {return _processes;}
}
