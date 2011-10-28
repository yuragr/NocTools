/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.link;

/**
 * This class will open web links in a different thread so it will not slow down the main application
 * @author Yuri
 */
public class LinkHandler extends Thread
{
    String _link;
    public LinkHandler(String link)
    {
        super("Link handler for " + link);
        _link = (link == null) ? link : new String(link);
    }

    @Override
    public void run()
    {
        if (_link != null)
        {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

            try {

                java.net.URI uri = new java.net.URI(_link);
                desktop.browse( uri );
            }
            catch ( Exception e )
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
