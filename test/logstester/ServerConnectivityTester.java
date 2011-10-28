/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package logstester;
import org.junit.*;
import static org.junit.Assert.*;
import noctools.log.SimpleLogJob;
import noctools.endpoint.Server;
import javax.swing.JList;
/**
 *
 * @author Yuri
 */


public class ServerConnectivityTester
{
    @Test
    public void testConnection1()
    {
        String serverName = "xxx";
        String serverIP = "192.168.0.1";
        Server testServer = new Server(serverName, serverIP, 0);
        SimpleLogJob job = new SimpleLogJob(testServer, 123, new JList());
        assertTrue(noctools.util.ConnectionTester.testConnectionByIp(testServer.getIP()));
    }

    @Test
    public void testConnection2()
    {
        String serverName = "xxx";
        String serverIP = "192.168.0.144";
        Server testServer = new Server(serverName, serverIP, 0);
        SimpleLogJob job = new SimpleLogJob(testServer, 123, new JList());
        assertFalse(noctools.util.ConnectionTester.testConnectionByIp(testServer.getIP()));
    }

    @Test
    public void testConnection3()
    {
        String serverName = "xxx";
        String serverIP = "10.10.10.2";
        Server testServer = new Server(serverName, serverIP, 0);
        SimpleLogJob job = new SimpleLogJob(testServer, 123, new JList());
        assertTrue(noctools.util.ConnectionTester.testConnectionByIp(testServer.getIP()));
    }

    @Test
    public void testConnectionByDns()
    {
        String serverName = "qa2008.fring.com";
        String serverIP = "10.10.10.236";
        Server testServer = new Server(serverName, serverIP, 0);
        assertTrue(noctools.util.ConnectionTester.testConnectionByDns(testServer.getName()));
    }

}
