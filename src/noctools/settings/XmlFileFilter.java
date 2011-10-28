/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.settings;
import java.io.File;
/**
 *
 * @author Yuri
 */
public class XmlFileFilter extends javax.swing.filechooser.FileFilter
{
    public boolean accept(File file)
    {
        if (file.isDirectory())
            return true;

        String txtExtention = ".xml";
        int nameLength = file.toString().length();
        if (file.toString().lastIndexOf(txtExtention) == nameLength - txtExtention.length())
            return true;
        else
            return false;
    }

    public String getDescription()
    {
        return "Extensible Markup Language file (*.xml)";
    }
}
