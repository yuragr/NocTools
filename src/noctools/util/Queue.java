/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package noctools.util;
import java.util.LinkedList;

/**
 * All purpose queue
 * @author Yuri
 */
public class Queue <T>
{
    private LinkedList<T> list;

    /**
     * Creates an empty stack (linked list stack)
     */
    public Queue()
    {
        list = new LinkedList <T>();
    }

    /**
     * Returns true if the stack is empty
     * @return true if the stack is empty. Otherwise - false
     */
    public boolean isEmpty()
    {
        return (list.size() == 0);
    }

    public void pushToFirstPlace(T item)
    {
        list.addFirst(item);
    }


    /**
     * Pushes the item to the stack
     * @param item
     */
    public void push(T item)
    {
        list.add(item);
    }

    /**
     * Pops the item from the stack
     * @return the top item
     */
    public T pop()
    {
        T item = list.removeFirst();
        return item;
    }

    /**
     * Returns the top item from the stack without poping it
     * @return the top item
     */
    public T peek()
    {
        return list.get(0);
    }

    /**
     * Returns the size of the stack
     * @return size of the stack
     */
    public int getSize()
    {
        return list.size();
    }

    @Override
    public String toString()
    {
        return list.toString();
    }

    public boolean contains(T item)
    {
        return list.contains(item);
    }

    public void clear()
    {
        while (!list.isEmpty())
            list.removeFirst();
    }

    public Object[] toArray()
    {
        return list.toArray();
    }

}
