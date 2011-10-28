/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package logstester;
import java.io.*;
import noctools.settings.*;
import noctools.log.*;
import noctools.endpoint.Server;
import org.junit.*;
import static org.junit.Assert.*;
/**
 *
 * @author Yuri
 */
public class DirectoryTester
{
    @Test
    public void dirTest1()
    {
        File localServerLogsDir = new File(NocToolsSettings.getLocalServerLogsDir());
        assertFalse(localServerLogsDir.exists());
    }

    @Test
    public void dirTest2()
    {
        File localServerLogsDir = new File(NocToolsSettings.getLocalServerLogsDir());
        if (!localServerLogsDir.exists())
            localServerLogsDir.mkdir();
        assertTrue(localServerLogsDir.exists());
        System.out.println(localServerLogsDir);
        System.out.println(localServerLogsDir.getAbsoluteFile());
        System.out.println(localServerLogsDir.getAbsoluteFile().toString());
    }

}
