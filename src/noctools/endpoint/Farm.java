/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.endpoint;

/**
 *
 * @author Yuri
 */
public class Farm extends Endpoint
{

    public Farm(String name)
    {
        setName(name);
    }

    public String toString()
    {
        return getName();
    }
}
