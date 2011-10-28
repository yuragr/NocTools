/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.util;
import java.net.NetworkInterface;
import java.net.InetAddress;
/**
 *
 * @author Yuri
 */
public class MACAddress
{
    public static String getMacAddress()
    {
        String macStr = "";
        try
        {

            /*
             * Get NetworkInterface for the current host and then read the
             * hardware address.
             */
            InetAddress address = InetAddress.getLocalHost();

            NetworkInterface ni = NetworkInterface.getByInetAddress(address);
            if (ni != null)
            {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null)
                    macStr = asHex(mac);
            }
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }

        return macStr;
    }

    public static String asHex (byte buf[])
    {
        StringBuffer strbuf = new StringBuffer(buf.length * 2);
        int i;

        for (i = 0; i < buf.length; i++)
        {
        if (((int) buf[i] & 0xff) < 0x10)
            strbuf.append("0");

        strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
        }

        if (strbuf.toString().length() == 0)
            return "00a7437bee0f";
        else
            return strbuf.toString();
    }
}
