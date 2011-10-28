/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.util;
import javax.crypto.*;
import javax.crypto.spec.*;
/**
 *
 * @author Yuri
 */
public class AES
{

    /*
     * encrypts the data and returns it as a string
     */
    public static String encrypt(String key, byte [] data) throws Exception
    {
        // Get the KeyGenerator
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128); // 192 and 256 bits may not be available

        //SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");

        // Instantiate the cipher
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(data);
        return asHex(encrypted);
    }

    /*
     * decrypts the hex string and returns it as a string
     */
    public static String decrypt(String key, String data) throws Exception
    {
        // Get the KeyGenerator
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128); // 192 and 256 bits may not be available

        //SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");

        // Instantiate the cipher
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(hexStringToByteArray(data));
        return new String(decrypted);
    }


    public static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
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
        return strbuf.toString();
    }
}
