package noctools.settings;

import java.util.*;
import java.io.*;
import org.dom4j.io.SAXReader;
import org.dom4j.*;
import org.dom4j.tree.*;
import noctools.endpoint.*;
import javax.swing.*;
import javax.swing.tree .*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.apache.log4j.*;

/**
 * One of the most important classes of the application. This class stores all the 
 * settings, and all the other classes address this class in order to get the settings.
 * Also, this class manages the settings.xml file and the reading the config.xml files
 * and building a tree of servers out of them.
 * @author Yuri
 */
public class NocToolsSettings
{
    /*
     *  for every new setting, add:
     * 1. create new varaible and give it a default setting
     * 2. load xml data to the varaible (settings.xml)
     * 3. store this varaible to xml (settings.xml)
     * 4. write set & get methods for this varaible
     */

    /*
     * the _configXmlData holds all the data that will be read from the config.xml files (the servers data)
     * it has the structure of worlds[countries[farms[servers]]]
     */
    private static HashMap<Endpoint, HashMap<Endpoint, HashMap<Endpoint, HashSet<Endpoint>>>> _configXmlData = null;
    private static HashMap<String, String> _credentials = new HashMap<String, String>();

    private static File _settingsFile = null;
    private static List<Element> _configXmlFiles = null;
    private static Document _linksSettingsXmlDocument = null;
    private static Document _settingsDocument = null;
    private static HashMap<String, String> _excludeMap = null;
    private static List<Element> _excludeList = null;
    private static List<String> _emailsList = null;

    public static final String ENDL = System.getProperty("line.separator");

    // general settings are inited to their default values
    
    // paths
    private static String _rdpDir = "c:\\noctools\\rdp\\";
    private static String _tempRdpDir = "c:\\noctools\\rdp\\temp\\";
    private static String _scriptsDir = "c:\\noctools\\scripts\\";
    private static String _iconsDir = "c:\\noctools\\icons\\";
    private static String _logsDir = "c:\\noctools\\logs\\";
    private static String _localServerLogsDir = "c:\\noctools\\ServerLogs\\";
    private static String _serverLogsDir = "c:\\Program Files (x86)\\Useful GPRS-3G-Bluetooth VOIP\\bins\\logs\\";
    private static String _server7zipLocation = "c:\\installs\\7zip\\7za.exe";
    private static String _local7zipLocation = "c:\\noctools\\7za.exe";
    
    // rdp settings
    private static boolean _useCredentialsManager = false;
    private static int _screenModeId = 2;
    private static int _desktopWidth = 1280;
    private static int _desktopHeight = 1024;
    private static boolean _useDns = true;
    private static int _rdpMaxNum = 20;
    private static boolean _rdpRestriction = true;
    
    // tree view settings
    private static boolean _showCountries = false;
    private static boolean _showFarms = true;
    private static boolean _showLinks = true;
    private static boolean _showServerPopupMenu = true;
    private static boolean _useShortServerNames = true;
    private static boolean _doubleClickOnServerOpensRdp = true;
    private static boolean _doubleClickOnEmptyCollapsesTree = true;
    private static boolean _rightClickOnEmptyClearsSelection = true;

    // db settings (for getting server logs)
    private static String _dbAddress = null;
    private static String _dbUser = null;
    private static String _dbName = null;
    private static String _webDB = "http://10.10.10.69:6060/";

    // server startup settings
    private static boolean _showServerStartupPanel = false;
    private static long _startupScriptDelay = 8000;
    private static String _startupScriptFile = "AutoLoadApps.exe";
    private static String _lastStartupScriptFile = "AutoLoadAndKill.exe";
    private static String _skypeCounterScriptFile = "pstasklist.exe";
    private static String _skypeCounterLogsDir = "c:\\noctools\\startup\\";
    private static long _waitBetweenRemotes = 50000; // 50 seconds
    private static long _maxWaitBetweenRemotes = 180000; // 3 minutes
    private static boolean _countSkypes = true;
    private static boolean _countSkypesAfterMaxWait = true;
    private static boolean _testConnection = false;
    private static int _maxFailuresPerServer = 3;
    private static int _maxConnectionFailuresPerServer = 3;
    private static int _maxPsexecFailuresPerServer = 0;

    // server startup contstants
    public static final long MAX_WAIT_BETWEEN_REMOTES = 600000; // 10 minutes
    public static final long MIN_WAIT_BETWEEN_REMOTES = 10000; // 10 seconds
    public static final int MAX_TASK_FAILURES_PER_SERVER = 10;
    public static final int MAX_CONNECTION_FAILURES_PER_SERVER = 10;
    public static final int MAX_PSEXEC_FAILURES_PER_SERVER = 10;
    public static final long MIN_STARTUP_SCRIPT_DELAY = 0;
    public static final long MAX_STARTUP_SCRIPT_DELAY = 60000; // 1 minute

    // other settings
    private static boolean _showTreeButtons = true;
    private static boolean _showWorkMode = true;
    private static String _scriptsXml = "c:\\noctools\\scripts.xml";
    private static String _linksSettingsXml = "c:\\noctools\\links.xml";
    private static String _documentationLink = "http://dataserver/index.php?title=NocTools";
    private static int _miniserversPerRemote = 80;
    private static boolean _showRemoteButtons = false;
    private static String _loggingLevel = "info";

    /**
     * Inits the settings from "settings.xml". If there is some error during the process,
     * than the default values will be used
     */
    public static void initSettings()
    {
        _excludeMap = new HashMap<String, String>();

        _credentials = new HashMap<String, String>();

        // default config.xml location. needed in case the settings file cannot be loaded
        _configXmlFiles = new LinkedList<Element>();

        Element fileElement = new DefaultElement("file");
        Element defaultConfigXml = new DefaultElement("fullPath");
        Element md5Element = new DefaultElement("MD5");
        defaultConfigXml.addText("C:\\noctools\\config.xml");
        fileElement.add(defaultConfigXml);
        fileElement.add(md5Element);
        _configXmlFiles.add(fileElement);

        // check if properties.xml is in the folder with the jar file
        _settingsFile = new File ("settings.xml");

        /*
         * if the settings.xml file doesn't exists in the same folder as the app file
         * or for some reason the app file thinks that it runs in different directory
         * (as sometimes happens on win7, then try to load the settings file from the
         * default location C:\noctools\NocToolsApp\
         */
        if (!_settingsFile.exists())
            _settingsFile = new File ("c:\\noctools\\NocToolsApp\\settings.xml");

        if (_settingsFile.exists())
        {
            try
            {
                //read the settings.xml file to _settingsDocument
                SAXReader reader = new SAXReader();
                _settingsDocument = reader.read(_settingsFile);
                Element settingsRoot = _settingsDocument.getRootElement();

                // load the paths
                try
                {
                    Element paths = settingsRoot.element("paths");

                    try {
                        _iconsDir = paths.element("iconsDir").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"iconsDir\" setting");}

                    try {
                        _rdpDir = paths.element("rdpDir").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"rdpDir\" setting");}

                    try {
                        _scriptsDir = paths.element("scriptsDir").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"scriptsDir\" setting");}

                    try {
                        _tempRdpDir = paths.element("tempRdpDir").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"tempRdpDir\" setting");}

