package org.wonderly.util.jini2;

import java.io.*;

public interface AppPersistenceIO {
	/**
	 *  A check to see if writes should occur.  Typically if
	 *  a read exception occurs, this would throw an IOException
	 *  from that point on to keep writes from occuring.
	 */
	public boolean isWriteable() throws IOException;
	public void writeAppState( ObjectOutputStream os ) throws IOException;
	public void readAppState( ObjectInputStream is ) throws IOException, ClassNotFoundException;
}