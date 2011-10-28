/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.script;

import noctools.endpoint.Server;
import noctools.util.Queue;

/**
 *
 * @author Yuri
 */
public class ScriptThreadGroup extends Thread
{
    private Queue<ScriptThread> _threadQueue = new Queue<ScriptThread>();

    public ScriptThreadGroup(Script script)
    {
        super("'" + script.getDescription() + "' S.T.G.");
        this.setDaemon(true);
        ScriptThread scriptThread = new ScriptThread(script, null);
        pushThread(scriptThread);
    }

    public ScriptThreadGroup(Server server, Script script)
    {
        super("'" + script.getDescription() + "' S.T.G.");
        this.setDaemon(true);
        ScriptThread scriptThread = new ScriptThread(script, server);
        pushThread(scriptThread);
    }

    private void pushThread(ScriptThread scriptThread)
    {
        Script pushedScrtipt = scriptThread.getScript();

        // if this script has a "script before" then recursivly add all the "script before's" you have to add
        if (pushedScrtipt.getScriptBefore() != null && !pushedScrtipt.getScriptBefore().equalsIgnoreCase(""))
        {
            String beforeScriptName = pushedScrtipt.getScriptBefore();
            Script beforeScript = new Script(ScriptsManager.getScript(beforeScriptName));
            beforeScript.askForArguments();
            ScriptThread beforeScriptThread = new ScriptThread(beforeScript, null);
            pushThread(beforeScriptThread);
        }

        // add the wanted script
        _threadQueue.push(scriptThread);

        // if this script has a "script after" then recursivly add all the "script after's" you have to add
        if (pushedScrtipt.getScriptAfter() != null && !pushedScrtipt.getScriptAfter().equalsIgnoreCase(""))
        {
            String afterScriptName = pushedScrtipt.getScriptAfter();
            Script afterScrtipt = new Script(ScriptsManager.getScript(afterScriptName));
            afterScrtipt.askForArguments();
            ScriptThread afterScriptThread = new ScriptThread(afterScrtipt, null);
            pushThread(afterScriptThread);
        }
    }

    @Override
    public void run()
    {
        while (!_threadQueue.isEmpty())
        {
            ScriptThread activeScriptThread = _threadQueue.pop();
            activeScriptThread.start();
            try {
                activeScriptThread.join();
            } catch (InterruptedException e) {
                _threadQueue.clear();
            }
        }
    }

    @Override
    public void finalize()
    {
        _threadQueue.clear();
    }
}
