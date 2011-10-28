/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import noctools.util.Queue;
import org.junit.*;
import static org.junit.Assert.*;
/**
 *
 * @author Yuri
 */
public class QueueTester
{
    @Test
    public void Test1()
    {
        Queue<Integer> intQueue = new Queue<Integer>();
        intQueue.push(1);
        intQueue.push(2);
        intQueue.push(3);

        System.out.println(intQueue);

        assertTrue(intQueue.pop() == 1);
        assertTrue(intQueue.pop() == 2);
        assertTrue(intQueue.pop() == 3);

        System.out.println(intQueue);

    }
}
