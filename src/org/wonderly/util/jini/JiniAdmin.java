package org.wonderly.util.jini;

import java.net.InetAddress;
import java.rmi.*;
import java.lang.reflect.*;
import java.rmi.server.*;

import net.jini.discovery.*;
import net.jini.admin.*;
import com.sun.jini.admin.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.entry.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.lease.*;
import net.jini.core.lease.*;
import com.sun.jini.lookup.entry.LookupAttributes;
import java.util.logging.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;
import org.wonderly.log.*;

/**
 *  This class provides a remoteable class that implements the Jini administration interfaces
 *  which allow the inheriting class to be treated as an Administrable service.  The DestroyAdmin
 *  interface is enabled via the constructor argument or the use of the setDestroyAdminEnabled()
 *  method.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class JiniAdmin implements JiniAdmins,Remote {
	protected PersistenceIO io;
	private boolean destroy;
	protected JoinManager mgr;
	protected LookupDiscoveryManager disco;
	protected Logger log;
	protected Remote exportedObj;

	public String toString() {
		return "JiniAdmin";
	}

	/**
	 *  @return getClass().getName().substring( getClass().getName().lastIndexOf('.')+1 );
	 */
	public String getName() {
		String nm = getClass().getName();
		int nmi = nm.lastIndexOf('.');
		return nm.substring(nmi+1);
	}

	/**
	 *  @return getClass().getName().substring(0, getClass().getName().lastIndexOf('.') );
	 */
	public String getPackage() {
		String nm = getClass().getName();
		int nmi = nm.lastIndexOf('.');
		return nm.substring(0,nmi);
	}

	/**
	 *  This method should export the passed object using an appropriate
	 *  exporter implementation.  The default here is to use
	 *  <code>UnicastRemoteObject.exportObject(Remote)</code>.
	 */
	public Remote exportObject( Object obj ) throws RemoteException {
		return UnicastRemoteObject.exportObject(this);
	}

	/**
	 *  Returns the actual exported object, the
	 *  default is to call <code>exportObject(this)</code>,
	 *  and store the result in <code>exportedObj</code>.
	 */
	public Remote getExportedObject() throws RemoteException {
		if( exportedObj != null )
			return exportedObj;
		return exportedObj = exportObject(this);
	}

	/**
	 *  The exported getStorageLocation from StorageLocationAdmin.
	 *  Delegates to PersistenceIO.getFile()
	 */
	public String getStorageLocation() {
		return io.getFile();
	}
	
	/**
	 *  The exported setStorageLocation from StorageLocationAdmin.
	 *  Delegates to PersistenceIO.setFile()
	 */
	 public void setStorageLocation( String name ) throws IOException {
	 	log.config( "update Storage Location to: "+name );
		PersistentData data = null;
		try {
			data = io.readState();
		} catch( ClassNotFoundException ex ) {
			log.log( Level.SEVERE, ex.toString(), ex );
			data = new PersistentData();
		}
		io.setFile( name );
		io.writeState(data);
	}

	/** Returns the default log instance name getClass().getName() */
	protected String logInstanceName() {
		return getClass().getName();
	}

	/**
	 *  Constructs an instance with the passed parameters.
	 *  Delegates to JiniAdmin( mgr, io, dm, false );
	 */
	public JiniAdmin( JoinManager mgr, PersistenceIO io,
			LookupDiscoveryManager dm ) throws IOException {
		this( mgr, io, dm, false );
	}
	
	/**
	 *  Constructs an instance using the passed parameters to control
	 *  interactions with Jini and the filesystem and JVM.
	 */
	public JiniAdmin( JoinManager mgr, PersistenceIO io,
			LookupDiscoveryManager dm, boolean enableDestroy ) throws IOException {
		this.io = io;
		this.mgr = mgr;
		this.destroy = enableDestroy;
		this.disco = dm;
		log = Logger.getLogger(logInstanceName());
		log.config("construct: mgr: "+mgr+", io: "+io+
			", dm: "+dm+", destroy: "+enableDestroy );
//		if( log == null ) {
//			log = new StdoutLogger(logInstanceName(),null);
//			log.setLevel( Level.ALL );
//		}
		try {
			PersistentData data = null;
			try {
				if( io != null ) {
					log.fine("Reading state from: "+io );
					data = io.readState();
				}
			} catch( IOException ex ) {
//				log.reportException(toString(),ex);
			}
			if( data == null ) {
				data = new PersistentData();
				log.fine("constructed default/initial PersistentData");
			}
			// If null, force to public groups only.
			if( data.groups == null ) {
				log.fine("initializing default groups to all groups");
				data.groups = new String[]{""};
			}

			dm.setGroups( data.groups );
		} catch( Exception ex ) {
			log.throwing( getClass().getName(), "State Not Loaded", ex );
//			log.reportException(toString(),ex);
		}
	}

	/**
	 *  Allows the JoinManager to be explicitly changed
	 */
	public void setJoinManager( JoinManager mgr ) {
		this.mgr = mgr;
	}

	/**
	 *  Allows the application to specify whether remote destroy is
	 *  enabled
	 */
	public void setDestroyAdminEnabled( boolean how ) {
		destroy = how;
	}

	/**
	 *  The exported destroy from DestroyAdmin
	 */ 
	public void destroy() throws RemoteException {
		log.warning("destroy call, enabled: "+destroy);
		if( destroy ) {
			new Thread() {
				public void run() {
					try { Thread.sleep(3); } catch( InterruptedException ex ) {}
					log.log(Level.INFO, "Service Stop requested: "+new Date() );
					System.exit(1);
				}
			}.start();
		} else {
			throw new RemoteException( "Service Destroy Not Enabled, Operation Ignored" );
		}
	}

	/**
	 *  The exported modifyLookupAttributes from JoinAdmin
	 */
	public void modifyLookupAttributes( Entry[] templ, Entry[] vals ) throws RemoteException {
		log.config("modifying Attributes, "+templ.length+" and "+vals.length );
		try {
			PersistentData data = io.readState();
			dumpItems( "before modify", log, data.attrs);
			mgr.modifyAttributes( templ, vals, true );
			if( data == null )
				throw new NullPointerException("No Data State returned from readState()");
			if( mgr == null )
				throw new NullPointerException( "No Persistence manager active");
			data.attrs = mgr.getAttributes();
			dumpItems("after manager modify", log, data.attrs );
			io.writeState( data );
		} catch( ClassNotFoundException ex ) {
			log.throwing( getClass().getName(), "modifyLookupAttributes", ex);
			ServerException exx = new ServerException( ex.toString(), ex );
			throw exx;
		} catch( IOException ex ) {
			log.throwing( getClass().getName(), "modifyLookupAttributes", ex);
			ServerException exx = new ServerException( ex.toString(), ex );
			throw exx;
		} catch( Exception ex ) {
			log.throwing( getClass().getName(), "modifyLookupAttributes", ex);
			ServerException exx = new ServerException( ex.toString(), ex );
			throw exx;
		}
	}
	protected static void dumpItems( String msg, Logger log, Entry[] items ) {
		// Do a debugging dump of all Entry objects we 
		// are using/advertising
		if( log.isLoggable( Level.FINE ) ) {
			for( int i = 0; i < items.length; ++i ) {
				log.finer( msg+": entry["+i+"]: "+items[i].getClass().getName() );
				Class c = items[i].getClass();
				Field f[] = c.getDeclaredFields();
				for( int j = 0; j < f.length;++j ) {
					if( (f[j].getModifiers() & Modifier.PUBLIC) == 0 )
						continue;
					try {
						log.finest("         "+f[j].getName()+
							": "+f[j].get(items[i])+"");
					} catch( IllegalAccessException ex ) {
						log.throwing( JiniAdmin.class.getName(),
							"startService", ex );
						log.fine("        "+f[j].getName()+": <unknown>");
					}
				}
			}
		}
	}


	/**
	 *  The exported addLookupAttributes from JoinAdmin
	 */
	public void addLookupAttributes( Entry[] attrs ) throws RemoteException {
		log.config("adding attributes: "+attrs.length );
		try {
			PersistentData data = io.readState();
			dumpItems( "read state", log, data.attrs );
			mgr.addAttributes( attrs, true );
			Entry[] newa = mgr.getAttributes();
			dumpItems( "after add", log, newa );
		log.fine("merging arrays");
			Vector v = mergeArrays( data.attrs, attrs );
			data.attrs = new Entry[v.size()];
			v.copyInto( data.attrs );
			dumpItems( "merged", log, data.attrs );
			log.fine("using "+io+" to write state("+data+")");
			io.writeState( data );
		} catch( ClassNotFoundException ex ) {
			throw new ServerException(ex.toString());
		} catch( IOException ex ) {
			throw new ServerException(ex.toString());
		}
	}

	/**
	 *  The exported addLookupGroups from JoinAdmin
	 */
	public void addLookupGroups( String[] groups ) throws RemoteException {
		log.config("adding lookup groups");
		StringBuffer l = new StringBuffer();
		l.append("Add lookup groups: ");
		for( int i = 0; i< groups.length; ++i ) {
			if( i > 0 )
				l.append(", ");
			l.append(groups[i] );
		}
		log.log(Level.FINE, l.toString() );
		try {
			PersistentData data = io.readState();
			disco.addGroups( groups );
			Vector v = mergeArrays( data.groups, groups );
			data.groups = new String[v.size()];
			v.copyInto( data.groups );
			io.writeState( data );
		} catch( ClassNotFoundException ex ) {
			throw new RemoteException(ex.toString());
		} catch( IOException ex ) {
			throw new RemoteException(ex.toString());
		}
	}
	
	/** Create a Vector out merging the two arrays */
	private Vector<Object> mergeArrays( Object[]dt, Object[]ar ) {
		Vector<Object> v = new Vector<Object>();

		for( int i = 0; i < dt.length; ++i ) {
			log.fine("mergeArrays: including base: "+dt[i]);
			v.addElement( dt[i] );
		}

		for( int i = 0; i < ar.length; ++i ) {
			if( v.contains( ar[i] ) == false ) {
				log.fine("mergeArrays: adding missing: "+ar[i]);
				v.addElement( ar[i] );
			}
		}
		return v;
	}

	/**
	 *  The exported setLookupGroups from JoinAdmin
	 */
	public void setLookupGroups( String[] groups ) throws RemoteException {
		StringBuffer l = new StringBuffer();
		l.append("Set lookup groups:");
		for( int i = 0; i< groups.length; ++i ) {
			l.append(" "+groups[i] );
		}
		log.log(Level.CONFIG, l.toString() );
		try {
			PersistentData data = io.readState();
			data.groups = groups;
			io.writeState( data );
			disco.setGroups(data.groups);
		} catch( ClassNotFoundException ex ) {
			throw new RemoteException(ex.toString());
		} catch( IOException ex ) {
			throw new RemoteException(ex.toString());
		}
	}

	/**
	 *  The exported removeLookupGroups from JoinAdmin
	 */
	public void removeLookupGroups( String[] groups ) throws RemoteException {
		StringBuffer l = new StringBuffer();
		l.append("remove lookup groups:");
		for( int i = 0; i< groups.length; ++i ) {
			l.append(" "+groups[i] );
		}
		log.log(Level.CONFIG,l.toString());
		try {
			PersistentData data = io.readState();
			disco.removeGroups( groups );
			data.groups = disco.getGroups();
			io.writeState( data );
		} catch( ClassNotFoundException ex ) {
//			log.reportException(toString(),ex);
			throw new RemoteException(ex.toString());
		} catch( IOException ex ) {
//			log.reportException(toString(),ex);
			throw new RemoteException(ex.toString());
		}
	}

	/**
	 *  The exported addLookupLocators from JoinAdmin
	 */
	public void addLookupLocators( LookupLocator[] locs ) throws RemoteException {
		log.config("adding lookup locators");
		try {
			PersistentData data = io.readState();
			disco.addLocators( locs );
			Vector v = mergeArrays( data.locators, locs );
			data.locators = new LookupLocator[ v.size() ];
			v.copyInto( data.locators );
			io.writeState( data );
		} catch( ClassNotFoundException ex ) {
			log.throwing( getClass().getName(), "addLookupLocators", ex);
			throw new RemoteException(ex.toString());
		} catch( IOException ex ) {
			log.throwing( getClass().getName(), "addLookupLocators", ex);
			throw new RemoteException(ex.toString());
		}
	}

	/**
	 *  The exported setLookupLocators from JoinAdmin
	 */
	public void setLookupLocators( LookupLocator[] locs ) throws RemoteException {
		log.config("set lookup locators");
		try {
			PersistentData data = io.readState();
			disco.setLocators( locs );
			data.locators = locs;
			io.writeState( data );
		} catch( ClassNotFoundException ex ) {
			log.throwing( getClass().getName(), "setLookupLocators", ex);
			throw new RemoteException(ex.toString());
		} catch( IOException ex ) {
			log.throwing( getClass().getName(), "setLookupLocators", ex);
			throw new RemoteException(ex.toString());
		}
	}

	/**
	 *  The exported removeLookupLocators from JoinAdmin
	 */
	public void removeLookupLocators( LookupLocator[] locs ) throws RemoteException {
		log.config("remove lookup locators");
		try {
			PersistentData data = io.readState();
			disco.removeLocators( locs );
			data.locators = locs;
			io.writeState( data );
		} catch( ClassNotFoundException ex ) {
			log.throwing( getClass().getName(), "removeLookupLocators", ex);
			throw new RemoteException(ex.toString());
		} catch( IOException ex ) {
			log.throwing( getClass().getName(), "removeLookupLocators", ex);
			throw new RemoteException(ex.toString());
		}
	}

	/**
	 *  Uses the exported getLookupAttributes from JoinAdmin.
	 */
	public Entry[] getLookupAttributes() throws RemoteException {
		return mgr.getAttributes();
	}

	/**
	 *  The exported getLookupGroups from JoinAdmin
	 */
	public String[] getLookupGroups() throws RemoteException {
		return disco.getGroups();
	}

	/**
	 *  The exported getLookupLocators from JoinAdmin
	 */
	public LookupLocator[] getLookupLocators() throws RemoteException {
		return disco.getLocators();
	}
}