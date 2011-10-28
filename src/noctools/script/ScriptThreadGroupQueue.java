/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.script;
import noctools.endpoint.Server;
import noctools.util.Queue;

/**
 * This class will represent the queue of ScriptThreads of a single script waiting to be executed
 * @author Yuri
 */
public class ScriptThreadGroupQueue extends Thread
{
    private Script _script;
    Queue<ScriptThreadGroup> _threadGroupQueue;

    public ScriptThreadGroupQueue(Script script)
    {
        super("'" + script.getDescription() + "' S.T.G.Q.");
        _script = script;
        _threadGroupQueue = new Queue<ScriptThreadGroup>();
        this.setDaemon(true);
        org.apache.log4j.Logger.getRootLogger().debug("Started ScriptThreadGroupQueue for \"" + _script.getDescription() + "\"");
    }


    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                if (_script.getWorkInParalel())
                {
                    while (!_threadGroupQueue.isEmpty())
                    {
                        _threadGroupQueue.pop().start();
                    }
                }
                else
                {
                    ScriptThreadGroup activeTreadGroup;
                    while (!_threadGroupQueue.isEmpty())
                    {
                        activeTreadGroup = _threadGroupQueue.pop();
                        activeTreadGroup.start();
                        try
                        {
                            Thread.sleep(100);
                            activeTreadGroup.join();
                        }
                        catch (InterruptedException ex)
                        {
                            org.apache.log4j.Logger.getRootLogger().warn("ScriptThreadGroupQueue for \"" + _script.getDescription() + "\" was interrupted!");
                            activeTreadGroup.interrupt();
                            synchronized (_threadGroupQueue)
                            {
                                _threadGroupQueue.clear();
                            }
                        }
                    }
                }
                
                synchronized (_threadGroupQueue)
                {
                    if (_threadGroupQueue.isEmpty())
                    {
                        _threadGroupQueue.wait();
                    }
                }
            }
            catch (InterruptedException ex)
            {
                if (ex.getMessage() != null)
                    org.apache.log4j.Logger.getRootLogger().error(ex.getMessage());
                ex.printStackTrace();
            }

        }
    }


    /**
     * Creates a new ScriptThreadGroup with the custom script of this queue, and pushes it to the queue
     */
    public synchronized void push()
    {
        synchronized (_threadGroupQueue)
        {
            Script script = new Script(_script);
            script.askForArguments();
            _threadGroupQueue.push(new ScriptThreadGroup(script));
            org.apache.log4j.Logger.getRootLogger().info("Execution of script \"" + _script.getDescription() + "\" was added to its queue"  );
            _threadGroupQueue.notifyAll();
        }
    }


    /**
     * Creates a new ScriptThreadGroup for each given server with the server script, and pushes it to the queue
     * @param servers
     */
    public void push(Server[] servers)
    {
        if (servers != null && servers.length > 0)
        {
            Script script = new Script(_script);
            script.askForArguments();

            String serversList = "";
            for (Server server : servers)
                serversList += server.getShortName() + " ";
            org.apache.log4j.Logger.getRootLogger().info("Adding execution of script \"" + _script.getDescription() + "\" for servers: " + serversList + "to the queue");

            for (Server server : servers)
                synchronized (_threadGroupQueue)
                {
                    ScriptThreadGroup scriptThreadGroup = new ScriptThreadGroup(server, script);
                    _threadGroupQueue.push(scriptThreadGroup);
                    _threadGroupQueue.notifyAll();
                }
        }
    }


    public Script getScript()
    {
        return _script;
    }

    public void setScript(Script script)
    {
        _script = script;
    }
}
