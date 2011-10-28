/*
 * NocToolsApp.java
 * 
 * Feature Requests
 * ================
 * Add a feature to synchronize all the other NocTools clients.
 * Add a feature to extract logs by fring userID.
 * Add tree view to Custom Scripts field. Allow sorting the scripts to different nodes (categories).
 */

package noctools;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class NocToolsApp extends SingleFrameApplication
{

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup()
    {
        show(new NocToolsView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root)
    {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of NocToolsApp
     */
    public static NocToolsApp getApplication()
    {
        return Application.getInstance(NocToolsApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args)
    {
        launch(NocToolsApp.class, args);
    }
}