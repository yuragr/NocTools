/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.startup;

import noctools.util.Queue;
import noctools.endpoint.Server;
import noctools.settings.NocToolsSettings;

/**
 * This class will represent the queue of all tasks of a single server
 * @author Yuri
 */
public class StartupTaskQueue implements Comparable
{
    private Queue <StartupTask> _tasksQueue;
    private int _failures = 0;
    private int _priority;
    private Server _server = null;
    private long _creationTime;
    private boolean _taskListFailed = false;
    private int _connectionFailures = 0;
    private int _psexecFailures = 0;

    public StartupTaskQueue(Server server, int priority)
    {
        _priority = priority;
        _tasksQueue = new Queue<StartupTask>();
        _server = server;
        if (server.getMiniservers() > 0)
        {
            // calculate the number of StartupTasks and create them and add them to the queue
            int remotesNum = server.getMiniservers() / NocToolsSettings.getMiniserversPerRemote();

            for (int i = 1; i <= remotesNum; i++)
            {
                _tasksQueue.push(new StartupTask(server, "remote" + i, i));
            }
        }
        _creationTime = System.currentTimeMillis();
    }

    public void putAsFitst(StartupTask task)
    {
        _tasksQueue.pushToFirstPlace(task);
    }

    public int getPriority()
    {
        return _priority;
    }

    public synchronized boolean isEmpty()
    {
        return _tasksQueue.isEmpty();
    }

    public synchronized StartupTask getNextTask()
    {
        return (!_tasksQueue.isEmpty()) ? _tasksQueue.pop() : null;
    }

    public synchronized StartupTask peekAtNextTask()
    {
        return (!_tasksQueue.isEmpty()) ? _tasksQueue.peek() : null;
    }

    public int compareTo(Object other)
    {
        if (other instanceof StartupTaskQueue)
        {
            StartupTaskQueue otherStartupTaskQueue = (StartupTaskQueue)other;

            if (otherStartupTaskQueue._priority > _priority)
                return -1;
            else if (otherStartupTaskQueue._priority < _priority)
                return 1;
            else return 0;
        }
        else
            throw new ClassCastException();
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null)
            return false;

        if (other instanceof StartupTaskQueue)
        {
            StartupTaskQueue otherTaskQueue = (StartupTaskQueue)other;

            if (this._server.equals(otherTaskQueue._server))
                return true;
            else
                return false;
        }
        else
            throw new ClassCastException();
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 13 * hash + (this._server != null ? this._server.hashCode() : 0);
        return hash;
    }

    public int getTasksNumber()
    {
        return _tasksQueue.getSize();
    }

    public Server getServer() {return _server;}

    @Override
    public String toString()
    {
        return _server.getShortName();
    }

    public void addFailure() {_failures ++;}

    public void addConnectionFailure() {_connectionFailures ++;}

    public void addPsexecFailure() {_psexecFailures++;}

    public int getFailures() {return _failures;}

    public int getConnectionFailures() {return _connectionFailures;}

    public int getPsexecFailures() {return _psexecFailures;}

    public long getCreationTime() {return _creationTime;}

    public void taskListFailed() {_taskListFailed = true;}

    public boolean isTaskListFailed() {return _taskListFailed;}
}
