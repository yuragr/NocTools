/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

/**
 *
 * @author Yuri
 */
public class CmdReader extends Thread
{
    private InputStream _is;
    private LinkedList<String> _linesList;

    public CmdReader(InputStream is)
    {
        super("CMD reader");
        _is = is;
        _linesList = new LinkedList<String>();
    }

    @Override
    public void run()
    {
        try
        {
            final BufferedReader br = new BufferedReader( new InputStreamReader( _is ), 80);
            String line;
            while ((line = br.readLine()) != null)
            {
                System.out.println(line);
                _linesList.add(line);
            }
        }
        catch (Exception e)
        {
            // TODO think what to do here
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();

        }

    }

    public LinkedList<String> getLinesList()
    {
        return _linesList;
    }

}
