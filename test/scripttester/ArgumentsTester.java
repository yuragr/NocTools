/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scripttester;
import org.junit.*;
import static org.junit.Assert.*;
/**
 *
 * @author Yuri
 */
public class ArgumentsTester
{
    @Test
    public void nullString()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments(null).size() == 0);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated(null));
    }

    @Test
    public void emptyString()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments("").size() == 0);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated(""));
    }

    @Test
    public void oneArgument()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments("%S").size() == 1);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated("%S"));
    }

    @Test
    public void twoArguments()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments("%S noctools").size() == 2);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated("%S noctools"));
    }

    @Test
    public void threeArguments()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments("%S noctools fsdkghj").size() == 3);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated("%S noctools fsdkghj"));
    }

    @Test
    public void fourArguments()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments("%S noctools {}Ddsg fffffff").size() == 4);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated("%S noctools {}Ddsg fffffff"));
    }

    @Test
    public void fiveArguments()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments("%S noctools ddsfs %IP yes").size() == 5);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated("%S noctools ddsfs %IP yes"));
    }

    @Test
    public void customArguments1()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments("%U[please enter a number]").size() == 1);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated("%U[please enter a number]"));
    }

    @Test
    public void customArguments2()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments("%U[please enter a number] %U[please enter a number]").size() == 2);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated("%U[please enter a number] %U[please enter a number]"));
    }

    @Test
    public void customArguments3WithSpaces()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments("%U[please       enter a number]        %U[please enter a number] %U[please enter a number]").size() == 3);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated("%U[please       enter a number]        %U[please enter a number] %U[please enter a number]"));
    }

    @Test
    public void noArguments1()
    {
        String testString = "%U[]";
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).size() == 1);
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).getFirst().equalsIgnoreCase(testString));
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void noArguments2()
    {
        String testString = "%U[\"\"]";
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).size() == 1);
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).getFirst().equalsIgnoreCase(testString));
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void noArguments3()
    {
        String testString = "[\"\"]";
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).size() == 1);
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).getFirst().equalsIgnoreCase(testString));
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void noArguments1WithSpace()
    {
        String testString = "%U[ ]";
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).size() == 1);
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).getFirst().equalsIgnoreCase(testString));
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void noArguments4()
    {
        assertTrue(noctools.script.ScriptEditor.extractArguments("%U[] %U[please enter a number]").size() == 2);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated("%U[] %U[please enter a number]"));
    }



    @Test
    public void longArguments1()
    {
        String testString = "[sdljgsd kdjshgs sdjhk]";
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).size() == 1);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void longArguments2()
    {
        String testString = "[sdljgsd kdjshgs sdjhk] [dsjgs ds gd  d d d d d]";
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).size() == 2);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void longArguments3()
    {
        String testString = "[sdl j  gsd k    djshgs sdjhk] [s] [dsjgs ds gd  d d d d d]";
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).size() == 3);
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void longArgumentsPath()
    {
        String testString = "[\"c:\\Documents and settings\\users\\\"]";
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).size() == 1);
        assertTrue(noctools.script.ScriptEditor.extractArguments(testString).getFirst().equalsIgnoreCase(testString));
        System.out.println(noctools.script.ScriptEditor.extractArguments(testString).getFirst());
        assertTrue(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void notValidate1()
    {
        String testString = "[ ksdhgksdg ";
        assertFalse(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void notValidate2()
    {
        String testString = " ksdhgksdg [";
        assertFalse(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void notValidate3()
    {
        String testString = "]ksdhgksdg[ ";
        assertFalse(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }

    @Test
    public void notValidate4()
    {
        String testString = "[ ksdhgksdg  sdjkghsd jks [ghksdhg k gksghk] s ] ";
        assertFalse(noctools.script.ScriptEditor.argumentIsValidated(testString));
    }
}
