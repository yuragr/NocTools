/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.startup;

import java.util.HashMap;
import javax.swing.JList;
import noctools.settings.NocToolsSettings;
import noctools.util.ConnectionTester;
import noctools.util.JListAppender;
import noctools.util.PsexecProcessCounter;

/**
 * This class will manage all server startups. It will have a queue of all the satrup taskQueues
 * @author Yuri
 */
public class StartupManager extends Thread
{
    private boolean _run;
    private StartupTask _currentTask = null;
    private static StartupTaskQueue _currentTasksQueue = null;
    private static StartupManager _instance = null;
    private HashMap<String, Long> _serverStartupTimes = null;
    private static StartupQueue _mainQueue = StartupQueue.getInstance(null); // we rely on the fact that it was already inited
    private static long _lastRemoveAll = 0;
    private static JListAppender _listAppender;
    private static boolean _isIdle = true;

    private StartupManager(JList serverSartupStatusList)
    {
        super("Startup Manager");
        _serverStartupTimes = new HashMap<String, Long>();
        _listAppender = new JListAppender(serverSartupStatusList);

    }

    public static StartupManager getInstance(JList serverSartupStatusList)
    {
        if (_instance == null)
        {
            _instance = new StartupManager(serverSartupStatusList);
        }

        return _instance;
    }