                    try {
                        _logsDir = paths.element("logsDir").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"logsDir\" setting");}

                    try {
                        _serverLogsDir = paths.element("serverLogsDir").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"serverLogsDir\" setting");}

                    try {
                        _localServerLogsDir = paths.element("localServerLogsDir").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"localServerLogsDir\" setting");}

                    try {
                        _server7zipLocation = paths.element("server7zipLocation").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"server7zipLocation\" setting");}

                    try {
                        _local7zipLocation = paths.element("local7zipLocation").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"local7zipLocation\" setting");}

                }
                catch (Exception e) {Logger.getRootLogger().error("Could not load all path settings");}

                // load rdp settings
                try
                {
                    Element rdpSettings = settingsRoot.element("rdpSettings");

                    try {
                        _useCredentialsManager = rdpSettings.element("useCredentialsManager").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"useCredentialsManager\" setting");}

                    try {
                        _desktopHeight = Integer.parseInt(rdpSettings.element("desktopHeight").getData().toString());
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"desktopHeight\" setting");}

                    try {
                        _desktopWidth = Integer.parseInt(rdpSettings.element("desktopWidth").getData().toString());
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"desktopWidth\" setting");}

                    try {
                        _rdpRestriction = rdpSettings.element("rdpRestriction").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"rdpRestriction\" setting");}

                    try {
                        _rdpMaxNum = Integer.parseInt(rdpSettings.element("rdpMaxNum").getData().toString());
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"rdpMaxNum\" setting");}

                    try {
                        _screenModeId = Integer.parseInt(rdpSettings.element("screenModeId").getData().toString());
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"screenModeId\" setting");}

                    try {
                        _useDns = rdpSettings.element("useDns").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"useDns\" setting");}

                } catch (Exception e) {Logger.getRootLogger().error("Could not load all rdp settings");}

                // load treeViewSettings
                try
                {
                    Element treeViewSettings = settingsRoot.element("treeViewSettings");

                    try {
                        _showFarms = treeViewSettings.element("showFarms").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"showFarms\" setting");}

                    try {
                        _showCountries = treeViewSettings.element("showCountries").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"showCountries\" setting");}

                    try {
                        _showLinks = treeViewSettings.element("showLinks").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"showLinks\" setting");}

                    try {
                        _showServerPopupMenu = treeViewSettings.element("showServerPopupMenu").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"showServerPopupMenu\" setting");}

                    try {
                        _useShortServerNames = treeViewSettings.element("useShortServerNames").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"useShortServerNames\" setting");}

                    try {
                        _doubleClickOnServerOpensRdp = treeViewSettings.element("doubleClickOnServerOpensRdp").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"doubleClickOnServerOpensRdp\" setting");}

                    try {
                        _doubleClickOnEmptyCollapsesTree = treeViewSettings.element("doubleClickOnEmptyCollapsesTree").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"doubleClickOnEmptyCollapsesTree\" setting");}

                    try {
                        _rightClickOnEmptyClearsSelection = treeViewSettings.element("rightClickOnEmptyClearsSelection").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"rightClickOnEmptyClearsSelection\" setting");}

                } catch (Exception e) {Logger.getRootLogger().error("Could not load all tree view settings");}

                // load config xmls
                try
                {
                    Element configXmls = settingsRoot.element("configXmls");

                    try {
                        _configXmlFiles = configXmls.elements("file");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load all \"file\" settings");}

                    // if we have servers that must be excluded from the tree - load them to the excludeMap
                    _excludeList = configXmls.elements("exclude");
                    if (_excludeList != null)
                    {
                        if (_excludeList.size() > 0)
                        {
                            for (Element tempElement : _excludeList)
                            {
                                String serverToExclude = tempElement.getData().toString();
                                _excludeMap.put(serverToExclude, serverToExclude);
                            }
                        }
                    }
                } catch (Exception e) {Logger.getRootLogger().error("Could not load \"configXmls\" setting");}

                // load user ettings
                try
                {
                    Element users = settingsRoot.element("users");

                    List<Element> usersList = users.elements("user");

                    for (Element userElement : usersList)
                    {
                        try
                        {
                            String name = userElement.element("name").getText();
                            String pass = userElement.element("pass").getText();
                            if (!name.equalsIgnoreCase("") && !pass.equalsIgnoreCase(""))
                                _credentials.put(name, pass);
                        }
                        catch (Exception e)
                        {
                            if (e.getMessage()  != null)
                                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                catch (Exception e) {Logger.getRootLogger().error("Could not load all \"users\" setting");}


                // load emails (for sending logs)
                try
                {
                    _emailsList = new LinkedList<String>();
                    Element emails = settingsRoot.element("emails");
                    List <Element> emailsElementList = emails.elements("email");

                    for (Element emailElement : emailsElementList)
                        _emailsList.add(emailElement.getData().toString());

                }
                catch (Exception e) {Logger.getRootLogger().error("Could not load all \"emails\" setting");}


                // load dbSettings
                try
                {
                    Element dbSettings = settingsRoot.element("dbSettings");

                    try {
                        _dbAddress = dbSettings.element("dbAddress").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"dbAddress\" setting");}

                    try {
                        _dbUser = dbSettings.element("dbUser").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"dbUser\" setting");}

                    try {
                        _dbName = dbSettings.element("dbName").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"dbName\" setting");}

                    try {
                        _webDB = dbSettings.element("webDB").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"webDB\" setting");}
                }
                catch (Exception e) {Logger.getRootLogger().error("Could not load all \"dbSettings\" setting");}


                // load server startup settings
                try
                {
                    Element serverStartupSettings = settingsRoot.element("serverStartupSettings");

                    try {
                        _showServerStartupPanel = serverStartupSettings.element("showServerStartupPanel").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"showServerStartupPanel\" setting");}

                    try {
                        setStartupScriptDelay(Long.parseLong(serverStartupSettings.element("startupScriptDelay").getData().toString()));
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"dbAddress\" setting");}

                    try {
                        _startupScriptFile = serverStartupSettings.element("startupScriptFile").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"startupScriptFile\" setting");}

                    try {
                        _lastStartupScriptFile = serverStartupSettings.element("lastStartupScriptFile").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"lastStartupScriptFile\" setting");}

                    try {
                        _skypeCounterScriptFile = serverStartupSettings.element("skypeCounterScriptFile").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"skypeCounterScriptFile\" setting");}

                    try {
                        _skypeCounterLogsDir = serverStartupSettings.element("skypeCounterLogsDir").getData().toString();
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"skypeCounterLogsDir\" setting");}

                    try {
                        _waitBetweenRemotes = Long.parseLong(serverStartupSettings.element("waitBetweenRemotes").getData().toString());
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"waitBetweenRemotes\" setting");}

                    try {
                        _maxWaitBetweenRemotes = Long.parseLong(serverStartupSettings.element("maxWaitBetweenRemotes").getData().toString());
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"maxWaitBetweenRemotes\" setting");}

                    try {
                        _countSkypes = serverStartupSettings.element("countSkypes").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"countSkypes\" setting");}

                    try {
                        _countSkypesAfterMaxWait = serverStartupSettings.element("countSkypesAfterMaxWait").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"countSkypesAfterMaxWait\" setting");}

                    try {
                        _testConnection = serverStartupSettings.element("testConnection").getData().toString().equalsIgnoreCase("yes");
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"testConnection\" setting");}

                    try {
                        setMaxFailuresPerServer(Integer.parseInt(serverStartupSettings.element("maxFailuresPerServer").getData().toString()));
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"maxFailuresPerServer\" setting");}

                    try {
                        setMaxConnectionFailuresPerServer(Integer.parseInt(serverStartupSettings.element("maxConnectionFailuresPerServer").getData().toString()));
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"maxConnectionFailuresPerServer\" setting");}

                    try {
                        setMaxPsexecFailuresPerServer(Integer.parseInt(serverStartupSettings.element("maxPsexecFailuresPerServer").getData().toString()));
                    } catch (Exception e) {Logger.getRootLogger().error("Could not load \"maxPsexecFailuresPerServer\" setting");}

                }
                catch (Exception e) {Logger.getRootLogger().error("Could not load all \"serverStartupSettings\" setting");}

                
                // read other settings
                try {
                    _showTreeButtons = settingsRoot.element("showTreeButtons").getData().toString().equalsIgnoreCase("yes");
                } catch (Exception e) {Logger.getRootLogger().error("Could not load \"showTreeButtons\" setting");}

                try {
                    _showWorkMode = settingsRoot.element("showWorkMode").getData().toString().equalsIgnoreCase("yes");
                } catch (Exception e) {Logger.getRootLogger().error("Could not load \"showWorkMode\" setting");}

                try {
                    _documentationLink = settingsRoot.element("documentationLink").getData().toString();
                } catch (Exception e) {Logger.getRootLogger().error("Could not load \"documentationLink\" setting");}

                try {
                    _linksSettingsXml = settingsRoot.element("linksSettingsXml").getData().toString();
                } catch (Exception e) {Logger.getRootLogger().error("Could not load \"linksSettingsXml\" setting");}

                try {
                    _scriptsXml = settingsRoot.element("scriptsXml").getData().toString();
                } catch (Exception e) {Logger.getRootLogger().error("Could not load \"scriptsXml\" setting");}

                try {
                    _miniserversPerRemote = Integer.parseInt(settingsRoot.element("miniserversPerRemote").getData().toString());
                } catch (Exception e) {Logger.getRootLogger().error("Could not load \"miniserversPerRemote\" setting");}

                try {
                    _showRemoteButtons = settingsRoot.element("showRemoteButtons").getData().toString().equalsIgnoreCase("yes");
                } catch (Exception e) {Logger.getRootLogger().error("Could not load \"showRemoteButtons\" setting");}

                try {
                    _loggingLevel = settingsRoot.element("loggingLevel").getData().toString();
                } catch (Exception e) {Logger.getRootLogger().error("Could not load \"loggingLevel\" setting");}
            }
            catch (Exception e)
            {
                Logger.getRootLogger().error("Could not load all settings. Default will be used");
                JOptionPane.showMessageDialog(null, "Error while reading the settings file. Default settings will be loaded");
            }
        } // end of if (_settingsFile.exists())
        else
        {
            Logger.getRootLogger().error("Settings file wasn't found. Default will be used");
            JOptionPane.showMessageDialog(null, "No \"" + _settingsFile.getAbsoluteFile() + "\" file was provided. Default settings will be loaded");
        }

        try
        {
            readConfigXmlFiles();
            readLinksSettingsXmlFile();
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Saves the settings to the "settings.xml" file
     */
    public static void saveSettingsToFile()
    {
        try
        {
            if (_settingsFile.exists())
                _settingsFile.delete();

            Document settingsDocument = DocumentHelper.createDocument();
            Element rootElement = new DefaultElement("nocToolsApp-Settings");
            settingsDocument.setRootElement(rootElement);

            // save the paths settings
            Element paths = new DefaultElement("paths");

            Element iconsDir = new DefaultElement("iconsDir");
            iconsDir.addText(_iconsDir);
            paths.add(iconsDir);

            Element rdpDir = new DefaultElement("rdpDir");
            rdpDir.addText(_rdpDir);
            paths.add(rdpDir);

            Element scriptsDir = new DefaultElement("scriptsDir");
            scriptsDir.addText(_scriptsDir);
            paths.add(scriptsDir);

            Element tempRdpDir = new DefaultElement("tempRdpDir");
            tempRdpDir.addText(_tempRdpDir);
            paths.add(tempRdpDir);

            Element logsDir = new DefaultElement("logsDir");
            logsDir.addText(_logsDir);
            paths.add(logsDir);

            Element serverLogsDir = new DefaultElement("serverLogsDir");
            serverLogsDir.addText(_serverLogsDir);
            paths.add(serverLogsDir);

            Element localServerLogsDir = new DefaultElement("localServerLogsDir");
            localServerLogsDir.addText(_localServerLogsDir);
            paths.add(localServerLogsDir);

            Element server7zipLocation = new DefaultElement("server7zipLocation");
            server7zipLocation.addText(_server7zipLocation);
            paths.add(server7zipLocation);

            Element local7zipLocation = new DefaultElement("local7zipLocation");
            local7zipLocation.addText(_local7zipLocation);
            paths.add(local7zipLocation);

            rootElement.add(paths);

            // save the rdp settings
            Element rdpSettings = new DefaultElement("rdpSettings");

            Element useCredentialsManager = new DefaultElement("useCredentialsManager");
            useCredentialsManager.addText(_useCredentialsManager == true ? "yes" : "no");
            rdpSettings.add(useCredentialsManager);

            Element desktopHeight = new DefaultElement("desktopHeight");
            desktopHeight.addText((new Integer(_desktopHeight)).toString());
            rdpSettings.add(desktopHeight);

            Element desktopWidth = new DefaultElement("desktopWidth");
            desktopWidth.addText((new Integer(_desktopWidth)).toString());
            rdpSettings.add(desktopWidth);

            Element rdpRestriction = new DefaultElement("rdpRestriction");
            rdpRestriction.addText(_rdpRestriction == true ? "yes" : "no");
            rdpSettings.add(rdpRestriction);

            Element rdpMaxNum = new DefaultElement("rdpMaxNum");
            rdpMaxNum.addText((new Integer(_rdpMaxNum)).toString());
            rdpSettings.add(rdpMaxNum);

            Element screenModeId = new DefaultElement("screenModeId");
            screenModeId.addText((new Integer(_screenModeId)).toString());
            rdpSettings.add(screenModeId);

            Element useDns = new DefaultElement("useDns");
            useDns.addText(_useDns == true ? "yes" : "no");
            rdpSettings.add(useDns);

            rootElement.add(rdpSettings);

            // save the tree view settings
            Element treeViewSetting = new DefaultElement("treeViewSettings");

            Element showFarms = new DefaultElement("showFarms");
            showFarms.addText(_showFarms == true ? "yes" : "no");
            treeViewSetting.add(showFarms);

            Element showCountries = new DefaultElement("showCountries");
            showCountries.addText(_showCountries == true ? "yes" : "no");
            treeViewSetting.add(showCountries);

            Element showLinks = new DefaultElement("showLinks");
            showLinks.addText(_showLinks == true ? "yes" : "no");
            treeViewSetting.add(showLinks);

            Element showServerPopupMenu = new DefaultElement("showServerPopupMenu");
            showServerPopupMenu.addText(_showServerPopupMenu == true ? "yes" : "no");
            treeViewSetting.add(showServerPopupMenu);

            Element useShortServerNames = new DefaultElement("useShortServerNames");
            useShortServerNames.addText(_useShortServerNames == true ? "yes" : "no");
            treeViewSetting.add(useShortServerNames);

            Element doubleClickOnServerOpensRdp = new DefaultElement("doubleClickOnServerOpensRdp");
            doubleClickOnServerOpensRdp.addText(_doubleClickOnServerOpensRdp == true ? "yes" : "no");
            treeViewSetting.add(doubleClickOnServerOpensRdp);

            Element doubleClickOnEmptyCollapsesTree = new DefaultElement("doubleClickOnEmptyCollapsesTree");
            doubleClickOnEmptyCollapsesTree.addText(_doubleClickOnEmptyCollapsesTree == true ? "yes" : "no");
            treeViewSetting.add(doubleClickOnEmptyCollapsesTree);

            Element rightClickOnEmptyClearsSelection = new DefaultElement("rightClickOnEmptyClearsSelection");
            rightClickOnEmptyClearsSelection.addText(_rightClickOnEmptyClearsSelection == true ? "yes" : "no");
            treeViewSetting.add(rightClickOnEmptyClearsSelection);

            rootElement.add(treeViewSetting);

            // save the config xmls settings
            Element configXmls = new DefaultElement("configXmls");

            if (_configXmlFiles.size() < 1)
            {
                Element fullPath = new DefaultElement("fullPath");
                fullPath.addText("c:\\noctools\\config.xml");
                rootElement.add(fullPath);
            }
            else
            {
               for (Element tempElement : _configXmlFiles)
               {
                   Element file = new DefaultElement("file");
                   


                   Element fullPath = new DefaultElement("fullPath");
                   fullPath.addText(tempElement.element("fullPath").getText());
                   file.add(fullPath);



/* no use for this anymore

                   // calculate the MD5 for this config.xml
                    try
                   {
                       Element md5Elemnt = new DefaultElement("MD5");
                       String md5 = FileMD5Generator.getMD5Checksum(fullPath.getText());
                       md5Elemnt.addText(md5);
                       file.add(md5Elemnt);
                   }
                   catch (Exception e) {}

*/


                   configXmls.add(file);
               }
            }

           if (_excludeMap.size() > 0)
           {
               Set<String> excludeSet = _excludeMap.keySet();
               Iterator<String> excludeSetIterator = excludeSet.iterator();
               while (excludeSetIterator.hasNext())
               {
                   Element excludeElement = new DefaultElement("exclude");
                   excludeElement.addText(excludeSetIterator.next());
                   configXmls.add(excludeElement);
               }
           }

            rootElement.add(configXmls);
            Element users = new DefaultElement("users");
            if (_credentials.size() > 0)
            {
                Set<String> usersSet = _credentials.keySet();
                Iterator<String> usersSetIterator = usersSet.iterator();
                while (usersSetIterator.hasNext())
                {
                    String name = usersSetIterator.next();
                    Element userElement = new DefaultElement("user");
                    Element nameElement = new DefaultElement("name");
                    Element passElement = new DefaultElement("pass");

                    nameElement.addText(name);
                    passElement.addText(_credentials.get(name));
                    
                    userElement.add(nameElement);
                    userElement.add(passElement);

                    users.add(userElement);
                }
            }

            rootElement.add(users);
            
            
            Element emails = new DefaultElement("emails");
            for (String email : _emailsList)
            {
                Element emailElement = new DefaultElement("email");
                emailElement.addText(email);
                emails.add(emailElement);
            }
            rootElement.add(emails);

            // save the db settings (future use)
            Element dbSettings = new DefaultElement("dbSettings");

            Element dbAddress = new DefaultElement("dbAddress");
            dbAddress.addText(_dbAddress == null ? "" : _dbAddress);
            dbSettings.add(dbAddress);

            Element dbUser = new DefaultElement("dbUser");
            dbUser.addText(_dbUser == null ? "" : _dbUser);
            dbSettings.add(dbUser);

            Element dbName = new DefaultElement("dbName");
            dbName.addText(_dbName == null ? "" : _dbName);
            dbSettings.add(dbName);

            Element webDB = new DefaultElement("webDB");
            webDB.addText(_webDB == null ? "" : _webDB);
            dbSettings.add(webDB);

            rootElement.add(dbSettings);

            // save the server startup settings
            Element serverStartupSettings = new DefaultElement("serverStartupSettings");

            Element showServerStartupPanel = new DefaultElement("showServerStartupPanel");
            showServerStartupPanel.addText(_showServerStartupPanel == true ? "yes" : "no");
            serverStartupSettings.add(showServerStartupPanel);

            Element startupScriptDelay = new DefaultElement("startupScriptDelay");
            startupScriptDelay.addText(Long.toString(_startupScriptDelay));
            serverStartupSettings.add(startupScriptDelay);

            Element startupScriptFile = new DefaultElement("startupScriptFile");
            startupScriptFile.addText(_startupScriptFile == null ? "" : _startupScriptFile);
            serverStartupSettings.add(startupScriptFile);

            Element lastStartupScriptFile = new DefaultElement("lastStartupScriptFile");
            lastStartupScriptFile.addText(_lastStartupScriptFile == null ? "" : _lastStartupScriptFile);
            serverStartupSettings.add(lastStartupScriptFile);

            Element skypeCounterScriptFile = new DefaultElement("skypeCounterScriptFile");
            skypeCounterScriptFile.addText(_skypeCounterScriptFile == null ? "" : _skypeCounterScriptFile);
            serverStartupSettings.add(skypeCounterScriptFile);

            Element skypeCounterLogsDir = new DefaultElement("skypeCounterLogsDir");
            skypeCounterLogsDir.addText(_skypeCounterLogsDir == null ? "" : _skypeCounterLogsDir);
            serverStartupSettings.add(skypeCounterLogsDir);

            Element waitBetweenRemotes = new DefaultElement("waitBetweenRemotes");
            waitBetweenRemotes.addText(Long.toString(_waitBetweenRemotes));
            serverStartupSettings.add(waitBetweenRemotes);

            Element maxWaitBetweenRemotes = new DefaultElement("maxWaitBetweenRemotes");
            maxWaitBetweenRemotes.addText(Long.toString(_maxWaitBetweenRemotes));
            serverStartupSettings.add(maxWaitBetweenRemotes);

            Element countSkypes = new DefaultElement("countSkypes");
            countSkypes.addText(_countSkypes == true ? "yes" : "no");
            serverStartupSettings.add(countSkypes);

            Element countSkypesAfterMaxWait = new DefaultElement("countSkypesAfterMaxWait");
            countSkypesAfterMaxWait.addText(_countSkypesAfterMaxWait == true ? "yes" : "no");
            serverStartupSettings.add(countSkypesAfterMaxWait);

            Element testConnection = new DefaultElement("testConnection");
            testConnection.addText(_testConnection == true ? "yes" : "no");
            serverStartupSettings.add(testConnection);

            Element maxFailuresPerServer = new DefaultElement("maxFailuresPerServer");
            maxFailuresPerServer.addText(Integer.toString(_maxFailuresPerServer));
            serverStartupSettings.add(maxFailuresPerServer);

            Element maxConnectionFailuresPerServer = new DefaultElement("maxConnectionFailuresPerServer");
            maxConnectionFailuresPerServer.addText(Integer.toString(_maxConnectionFailuresPerServer));
            serverStartupSettings.add(maxConnectionFailuresPerServer);

            Element maxPsexecFailuresPerServer = new DefaultElement("maxPsexecFailuresPerServer");
            maxPsexecFailuresPerServer.addText(Integer.toString(_maxPsexecFailuresPerServer));
            serverStartupSettings.add(maxPsexecFailuresPerServer);

            rootElement.add(serverStartupSettings);


            // save other settings
            Element showTreeButtons = new DefaultElement("showTreeButtons");
            showTreeButtons.addText(_showTreeButtons == true ? "yes" : "no");
            rootElement.add(showTreeButtons);

            Element showWorkMode = new DefaultElement("showWorkMode");
            showWorkMode.addText(_showWorkMode == true ? "yes" : "no");
            rootElement.add(showWorkMode);

            Element documentationLink = new DefaultElement("documentationLink");
            documentationLink.addText(_documentationLink);
            rootElement.add(documentationLink);

            Element linksSettingsXml = new DefaultElement("linksSettingsXml");
            linksSettingsXml.addText(_linksSettingsXml);
            rootElement.add(linksSettingsXml);

            Element scriptsXml  = new DefaultElement("scriptsXml");
            scriptsXml.addText(_scriptsXml);
            rootElement.add(scriptsXml);

            Element miniserversPerRemote  = new DefaultElement("miniserversPerRemote");
            miniserversPerRemote.addText((new Integer(_miniserversPerRemote)).toString());
            rootElement.add(miniserversPerRemote);

            Element showRemoteButtons = new DefaultElement("showRemoteButtons");
            showRemoteButtons.addText(_showRemoteButtons == true ? "yes" : "no");
            rootElement.add(showRemoteButtons);

            Element loggingLevel = new DefaultElement("loggingLevel");
            loggingLevel.addText(_loggingLevel);
            Comment loggingComment = new DefaultComment("For logging level use: debug / info / error");
            loggingLevel.add(loggingComment);
            rootElement.add(loggingLevel);

            try
            {
                OutputFormat format = OutputFormat.createPrettyPrint();
                format.setLineSeparator(ENDL);
                format.setIndent("\t");
                XMLWriter writer = new XMLWriter(new FileWriter(_settingsFile), format);
                writer.write(settingsDocument);
                writer.close();
            }
            catch (Exception e)
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }

//            JOptionPane.showMessageDialog(null, "The settings were saved - " + _settingsFile.getAbsoluteFile());
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null, "The settings were not saved!");
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getRdpDir()
    {
        return (_rdpDir == null) ? null : new String(_rdpDir);
    }

    public static String getLogsDir()
    {
        return (_logsDir == null) ? null : new String(_logsDir);
    }

    public static String getServerLogsDir()
    {
        return (_serverLogsDir == null) ? null : new String(_serverLogsDir);
    }

    public static String getLocalServerLogsDir()
    {
        return (_localServerLogsDir == null) ? null : new String(_localServerLogsDir);
    }

    public static String getServer7zipLocation()
    {
        return (_server7zipLocation == null) ? null : new String(_server7zipLocation);
    }

    public static String getLocal7zipLocation()
    {
        return (_local7zipLocation == null) ? null : new String(_local7zipLocation);
    }

    public static String getTempRdpDir()
    {
        return (_tempRdpDir == null) ? null : new String(_tempRdpDir);
    }

    public static int getScreenModeId()
    {
        return _screenModeId;
    }

    public static int getDesktopWidth()
    {
        return _desktopWidth;
    }

    public static int getDesktopHeight()
    {
        return _desktopHeight;
    }

    public static boolean getUseDns()
    {
            return _useDns;
    }

    public static  int getRdpMaxNum()
    {
        return _rdpMaxNum;
    }
    
    public static boolean getUseCredentialsManager()
    {
        return _useCredentialsManager;
    }

    public static boolean getUseShortServerNames()
    {
        return _useShortServerNames;
    }

    public static boolean getShowTreeButtons()
    {
        return _showTreeButtons;
    }

    public static boolean getShowWorkMode()
    {
        return _showWorkMode;
    }

    public static List <String> getEmailsList()
    {
        return _emailsList;
    }

    public static String getDbAddress()
    {
        return (_dbAddress == null) ? null : new String(_dbAddress);
    }

    public static String getDbUser()
    {
        return (_dbUser == null) ? null : new String(_dbUser);
    }

    public static String getDbName()
    {
        return (_dbName == null) ? null : new String(_dbName);
    }

    public static String getWebDb()
    {
        return (_webDB == null) ? null : new String(_webDB);
    }

    public static String getScriptsDir()
    {
        return (_scriptsDir == null) ? null : new String(_scriptsDir);
    }

    public static String getScriptsXml()
    {
        return (_scriptsXml == null) ? null : new String(_scriptsXml);
    }

    public static String getIconsDir()
    {
        return (_iconsDir == null) ? null : new String(_iconsDir);
    }

    public static boolean getRdpRestriction()
    {
            return _rdpRestriction;
    }

    public static String getDocumentationLink()
    {
        return (_documentationLink == null) ? null : new String(_documentationLink);
    }

    public static boolean getShowCountries()
    {
        return _showCountries;
    }

    public  static boolean getShowFarms()
    {
         return _showFarms;
    }

    public  static boolean getShowLinks()
    {
        return _showLinks;
    }

    public static boolean getShowServerPopupMenu()
    {
        return _showServerPopupMenu;
    }

    public  static boolean getDoubleClickOnServerOpensRdp()
    {
        return _doubleClickOnServerOpensRdp;
    }

    public  static boolean getDoubleClickOnEmptyCollapsesTree()
    {
        return _doubleClickOnEmptyCollapsesTree;
    }

    public  static boolean getRightClickOnEmptyClearsSelection()
    {
        return _rightClickOnEmptyClearsSelection;
    }

    public static HashMap<String, String> getCredentials()
    {
        return _credentials;
    }

    public  static int getMiniserversPerRemote()
    {
        return _miniserversPerRemote;
    }

    public static Long getStartupScriptDelay()
    {
        return _startupScriptDelay;
    }

    public  static String getStartupScriptFile()
    {
        return (_startupScriptFile == null) ? null : new String(_startupScriptFile);
    }


    public  static String getSkypeCounterScriptFile()
    {
        return (_skypeCounterScriptFile == null) ? null : new String(_skypeCounterScriptFile);
    }

    public  static String getSkypeCounterLogsDir()
    {
        return (_skypeCounterLogsDir == null) ? null : new String(_skypeCounterLogsDir);
    }

    public  static long getWaitBetweenRemotes()
    {
        return _waitBetweenRemotes;
    }

    public  static long getMaxWaitBetweenRemotes()
    {
        return _maxWaitBetweenRemotes;
    }

    public  static boolean getCountSkypes()
    {
        return _countSkypes;
    }

    public  static boolean getCountSkypesAfterMaxWait()
    {
        return _countSkypesAfterMaxWait;
    }

    public  static boolean getTestConnection()
    {
        return _testConnection;
    }

    public static int getMaxFailuresPerServer()
    {
        return _maxFailuresPerServer;
    }

    public static int getMaxConnectionFailuresPerServer()
    {
        return _maxConnectionFailuresPerServer;
    }

    public static int getMaxPsexecFailuresPerServer()
    {
        return _maxPsexecFailuresPerServer;
    }

    public static String getLastStartupScriptFile()
    {
        return (_lastStartupScriptFile == null) ? null : new String(_lastStartupScriptFile);
    }

    public static boolean getShowRemoteButtons()
    {
        return _showRemoteButtons;
    }

    public static String getLoggingLevel()
    {
        return (_loggingLevel == null) ? null : new String(_loggingLevel);
    }

    public static boolean getShowServerStartupPanel()
    {
        return _showServerStartupPanel;
    }

    public  static void setShowServerStartupPanel(boolean showServerStartupPanel)
    {
        _showServerStartupPanel = showServerStartupPanel;
    }

    public void setLoggingLevel(String loggingLevel)
    {
        _loggingLevel = loggingLevel == null ? null : new String(loggingLevel);
    }

    public  static void setShowRemoteButtons(boolean showRemoteButtons)
    {
        _showRemoteButtons = showRemoteButtons;
    }

    public  static void setLastStartupScriptFile(String lastStartupScriptFile)
    {
        _lastStartupScriptFile = lastStartupScriptFile == null ? null : new String(lastStartupScriptFile);
    }

    public static void setMaxFailuresPerServer(int maxFailuresPerServer)
    {
        if (maxFailuresPerServer >= 0 && maxFailuresPerServer <= MAX_TASK_FAILURES_PER_SERVER)
            _maxFailuresPerServer = maxFailuresPerServer;
    }

    public static void setMaxConnectionFailuresPerServer(int maxConnectionFailuresPerServer)
    {
        if (maxConnectionFailuresPerServer >= 0 && maxConnectionFailuresPerServer <= MAX_CONNECTION_FAILURES_PER_SERVER)
            _maxConnectionFailuresPerServer = maxConnectionFailuresPerServer;
    }

    public static void setMaxPsexecFailuresPerServer(int maxPsexecFailuresPerServer)
    {
        if (maxPsexecFailuresPerServer >= 0 && maxPsexecFailuresPerServer <= MAX_PSEXEC_FAILURES_PER_SERVER)
            _maxPsexecFailuresPerServer = maxPsexecFailuresPerServer;
    }

    public  static void setTestConnection(boolean testConnection)
    {
        _testConnection = testConnection;
    }

    public  static void setCountSkypes(boolean countSkypes)
    {
        _countSkypes = countSkypes;
    }

    public  static void setCountSkypesAfterMaxWait(boolean countSkypesAfterMaxWait)
    {
        _countSkypesAfterMaxWait = countSkypesAfterMaxWait;
    }

    public  static void setMaxWaitBetweenRemotes(long maxWaitBetweenRemotes)
    {
        if (maxWaitBetweenRemotes >= MIN_WAIT_BETWEEN_REMOTES && maxWaitBetweenRemotes <= MAX_WAIT_BETWEEN_REMOTES)
            _maxWaitBetweenRemotes = maxWaitBetweenRemotes;
    }

    public  static void setWaitBetweenRemotes(long waitBetweenRemotes)
    {
        if (waitBetweenRemotes >= MIN_WAIT_BETWEEN_REMOTES && waitBetweenRemotes <= MAX_WAIT_BETWEEN_REMOTES)
            _waitBetweenRemotes = waitBetweenRemotes;
    }

    public  static void setSkypeCounterScriptFile(String skypeCounterScriptFile)
    {
        _skypeCounterScriptFile = skypeCounterScriptFile == null ? null : new String(skypeCounterScriptFile);
    }

    public  static void setSkypeCounterLogsDir(String skypeCounterLogsDir)
    {
        _skypeCounterLogsDir = skypeCounterLogsDir == null ? null : new String(skypeCounterLogsDir);
    }

    public  static void setStartupScriptFile(String startupScriptFile)
    {
        _startupScriptFile = startupScriptFile == null ? null : new String(startupScriptFile);
    }

    public static void setStartupScriptDelay(long startupScriptDelay)
    {
        if (startupScriptDelay >= MIN_STARTUP_SCRIPT_DELAY && startupScriptDelay <= MAX_STARTUP_SCRIPT_DELAY)
            _startupScriptDelay = startupScriptDelay;
    }

    public static void setShowCountries(boolean showCountries) 
    {
        _showCountries = showCountries;
    }

    public  static void setShowFarms(boolean showFarms)
    {
        _showFarms = showFarms;
    }

    public  static void setShowTreeButtons(boolean showTreeButtons)
    {
        _showTreeButtons = showTreeButtons;
    }

    public  static void setShowWorkMode(boolean showWorkMode)
    {
        _showWorkMode = showWorkMode;
    }

    public  static void setShowLinks(boolean showLinks)
    {
        _showLinks = showLinks;
    }

    public  static void setShowServerPopupMenu(boolean showServerPopupMenu)
    {
        _showServerPopupMenu = showServerPopupMenu;
    }

    public  static void setDoubleClickOnServerOpensRdp(boolean doubleClickOnServerOpensRdp)
    {
        _doubleClickOnServerOpensRdp = doubleClickOnServerOpensRdp;
    }

    public  static void setDoubleClickOnEmptyCollapsesTree(boolean doubleClickOnEmptyCollapsesTree)
    {
        _doubleClickOnEmptyCollapsesTree = doubleClickOnEmptyCollapsesTree;
    }

    public  static void setRightClickOnEmptyClearsSelection(boolean rightClickOnEmptyClearsSelection)
    {
        _rightClickOnEmptyClearsSelection = rightClickOnEmptyClearsSelection;
    }

    public static void setDbAddress(String dbAddress)
    {
        _dbAddress = dbAddress == null ? null : new String(dbAddress);
    }

    public static void setDbUser(String dbUser)
    {
        _dbUser = dbUser == null ? null : new String(dbUser);
    }

    public static void setDbName(String dbName)
    {
        _dbName = dbName == null ? null : new String(dbName);
    }
    
    public  static void setRdpRestriction(boolean rdpRestriction)
    {
        _rdpRestriction = rdpRestriction;
    }

    public static void setUseCredentialsManager(boolean useCredentialsManager)
    {
        _useCredentialsManager = useCredentialsManager;
    }

    public static String getLinksSettingsXml()
    {
        return (_linksSettingsXml == null) ? null : new String(_linksSettingsXml);
    }

    public static void setUseShortServerNames(boolean useShortServerNames)
    {
        _useShortServerNames = useShortServerNames;
    }

    public static HashMap<String, String> getExcludeMap()
    {
        return _excludeMap;
    }

    public static List<Element> getConfigXmlFiles()
    {
        return _configXmlFiles;
    }

    public static void setConfigXmlFiles(List<Element> newConfigXmlFiles)
    {
        _configXmlFiles = newConfigXmlFiles;
    }

    public static void readConfigXmlFiles() throws Exception
    {

        // read all the config.xml documents to the configXmlDocuments linked list
        LinkedList<Document> configXmlDocuments = new LinkedList<Document>();
        SAXReader reader = new SAXReader();
        
        for (Element configXmlFile :_configXmlFiles)
        {
            Element fullPath = configXmlFile.element("fullPath");
            try
            {
                // read each config.xml file to the configXmlDocuments linked list
                Document tempDocument = reader.read(fullPath.getData().toString());
                configXmlDocuments.add(tempDocument);
            }
            catch (Exception e)
            {
                JOptionPane.showMessageDialog(null, "Couldn't load the file " + fullPath.getData().toString(), "Error", JOptionPane.ERROR_MESSAGE);
                if (new File(fullPath.getData().toString()).exists())
                    org.apache.log4j.Logger.getRootLogger().error("Couldn't load the file " + fullPath.getData().toString() + ",  although it exists");
                else
                    org.apache.log4j.Logger.getRootLogger().error("Couldn't load the file " + fullPath.getData().toString() + ". It doen't exists");
            }
        }

        _configXmlData = new HashMap<Endpoint, HashMap<Endpoint, HashMap<Endpoint, HashSet<Endpoint>>>>();

        // for each document in the document list, we must merge them all to the _world
        for (Document tempDocument : configXmlDocuments)
        {
            Element configXmlRoot = tempDocument.getRootElement();
            Element hosts = configXmlRoot.element("hosts");

            mergeWithExistingData(hosts);
        }
    }

    /**
     * Merges the hosts data of the hostsRoot element with the already existing _configXmlData
     * @param hostsRoothosts information from the config xml doc
     */
    private static void mergeWithExistingData(Element hostsRoot)
    {
        List<Element> worlds = hostsRoot.elements("node-info");
        for (Element tempWorldElement :worlds)
        {
            if (tempWorldElement.attribute("level").getData().toString().equalsIgnoreCase("world"))
            {
                Endpoint newWorld = new World(tempWorldElement.attributeValue("id"));
                
                // if this world already exists
                if (_configXmlData.containsKey(newWorld))
                {
                    // get the countries of this world and for each country in the tempWorld, check if it already exists;
                    List<Element> countriesList = tempWorldElement.elements("node-info");
                    HashMap<Endpoint, HashMap<Endpoint, HashSet<Endpoint>>> countriesMap = _configXmlData.get(newWorld);

                    for (Element tempCountryElement : countriesList)
                    {
                        if (tempCountryElement.attributeValue("level").equalsIgnoreCase("country"))
                        {
                            Country newCountry = new Country(tempCountryElement.attributeValue("id"));
                            // if this country already exists
                            if (countriesMap.containsKey(newCountry))
                            {
                                // if this country already exists, then check for each farm - if it exists
                                HashMap<Endpoint, HashSet<Endpoint>> farmsMap = countriesMap.get(newCountry);
                                List<Element>farmsList = tempCountryElement.elements("node-info");

                                for (Element tempFarmElement : farmsList)
                                {
                                    if (tempFarmElement.attributeValue("level").equalsIgnoreCase("site"))
                                    {
                                        Farm newFarm = new Farm(tempFarmElement.attributeValue("id"));

                                        //if the farm already exists
                                        if (farmsMap.containsKey(newFarm))
                                        {
                                            HashSet<Endpoint> serversSet = farmsMap.get(newFarm);
                                            List<Element> serversList = tempFarmElement.elements("node-info");

                                            // for each server, check if it exists. if not, then add it
                                            for (Element tempServerElement : serversList)
                                            {
                                                if (tempServerElement.attributeValue("level").equalsIgnoreCase("host"))
                                                {
                                                    int miniservers = 0;
                                                    Element miniserversElement = tempServerElement.element("required-server-count");
                                                    if (miniserversElement != null)
                                                    {
                                                        try
                                                        {
                                                            miniservers = Integer.parseInt(miniserversElement.getData().toString());
                                                        }
                                                        catch (NumberFormatException e)
                                                        {
                                                            if (e.getMessage()  != null)
                                                                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                    String address = tempServerElement.element("address").getText();

                                                    Server newServer = new Server(tempServerElement.attributeValue("id"), address, miniservers);

                                                    // if the server doesn't exists - add it. otherwise - do nothing
                                                    if (!serversSet.contains(newServer))
                                                    {
                                                        addServer(serversSet, tempServerElement);
                                                    }
                                                }
                                            }
                                        }
                                        // if the farm doesn't exists, add it and all it servers
                                        else
                                        {
                                            addFarm(farmsMap, tempFarmElement);
                                        }
                                    }
                                }
                            }
                            // if this country doesn't exist - add it, and all it's farms and servers
                            else
                            {
                                addCountry(countriesMap, tempCountryElement);
                            }
                        }
                    }
                }
                // if this world doesn't exists, add it, and all its countries and their farsm and all their servers
                else
                {
                    addWorld(tempWorldElement);
                }
            }
        }
    }

    private static void addServer(HashSet<Endpoint> serversSet, Element serverElement)
    {
        int miniservers = 0;
        Element miniserversElement = serverElement.element("required-server-count");
        if (miniserversElement != null)
        {
            try
            {
                miniservers = Integer.parseInt(miniserversElement.getData().toString());
            }
            catch (NumberFormatException e)
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
        }
        String address = serverElement.element("address").getText();
        if (!_excludeMap.containsKey(serverElement.attributeValue("id").toString()))
        {
            Endpoint newServer = new Server(serverElement.attributeValue("id"), address, miniservers);
            serversSet.add(newServer);
        }
        else
            Logger.getRootLogger().debug("The server \"" + serverElement.attributeValue("id").toString() + "\" was excluded from the tree");
    }
    
    private static void addFarm(HashMap<Endpoint, HashSet<Endpoint>> farmsMap, Element farmElement)
    {
        Endpoint newFarm = new Farm(farmElement.attributeValue("id"));
        List<Element> serversList = farmElement.elements("node-info");

        HashSet<Endpoint> newServersSet = new HashSet<Endpoint>();
        farmsMap.put(newFarm, newServersSet);
        for (Element tempServerElement : serversList)
        {
            addServer(newServersSet, tempServerElement);
        }
    }

    private static void addCountry(HashMap<Endpoint, HashMap<Endpoint, HashSet<Endpoint>>> countriesMap, Element countryElement)
    {
        Endpoint newCountry = new Country(countryElement.attributeValue("id"));
        List<Element> farmsList = countryElement.elements("node-info");

        HashMap<Endpoint, HashSet<Endpoint>> newFarmsMap = new HashMap<Endpoint, HashSet<Endpoint>>();
        countriesMap.put(newCountry, newFarmsMap);
        for (Element tempFarmElement : farmsList)
        {
            addFarm(newFarmsMap, tempFarmElement);
        }
    }

    private static void addWorld(Element worldElement)
    {
        Endpoint newWorld = new World(worldElement.attributeValue("id"));
        List<Element> countriesList = worldElement.elements("node-info");

        HashMap<Endpoint, HashMap<Endpoint, HashSet<Endpoint>>> newCountriesMap = new HashMap<Endpoint, HashMap<Endpoint, HashSet<Endpoint>>>();
        _configXmlData.put(newWorld, newCountriesMap);
        for (Element tempCountryElement : countriesList)
        {
            addCountry(newCountriesMap, tempCountryElement);
        }
    }

    /**
     * reads the links.xml file to  _linksSettingsXmlDocument
     */
     public static void readLinksSettingsXmlFile()
     {
         SAXReader reader = new SAXReader();
         try
         {
            _linksSettingsXmlDocument = reader.read(getLinksSettingsXml());
         }
         catch (DocumentException e)
         {
             // If there was a problem reading the links document, create an empty links XML file
             _linksSettingsXmlDocument = createEmptyLinksXmlFile(getLinksSettingsXml());
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
         }
     }
    
    /**
     * Reads the config.xml file and constructs a tree of ComparableMutableTreeNodees
     * @param xmlFile location of config.xml
     * @return ComparableMutableTreeNode that contains all the tree
     * @throws Exception org.dom4j.DocumentException
     */
    public static ComparableMutableTreeNode getTree() throws Exception
    {

        boolean showFarms = getShowFarms();
        boolean showCountries = getShowCountries();

        Element linkRoot = _linksSettingsXmlDocument.getRootElement();
        Element fringRoot = linkRoot.element("world");

        // get the fring endpoint
        Iterator<Endpoint> worldIterator = _configXmlData.keySet().iterator();
        Endpoint fring = worldIterator.next();
        fring.clearLinks();

        // add frings links to fring object
        List<Element> fringLinks = fringRoot.elements("link");
        for (Element linkElement: fringLinks)
        {
            String linkDescription = linkElement.attribute("description").getData().toString();
            String link = linkElement.getData().toString();
            fring.addLink(linkDescription, link);
        }

        HashMap<String, Element> countriesLinksMap = new HashMap<String, Element>();
        List<Element> countriesLinksList = fringRoot.elements("country");
        for (Element tempCountry: countriesLinksList)
            countriesLinksMap.put(tempCountry.attribute("name").getData().toString(), tempCountry);

        // create the first ComparableMutableTreeNode instance, that will be the root of the tree
        ComparableMutableTreeNode fringTreeNode = new ComparableMutableTreeNode(fring);

        // get all fring's children (countries)
        HashMap<Endpoint, HashMap<Endpoint, HashSet<Endpoint>>> countriesMap = _configXmlData.get(fring);
        Iterator<Endpoint> countriesIterator = countriesMap.keySet().iterator();

        while (countriesIterator.hasNext())
        {

            Endpoint countryEndpoint = countriesIterator.next();
            countryEndpoint.clearLinks();

            HashMap<String, Element> farmLinksMap = new HashMap<String, Element>();
            // add this country's links to its endpoint, and create a hashMap of elements of the farms
            List<Element> countryLinksList = null;
            List<Element> farmsList = null;
            if (countriesLinksMap.containsKey(countryEndpoint.getName()))
            {
                Element countryLinks = countriesLinksMap.get(countryEndpoint.getName());

                countryLinksList = countryLinks.elements("link");
                farmsList = countryLinks.elements("farm");
                for (Element tempFarm : farmsList)
                    farmLinksMap.put(tempFarm.attribute("name").getData().toString(), tempFarm);
            }

            ComparableMutableTreeNode countryTreeNode = new ComparableMutableTreeNode(countryEndpoint);

            // for each country element, find the farms that this country contains
            HashMap<Endpoint, HashSet<Endpoint>> farmsMap = countriesMap.get(countryEndpoint);
            Iterator<Endpoint> farmsIterator = farmsMap.keySet().iterator();

            while (farmsIterator.hasNext())
            {
                Endpoint farmEndpoint = farmsIterator.next();
                farmEndpoint.clearLinks();

                List <Element> farmLinksList = null;
                // if there are links for this farm, create a list of them
                if (farmLinksMap.containsKey(farmEndpoint.getName()))
                {
                    Element farmLinks = farmLinksMap.get(farmEndpoint.getName());
                    farmLinksList = farmLinks.elements("link");
                }

                ComparableMutableTreeNode farmTreeNode = new ComparableMutableTreeNode(farmEndpoint);

                // for each farm element, find the server elements that it contains
                HashSet<Endpoint> serversSet = farmsMap.get(farmEndpoint);
                Iterator<Endpoint> serverIterator = serversSet.iterator();

                while (serverIterator.hasNext())
                {

                    Endpoint serverEndpoint = serverIterator.next();
                    serverEndpoint.clearLinks();

                    // if we are supposed to show the farms, then add this server to its farm
                    if (showFarms)
                        farmTreeNode.add(new ComparableMutableTreeNode(serverEndpoint));

                    // when showing only countries
                    else if (!showFarms && showCountries)
                        countryTreeNode.add(new ComparableMutableTreeNode(serverEndpoint));
                    else if (!showFarms && !showCountries)
                        fringTreeNode.add(new ComparableMutableTreeNode(serverEndpoint));

                    // if we are not suppose to show the farms, and do show the countries, the add this server to its country

                }
                if (showFarms)
                {
                    farmTreeNode.sortChildren();

                    // add links to this farm
                    if (farmLinksList != null)
                    {
                        for (Element linkElement : farmLinksList)
                        {
                            String linkDescription = linkElement.attribute("description").getData().toString();
                            String link = linkElement.getData().toString();
                            farmEndpoint.addLink(linkDescription, link);
                        }
                    }

                    if (showCountries)
                        countryTreeNode.add(farmTreeNode);
                    else
                        fringTreeNode.add(farmTreeNode);
                }
                
                else if (showCountries) // show only countries add the farm links to their country objects
                {
                    if (farmLinksList != null)
                    {
                        for (Element linkElement : farmLinksList)
                        {
                            String linkDescription = linkElement.attribute("description").getData().toString();
                            String link = linkElement.getData().toString();
                            countryEndpoint.addLink(linkDescription, link);                            
                        }
                    }                    
                }
                else // show nothing - add the links to the fring world object
                {
                    if (farmLinksList != null)
                    {
                        for (Element linkElement : farmLinksList)
                        {
                            String linkDescription = linkElement.attribute("description").getData().toString();
                            String link = linkElement.getData().toString();
                            fring.addLink(linkDescription, link);                            
                        }
                    }                    
                }
            }

            if (showCountries)
            {
                if (countryLinksList != null)
                {
                    for (Element linkElement : countryLinksList)
                    {
                        String linkDescription = linkElement.attribute("description").getData().toString();
                        String link = linkElement.getData().toString();
                        countryEndpoint.addLink(linkDescription, link);
                    }
                }

                
                countryTreeNode.sortChildren();
                fringTreeNode.add(countryTreeNode);
            }
            else
            {
                // add country links to the fring object
                if (countryLinksList != null)
                {
                    for (Element linkElement : countryLinksList)
                    {
                        String linkDescription = linkElement.attribute("description").getData().toString();
                        String link = linkElement.getData().toString();
                        fring.addLink(linkDescription, link);
                    }
                }
            }

        }
        fringTreeNode.sortChildren();
        return fringTreeNode;
    }

    /**
     * method that adds the link to the links document
     * @param node the node that has the endpoint with the updated links
     */
    public static void updateLinksDocument(TreeNode nodes[])
    {
        Element currentElement = _linksSettingsXmlDocument.getRootElement();
        for (int i = 0; i < nodes.length; i++)
        {
            Endpoint endpoint = (Endpoint)(((ComparableMutableTreeNode)nodes[i]).getUserObject());
            String type = endpoint.getClass().toString().toLowerCase();
            type = type.substring(type.lastIndexOf('.') + 1);

            Element childElement = null;

            // we have taken all the elements of endpoint's type. we have to find the exect one that has the same name as the endpoint
            List <Element> childElements = currentElement.elements(type);
            for (Element tempElement : childElements)
                if (tempElement.attribute("name").getData().toString().equalsIgnoreCase(endpoint.getName()))
                    childElement = tempElement;
            
            if (!type.equalsIgnoreCase(currentElement.getName()))
            {
                if (childElement == null)
                {
                    DefaultElement newElement = new DefaultElement(type);
                    DefaultAttribute newAttribute = new DefaultAttribute("name", endpoint.getName());
                    newElement.add(newAttribute);
                    currentElement.add(newElement);
                    currentElement = newElement;
                }
                else
                    currentElement = childElement;
            }
        }
        // we are at the last element in the document, and at the last node of the nodes array, which contains the endpoint with the updated links

        // we need to remove all his links and replace them with the endpoint's links
        List<Element> elementsToDetach = currentElement.elements("link");
        for (Element tempElement : elementsToDetach)
        {
            tempElement.detach();
        }

        Endpoint endpoint = (Endpoint)(((ComparableMutableTreeNode)nodes[nodes.length - 1]).getUserObject());
        HashMap<String, String> links = endpoint.getLinks();
        if (links != null)
        {
            Set<String> keys = links.keySet();
            for (String key : keys)
            {
                DefaultElement newElement = new DefaultElement("link");
                DefaultAttribute newAttribute = new DefaultAttribute("description", key);
                newElement.addText(links.get(key));
                newElement.add(newAttribute);
                currentElement.add(newElement);
            }
        }
        
       // write all links to the links file
       try
        {
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setLineSeparator(NocToolsSettings.ENDL);
            XMLWriter writer = new XMLWriter(new FileWriter(NocToolsSettings.getLinksSettingsXml()), format);
            writer.write(_linksSettingsXmlDocument);
            writer.close();
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates an empty links file, only with "fring" node
     * @param filePath path of the file
     * @return the new document with the "fring" node
     */
    private static Document createEmptyLinksXmlFile(String filePath)
    {
        if (filePath != null)
        {
            File linksXmlFile = new File(filePath);

            // if the file doesn't exists - create an empty file
            if (!linksXmlFile.exists())
            {
                Document newLinksDocument = new DefaultDocument();
                Element root = new DefaultElement("root");
                Element worldElement = new DefaultElement("world");
                worldElement.add(new DefaultAttribute("name", "fring"));
                root.add(worldElement);
                newLinksDocument.add(root);

                //write the document to xml file
               try
                {
                    OutputFormat format = OutputFormat.createPrettyPrint();
                    format.setLineSeparator(NocToolsSettings.ENDL);
                    XMLWriter writer = new XMLWriter(new FileWriter(filePath), format);
                    writer.write(newLinksDocument);
                    writer.close();

                    return newLinksDocument;
                }
                catch (Exception e)
                {
                    if (e.getMessage()  != null)
                        org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
