/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.util;

import java.net.InetAddress;

/**
 *
 * @author Yuri
 */
public class ConnectionTester
{
    private ConnectionTester() {};
    public static boolean testConnectionByIp(String ip)
    {
        try
        {
            int timeout = 3000;
            InetAddress address = InetAddress.getByName(ip);

            if (address.isReachable(timeout))
            {
                return true;
            }
            else
            {
                System.out.println("Connection test error: Could not establish connection to " + ip);
                return false;
            }
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static boolean testConnectionByDns(String dns)
    {
        try
        {
            int timeout = 3000;
            InetAddress address = InetAddress.getByName(dns);

            if (address.isReachable(timeout))
            {
                return true;
            }
            else
            {
                System.out.println("Connection test error: Could not establish connection to " + dns);
                return false;
            }
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

}
