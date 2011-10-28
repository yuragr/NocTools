/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.log;
import noctools.*;
import java.io.File;
import javax.swing.*;
import java.awt.Desktop;
import noctools.settings.NocToolsSettings;
import java.net.URI;

/**
 * Singleton class that will controll the log extracting jobs
 * @author Yuri
 */
public class LogsManager extends Thread
{
    private static LogsManager _logsManager = null;
    private static String _email = null;
    private static String _subject = null;
    private static boolean _available = true;
    private static LogJob _job = null;
    private static JList _extractionStatusList = null;

    private LogsManager(){}

    public static synchronized LogsManager getInstance(LogJob job, String email, JList extractionStatusList, String subject)
    {
        if (_logsManager  == null || _available)
        {
            _extractionStatusList = extractionStatusList;
            _logsManager = new LogsManager();
            _available = false;
            _job = job;
            _email = email;
            _subject = subject;
            return _logsManager;
        }
        else
            return null;
    }

    @Override
    public void run()
    {
        _available = false;

        NocToolsView.updateStatusMessage(_extractionStatusList, "Begining the extraction");
        _job.start();

        try
        {
            _job.join();
            File logFile = _job.getLogFile();
            if (logFile != null)
            {
                if (_email != null)
                {
                    if (Desktop.isDesktopSupported())
                    {
                        if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                        {
                            NocToolsView.updateStatusMessage(_extractionStatusList, "Opening " + NocToolsSettings.getLocalServerLogsDir());
                            JOptionPane.showMessageDialog(null, "Please drag the log file to the email window");
                            Desktop.getDesktop().open(new File(NocToolsSettings.getLocalServerLogsDir()));
                            
                            if (Desktop.getDesktop().isSupported(Desktop.Action.MAIL))
                            {
                                NocToolsView.updateStatusMessage(_extractionStatusList, "Opening email client. Please attach the log file");
                                URI uriMailTo = new URI("mailto", _email + "?SUBJECT=" + _subject + "&BODY=\n\nServer's local time is inside ths log.\n\nSent By NocTools", null);
                                Desktop.getDesktop().mail(uriMailTo);

                            }
                            else
                                NocToolsView.updateStatusMessage(_extractionStatusList, "Email is not supported. The log file wasn't sent!!!");
                            
                        }
                    }


                }
                else
                {
                    // there was no email request, so just open the logs folder
                    if (Desktop.isDesktopSupported())
                    {
                        if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                        {
                            NocToolsView.updateStatusMessage(_extractionStatusList, "Opening " + NocToolsSettings.getLocalServerLogsDir());
                            Desktop.getDesktop().open(new File(NocToolsSettings.getLocalServerLogsDir()));
                        }
                    }
                }
            }
            else
                NocToolsView.updateStatusMessage(_extractionStatusList, "Extraction failed");
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }


        _available = true;

    }
}
