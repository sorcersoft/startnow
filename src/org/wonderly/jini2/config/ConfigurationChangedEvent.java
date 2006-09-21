package org.wonderly.jini2.config;

import net.jini.core.event.*;
import java.rmi.*;
import java.io.*;

/**
 *  This class is passed in the notify(RemoteEvent) call
 *  from the ManagedConfiguration.addConfigurationListener()
 *  setup.  
 *  @author Gregg Wonderly - gregg.wonderly@pobox.com
 */
public class ConfigurationChangedEvent extends RemoteEvent {
	/** Configuration was updated or initialized */
	public static final int CONF_UPDATED = 1;
	/** Configuration was removed */
	public static final int CONF_REMOVED = 2;

	/**
	 *  @param id the id associated with the updated configuration
	 *  @param type one of the CONF_* values defined here
	 *  @param seqno the sequence number of the change
	 *  @throws IOException if there is a Marshalling error
	 */
	public ConfigurationChangedEvent( ConfigurableId id, 
			int type, int seqno ) throws IOException {
		super( id.toString(), type, seqno,
			new MarshalledObject( id ) );
	}
	
	/**
	 *  Returns the ConfigurableId associated with the
	 *  configuration that has changed
	 *  @throws IOException if there is a unmarshalling error
	 *  @throws ClassNotFoundException if there is an unmarshalling error
	 */
	public ConfigurableId getConfigurableId() 
			throws IOException, ClassNotFoundException {
		return (ConfigurableId)getRegistrationObject().get();
	}
}