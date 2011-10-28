/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.log;
import java.io.File;
import noctools.endpoint.Server;
import java.net.InetAddress;
/**
 *
 * @author Yuri
 */
public abstract class LogJob extends Thread
{
    protected File _logFile = null;

    public LogJob()
    {
        super("Log job");
    }

    @Override
    public abstract void run();
    public File getLogFile() {return _logFile;}

}
