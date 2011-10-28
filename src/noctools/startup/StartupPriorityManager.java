/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.startup;

/**
 * This class will represent the startup priority. The priority will be calculated
 * using user defined settings. Maybe in the future the priority will be dynamic. Maybe
 * the priority will be set by server local time (day - high priority, night low priority... etc...)
 *
 * ************** low priority value = higher priority.   **************
 * ************** 1 is the highest priority                                **************
 *
 * @author Yuri
 */
public class StartupPriorityManager
{
    public static int getPriority()
    {

        // TODO find out how to solve a problem: if there is one server with high priority and all the others have low priority
        // so all the others will wait untill this server is done

        // TODO find out how to set priority by time zone

        return 1;
    }
}
