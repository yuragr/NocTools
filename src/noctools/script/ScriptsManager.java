/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.script;

import noctools.settings.NocToolsSettings;
import java.io.*;
import org.dom4j.io.XMLWriter;
import java.util.*;
import java.util.Map.Entry;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.*;
import noctools.endpoint.*;
import javax.swing.JOptionPane;
import java.io.File;
import noctools.NocToolsWorker;
/**
 *
 * @author Yuri
 */
public class ScriptsManager
{
    private static Document _scriptsDocument;
    private static TreeMap<String, Script> _selectedScripts= new TreeMap<String, Script>();
    private static TreeMap<String, Script> _scripts= new TreeMap<String, Script>();
    private static TreeMap<String, ScriptThreadGroupQueue> _activeScriptQueues = new TreeMap<String, ScriptThreadGroupQueue>();

    public static void loadScriptsFromXml()
    {
        _scriptsDocument = readScriptsXml();
        Element root = _scriptsDocument.getRootElement();
        List<Element> scriptElementsList = root.elements("script");

        for (Element e1 : scriptElementsList)
        {
            try
            {
                String filename = e1.element("filename").getData().toString();
                String type = e1.element("type").getData().toString();
                String description = e1.element("description").getData().toString();

                String showWarningStr = e1.element("runApproval").getData().toString();
                String paralelRunStr = e1.element("paralelRun").getData().toString();
                String quickScriptStr = e1.element("quickScript").getData().toString();
                
                // if there is no element "scriptBefore", the string "scriptBefore" will be null otherwise, it will have it's data
                String scriptBefore = (e1.element("scriptBefore") == null) ? (null) : e1.element("scriptBefore").getData().toString();

                // if there is no element "scriptAfter", the string "scriptBefore" will be null otherwise, it will have it's data
                String scriptAfter = (e1.element("scriptAfter") == null) ? (null) : e1.element("scriptAfter").getData().toString();

                boolean workOnWholeFarmsCountries;
                if (e1.element("workOnWholeFarmsCountries") == null)
                    workOnWholeFarmsCountries = false;
                else
                    workOnWholeFarmsCountries = e1.element("workOnWholeFarmsCountries").getData().toString().equalsIgnoreCase("yes");

                Element parametersElement = e1.element("parameters");
                boolean showWarning = showWarningStr.equalsIgnoreCase("yes");
                boolean paralelRun = paralelRunStr.equalsIgnoreCase("yes");
                boolean quickScript = quickScriptStr.equalsIgnoreCase("yes");

                boolean appearInPopupMenu;
                if (e1.element("appearInPopupMenu") == null)
                    appearInPopupMenu = false;
                else
                    appearInPopupMenu = e1.element("appearInPopupMenu").getData().toString().equalsIgnoreCase("yes");

                String endMessage;
                if (e1.element("messageInTheEnd") != null)
                    if (!e1.element("messageInTheEnd").getData().toString().equalsIgnoreCase(""))
                        endMessage = e1.element("messageInTheEnd").getData().toString();
                    else endMessage = null;
                else endMessage = null;

                Script tempScript = new Script(filename, type, description, showWarning, paralelRun,
                                                                                          quickScript, endMessage, scriptBefore, scriptAfter,
                                                                                          workOnWholeFarmsCountries, appearInPopupMenu);

                // get the arguments for this specific script
                List <Element> parametersList = parametersElement.elements("parameter");
                for (Element e2 : parametersList)
                    tempScript.addArgument(e2.getData().toString());

                _scripts.put(description, tempScript);
            }
            /*
             * if some of the elements didn't existed in the xml file, then an exception will be thrown
             * if so, the specific script will not be added to the scripts list
             */
            catch (Exception e)
            {
                org.apache.log4j.Logger.getRootLogger().error("Someting went wrong when loading the scripts.xml file");
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }

        } // end of - for (Element e1 : scriptElementsList)
        
    }

    /**
     * Reads the scripts.xml file to a Document instance and returns it
     * @return Document instance of the scripts.xml file
     */
    public static Document readScriptsXml()
    {
        Document document = null ;
        try
        {
            SAXReader reader = new SAXReader();
            document = reader.read(NocToolsSettings.getScriptsXml());
            return document;
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }
        return document;
    }

    /**
     * Returns the TreeMap of all scripts
     * @return TreeMap of all scripts
     */
    public static TreeMap<String, Script> getScripts()
    {
        return _scripts;
    }

    /**
     * Returns the name of all the custom scripts in the scripts file
     * @return LinkedList of all custom scripts descriptions
     */
    public static LinkedList <String> getCustomScripts()
    {
        LinkedList <String> customScripts = new LinkedList<String>();
        Object [] scriptsArr = _scripts.values().toArray();
        for (Object o : scriptsArr)
        {
            if (o instanceof Script)
            {
                if (((Script)o).getType().equalsIgnoreCase("custom"))
                    customScripts.add(((Script)o).getDescription());
            }
            else
                throw new ClassCastException();
        }
        return customScripts;
    }

