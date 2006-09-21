package org.wonderly.jini2.config;

import java.io.*;
import java.util.*;

/**
 *  This interface is implemented by classes that 
 *  provide a mechanism for persisting {@link ConfigurationSet}
 *  instances. 
 */
public interface ConfigurationStoreProvider {
	/** Store a ConfigurationSet */
	public void storeConfigurationSet( ConfigurationSet conf ) throws IOException;
	/**
	 *  Retrieve the indicated ConfigurationSet
	 *  @return null if not found
	 */
	public ConfigurationSet retrieveConfigurationSet( ConfigurableId id ) throws IOException;
	/**
	 *  Remove the indicated configuration set from the
	 *  store, and return it.
	 *  @return null if no such entry was found
	 */
	public ConfigurationSet deleteConfigurationSet( ConfigurableId id ) throws IOException;
	
	/**
	 *  Get the List of ConfigurableId instances that represent
	 *  the ConfigurationSets that are stored
	 */
	public List<ConfigurableId> getConfigurationSetKeys() throws IOException;
	
	/** 
	 *  Get the count of ConfigurationSet entries
	 *  that are stored
	 */
	public int getCount() throws IOException;
}