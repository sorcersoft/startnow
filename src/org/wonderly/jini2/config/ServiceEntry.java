package org.wonderly.jini2.config;

import net.jini.lookup.entry.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.admin.*;
import java.rmi.*;
import java.io.*;
import java.util.logging.*;

/**
 *  This class is used to represent each service that is
 *  found to have a ConfigurableId Entry in its registration
 * 
 */
class ServiceEntry {
	protected String name;
	protected ServiceItem itm;
	protected boolean checkAdmin;
	protected boolean isAdmin;
	static Logger log = Logger.getLogger( JiniConfigEditor.class.getName() );

	public String getName() {
		return name;
	}
	public boolean isConfigurable() {
		for( int i = 0; i< itm.attributeSets.length; ++i ) {
			if( itm.attributeSets[i] instanceof ConfigurableId )
				return true;
		}
		return false;
	}

	public Entry getEntry( Class cls ) {
		for( int i = 0; i< itm.attributeSets.length; ++i ) {
			if( itm.attributeSets[i].getClass() == cls )
				return itm.attributeSets[i];
		}
		return null;
	}

	public static String getName( ServiceItem item ) {
		Entry arr[] = item.attributeSets;
		String name = null;
		for( int i = 0; i < arr.length; ++i ) {
			if( arr[i] == null )
				continue;
			log.finest( "Entry["+i+"]: "+arr[i].getClass().getName() );
			if( arr[i] instanceof Name ) {
				name = ((Name)arr[i]).name;
			} else if( arr[i] instanceof ServiceInfo && name == null ) {
				name = ((ServiceInfo)arr[i]).name;
			}
		}
		if( name == null )
			name = item.serviceID.toString();

		return name;
	}

	public void updateItem( ServiceItem item ) {
		name = getName(item);
		itm = item;
		checkAdmin = false;
		isAdmin = false;
	}
	
	public boolean isAdministrable() throws RemoteException {
		if( checkAdmin ) {
			return isAdmin;
		} else {
			if( itm.service instanceof Administrable == false )
				return false;
			Object a = ((Administrable)itm.service).getAdmin();
			checkAdmin = true;
			return isAdmin = a instanceof JoinAdmin;
		}
	}

	public String toString() {
		return name+" - "+itm.serviceID;
	}

	public boolean equals( Object obj ) {
		if( obj instanceof ServiceEntry == false )
			return false;
		return ((ServiceEntry)obj).itm.serviceID.equals(itm.serviceID);
	}

	public int hashCode() {
		return itm.serviceID.hashCode();
	}

	public ServiceEntry( String name, ServiceItem itm ) {
		this.name = name;
		this.itm = itm;
		try {
			isAdministrable();
		} catch( RemoteException ex ) {
			log.log( Level.SEVERE, ex.toString(), ex );
		}
	}
}