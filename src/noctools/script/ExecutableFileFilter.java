/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.script;
import java.io.File;
/**
 * File filter to be used in JFileChooser dialoges
 * @author Yuri
 */
public class ExecutableFileFilter extends javax.swing.filechooser.FileFilter
{
    public boolean accept(File file)
    {
        if (file.isDirectory())
            return true;

        int nameLength = file.toString().length();

        if (file.toString().lastIndexOf(".vbs") == nameLength - ".vbs".length() ||
               file.toString().lastIndexOf(".cmd") == nameLength - ".cmd".length() ||
               file.toString().lastIndexOf(".exe") == nameLength - ".exe".length() ||
               file.toString().lastIndexOf(".bat") == nameLength - ".bat".length())
            return true;
        else
            return false;

    }

    public String getDescription()
    {
        return "Executable files (*.vbs, *.cmd, *.bat, *.exe)";
    }
}
