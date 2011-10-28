/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.HashMap;
import noctools.util.PsexecProcessCounter;
import noctools.util.TasklistProcessCounter;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Yuri
 */
public class ProcessTester
{
//    @Test
//    public void processCount1()
//    {
//        RemoteProcessCounter counter = new RemoteProcessCounter("qa2008.fring.com", "remote1");
//    }

    @Test
    public void processCount1()
    {

        noctools.settings.NocToolsSettings.getCredentials().put("remote1", "3fd1804c93a7cbfd631363d105f42cd2");
//public PsexecProcessCounter(String remoteMachine, String userFilter, String user, String logFile)

        PsexecProcessCounter counter = new PsexecProcessCounter("qa2008.fring.com", "hms1",  "remote1", "c:\\noctools\\logs\\log.log");
        counter.start();
        try
        {
            counter.join();
        } 
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        assertTrue(counter.getProcesses() == 1);
    }
}
