package noctools.endpoint;
import java.net.URL;
import java.net.MalformedURLException;
/**
 *
 * @author Yuri
 */
public class Server extends Endpoint
{
    String _ip;
    String _shortName;
    int _miniservers;
    int _currentRdp;

    public Server()
    {
        setName(new String());
        _ip = new String();
        _shortName = new String();
        _miniservers = 0;
        _currentRdp = 0;
    }

    public Server(String serverName, String serverIP, int miniservers)
    {
        setName(serverName);
        _ip = new String(serverIP);
        _shortName = new String(getName());
        _miniservers = miniservers;
        if (_shortName.indexOf("apps") > 0)
        {
            _shortName = _shortName.replaceAll("useful-apps", "");
            _shortName = _shortName.replaceAll("fring", "");
            _shortName = _shortName.replaceAll("apps", "");
            _shortName = _shortName.replaceAll("..com", "");
            _shortName = _shortName.toUpperCase();
        }
    }

    public Server(Server other)
    {
        setName(new String(other.getName()));
        _ip = new String(other._ip);
        _shortName = new String(other._shortName);
        _miniservers = other._miniservers;
        _currentRdp = other._currentRdp;
    }

    public void setIP(String ip)
    {
        _ip = (ip == null) ? new String() : new String(ip);
    }

    public void setServerName(String serverName)
    {
        setName(serverName);
    }

    public String getIP() {return new String(_ip);}

    public int getMiniservers() {return _miniservers;}

    public void setMiniservers(int miniservers)
    {
        if (miniservers >= 0)
            _miniservers = miniservers;
    }

    public String getShortName()
    {
        if (_shortName != null)
            return new String (removeSpecialChars(_shortName));
        else return new String("");
    }

    @Override
    public String toString()
    {
        String returnString = new String();

        if (noctools.settings.NocToolsSettings.getUseShortServerNames())
            returnString+= _shortName;
        else
            returnString += getName();

        if (_ip != null)
            if (!_ip.equalsIgnoreCase(""))
                returnString +=  " : " + _ip;

        if (_miniservers > 0)
        {
            returnString += " (" + _miniservers + ")";
            if ((_currentRdp)  * noctools.settings.NocToolsSettings.getMiniserversPerRemote() < _miniservers)
                returnString += " <" + (_currentRdp + 1) + ">";
            else
                returnString += " <X>";
        }

        return returnString;
    }

    public String removeSpecialChars(String name)
    {
        String string = null;
        if (name != null)
        {
            string = new String(name);
            string = string.replace("/", "");
            string = string.replace("\\", "");
            string = string.replace("|", "");
            string = string.replace("\"", "");
            string = string.replace("*", "");
            string = string.replace(":", "");
            string = string.replace(">", "");
            string = string.replace("<", "");
        }
        return string;
    }

    public boolean checkCorrectDns()
    {
        // try to build URL instance with servers address.
        // if there is and exception, than this is not a legal address
        try
        {
            URL test = new URL("http://" + getName());
        }
        catch (MalformedURLException e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof Server)
        {
            Server otherServer = (Server)other;
            if (_ip.equalsIgnoreCase(otherServer._ip) && this.getName().equalsIgnoreCase(otherServer.getName()))
                return true;
            else
                return false;
        }
        else
            return false;
    }

    @Override
    public int hashCode()
    {
        return (this.getIP() + this.getName()).hashCode();
    }

    public String getDnsName()
    {
        String dnsName = this.getName();
        int dot = dnsName.indexOf('.');
        if (dot >= 0)
            return dnsName.substring(0, dot);
        else
            return this.getName();
    }

    public int getCurrentRdp() {return _currentRdp;}

    public void setCurrentRdp(int currentRdp) {_currentRdp = currentRdp;}

    public boolean isSkypeless() {return false;}

}
