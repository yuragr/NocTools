/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package logstester;
import noctools.log.*;
import noctools.endpoint.Server;
import org.junit.*;
import javax.swing.JList;
/**
 *
 * @author Yuri
 */
public class JobTester
{
    @Test
    public void jobCreationTest()
    {
        String serverName = "qa2008.fring.com";
        String serverIP = "10.10.10.236";
        Server testServer = new Server(serverName, serverIP, 240);
        SimpleLogJob job = new SimpleLogJob(testServer, 18, new JList());
        job.start();
        try {
            job.join();
        }
        catch (InterruptedException e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }

    }
}
