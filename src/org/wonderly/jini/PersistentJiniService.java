package org.wonderly.jini;

import java.util.*;
import java.security.*;
import java.security.acl.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.lang.reflect.*;

import org.wonderly.util.jini.*;
import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.core.entry.*;
import net.jini.core.lease.*;
import net.jini.lease.*;
import net.jini.admin.*;
import net.jini.entry.*;

import java.util.logging.*;
import org.wonderly.log.*;
import java.awt.Image;
import java.rmi.*;
import java.rmi.server.*;

/**
 *  This class provides a base class for creating persistant Jini services.
 *  This class provides all the necessary things needed to have a persistent
 *  configuration associated with all of the Jini administrative configuration.
 *  It uses the JiniAdmin class to perform all of the standard Jini admin
 *  tasks.  This class does not implement the Adminstrable interface, that
 *  is left to subclasses so that remote access is delegated only if you
 *  wish it to be.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class PersistentJiniService implements Remote {
	protected JoinManager join;
	protected JiniAdmin adm;
	protected String curFile;
	protected LookupDiscoveryManager lm;
	protected Logger log;
	protected PersistentData lastdata;
	protected Remote exportedObj;
	protected String myName;
	
	public String getName() throws IOException,ClassNotFoundException {
		PersistentData data = null;
		try {
			data = readObjState();
		} catch( FileNotFoundException ex ) {
			reportException(ex);
			return myName;
		}

		String ret = null;
		Entry []arr = data.attrs;
		if( arr != null ) {
			for( int i = 0; i < arr.length; ++i ) {
				if( arr[i] instanceof Name ) {
					ret = ((Name)arr[i]).name;
				} else if( arr[i] instanceof ServiceInfo ) {
					// Name overrides ServiceInfo
					if( ret == null )
						ret = ((ServiceInfo)arr[i]).name;
				}
			}
		}
		if( ret == null ) {
			ret = myName;
		}
		return ret;
	}
	

	/**
	 *  @return getClass().getName().substring(0, getClass().getName().lastIndexOf('.') );
	 */
	public String getPackage() {
		String nm = getClass().getName();
		int nmi = nm.lastIndexOf('.');
		return nm.substring(0,nmi);
	}
	
	public PersistentJiniService() throws RemoteException {
	}

	/**
	 *  Get the JiniAdmin instance that we are using to
	 *  manage the administration of this instance.  If you
	 *  want remote access to the JiniAdmin stub, you should
	 *  create a subclass of this which implements an interface
	 *  (Administrable) that has <code>getAdmin()</code> in it.
	 *  Then, you can <code>getAdmin()</code> to get the reference.
	 *  @see JiniAdmin
	 */
	public Object getAdmin() throws RemoteException {
		return adm.getExportedObject();
	}
	
	/**
	 *  Get the current serialized file name we are using
	 */
	public String getObjFile() {
		return curFile;
	}
	
	/**
	 *  Set the name of the file to write to.  The default
	 *  implementation does not allow setting the name of the
	 *  file after it is set using the constructor.
	 */
	public void setObjFile( String name ) throws IOException {
		throw new IOException("Storage Location Not Changable");
	}
	
	/**
	 *   Class to listen for our serviceID if we don't have one yet
	 */
	protected class IDListener implements ServiceIDListener {
		public void serviceIDNotify( ServiceID id ) {
			log.fine("got Service ID: "+id );
			PersistentData state = new PersistentData();
			state.id = id;
			state.attrs = join.getAttributes();
			state.groups = lm.getGroups();
			state.locators = lm.getLocators();
			try {
				writeObjState( state );
			} catch( IOException ex ) {
				reportException(ex);
				join.terminate();
			}
		}
	}
	
	/**
	 *  Report exceptions to the Writer passed to our constructor.
	 */
	protected void reportException( Throwable ex ) {
		if( log != null )
			log.throwing( getClass().getName(), "Exception: "+ex, ex );
		else
			ex.printStackTrace();
	}

	public String logInstanceName() throws IOException {
		try {
			return getPackage()+"."+getName();
		} catch( ClassNotFoundException ex ) {
			IOException ioex = new IOException( "Error Getting Service Name" );
			ioex.initCause( ex );
			throw ioex;
		}
	}

	/**
	 *  Start the service registration process using the passed data.
	 *
	 *  @param name the default initial name for the service.
	 *  @param serFile the file to store the serialized data for service config into.
	 *  @param items the Entry objects used to describe the service initially
	 *  @param groups the groups to look for LUS in
	 *  @param lookupLocators any unicast locators to use
	 *  @param logWriter the stream to send exception messages
	 *         and other progress/error messages to
	 */
	public void startService( String name, 
			String serFile, Entry[]items, 
			String[]groups, String[]lookupLocators,
			Logger logWriter ) throws IOException {

		curFile = new File( serFile ).toString();

		myName = name;
		if( logWriter == null ) {
			log = LogManager.getLogManager().getLogger( logInstanceName() );
			if( log == null ) {
				log = new StdoutLogger( logInstanceName(), null );
				log.setLevel( Level.ALL );
			}
		} else {
			log = logWriter;
		}

		log.fine( "starting Jini registration" );

		if( name == null )
			name = getClass().getName().substring( getClass().getName().lastIndexOf('.') + 1 );

		ServiceID id = null;
		PersistentData data = null;
		try {
			data = readObjState();
			if( data.attrs != null )
				items = data.attrs;
			else
				data.attrs = items;
			id = data.id;
		} catch( ClassNotFoundException ex ) {
			reportException(ex);
			log.fine("Continuing After Exception, Assuming no State Exists");
			data = null;
		} catch( IOException ex ) {
			reportException(ex);
			log.fine("Continuing After Exception, Assuming no State Exists");
			data = null;
		}

		// Make sure there are some default Entry objects
		if( items == null ) {
			items = new Entry[] {
				new Name( name ),
				new Location(),
				new ServiceInfo( name,null,null,null,null,null )
			};
		}

		// If we could not read data, setup default data.
		if( data == null ) {
			data = new PersistentData();
			data.attrs = items;
			data.id = id;
			data.groups = groups;
		} else if( data.groups == null )
			data.groups = groups;

		// Do a debugging dump of all Entry objects we are using/advertising
		for( int i = 0; i < items.length; ++i ) {
			log.fine( "entry["+i+"]: "+items[i].getClass().getName() );
			Class c = items[i].getClass();
			Field f[] = c.getDeclaredFields();
			for( int j = 0; j < f.length;++j ) {
				if( (f[j].getModifiers() & Modifier.PUBLIC) == 0 )
					continue;
				try {
					log.fine("        "+f[j].getName()+": "+f[j].get(items[i])+"");
				} catch( IllegalAccessException ex ) {
					log.fine("        "+f[j].getName()+": <unknown>");
				}
			}
		}

		log.fine("groups cnt: "+data.groups.length);
		for(int i = 0; i < data.groups.length; ++i ) {
			log.fine("  Group["+i+"]: \""+data.groups[i]+"\"" );
		}

		log.fine( "Checking for a security manager");
		if( System.getSecurityManager() == null ) {
			log.fine( "Starting RMI security Manager");
			System.setSecurityManager( new RMISecurityManager() );
		}

//		LookupLocatorDiscovery locDis = null;
		LookupLocator[]locators = null;
		if( lookupLocators != null ) {
			// create an array of LookupLocator objects to store into.
			locators = new LookupLocator[ lookupLocators.length ];
			// fill in the array
			for( int i = 0; i < lookupLocators.length; ++i ) {
				log.fine("using locator: "+lookupLocators[i] );
				locators[i] = new LookupLocator( lookupLocators[i] );
			}
//			// Create the discovery manager for Unicast lookups.
//			locDis = new LookupLocatorDiscovery( locators );
		}

		log.fine( "creating lookup discovery manager" );
		lm = new LookupDiscoveryManager( data.groups, locators, new DiscoveryListener() {
			public void discarded( DiscoveryEvent ev ) {
				ServiceRegistrar regs[] = ev.getRegistrars();
				for( int i = 0; i < regs.length; ++i ) {
					try {
						log.fine( "discarded LUS: "+regs[i].getLocator() );
					} catch( Exception ex ) {
						reportException(ex);
					}
				}
			}
			public void discovered( DiscoveryEvent ev ) {
				ServiceRegistrar regs[] = ev.getRegistrars();
				for( int i = 0; i < regs.length; ++i ) {
					try {
						log.fine( "discovered LUS: "+regs[i].getLocator() );
					} catch( Exception ex ) {
						reportException(ex);
					}
				}
			}
		});

		// If we have a service ID just join otherwise use a listener
		// that will write out our new ID once we learn it from an LUS.
		if( id != null ) {
			log.fine( "starting service with id: "+id );
			join = new JoinManager( getExportedObject(), items, id, lm, null );
		} else {
			log.fine( "waiting for service id" );
			join = new JoinManager( getExportedObject(), items, new IDListener(), lm, null );
		}

		log.fine("creating JiniAdmin for: "+join+", "+lm );
		adm = new JiniAdmin( join, new  PersistenceIO(){
				public PersistentData readState() throws IOException,ClassNotFoundException {
					return readObjState();
				}
				public void writeState( PersistentData state ) throws IOException {
					writeObjState( state );
				}
				public void setFile( String file ) throws IOException {
					setObjFile( file );
				}
				public String getFile() {
					return getObjFile();
				}
			}, lm, false );

		// If the VM terminates, cleanup the Join
		Runtime.getRuntime().addShutdownHook( new Thread("Jini shutdown - "+this) {
			public void run() {
				join.terminate();
			}
		});
	}
	
	public Remote exportObject() throws IOException {
		return UnicastRemoteObject.exportObject(this);
	}

	/**
	 *  Returns the actual exported object, default is <code>this</code>
	 */
	public Remote getExportedObject() throws IOException {
		if( exportedObj != null )
			return exportedObj;
		return exportedObj = exportObject();
	}

	/**
	 *  Unexport the object, using force to control when.
	 *  @param force true causes the object to be forced out, false waits till all
	 *  outstanding references are released.
	 */
	public boolean unexportObject(boolean force) throws IOException {
		return UnicastRemoteObject.unexportObject( this, force );
	}

	/**
	 *  Unexport the object when all references have been released
	 */
	public boolean unexportObject() throws IOException {
		return unexportObject( false );
	}

	/**
	 *  Override this to serialize any additional data in the
	 *  serFile passed to the constructor
	 */
	protected void writeAppState( ObjectOutputStream os ) throws IOException {
	}
	
	/**
	 *  Override this to deserialize any additional data in the
	 *  serFile passed to the constructor
	 */
	protected void readAppState( ObjectInputStream is ) throws IOException,ClassNotFoundException {
	}

	/**
	 *  Writes out the complete serialized state including the
	 *  <code>state</code> parameter and calling <code>writeAppState()</code>.
	 *  The default implementation uses an ObjectOutputStream.
	 */
	public void writeObjState( PersistentData state ) throws IOException {
		FileOutputStream fs = new FileOutputStream( new File( curFile ) );
		try {
			ObjectOutputStream os = new ObjectOutputStream( fs );
			os.writeObject( state );
			writeAppState( os );
			os.close();
		} finally {
			fs.close();
		}
		lastdata = state;
	}
	
	/**
	 *  Reads in the complete serialized state returning the
	 *  PersistantData passed to <code>writeObjState()</code>,
 	 *  also calling <code>readAppState()</code>.
	 *  The default implementation uses an ObjectInputStream.
	 */
	public PersistentData readObjState() throws IOException,ClassNotFoundException {
		PersistentData data = null;
		FileInputStream fs = new FileInputStream( new File( curFile ) );
		try {
			ObjectInputStream is = new ObjectInputStream( fs );
			data = (PersistentData)is.readObject();
			try {
				readAppState( is );
			} catch( EOFException ex ) {
			}
			is.close();
		} finally {
			fs.close();
		}
		lastdata = data;
		return data;
	}
}
