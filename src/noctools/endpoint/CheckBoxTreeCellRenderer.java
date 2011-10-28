/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.endpoint;
import it.cnr.imaa.essi.lablib.gui.checkboxtree.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;

/**
 * Renderer of the checkbox tree items
 * @author Yuri
 */
public class CheckBoxTreeCellRenderer extends DefaultCheckboxTreeCellRenderer
{
    private static final int COUNTRY_LENGTH = 2;
    private static HashMap <String, ImageIcon> _countryIcons = new HashMap <String, ImageIcon>();
    public CheckBoxTreeCellRenderer()
    {
        super();
    }

     /*
     * Decorates the CheckBoxTree item according to selection and changes icons according to countries
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object object, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, object, selected, expanded, leaf, row, hasFocus);
        Object endpoint = ((ComparableMutableTreeNode)object).getUserObject();

        // if this is a selection mode and this server is selected and it is a server object
        if (ServersTreeManager.isSelectionMode() && selected && leaf)
        {
            if (endpoint instanceof Server)
            {
                if (selected)
                {
                    label.setForeground(Color.WHITE);
                    label.setBackgroundSelectionColor(Color.RED);
                }
                else
                    label.setForeground(Color.RED);
                
                label.setText(endpoint.toString());
            }
        }

        // if this is checked mode and there are checked servers, and this server is checked
        else if (ServersTreeManager.isCheckedMode() && ServersTreeManager.areThereCheckedServers())
        {
            if (checkBox.isSelected())
            {
                if (selected)
                {
                    label.setForeground(Color.WHITE);
                    label.setBackgroundSelectionColor(Color.RED);
                }
                else
                    label.setForeground(Color.RED);
                label.setText(endpoint.toString());
            }
            else
            {
                if (selected)
                {
                    label.setForeground(Color.WHITE);
                    label.setBackgroundSelectionColor(Color.BLUE);
                }

                label.setText(endpoint.toString());
            }
        }

        // if this is a farm or a country, then we have to adjust the icons
        if ((endpoint instanceof Farm || endpoint instanceof Country) && endpoint.toString().length() > 1)
        {
            ImageIcon icon = _countryIcons.get(endpoint.toString().substring(0, COUNTRY_LENGTH).toLowerCase());
            if (icon != null)
                label.setIcon(icon);
            else
            {
                File iconFile = new File(noctools.settings.NocToolsSettings.getIconsDir() + endpoint.toString().substring(0, COUNTRY_LENGTH).toLowerCase() + ".gif");
                if (iconFile.exists())
                {
                    ImageIcon newIcon = new ImageIcon(iconFile.getPath());
                    label.setIcon(new ImageIcon(iconFile.getPath()));
                    _countryIcons.put(endpoint.toString().substring(0, COUNTRY_LENGTH).toLowerCase(), newIcon);
                }
            }
        }

        return this;
    }
}
