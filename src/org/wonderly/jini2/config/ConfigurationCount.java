package org.wonderly.jini2.config;

import net.jini.core.entry.*;

/**
 *  This simple entry keeps track of how many
 *  different ConfigurationSet instances a
 *  particular ManagedConfiguration instance
 *  has stored.  A service should be updated each time
 *  that a ConfigurationSet is added, removed or
 *  otherwise updated so that remote interest in
 *  those changes can act based on the service
 *  state change, instead of having to use another
 *  event type.
 */
public class ConfigurationCount implements Entry {
	/** The count of ConfigurationSets stored */
	public Integer count;
	/** Construct a new instance with the indicated count */
	public ConfigurationCount( int val ) {
		count = new Integer(val);
	}
	/** noargs constructor for serialization and reggie */
	public ConfigurationCount() {
	}
}