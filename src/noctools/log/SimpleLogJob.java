/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.log;
import noctools.endpoint.Server;
import noctools.settings.NocToolsSettings;
import java.io.FileWriter;
import java.io.File;
import javax.swing.JList;
import noctools.NocToolsView;
import noctools.util.ConnectionTester;

/**
 *
 * @author Yuri
 */
public class SimpleLogJob extends LogJob
{
    private Server _server;
    private int _logNumber;
    JList _extractionStatusList = null;

    public SimpleLogJob(Server server, int logNumber,  JList extractionStatusList)
    {
        _server = server;
        _logNumber = logNumber;
        _extractionStatusList = extractionStatusList;
    }

    @Override
    public void run()
    {
        String endl = System.getProperty ("line.separator");
        String serverIP;

        NocToolsView.updateStatusMessage(_extractionStatusList, "Testing connection to " + _server.getShortName() + "");
        //test connection to the server
        if (ConnectionTester.testConnectionByIp(_server.getIP()))
        {
            serverIP = _server.getIP();
            
            //create the main script file
            File jobFile = new File ("\\\\"+ serverIP + "\\c$\\job.vbs");

            try
            {
                // create the job file
                NocToolsView.updateStatusMessage(_extractionStatusList, "Creating the job file on " + _server.getShortName() + " (c:\\job.vbs)");
                FileWriter out = new FileWriter(jobFile);
                out.write("Dim fso" + endl);
                out.write("Set fso = CreateObject(\"Scripting.FileSystemObject\")" + endl);
                out.write("Dim oShell" + endl);
                out.write("Set oShell = WScript.CreateObject (\"WScript.shell\")" + endl);

                String originalLogFile = NocToolsSettings.getServerLogsDir() + "ms." + _logNumber + ".log";
                String tempLogFile = "c:\\" + _server.getShortName() + ".ms." + _logNumber + ".log";
                String compressedLogFile = "c:\\" + _server.getShortName() + ".ms." + _logNumber + ".log.zip";

                // create the temp log file
                out.write("oShell.run \"cmd /c echo "  + _server.getName() + " - Start of the log: > \"\"" + tempLogFile + "\"\"\", 0, true" + endl);

                // write the local time
                out.write("oShell.run \"cmd /c net time \\\\localhost >> \"\"" + tempLogFile + "\"\"\", 0, true" + endl);

                // dump the original file to the temp log file
                out.write("oShell.run \"cmd /c type \"\"" + originalLogFile + "\"\" >> \"\"" + tempLogFile + "\"\"\", 0, true" + endl);

                // check if compression is available
                out.write("if fso.FileExists(\"" + NocToolsSettings.getServer7zipLocation() + "\") = True then" + endl);

                // if the compression is available, compress the temp log file
                out.write("oShell.run \"" + NocToolsSettings.getServer7zipLocation() + " a  \"\"" + compressedLogFile + "\"\" \"\"" + tempLogFile + "\"\"\", 0, true" + endl);

                // and then delete the temp log file
                out.write("oShell.run \"cmd /c del \"\"" + tempLogFile + "\"\" /Q\", 0, true" + endl);

                out.write("end if" + endl);

                out.write("Set oShell = Nothing" + endl);
                out.close();

                jobFile = null;
                out = null;

                NocToolsView.updateStatusMessage(_extractionStatusList, "Executing the job file on the " + _server.getShortName() + " (c:\\job.vbs)");
                Runtime rt = Runtime.getRuntime();
                Process jobProcess = rt.exec("cmd /c psexec -accepteula \\\\" + _server.getIP() + " cscript c:\\job.vbs");

                File log = new File ("\\\\" + serverIP + "\\C$\\" + _server.getShortName() + ".ms." + _logNumber + ".log");
                File zipped = new File ("\\\\" + serverIP + "\\C$\\" + _server.getShortName() + ".ms." + _logNumber + ".log.zip");

                boolean jobLogExists = false;
                int loopCounter = 1;
                int loopTimeout = 6;

                // check if the job log file was created, and if it wes created, we need to check if the log file was zipped
                do
                {
                    //Sleeping 10 seconds
                    NocToolsView.updateStatusMessage(_extractionStatusList, "Waiting 10 seconds");
                    Thread.currentThread().sleep(10000);

                    if (log.exists() || zipped.exists())
                    {
                        //The job was finished
                        NocToolsView.updateStatusMessage(_extractionStatusList, "The job was finished (c:\\job.vbs)");
                        jobLogExists = true;
                    }
                    else
                    {
                        jobProcess.destroy();
                        Process killPsexec = rt.exec("cmd /c taskkill /F /IM psexec.exe");
                        killPsexec.waitFor();
                        killPsexec.destroy();

                        //Restarting the job
                        NocToolsView.updateStatusMessage(_extractionStatusList, "The job wasn't finished (c:\\job.vbs). Restarting - attempt " + loopCounter + " of " + loopTimeout);
                        jobProcess = rt.exec("cmd /c psexec -accepteula \\\\" + _server.getIP() + " cscript c:\\job.vbs");
                    }
                    loopCounter++;
                }
                while (!jobLogExists && loopCounter <= loopTimeout);

                jobProcess.destroy();
                Process killPsexec = rt.exec("cmd /c taskkill /F /IM psexec.exe");
                killPsexec.waitFor();
                killPsexec.destroy();

                // delete the job file
                NocToolsView.updateStatusMessage(_extractionStatusList, "Deleting the job file from " + _server.getShortName() + " (c:\\job.vbs)");
                Process pr1 = rt.exec("cmd /c del \\\\" + serverIP +  "\\c$\\job.vbs /Q");
                pr1.waitFor();
                pr1.destroy();

                // check if the log file exists
                if (jobLogExists)
                {
                    File localServerLogsDir = new File(NocToolsSettings.getLocalServerLogsDir());

                    // if the ServerLogs directory wasn't created - create it
                    if (!localServerLogsDir.exists())
                        localServerLogsDir.mkdir();

                    log = new File (NocToolsSettings.getLocalServerLogsDir() + _server.getShortName() + ".ms." + _logNumber + ".log");
                    zipped = new File (NocToolsSettings.getLocalServerLogsDir() + _server.getShortName() + ".ms." + _logNumber + ".log.zip");

                    // clean the local folder so that there will not be any naming problem
                    if (log.exists())
                        log.delete();
                    if (zipped.exists())
                        zipped.delete();

                    // copy the log to the local logs folder
                    NocToolsView.updateStatusMessage(_extractionStatusList, "Copying the log file from " + _server.getShortName() + " to " + NocToolsSettings.getLocalServerLogsDir());
                    pr1 = rt.exec("cmd /c copy \"\\\\" + serverIP +  "\\c$\\" + _server.getShortName() + ".ms." + _logNumber + ".*\" " + localServerLogsDir.getAbsolutePath());
                    pr1.waitFor();
                    pr1.destroy();

                    // delete the log file from the server
                    pr1 = rt.exec("cmd /c del \"\\\\" + serverIP +  "\\c$\\" + _server.getShortName() + ".ms." + _logNumber + ".*\" /Q");
                    pr1.waitFor();
                    pr1.destroy();

                    /*
                     * if we have local compression available, and the log is not compressed -
                     * compress it and delete the uncompressed log file
                     */
                    File  zipLocation = new File(NocToolsSettings.getLocal7zipLocation());
                    if (!zipped.exists() && zipLocation.exists() && log.exists())
                    {
                        // compress the log file
                        NocToolsView.updateStatusMessage(_extractionStatusList, "Archiving the log file");
                        pr1 = rt.exec("cmd /c \" " + NocToolsSettings.getLocal7zipLocation() + " a " + zipped.getAbsolutePath() + " " + log.getAbsolutePath() + "\"");
                        pr1.waitFor();
                        pr1.destroy();

                        // if the compression was successfull - delete the uncompressed log file
                        if (zipped.exists())
                        {
                            _logFile = zipped;
                            log.delete();
                        }
                        else
                            _logFile = log;
                    }
                    else if (zipped.exists())
                    {
                        if (log.exists())
                            log.delete();
                        _logFile = zipped;
                    }
                    else
                        _logFile = null;
                }
                else
                {
                    _logFile = null;
                }

            }
            catch (Exception e)
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
        }
        else
        {
            NocToolsView.updateStatusMessage(_extractionStatusList, "Connection could not be established!!!");
            return;
        }
    }

}