    /**
     * Returns a list of "quick" scripts. Those scripts will be displayed in the main panel
     * @param type script type: Custom or Server
     * @return LinkedList of the quick scripts according to requested type
     */
    public static LinkedList <Script> getQuickScripts(String type)
    {
        LinkedList <Script> scriptsList = new LinkedList<Script>();
        Object [] scriptsArr = _scripts.values().toArray();
        for (Object o : scriptsArr)
        {
            if (o instanceof Script)
            {
                if (((Script)o).getType().equalsIgnoreCase(type) && ((Script)o).getQickScript())
                    scriptsList.add((Script)o);
            }
            else
                throw new ClassCastException();
        }
        return scriptsList;
    }

    /**
     * Creates a ScriptThreadGroup instance with the currntly selected servers 
     * (if this is a server script). If this is a custom script, then a ScriptThreadGroup
     * instance is created anyway, and it will have only one ScriptThread instance
     * @param description description of the script as it appears in the scripts
     * table
     */
    public static void runScript(String description)
    {
        // check wether the script with the given description exists
        Script checkScript = _scripts.get(description);

        // check wether the script file exists
        File scriptFile = new File(NocToolsSettings.getScriptsDir() + checkScript.getScriptFile());
        if (scriptFile.exists())
        {
            if (checkScript != null)
            {
                Script tempScript = new Script(checkScript);
                ScriptThreadGroupQueue scriptThreadGroupQueue = _activeScriptQueues.get(tempScript.getDescription());
                if (scriptThreadGroupQueue == null)
                {
                    scriptThreadGroupQueue = new ScriptThreadGroupQueue(tempScript);
                    _activeScriptQueues.put(tempScript.getDescription(), scriptThreadGroupQueue);
                    NocToolsWorker.getInstance().addTask(scriptThreadGroupQueue);
                }


                if (tempScript.getType().equalsIgnoreCase("server"))
                {
                    if (ServersTreeManager.isSelectionMode() && ServersTreeManager.getSelectedServer() != null)
                    {
                        Server[] singleServerArray = new Server[1];
                        singleServerArray[0] = ServersTreeManager.getSelectedServer();
                        scriptThreadGroupQueue.push(singleServerArray);

                    }
                    if (ServersTreeManager.isCheckedMode() && ServersTreeManager.areThereCheckedServers())
                    {
                        Server[] servers = ServersTreeManager.getCheckedServers();
                        scriptThreadGroupQueue.push(servers);
                    }
                }

                // if this is not a server script
                else
                {
                    scriptThreadGroupQueue.push();
                }
            } // end of - if (tempScript != null)
        }
        else
            JOptionPane.showMessageDialog(null, "The script file \"" + scriptFile.getName() + 
                                                                                        "\" doesn't exists in " + NocToolsSettings.getScriptsDir(),
                                                                                        "Error", JOptionPane.ERROR_MESSAGE);
    }


    /**
     * Sets the _selectedScripts according to the selections in the scripts table.
     * Actually there is only one script that can be selected, and not an array of scripts
     * This is for future versions
     * @param descriptions Array of selected script descriptions
     */
    public static void setSelectedScriptsFromScriptsTab(String[] descriptions)
    {
        _selectedScripts.clear();
        for (int i = 0; i < descriptions.length; i++)
        {
            if (_scripts.containsKey(descriptions[i])) // should be always true
            {
                // get the selected script from all scripts and put it in to the selected scripts
                Script tempScript = new Script(_scripts.get(descriptions[i]));
                _selectedScripts.put(tempScript.getDescription(), tempScript);
            }
        }
    }

    /**
     * Returns a script instance by its description
     * @param desctiption Script's description
     * @return Script instance
     */
    public static Script getScript(String desctiption)
    {
        return new Script(_scripts.get(desctiption));
    }

    /**
     * Updates a script after editing or ads a new script after duplicating or creating
     * a new script. If oldScript is not null, it is replaced by the newScript. Otherwise,
     * newScript is added anyway.
     * @param newScript the new script
     * @param oldScript old script to replace with the new one. Might be null
     */
    public static void updateScript(Script newScript, Script oldScript)
    {
        if (oldScript!= null)
        {
            _scripts.remove(oldScript.getDescription());
            
            // if the scripts name was changed, then it should change in all other scrips that use this script
            if (!oldScript.getDescription().equalsIgnoreCase(newScript.getDescription()))
            {
                Iterator<Entry<String, Script>> iterator = _scripts.entrySet().iterator();

                while (iterator.hasNext())
                {
                    Script tempScript = iterator.next().getValue();
                    String scriptBefore = tempScript.getScriptBefore();
                    if (scriptBefore != null)
                    {
                        if (oldScript.getDescription().equalsIgnoreCase(scriptBefore))
                            tempScript.setScriptBefore(newScript.getDescription());
                    }

                    String scriptAfter = tempScript.getScriptAfter();
                    if (scriptAfter != null)
                    {
                        if (oldScript.getDescription().equalsIgnoreCase(scriptAfter))
                            tempScript.setScriptAfter(newScript.getDescription());
                    }
                }
            }
        }

        // update the ScriptThreadGroupQueues
        if (_activeScriptQueues.containsKey(oldScript.getDescription()))
        {
            ScriptThreadGroupQueue queue = _activeScriptQueues.remove(oldScript.getDescription());
            queue.setScript(newScript);
            _activeScriptQueues.put(newScript.getDescription(), queue);
        }

        _scripts.put(newScript.getDescription(), newScript);

        saveScriptsToXml();
    }

