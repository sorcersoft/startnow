package org.wonderly.jini.serviceui;

import javax.swing.*;
import java.util.Vector;
import java.util.Enumeration;
import javax.swing.event.*;

/**
 *  This is an extension of the AbstractListModel that
 *  uses a vector, like default list model, but allows
 *  direct access to the vect reference so that you
 *  can simply do <code>setContents(Vector&lt;T&gt;)</code> to change
 *  the model data.
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class VectorListModel<T> extends AbstractListModel
{
    protected Vector<T> vect = new Vector<T>();

    public int getSize() {
		return vect.size();
    }

    public T getElementAt(int index) {
    	if( index >= vect.size() )
    		return null;
		return vect.elementAt(index);
    }

    public void trimToSize() {
		vect.trimToSize();
    }

    public void ensureCapacity(int minCapacity) {
		vect.ensureCapacity(minCapacity);
    }

    public void setSize(int newSize) {
		int oldSize = vect.size();
		vect.setSize(newSize);
		if (oldSize > newSize) {
		    fireIntervalRemoved(this, newSize, oldSize-1);
		}
		else if (oldSize < newSize) {
		    fireIntervalAdded(this, oldSize, newSize-1);
		}
    }

    public void copyInto(Object anArray[]) {
		vect.copyInto(anArray);
    }

	public void setContents( Vector<T> v ) {
		vect = v;
		fireContentsChanged( this, 0, v.size()-1);
	}
	
	public Vector<T> getContents() {
		return vect;
	}

    public int capacity() {
		return vect.capacity();
    }

    public int size() {
		return vect.size();
    }

    public boolean isEmpty() {
		return vect.isEmpty();
    }

    public int indexOf(T elem) {
		return vect.indexOf(elem);
    }

    public int indexOf(T elem, int index) {
		return vect.indexOf(elem, index);
    }

    public int lastIndexOf(T elem) {
		return vect.lastIndexOf(elem);
    }

    public int lastIndexOf(T elem, int index) {
		return vect.lastIndexOf(elem, index);
    }
   
    public Enumeration elements() {
		return vect.elements();
    }

    public boolean contains(T elem) {
		return vect.contains(elem);
    }

    public T elementAt(int index) {
		return vect.elementAt(index);
    }

    public void setElementAt(T obj, int index) {
		vect.setElementAt(obj, index);
		fireContentsChanged(this, index, index);
    }

    public T firstElement() {
		return vect.firstElement();
    }

    public T lastElement() {
		return vect.lastElement();
    }

    public void removeElementAt(int index) {
		vect.removeElementAt(index);
		fireIntervalRemoved(this, index, index);
    }

    public void insertElementAt(T obj, int index) {
		vect.insertElementAt(obj, index);
		fireIntervalAdded(this, index, index);
    }

    public void addElement(T obj) {
		int index = vect.size();
		vect.addElement(obj);
		fireIntervalAdded(this, index, index);
    }

    public boolean removeElement(T obj) {
		int index = indexOf(obj);
		boolean rv = vect.removeElement(obj);
		if (index > 0) {
		    fireIntervalRemoved(this, index, index);
		}
		return rv;
    }
   
    public void removeAllElements() {
		int index1 = vect.size()-1;
		vect.removeAllElements();
		if (index1 >= 0) {
		    fireIntervalRemoved(this, 0, index1);
		}
    }


    public String toString() {
		return vect.toString();
    }

    public Object[] toArray() {
		Object[] rv = new Object[vect.size()];
		vect.copyInto(rv);
		return rv;
    }

    public T get(int index) {
		return vect.elementAt(index);
    }

    public T set(int index, T element) {
		T rv = vect.elementAt(index);
		vect.setElementAt(element, index);
		fireContentsChanged(this, index, index);
		return rv;
    }

    public void add(int index, T element) {
		vect.insertElementAt(element, index);
		fireIntervalAdded(this, index, index);
    }

    public T remove(int index) {
		T rv = vect.elementAt(index);
		vect.removeElementAt(index);
		fireIntervalRemoved(this, index, index);
		return rv;
    }

    public void clear() {
		int index1 = vect.size()-1;
		vect.removeAllElements();
		if (index1 >= 0) {
		    fireIntervalRemoved(this, 0, index1);
		}
    }

    public void removeRange(int fromIndex, int toIndex) {
		for(int i = toIndex; i >= fromIndex; i--) {
		    vect.removeElementAt(i);
		}
		fireIntervalRemoved(this, fromIndex, toIndex);
    }
}
