package noctools.endpoint;
import java.util.HashMap;
import java.net.URL;
import java.net.MalformedURLException;
/**
 * Absract class that will represent endpoints in the network (servers/farms/countries)
 * @author Yuri
 */
public abstract class Endpoint implements Comparable
{
    private String _name;
    private HashMap<String, String> _links;

    public Endpoint()
    {
        _links = null;
    }

    /**
     * Sets the name of this endpoint
     * @param name name of the new endpoing
     */
    protected void setName(String name)
    {
            _name = (name != null) ? new String(name) : null;
    }

    public int compareTo(Object other)
    {
        if (other instanceof Endpoint)
            return _name.compareTo(((Endpoint)other)._name);
        else
            throw new ClassCastException();
    }

    public boolean equals(Object other)
    {
        if (compareTo(other) == 0)
            return true;
        else
            return false;
    }

    /**
     * Returns the name of the endpoint
     * @return name of the endpoint
     */
    public String getName()
    {
        return (_name != null) ? new String(_name) :null;
    }

    /**
     * each concrete class will have to implement this
     * @return
     */
    @Override
    public abstract String toString();


    /**
     * Adds a web link to this endpoint that will be displayed in the popup menu
     * @param description description of this web link
     * @param linkToAdd the link itself
     */
    public void addLink(String description, String linkToAdd) 
    {
        if (checkLink(linkToAdd)  && description != null)
        {

            // the link is ok, so we will add it to the links hashmap. If it wasn't created, than we will create one
            if (_links == null)
                _links = new HashMap<String, String>();

            _links.put(description, linkToAdd);

        }// end of if (linkToAdd != null)
    }

    /**
     * removes a link from the links list by it's description
     * @param description
     */
    public void removeLink(String description)
    {
        if (_links != null)
        {
            _links.remove(description);
        }
    }

    public void clearLinks()
    {
        if (_links != null)
            _links.clear();
    }

    /**
     * returns the entire HashMap of the links
     * @return the HashMap of the links. If there are no links - null is returned
     */
    public HashMap<String, String> getLinks()
    {
        if (_links != null)
        {
            if (_links.isEmpty())
                return null;
            else
                return _links;
        }
        return null;
    }

    /**
     * Sets the links HashMap of this endpoint. If the endpoint has no entries, then it sets the internal links HashMap to be null
     * @param links
     */
    public void setLinks(HashMap<String, String> links)
    {
        _links = (links.size() == 0) ? null : links;
    }

    /**
     * Checks if the web link is a legal link
     * @param linkToAdd the web link string
     * @return true - if the web link was ok. False - if the web link wasn't ok
     */
    public static boolean checkLink(String linkToAdd)
    {
        if (linkToAdd == null)
            return false;

        if (linkToAdd.compareToIgnoreCase("") == 0)
            return false;

        // try to build a URL instance with the link address.
        // if there is and exception, than this is not a legal address
        try
        {
            URL test = new URL(linkToAdd);
        }
        catch (MalformedURLException e)
        {
            if (e.getMessage() != null)
                if (!e.getMessage().contains("no protocol"))
                {
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                    e.printStackTrace();
                    return false;
                }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return this.getName().hashCode();
    }
}