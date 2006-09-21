package org.wonderly.jini2.config;

import net.jini.config.*;
import java.rmi.*;
import java.io.*;
import net.jini.id.*;
import net.jini.id.*;
import net.jini.core.entry.*;

/**
 *  This class provides the indication of which
 *  configuration a service should be using.  It is
 *  attached to the service registration either at
 *  initial registry, or via a configuration manager
 *  application thereafter.
 */
public class ConfigurableId implements Entry {
	public Uuid uuid;
	public String name;
	private static final long serialVersionUID = 1;

	public String getName() {
		return name;
	}

	public String toString() {
		return uuid.toString();//name; //"ConfigurableId("+uuid+")";
	}
	
	public ConfigurableId() {
		// For serialization
	}

	public ConfigurableId( String name ) {
		this.name = name;
	}

	public ConfigurableId( String name, Uuid id ) {
		this(name);
		uuid = id;
	}

	public boolean equals( Object id ) {
		if( id instanceof ConfigurableId == false ) {
			return false;
		}
		
		return ((ConfigurableId)id).uuid.equals(uuid);
	}

	public int hashCode() {
		return uuid.hashCode();
	}
}