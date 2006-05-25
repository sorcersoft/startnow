/*
 * JMXConnectorAccess.java
 *
 * Created on March 28, 2006, 11:28 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.wonderly.jini2.jmx;

import java.util.Map;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import javax.management.remote.JMXConnector;

/**
 *
 * @author gregg
 */
public interface JMXConnectorAccess {
	/**
	 *  Describes the type of credentials that should be provided.  Typically
	 *  this is just informational to allow a user to know that they can meet
	 *  the requirements for authentication.
	 */
	public String getCredentialDescription();
	/**
	 *  Use this method to gain access to the map prior to calling getConnector()
	 *  so that it can be augmented with any additional parameters.
	 */
	public Map<String,?> getEnvironment() throws IOException;

	public void setEnvironment( Map<String,Serializable> env ) throws IOException;

	/**
	 *  Creates a JMXConnector for local use.  The environment returned by
	 *  {@link #getEnvironment} is used to create the connector.  That environment
	 *  can be augmented as needed to complete the required information for making
	 *  the connection back to the server.
	 *  <p>
	 *  It is anticipated that the classloader for this JMXConnector instance will
	 *  contain any additional classes needed to resolve the JMXConnector.  This
	 *  method sets the context class loader to the class loader of the current
	 *  class.
	 */
	public JMXConnector getConnector() throws MalformedURLException, IOException;
	/**
	 *  Creates a JMXConnector for local use.  The environment returned by
	 *  {@link #getEnvironment} is used to create the connector.  That environment
	 *  can be augmented as needed to complete the required information for making
	 *  the connection back to the server.
	 *  <p>
	 *  @param ctxld the context class loader to use for resolving the connector.
	 */
	public JMXConnector getConnector( ClassLoader ctxld ) throws MalformedURLException, IOException;
}
