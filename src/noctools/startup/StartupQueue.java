/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.startup;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import noctools.endpoint.Server;
import noctools.util.Queue;

/**
 *
 * @author Yuri
 */
public class StartupQueue
{
    private static StartupQueue _instance = null;
    private Queue<StartupTaskQueue> _serversQueue = null;
    private boolean _paused;
    private JList _queueList;

    private StartupQueue(JList queueList)
    {
        _queueList = queueList;
        _serversQueue = new Queue<StartupTaskQueue>();
    }
    
    public static StartupQueue getInstance(JList queueList)
    {
        if (_instance == null)
        {
            _instance = new StartupQueue(queueList);
        }
        return _instance;
    }
    
    public synchronized void addServer(Server server)
    {
        if (server != null)
        {
            // create a new startup task queue
            StartupTaskQueue taskQueue = new StartupTaskQueue(server, StartupPriorityManager.getPriority());

            // check if this server is already in the queue
            if (_serversQueue.contains(taskQueue))
            {
                // TODO find a solution that will not hold up this function
            }
            else
            {
                _serversQueue.push(taskQueue);
                updateListModel();
                this.notifyAll();
            }
        }
    }
    public synchronized boolean isEmpty()
    {
        return _serversQueue.isEmpty();
    }

    public synchronized StartupTaskQueue pop()
    {
        StartupTaskQueue startupTaskQueue = _serversQueue.pop();
        updateListModel();
        return startupTaskQueue;
    }

    public synchronized void push(StartupTaskQueue taskQueue)
    {
        _serversQueue.push(taskQueue);
        updateListModel();
    }

    public void pause()
    {
        _paused = true;
    }

    public synchronized boolean isPaused()
    {
        return _paused;
    }

    public synchronized void unpause()
    {
        _paused = false;
        this.notifyAll();
    }

    public synchronized void removeAll()
    {
        _serversQueue.clear();
        updateListModel();
    }

    private synchronized void updateListModel()
    {
        DefaultListModel newModel =  new DefaultListModel();

        Object [] tasks = _serversQueue.toArray();

        for (Object task : tasks)
        {
            newModel.addElement(task);
        }

        synchronized (_queueList)
        {
            _queueList.setModel(newModel);
        }
        
        _queueList.repaint();
    }

    public synchronized StartupTaskQueue peek()
    {
        return _serversQueue.peek();
    }

    public synchronized int getSize() {return _serversQueue.getSize();}

    public String toString()
    {
        return _serversQueue.toString();
    }
}
