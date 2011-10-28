/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.DefaultListModel;
import javax.swing.JList;

/**
 *
 * @author Yuri
 */
public class JListAppender extends Thread
{
    private JList _list;
    private final int LIST_MAX_SIZE = 5000;
    private Queue<String> _updatesQueue;

    public JListAppender(JList list)
    {
        _list = list;
        _updatesQueue = new Queue<String>();
        this.setDaemon(true);
        this.start();
    }

    @Override
    public synchronized void run()
    {
        while (true)
        {
            if (_updatesQueue.isEmpty())
            {
                try
                {
                    this.wait();
                }
                catch (InterruptedException e)
                {
                }

            }
            else
            {
                while (!_updatesQueue.isEmpty())
                {
                    try
                    {
                        Thread.sleep(450);
                    }
                    catch (InterruptedException e)
                    {
                    }

                    updateList();
                }
            }
        }
    }

    public synchronized void addLine(String line)
    {
        _updatesQueue.push(line);
        notifyAll();
    }

    private synchronized void updateList()
    {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        String time = timeFormat.format(Calendar.getInstance().getTime());

        _list.clearSelection();

        String message = _updatesQueue.pop();

        if (message != null)
        {
            DefaultListModel model = (DefaultListModel)_list.getModel();
            model.addElement(time + " : " + message);

            int lastIndex = _list.getModel().getSize() - 1;
            if (lastIndex >= 0)
            {
                _list.ensureIndexIsVisible(lastIndex);
            }
        }
        else
        {
            DefaultListModel model = (DefaultListModel)_list.getModel();
            model.clear();
        }

        // make sure that the list is not too big.
        if (_list.getModel().getSize() >= LIST_MAX_SIZE)
            ((DefaultListModel)_list.getModel()).remove(0);

        //_list.repaint();
    }
}