    /**
     * Searches for the script by it's description
     * @param description script's description
     * @return True if the script with the given description already exists.
     */
    public static boolean findDescriptionIgnoreCase(String description)
    {
        if (description != null)
        {
            Collection scriptsCollection = _scripts.values();
            for (Object tempScriptObject : scriptsCollection)
            {
                if (tempScriptObject instanceof Script)
                {
                    Script tempScript = (Script)tempScriptObject;
                    if (tempScript.getDescription().equalsIgnoreCase(description))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Deletes a script by its description. At the end, all the scripts are written to the
     * scripts.xml file
     * @param desctiption script's description
     */
    public static void deleteScript(String desctiption)
    {
        if (desctiption != null)
        {
            // TODO save a backup of the scripts File
            _selectedScripts.remove(desctiption);
            _scripts.remove(desctiption);

            saveScriptsToXml();
        }
    }

    public static void saveScriptsToXml()
    {
        File scriptsFile = new File(NocToolsSettings.getScriptsXml());
        if (scriptsFile.exists())
            scriptsFile.delete();

        Document document = DocumentHelper.createDocument();
        Element rootElement = document.addElement("root");

        
        Collection scriptsCollection = _scripts.values();
        for (Object tempScriptObject : scriptsCollection)
        {
            if (tempScriptObject instanceof Script)
            {
                Script tempScript = (Script)tempScriptObject;
                Element scriptElement = rootElement.addElement("script");

                Element descriptionElement = scriptElement.addElement("description");
                descriptionElement.setText(tempScript.getDescription());

                Element fileNameElement = scriptElement.addElement("filename");
                fileNameElement.setText(tempScript.getScriptFile());

                Element typeElement = scriptElement.addElement("type");
                typeElement.setText(tempScript.getType());

                Element workOnWholeFarmsCountriesElement = scriptElement.addElement("workOnWholeFarmsCountries");
                workOnWholeFarmsCountriesElement.addText(tempScript.getWorkOnWholeFarmsCountries() ? "yes" : "no");

                Element runApprovalElement = scriptElement.addElement("runApproval");
                runApprovalElement.addText(tempScript.getShowWarning() ? "yes" : "no");

                Element paralelRunElement = scriptElement.addElement("paralelRun");
                paralelRunElement.addText(tempScript.getWorkInParalel() ? "yes" : "no");

                Element messageInTheEndElement = scriptElement.addElement("messageInTheEnd");
                messageInTheEndElement.setText(tempScript.getEndMessage() != null ? tempScript.getEndMessage() : "");

                Element quickScriptElement = scriptElement.addElement("quickScript");
                quickScriptElement.setText(tempScript.getQickScript() ? "yes" : "no");

                Element appearInPopupMenuElement = scriptElement.addElement("appearInPopupMenu");
                appearInPopupMenuElement.setText(tempScript.getAppearInPopupMenu() ? "yes" : "no");

                Element parametersElement = scriptElement.addElement("parameters");

                LinkedList<String> argumentsList = tempScript.getArgumentsList();
                if (argumentsList.isEmpty())
                {
                    parametersElement.addText("");
                }
                else
                {
                    for (String tempArgument : argumentsList)
                    {
                        Element parameterElement = parametersElement.addElement("parameter");
                        parameterElement.setText(tempArgument);
                    }
                }

                Element scriptBeforeElement = scriptElement.addElement("scriptBefore");
                scriptBeforeElement.setText(tempScript.getScriptBefore() != null ? tempScript.getScriptBefore() : "");

                Element scriptAfterElement = scriptElement.addElement("scriptAfter");
                scriptAfterElement.setText(tempScript.getScriptAfter() != null ? tempScript.getScriptAfter() : "");

            }
        }

        try
        {
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setLineSeparator(NocToolsSettings.ENDL);
            //format.setIndentSize(3);
            format.setIndent("\t");
            XMLWriter writer = new XMLWriter(new FileWriter(NocToolsSettings.getScriptsXml()), format);
            writer.write(document);
            writer.close();
        }
        catch (Exception e)
        {
            if (e.getMessage()  != null)
                org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
            e.printStackTrace();
        }

    }
}