    @Override
    public void run()
    {
        _run = true;
        org.apache.log4j.Logger.getRootLogger().debug("Waiting for tasks");
        _listAppender.addLine("Waiting for tasks");

        while (_run)
        {
            if (_mainQueue.isEmpty() && _run || _mainQueue.isPaused())
            {
                synchronized (_mainQueue)
                {
                    try
                    {
                        if (_mainQueue.isPaused())
                        {
                            org.apache.log4j.Logger.getRootLogger().debug("Paused. Press \"Resume\" to continue");
                            _listAppender.addLine("Paused. Press \"Resume\" to continue");
                        }

                        _isIdle = true;
                        _mainQueue.wait();
                        _isIdle = false;

                        // if this thread was awaken by "remove all"
                        if (_mainQueue.isEmpty())
                        {
                            org.apache.log4j.Logger.getRootLogger().debug("Waiting for tasks");
                            _listAppender.addLine("Waiting for tasks");
                        }
                    }
                    catch (InterruptedException e)
                    {
                        if (e.getMessage()  != null)
                            org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            else
            {
                if (!_mainQueue.isEmpty())
                {
                    prepareAndExecuteTask();
                    finalizeTask();
                }
                if (_mainQueue.isEmpty())
                {
                    org.apache.log4j.Logger.getRootLogger().debug("Waiting for tasks");
                    _listAppender.addLine("Waiting for tasks");
                }
            }
        }
    }


    private synchronized void prepareAndExecuteTask()
    {
        // get the task and start evaluating if we can begin the task
        _currentTasksQueue = _mainQueue.peek();
        _currentTask = _currentTasksQueue.peekAtNextTask();
        waitIfPaused();
        if (evaluateTask() && !userDidRemoveAll())
        {
            waitIfPaused();
            if (evaluateConnection() && !userDidRemoveAll()) // evaluate connection another time if the first time wasn't successfull
            {
                waitIfPaused();
                if (evaluateSkypes() && !userDidRemoveAll())
                {
                    waitIfPaused();
                    org.apache.log4j.Logger.getRootLogger().info(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Starting the task");
                    _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Starting the task");
                    _currentTask.start();
                    try
                    {
                        _currentTask.join();
                        _serverStartupTimes.put(_currentTasksQueue.getServer().getName(), System.currentTimeMillis());
                        org.apache.log4j.Logger.getRootLogger().info(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Task completed");
                        _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Task completed");
                    }
                    catch (Exception e) {e.printStackTrace();}

                }
            }
        }
    }

    public synchronized void waitIfPaused()
    {
        synchronized (_mainQueue)
        {
            try
            {
                if (_mainQueue.isPaused())
                {
                    org.apache.log4j.Logger.getRootLogger().debug("Paused. Press \"Resume\" to continue");
                    _listAppender.addLine("Paused. Press \"Resume\" to continue");
                    _mainQueue.wait();
                }
            }
            catch (InterruptedException e)
            {
                if (e.getMessage()  != null)
                    org.apache.log4j.Logger.getRootLogger().error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void finalizeTask()
    {
        try
        {
            // if the main queue is not empty, and nothing has changed
            if (!_mainQueue.isEmpty()  && _currentTasksQueue.equals(_mainQueue.peek()))
            {

                // remove the task from this server's queue only if no tasks were created for this server's queue
                if (!_currentTasksQueue.isEmpty() && _currentTask.equals(_currentTasksQueue.peekAtNextTask()))
                    if (_currentTask.wasExecuted())
                        _currentTask = _currentTasksQueue.getNextTask();

                // check if there is a need to return this serv's queue back to the main queue
                boolean empty = _currentTasksQueue.isEmpty();
                boolean connectionFailures = _currentTasksQueue.getConnectionFailures() > NocToolsSettings.getMaxConnectionFailuresPerServer();
                boolean taskFailures = _currentTasksQueue.getFailures() > NocToolsSettings.getMaxFailuresPerServer();
                boolean psexecFailures = _currentTasksQueue.getPsexecFailures() > NocToolsSettings.getMaxPsexecFailuresPerServer();
                boolean removeAll = userDidRemoveAll();

                if (empty || connectionFailures || taskFailures || psexecFailures ||removeAll)
                {
                    if (connectionFailures)
                    {
                        _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : There were too many connection failures. Removing server");
                        org.apache.log4j.Logger.getRootLogger().error("Removed " + _currentTasksQueue + " from the main quqeue. Too many connection failures");
                    }

                    if (taskFailures)
                    {
                        _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : There were too many task failures. Removing server");
                        org.apache.log4j.Logger.getRootLogger().error("Removed " + _currentTasksQueue + " from the main quqeue. Too many task failures");
                    }

                    if (psexecFailures)
                    {
                        _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : There were too many psexec failures. Removing server");
                        org.apache.log4j.Logger.getRootLogger().error("Removed " + _currentTasksQueue + " from the main quqeue. Too many task failures");
                    }

                    // don't return the server's queue back to the main queue
                    _mainQueue.pop();
                }
                else
                {
                    // there are no reasons not to return this server's queue back to the main queue
                    _currentTasksQueue = _mainQueue.pop();
                    _mainQueue.push(_currentTasksQueue);
                    Thread.sleep(1500);
                }
            }
        }
        catch (Exception e)
        {
            org.apache.log4j.Logger.getRootLogger().error(e);
            e.printStackTrace();
        }
        finally
        {
            _currentTasksQueue = null;
            _currentTask = null;
        }
    }

    private boolean evaluateSkypes()
    {
        org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Starting evaluating skypes");
        boolean skypesOK = true;
        // do we have to count skypes at all?
        if (NocToolsSettings.getCountSkypes())
        {
            PsexecProcessCounter counter;
        
            // if this is not the first task of this server, we have to see if the previous remote had finished
            if (_currentTask.getTaskNumber() > 1)
            {
                org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : This is not the first task");
                // if we don't have to count skypes after we have waited "max time between remotes", return true
                long currentTime = System.currentTimeMillis();
                Long lastTask = _serverStartupTimes.get(_currentTasksQueue.getServer().getName());
                long difference = currentTime - (lastTask == null ? 0 : lastTask);
                if (!NocToolsSettings.getCountSkypesAfterMaxWait() && difference > NocToolsSettings.getMaxWaitBetweenRemotes())
                {
                    org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : It has been too much time since the last task. Skypes will not be counted");
                    _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : It has been too much time since the last task. Skypes will not be counted");
                    return true;
                }

                int previousLastHms = (_currentTask.getTaskNumber() - 2) * 100 + (NocToolsSettings.getMiniserversPerRemote() - 1);

                org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Checking for skype on HMS" + previousLastHms);
                _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Checking for skype on HMS" + previousLastHms);
                counter = checkSkypePerUser(previousLastHms);

                if (!counter.isPsexecSuceeded())
                {
                    org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Psexec had failed!");
                    _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Psexec had failed!");
                    skypesOK = false;
                }
                // if there is no skype on the last hms of the previous remote, we have to check why
                // Maybe it was due to a faliure to start the remote, or maybe it just didn't finished
                else if (counter.getProcesses() != 1)
                {
                    skypesOK = false;

                    org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : There is no skype on HMS" + previousLastHms);
                    _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : There is no skype on HMS" + previousLastHms);

                    int previousFirstHms =  (_currentTask.getTaskNumber() - 2) * 100 + 1;
                    org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Checking for skype on HMS" + previousFirstHms + " (Ensuring that the previous task was successful)");
                    _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Checking for skype on HMS" + previousFirstHms + " (Ensuring that the previous task was successful)");

                    counter = checkSkypePerUser(previousFirstHms);

                    if (!counter.isPsexecSuceeded())
                    {
                        org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Psexec had failed!");
                        _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Psexec had failed!");
                    }
                    else if (counter.getProcesses() != 1)
                    {
                        org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : There is no skype on HMS" + previousFirstHms + " either!");
                        _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : There is no skype on HMS" + previousFirstHms + " either!");
                        // the previous remote wasn't started at all!!! it means that the previous task had failed
                        // we have to add to check if there were too much failures already for this server

                        _currentTasksQueue.addFailure();

                        // if there weren't too much failures - recreate the previous task
                        if (NocToolsSettings.getMaxFailuresPerServer() >= _currentTasksQueue.getFailures())
                        {
                            org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : The previous task had failed. Recreating it");
                            _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : The previous task had failed. Recreating it");
                            _currentTasksQueue.putAsFitst(new StartupTask(_currentTasksQueue.getServer(), "remote" + (_currentTask.getTaskNumber() - 1), _currentTask.getTaskNumber() - 1));
                        }
                    }
                }
            }
            
            if (skypesOK) // if all the tests so far were ok
            {
                // check skypes on the first hms of the current task. There is a possibility that this remote was already started
                int currentFirstHms = (_currentTask.getTaskNumber() - 1) * 100 + 1;
                org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Checking for skype on HMS" + currentFirstHms);
                _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Checking for skype on HMS" + currentFirstHms);
                counter =  checkSkypePerUser(currentFirstHms);
                if (!counter.isPsexecSuceeded())
                {
                    org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Psexec had failed!");
                    _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Psexec had failed!");
                    skypesOK = false;
                }
                // if someone already started this remote, then we have to remove this task
                else if (counter.getProcesses() > 0)
                {
                    org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : This task was already executed on this server");
                    _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : This task was already executed on this server");
                    _currentTask.setWasExecuted(true);
                    // TODO - add a binary search algorithm that will test in as much less than possible checks what is the status of the server
                    skypesOK = false;
                }
            }
        }
        return skypesOK;
    }

    private PsexecProcessCounter checkSkypePerUser(int hmsNum)
    {
        String serverIP = _currentTasksQueue.getServer().getIP();
        String logFileName = NocToolsSettings.getLogsDir() + _currentTasksQueue.getServer().getShortName() +  ".hms" + hmsNum + ".log";
        PsexecProcessCounter counter = new PsexecProcessCounter(serverIP, "hms" + hmsNum,  "remote1", logFileName);
        counter.start();
        try
        {
            counter.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (!counter.isPsexecSuceeded())
                _currentTasksQueue.addPsexecFailure();
        }
        org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Finished checking skypes on " + hmsNum + ". Found " + counter);
        return counter;
    }

    private boolean evaluateConnection()
    {
        boolean connectionOK = true;

        // should we test connection to the server?
        if (NocToolsSettings.getTestConnection())
        {
            connectionOK = false;
            // test connection until it is ok, or until we had too much connection failures
            while (!connectionOK && _currentTasksQueue.getConnectionFailures() <= NocToolsSettings.getMaxConnectionFailuresPerServer())
            {
                org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Testing connection to server...");
                _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Testing connection to server...");
                connectionOK = ConnectionTester.testConnectionByIp(_currentTasksQueue.getServer().getIP());

                // there is no connection to the server
                if (!connectionOK)
                {
                    _currentTasksQueue.addConnectionFailure();
                    org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : No connection to server!");
                    _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : No connection to server!");
                }
            }
        }
        return connectionOK;
    }

    private synchronized boolean evaluateTask()
    {
        boolean evaluateOK = true;

        org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Checking if it is ok to begin");
        _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Checking if it is ok to begin");
        // are there previous tasks for this server?
        if (_serverStartupTimes.containsKey(_currentTasksQueue.getServer().getName()))
        {
            long lastActivity = _serverStartupTimes.get(_currentTasksQueue.getServer().getName());
            long minWait = NocToolsSettings.getWaitBetweenRemotes();

            // should we wait for the tasks to finish on this server?
            if (System.currentTimeMillis() - lastActivity < minWait)
            {
                // are there other servers in the queue besides this one?
                long waitTime = minWait - (System.currentTimeMillis() - lastActivity);
                if (_mainQueue.getSize() > 1)
                {
                    evaluateOK = false;
                    org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Cannot start the task. Need to wait at least " + waitTime / 1000 + " seconds. Moving to the next server");
                    _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Cannot start the task. Need to wait at least " + waitTime / 1000 + " seconds. Moving to the next server");
                }
                else
                {
                    try
                    {
                        synchronized (_mainQueue)
                        {
                            org.apache.log4j.Logger.getRootLogger().debug(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Previous tasks aren't finished. Waiting " + waitTime / 1000 + " seconds");
                            _listAppender.addLine(_currentTasksQueue.getServer().getShortName() + " - " + _currentTask + " : Previous tasks aren't finished. Waiting " + waitTime / 1000 + " seconds");
                            _mainQueue.wait(waitTime);
                            // we have waited the minimum required time, so it is ok to begin
                        }
                    }
                    catch (InterruptedException e)
                    {
                        evaluateOK = false;
                    }
                }
            }
        }
        return evaluateOK;
    }

    public boolean userDidRemoveAll()
    {
        if (_currentTasksQueue != null)
            return _lastRemoveAll > _currentTasksQueue.getCreationTime() ? true : false;
        else
            return false;

    }

    public void removeAllServers()
    {
        _lastRemoveAll = System.currentTimeMillis();
        synchronized (_mainQueue)
        {
            _mainQueue.removeAll();
        }
    }

    public static void pause()
    {
        _mainQueue.pause();
        org.apache.log4j.Logger.getRootLogger().debug("The next startup task will be paused");
        _listAppender.addLine("The next startup task will be paused");
    }

    public static boolean isPaused()
    {
        return _mainQueue.isPaused();
    }

    public static void unpause()
    {
        _mainQueue.unpause();
        if (_mainQueue.isEmpty() && _currentTasksQueue == null)
            ; //don't do anything
        else
        {
            org.apache.log4j.Logger.getRootLogger().debug("Resuming");
            _listAppender.addLine("Resuming");
        }
    }

    public static boolean isIdle()
    {
        return _isIdle;
    }
}
