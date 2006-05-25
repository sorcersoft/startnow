package org.wonderly.util.jini;

import java.io.*;

/**
 *  A simple class to wrap the IO mechanisms for serialization of
 *  Jini related state data.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */

public interface PersistenceIO {
	public PersistentData readState() throws IOException,ClassNotFoundException;
	public void writeState( PersistentData data ) throws IOException;
	public void setFile( String name ) throws IOException;
	public String getFile();
}
