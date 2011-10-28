/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools;

import javax.swing.SwingWorker;
import noctools.util.Queue;

/**
 * This class executes all the threads of the app
 * @author Yuri
 */
public class NocToolsWorker extends SwingWorker // singleton
{
    Queue<Thread> _tasks = null;
    private static NocToolsWorker _worker = null;
    private static boolean _run = true;


    public void stop()
    {
        _run = false;
    }

    public static synchronized NocToolsWorker getInstance()
    {
        if (_worker == null)
        {
            _worker = new NocToolsWorker();
            _worker.execute();
        }

        return _worker;
    }

    private NocToolsWorker()
    {
        _tasks = new Queue<Thread>();
    }

    @Override
    protected synchronized Object doInBackground()
    {
        while (_run)
        {
            if (_tasks.isEmpty() && _run)
            {
                try
                {
                    this.wait();
                }
                catch (InterruptedException e)
                {
                    if (e.getMessage()  != null)
                        org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                    e.printStackTrace();
                }
            }
            else if (_run)
                _tasks.pop().start();
        }
        return new Object();
    }
    
    public synchronized void addTask(Thread task)
    {
        _tasks.push(task);
        this.notifyAll();
    }
}
