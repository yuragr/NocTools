/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.script;
import java.util.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 *
 * @author Yuri
 */
public class Script
{
    private String _scriptFile;
    private boolean _showWarning;
    private boolean _workInParalel;
    private String _scriptType;
    private LinkedList<String> _arguments;
    private String _description;
    private String _endMessage;
    private boolean _quickScript;
    private String _scriptBefore;
    private String _scriptAfter;
    private boolean _workOnWholeFarmsCountries;
    private boolean _appearInPopupMenu;
    private boolean _allowExecution;

    private static String _fileChoserDir = "c:\\";

    public Script(String scriptFile, String scriptType, String description,
                                      boolean showWarning, boolean workInParalel, boolean quickScript,
                                      String endMessage, String scriptBefore, String scriptAfter,
                                      boolean workOnWholeFarmsCountries, boolean appearInPopupMenu)
    {
        _endMessage = (endMessage == null ? null : new String(endMessage));
        _arguments = new LinkedList<String>();
        _scriptFile = new String(scriptFile);
        _scriptType = new String(scriptType);
        _description = new String(description);
        _showWarning = showWarning;
        _workInParalel = workInParalel;
        _quickScript = quickScript;
        _scriptBefore = (scriptBefore == null ? null : new String(scriptBefore));
        _scriptAfter = (scriptAfter == null ? null : new String(scriptAfter));
        _workOnWholeFarmsCountries = workOnWholeFarmsCountries;
        _appearInPopupMenu = appearInPopupMenu;
        _allowExecution = true;
    }

    public Script(Script other)
    {
        if (other != null)
        {
            _scriptFile = new String(other._scriptFile);
            _showWarning = other._showWarning;
            _workInParalel = other._workInParalel;
            _scriptType = new String(other._scriptType);
            _description = new String(other._description);
            _quickScript = other._quickScript;

            _endMessage = (other._endMessage == null ? null : new String(other._endMessage));

            _arguments = new LinkedList<String>();
            if (!other._arguments.isEmpty())
            {
                for (String otherArgument :other._arguments)
                {
                    String argument = new String(otherArgument);
                    _arguments.add(argument);
                }
            }
            _scriptBefore = (other._scriptBefore == null ? null : new String(other._scriptBefore));
            _scriptAfter = (other._scriptAfter == null ? null : new String(other._scriptAfter));
            _workOnWholeFarmsCountries = other._workOnWholeFarmsCountries;
            _appearInPopupMenu = other._appearInPopupMenu;
            _allowExecution = other._allowExecution;
        }
    }

    public void addArgument(String argument)
    {
        _arguments.add(argument);
    }

    public void setArgumentsList(LinkedList<String> arguments)
    {
        if (arguments != null)
            _arguments = arguments;
    }

    public String getType()
    {
        return (_scriptType == null ? null : new String (_scriptType));
    }

    @Override
    public String toString()
    {
        return new String(_description);
    }

    public String getScriptFile()
    {
        return new String(_scriptFile);
    }

    public String getScriptFileExtention()
    {
        int nameLength = _scriptFile.toString().length();
        if (_scriptFile.lastIndexOf(".vbs") == nameLength - ".vbs".length())
            return ".vbs";
        else if (_scriptFile.lastIndexOf(".cmd") == nameLength - ".cmd".length())
            return ".cmd";
        else if (_scriptFile.lastIndexOf(".bat") == nameLength - ".bat".length())
            return ".bat";
        else
            return "";
    }

    public LinkedList<String> getArgumentsList()
    {
        return _arguments;
    }

    public String getDescription()
    {
        return new String(_description);
    }

    public String getEndMessage()
    {
        return (_endMessage == null ? null : new String (_endMessage));
    }
    public boolean getShowWarning()
    {
        return _showWarning;
    }

    public boolean getQickScript()
    {
        return _quickScript;
    }

    public boolean getWorkInParalel()
    {
        return _workInParalel;
    }

    public String getScriptBefore()
    {
        return (_scriptBefore == null? null : new String(_scriptBefore));
    }

    public String getScriptAfter()
    {
        return (_scriptAfter == null? null : new String(_scriptAfter));
    }

    public boolean getWorkOnWholeFarmsCountries()
    {
        return _workOnWholeFarmsCountries;
    }

    public boolean getAllowExecution()
    {
        return _allowExecution;
    }

    public boolean getAppearInPopupMenu()
    {
        return _appearInPopupMenu;
    }

    public void setScriptBefore(String scriptBefore)
    {
        _scriptBefore = scriptBefore == null ? null : new String(scriptBefore);
    }

    public void setScriptAfter(String scriptAfter)
    {
        _scriptAfter = scriptAfter == null ? null : new String(scriptAfter);
    }

    public void setWorkOnWholeFarmsCountries(boolean workOnWholeFarmsCountries)
    {
        _workOnWholeFarmsCountries = workOnWholeFarmsCountries;
    }

    public void setAppearInPopupMenu(boolean appearInPopupMenu)
    {
        _appearInPopupMenu = appearInPopupMenu;
    }


    @Override
    public boolean equals(Object other)
    {
        if (other instanceof Script)
        {
            Script otherScript = (Script)other;
            return this._description.equals(otherScript._description);
        }
        else
            return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (this._description != null ? this._description.hashCode() : 0);
        return hash;
    }


    /**
     * Checks if we need to ask the user about some arguments before running the script
     * @param argumentsList script's arguments
     */
    public void askForArguments()
    {
        try
        {
            if (_arguments != null)
            {
                if (_arguments.size() != 0)
                {
                    int i = 0;
                    for (i = 0; i < _arguments.size(); i++)
                    {
                        String tempArgument = _arguments.get(i);
                        if (tempArgument.indexOf("%U[") == 0 || tempArgument.indexOf("%u[") == 0)
                        {
                            // if we found an argument that we have to ask the user - ask the user, and replace it with the answer
                            String message = new String (tempArgument.substring(3, tempArgument.length() - 1));
                            Thread.sleep(50);
                            String value = JOptionPane.showInputDialog(null, message, getDescription(), javax.swing.JOptionPane.QUESTION_MESSAGE );
                            Thread.sleep(50);
                            _arguments.remove(i);

                            // if the user entered an empty value or pressed "cancel", don't execute the script
                            if (value == null || value.equalsIgnoreCase(""))
                                _allowExecution = false;
                            else
                                _arguments.add(i, value);

                        }

                        // if we have to present a file chooser dialog
                        if (tempArgument.equalsIgnoreCase("%f"))
                        {
                            Thread.sleep(50);
                            JFileChooser fc = new JFileChooser();
                            Thread.sleep(50);
                            fc.setAcceptAllFileFilterUsed(true);
                            fc.setCurrentDirectory(new java.io.File(_fileChoserDir));
                            Thread.sleep(50);
                            int returnVal = fc.showDialog(null, "Select");
                            Thread.sleep(50);

                            _arguments.remove(i);

                            if (returnVal == JFileChooser.APPROVE_OPTION)
                            {
                                _fileChoserDir = fc.getCurrentDirectory().toString();
                                _arguments.add(i, fc.getSelectedFile().getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
        catch (InterruptedException e)
        {}
    } // end of askForArguments(Script script)

}
