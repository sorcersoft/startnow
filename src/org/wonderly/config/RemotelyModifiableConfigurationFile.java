package org.wonderly.config;

import java.rmi.*;
import net.jini.config.*;

/**
 *  This interface defines the method used to provide a dynamic,
 *  remote interface to configuration changes.
 *
 *  @author Gregg Wonderly - gregg@wonderly.org
 */
public interface RemotelyModifiableConfigurationFile extends Remote {
	public void setEntry( String comp, String name, Object val, String cfgText ) throws RemoteException, ConfigurationException;
}