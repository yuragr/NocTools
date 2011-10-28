/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.util;

/**
 *
 * @author Yuri
 */
public class Key
{
    public static String getKey()
    {
        return MACAddress.getMacAddress().toLowerCase() + MACAddress.getMacAddress().substring(1, 5);
    }
}
