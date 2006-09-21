package org.wonderly.jini2.config;

import net.jini.config.*;
import java.rmi.*;
import java.io.*;
import java.util.*;
import net.jini.core.event.*;

/**
 *  This interface describes the operations a service
 *  should provide to support remote Configuration
 *  management.  It provides methods to get and update
 *  ConfigurationSet instances associate with a particular
 *  ConfigurableId.  It also provides a listener
 *  interface to allow RemoteEvents to be fired to tell
 *  other entities of changes in the ConfigurationSet
 */
public interface ManagedConfiguration extends Remote {
	public void storeConfiguration( ConfigurationSet conf ) throws IOException;
	public void removeConfiguration( ConfigurableId id ) throws IOException;
	public ConfigurationSet getConfiguration( ConfigurableId id ) throws IOException;
	public void addConfigurationListener( ConfigurableId id, RemoteEventListener lis ) throws IOException;
	public List getConfigurationKeys() throws IOException;
	/**
	 *  @return true if lister was found and removed, false if not found
	 */
	public boolean removeConfigurationListener( ConfigurableId id, RemoteEventListener lis ) throws IOException;
}