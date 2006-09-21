package org.wonderly.jini2;

import java.io.*;
import java.rmi.*;
import org.wonderly.jini.serviceui.*;

/**
 *  This interface defines access to a codebase server which
 *  allows a service to get the httpmd: URL information for
 *  accessing a particular jar file.
 */
public interface HttpServerAccess extends RemoteAdministrable {
//	/**
//	 *  If you know the sum and the path, you can ask the server
//	 *  for the stream to the file with getFile().
//	 *  @param path URL path
//	 *  @param sum the sum that we know
//	 */
//  This will have to be a local method in a smart proxy...
//	public InputStream getFile( String path, String sum ) throws java.io.IOException;
	/**
	 *  If you know the sum and the path, you can ask the server
	 *  if it can serve that version of the indicated path
	 *  @param path URL path.
	 *  @param type the type of digest
	 *  @param digest the digest that we think the file should have
	 */
	public boolean canServe( String path, String type, String digest ) throws java.io.IOException;
	/**
	 *  If you just know the path, you can ask the server for
	 *  the sum of the version that it has.
	 *  @param path URL path.
	 *  @param type the type of sum desired as in "md5"
	 */
	public String sumFor( String path, String type ) throws IOException;
}