package org.wonderly.jini2;

import java.security.*;
import java.io.*;
import java.lang.reflect.*;
//import org.cheiron.jsc.JSCException;
//import org.cheiron.jsc.JSCFailure;
//import org.cheiron.jsc.ServiceContext;
//import org.cheiron.jsc.ServiceState;

import org.wonderly.util.jini.*;
import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.core.entry.*;
import net.jini.admin.*;

import java.util.logging.*;
import java.rmi.*;
import java.util.Arrays;
import net.jini.config.*;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.export.Exporter;
import org.wonderly.util.jini2.JiniAdmin;
import org.wonderly.util.jini2.AppPersistenceIO;
import org.wonderly.jini.start.ServiceIDAccessor;
import net.jini.security.*;
import javax.security.auth.Subject;
import javax.security.auth.login.*;
//import org.cheiron.jsc.JiniService;

/**
 *  This class provides a base class for creating persistant Jini services under
 *  the Jini 2.0 specifications.  It provides for the use of a Configuration.
 *  The configuration source is the class name by default via the
 *  <code>getPackage()</code> and the <code>getName()</code> methods.
 *	<p>
 *  The ConfigurableJiniApplication subclass has a great deal of functionality
 *  in the form of convienence classes to process configuration and logging
 *  setup.  Please consult that documentation for more specifics.
 *  <p>
 *  This class creates an instance of the JiniAdmin class that needs to have
 *  a configuration entry specified in the configuration file under the
 *	class name of JiniAdmin.  In particular, this JiniAdmin instance is the
 *  registered object that implements the JiniAdmins interface's super
 *	interfaces.  Thus, the correct exporter for that interface and
 *  any associated contraints needs to specified in that configuration.
 *
 *  <pre>
 *	org.wonderly.util.jini2.JiniAdmin {
 *		exporter = ...
 *		locators = ...
 *		preparer = ...
 *		entries = ...
 *	}
 *  </pre>
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class PersistentJiniService 
		extends ConfigurableJiniApplication
		implements AppPersistenceIO,ServiceIDAccessor,Remote {
//	protected JoinManager join;
//	protected JiniAdmins adm;
	protected Exporter admexp;
	protected JoinContext ctx;
//	protected String curFile;
//	protected LookupDiscoveryManager lm;
//	protected PersistentData lastdata;
	protected Remote exportedObj;
//	protected String myName;
	protected boolean writeable = true;
	
	/**
	 *  Provides access to {@link net.jini.admin.JoinAdmin#modifyLookupAttributes(net.jini.core.entry.Entry[],net.jini.core.entry.Entry[])} to update the
	 *  value of a lookup attribute entry.
	 */
	public void modifyAttributes( Entry[]templ, Entry[]sets ) throws IOException {
		log.fine("ModifyAttributes from app" );
		dumpItems( log, templ );
		dumpItems( log, sets );
		((JoinAdmin)ctx.adm).modifyLookupAttributes( templ, sets );
		try {
			ctx.state = readObjState(log, ctx.curFile, ctx.appio);
			dumpItems( log, ctx.state.attrs );
		} catch( Exception ex ) {
			log.log( Level.SEVERE, ex.toString(), ex );
		}
	}
	
	/**
	 *  Provides access to {@link net.jini.admin.JoinAdmin#modifyLookupAttributes(net.jini.core.entry.Entry[],net.jini.core.entry.Entry[])} to update the
	 *  value of a lookup attribute entry.
	 */
	public void addLookupAttributes( Entry[]sets ) throws IOException {
		log.fine("Add lookup attributes from app");
		dumpItems( log, sets );
		((JoinAdmin)ctx.adm).addLookupAttributes(  sets );
		try {
			ctx.state = readObjState(log, ctx.curFile, ctx.appio);
			dumpItems( log, ctx.state.attrs );
		} catch( Exception ex ) {
			log.log( Level.SEVERE, ex.toString(), ex );
		}
	}
	
	/**
	 *  Provides access to {@link net.jini.admin.JoinAdmin#modifyLookupAttributes(net.jini.core.entry.Entry[],net.jini.core.entry.Entry[])} to update the
	 *  value of a lookup attribute entry.
	 */
	public Entry[] getLookupAttributes( ) throws IOException {
		return ((JoinAdmin)ctx.adm).getLookupAttributes( );
	}

	/**
	 *  Update current state on disk, terminate join manager, unexport
	 *  service object, unexport JiniAdmins object.
	 */
	public void terminate() throws IOException,ConfigurationException {
		updateState();
		if( ctx.join != null ) {
			ctx.join.terminate();
			ctx.join = null;
		}
		unexportObject();
		if( admexp != null )
			admexp.unexport(true);
		ctx.adm = null;
		admexp = null;
	}

	public ServiceID getServiceID() {
		synchronized( ctx ) {
			if( ctx.haveServiceID == false ) {
				try {
					ctx.wait();
				} catch( Exception ex ) {
				}
			}
		}
		return ctx.state.id;
	}
	
	public boolean isWriteable() throws IOException {
		if( writeable )
			return true;
		throw new IOException("DataStore corruption detected, write inhibited");
	}

	protected void updateState() throws IOException {
		log.fine("Updating service persistent state: "+ctx.state );
		if( isWriteable() )
			writeObjState( ctx.state );
	}

	/**
	 *  Creates an instance of PersistentJiniSerice that exports the passed
	 *  Remote Object and uses the passed Configuration.  Any desired
	 *  login context should be established before calling this method.
	 */
	public static PersistentJiniService create( Remote obj,
			Configuration conf ) 
				throws IOException, ConfigurationException {
		PersistentJiniService perf = new PersistentJiniService( conf );
		perf.exportedObj = perf.exportObject( obj );
//		perf.startService();
		return perf;
	}

	public PersistentJiniService( Configuration conf 
			) throws IOException, ConfigurationException {
		super(conf);
	}

	public PersistentJiniService( String args[] 
			) throws IOException, ConfigurationException {
		super(args);
	}
	
	/**
	 *  Use the Exporter returned from <code>getExporter</code> to
	 *  cache an instance of an Exporter for the passed Remote.
	 *  The cached value is created once, and the referenced
	 *  thereafter.
	 */
	public Remote exportObject( Remote obj 
				) throws IOException, ConfigurationException {
		Exporter exp = getExporter(obj, true);
		Remote r = exp.export(obj);
		log.fine("Exported "+obj+" with "+exp+
			", got: "+r );
		return r;
	}
	
	/**
	 *  Use the Exporter returned from <code>getExporter</code> to
	 *  cache an instance of an Exporter for the passed Remote.
	 *  The cached value is created once, and the referenced
	 *  thereafter.
	 */
	public Remote exportObject( Remote obj, NameableObject name
			) throws IOException, ConfigurationException {
		Exporter exp = getExporter(name);
		Remote r = exp.export(obj);
		log.fine("Exported "+obj+
			" using "+name+" with "+exp+", got: "+r );
		return r;
	}
	
	public void setExportedObject( Remote rem ) {
		exportedObj = rem;
	}

	/**
	 *  This method is a convenience method for the use of subclasses.
	 *  It provides a simple way to get the service object corresponding
	 *  to using the configured Exporter on <code>this</code>.  If
	 *  your services object is not derived from exporting
	 *  <code>this</code>, you should override this method to return 
	 *  the correct object for your use, and the use of any subclass
	 *  of your class that might not know how to derive the
	 *  object.
	 */
	public Object getExportedObject() 
					throws IOException, ConfigurationException {
		if( exportedObj != null )
			return exportedObj;
		return exportedObj = getExporter( this, true ).export(this);
	}

	public String getName() {
//		if( myName != null )
//			return myName.replace(' ','_');
		return super.getName();
	}

	/**
	 *  Create a new persistent service using the <code>configuration.cfg</code>
	 *  Configuration file for initialization
	 */
	public PersistentJiniService() throws IOException,ConfigurationException {
		this(new String[]{"configuration.cfg"});
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
		return ctx.expadm;
	}
	
	public JiniAdmin getJiniAdmin() {
		return ctx.adm;
	}
	
	/**
	 *  Get the current serialized file name we are using
	 */
	public String getObjFile() {
		return ctx.curFile;
	}
	
	/**
	 *  Set the name of the file to write to.  The default
	 *  implementation does not allow setting the name of the
	 *  file after it is set using the constructor.
	 */
	public void setObjFile( String name ) throws IOException {
		throw new IOException("Storage Location Not Changeable");
	}
	
	/**
	 *  Class to listen for our serviceID if we don't have one yet
	 *  This listener will write the learned service id out to
	 *  the persistent stream.
	 */
	protected static class IDListener implements ServiceIDListener {
		Logger log;
		JoinContext ctx;
		public IDListener( Logger log, JoinContext ctx ) {
			this.log = log;
			this.ctx = ctx;
		}
		public void serviceIDNotify( ServiceID id ) {
			log.info("got Service ID: "+id );
			if( ctx.state == null )
				ctx.state = new PersistentData();
			ctx.state.attrs = ctx.join.getAttributes();
			ctx.state.groups = ctx.lm.getGroups();
			ctx.state.locators = ctx.lm.getLocators();
			ctx.state.id = id;
			try {
				try {
					// Check if writable and write it out if so.
					if( ctx.appio.isWriteable() ) {
						writeObjState( ctx.state, ctx.curFile, ctx.appio );
					}
					ctx.haveServiceID = true;
				} catch( IOException ex ) {
					log.throwing( PersistentJiniService.class.getName(),
						"startService", ex );
					ctx.join.terminate();
				}
			} finally {
				synchronized(ctx) {
					ctx.notifyAll();
				}
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
	
	public Object getEntry( String name, 
				Class clazz ) throws ConfigurationException {
		return conf.getEntry( getConfigComp(),
			name, clazz );
	}

	public Object getEntry( String name, Class clazz,
				Object def ) throws ConfigurationException {
		return conf.getEntry( getConfigComp(),
			name, clazz, def );
	}

	/**
	 *  Stop active join manager, and if were active, update stored state on
	 *  disk after terminating the service so that a start will reload the
	 *  last active state.
	 *  @return true if join manager was stopped
	 */
	public boolean stopService() throws IOException, ConfigurationException {
		if( ctx.join != null ) {
			ctx.join.terminate();
			ctx.join = null;
			updateState();
			return true;
		}
		return false;
	}

	public void startService() throws IOException, ConfigurationException {
		startService( (String)getEntry( "serviceName", String.class, "Service" ),
			(String)getEntry( "persistFile", String.class, "service.ser" ) );
	}

	/**
	 *  Start the service registration process using the passed data.
	 *  If the configuration entry, "loginContext" is set to an instance of
	 *  LoginContext, then a JAAS login will be executed prior to
	 *  starting the service, and the associated login Subject will be
	 *  used in a {@link javax.security.auth.Subject#doAsPrivileged(Subject,java.security.PrivilegedExceptionAction,java.security.AccessControlContext)} call.
	 *
	 *  @param name the default initial name for the service.
	 *  @param serFile the file to store the serialized data 
	 *  for service config into.
	 */
	public void startService( final String name, 
			final String serFile ) throws IOException, ConfigurationException {
		LoginContext lgnCtx = null;
		try {
			lgnCtx = (LoginContext)conf.getEntry( getClass().getName(),
			"loginContext", LoginContext.class );
		} catch( NoSuchEntryException ex ) {
			log.log(Level.CONFIG, ex.toString(), ex );
		}

		Principal[]serviceUser = null;
		PrivilegedExceptionAction init = new PrivilegedExceptionAction() {
			public Object run() throws IOException, ConfigurationException {
				doStartService( name, serFile );
				return null;
			}
		};

		try {
			if( lgnCtx != null ) {
				log.fine("using login context: "+lgnCtx );
				lgnCtx.login();
				    Subject.doAsPrivileged(
						lgnCtx.getSubject(), init, null);
			} else {
				log.fine("no login context provided");
				init.run();
			}
		} catch (PrivilegedActionException e) {
		    throw (IOException)new IOException(
		    	e.getCause().toString()).initCause(e.getCause());
		} catch ( LoginException e) {
		    throw (IOException)new IOException(e.toString()).initCause(e);
		} catch ( Exception e) {
		    throw (IOException)new IOException(e.toString()).initCause(e);
		}
	}

	protected void doStartService( String name, String serFile
			) throws IOException,ConfigurationException {
		log.fine("Starting service \""+name+"\" from \""+serFile+"\"" );
		ctx = startService( name, serFile, 
			getExportedObject(), addNameIfMissing(), this, this );
//		this.curFile = ctx.curFile;
//		this.myName = ctx.myName;
//		this.join = ctx.join;
		admexp = getExporter( (NameableObject)ctx.adm, true );
		log.finer("Exporting "+ctx.adm+" with: "+admexp );
		ctx.expadm = (JiniAdmins)admexp.export(ctx.adm);
//		lastdata = ctx.state;
		log.fine("Setting lastdata("+ctx.state+") to reference: "+ctx.state+", id: "+ctx.state.id );
	}

	/**
	 *  Used to manage all the returned information from the
	 *  static start() method
	 */
	public static class JoinContext {
		public transient boolean haveServiceID;
		/** The persistence file to be used */
		public String curFile;
		/** The name of the exported service */
		public String myName;
		/** The lookup discovery manager to be used */
		public LookupDiscoveryManager lm;
		/** The Join manager to be used */
		public JoinManager join;
		/** The instance of JiniAdmins that was created */
		public JiniAdmin adm;
		public JiniAdmins expadm;
		/** The persistent data as initially created/read */
		public PersistentData state;
		/** Persistence IO for application */
		public AppPersistenceIO appio;
	}

	/**
	 *  If returns true, a Name entry will be added
	 *  to the defaults using the name passed to
	 *  startService, if none is provided otherwise.
	 */
	public boolean addNameIfMissing() {
		return true;
	}

	/**
	 *  Start the service registration process using the passed data.
	 *
	 *  @param name the default initial name for the service.
	 *  @param serFile the file to store the serialized data 
	 *  for service config into.
	 */
	public static JoinContext startService( String name, 
			String serFile, Object exported,
			ConfigurableJiniApplication app,
			AppPersistenceIO appio )
			throws IOException, ConfigurationException {
		return startService( name, serFile, exported, true,
			app, appio );
	}

	/**
	 *  Start the service registration process using the passed data.
	 *
	 *  @param name the default initial name for the service.
	 *  @param serFile the file to store the serialized data
	 *  @param exported the exported or otherwise service object
	 *  @param addName true to add {@link Name} {@link Entry} to Attributes
	 *  @param app the ConfigurableJiniApplication instance to use
	 *  @param appio the {@link AppPersistenceIO} instance to use for persistence.
	 *
	 *  @see #startService(String, String, Object, boolean, Configuration, String, AppPersistenceIO)
	 */
	public static JoinContext startService( String name, 
			String serFile, Object exported, boolean addName,
			ConfigurableJiniApplication app,
			AppPersistenceIO appio )
			throws IOException, ConfigurationException {

		final JoinContext ctx = new JoinContext();
		Entry[]items = app.getInitialEntrys();
		String[]groups = app.getGroups();
	
		ctx.curFile = new File( serFile ).toString();
		final Logger log = app.log;

		log.fine( "starting Jini registration: "+app );

		if( name == null ) {
			name = exported.getClass().getName().substring(
				exported.getClass().getName().lastIndexOf('.') + 1 );
		}
		ctx.myName = name;

		ServiceID id = null;
		PersistentData data = null;
		try {
			log.fine("Reading Object state from: "+ctx.curFile );
			data = readObjState(log, ctx.curFile, appio );
			if( data == null )
				throw new IOException( "Data null" );
			if( data.attrs != null )
				items = data.attrs;
			else
				data.attrs = items;
			id = data.id;
			ctx.state = data;
		} catch( ClassNotFoundException ex ) {
			log.throwing( PersistentJiniService.class.getName(),
				"startService", ex );
			log.warning("Continuing After Exception, Assuming no State Exists");
			data = null;
		} catch( InvalidClassException ex ) {
			log.throwing( PersistentJiniService.class.getName(),
				"startService", ex );
			log.warning("Continuing After Exception, Assuming no State Exists");
			data = null;
		} catch( IOException ex ) {
			log.throwing( PersistentJiniService.class.getName(),
				"startService", ex );
			log.warning("Continuing After Exception, Assuming no State Exists");
			data = null;
		}

		log.fine( "service id: "+id );
		log.fine( "items count="+items.length );
		// Make sure there are some default Entry objects
		if( items == null ) {
			items = new Entry[] {
				new Name( ctx.myName = name ),
				new Location(),
				new ServiceInfo( name,null,null,null,null,null )
			};
		} else {
			// Find out name
			boolean haveName = false;
			for( int i = 0; i < items.length; ++i ) {
				if( items[i] instanceof Name ) {
					ctx.myName = ((Name)items[i]).name;
					haveName = true;
					break;
				} else if( items[i] instanceof ServiceInfo ) {
					ctx.myName = ((ServiceInfo)items[i]).name;
					haveName = true;
				}
			}
			log.fine( "haveName: "+haveName+
				", addName: "+addName+
				", ctx.myName: "+ctx.myName );
			if( !haveName && addName ) {
				Entry nitems[] = new Entry[ items.length + 1 ];
				for( int i = 0; i < items.length; ++i ) {
					nitems[i] = items[i];
				}
				log.fine("Adding Name Entry at: "+items.length+
					", name: "+name );
				nitems[items.length] = new Name(name);
				items = nitems;
			}
		}

		// If we could not read data, setup default data.
		if( data == null ) {
			data = new PersistentData();
			data.attrs = items;
			data.id = id;
			data.groups = groups;
		} else if( data.groups == null ) {
			data.groups = groups;
		}

		dumpItems( log, items );

		if( data.groups == null )
			data.groups = new String[0];
		log.fine("groups cnt: "+data.groups.length);
		for(int i = 0; i < data.groups.length; ++i ) {
			log.finer("  Group["+i+"]: \""+data.groups[i]+"\"" );
		}

		LookupLocator[]locators = app.getLocators();
		log.fine( "creating lookup discovery manager for "+
			(locators == null ? 0 : locators.length)+" locators, "+
			"groups="+(groups == null ? "<null>" : Arrays.toString( data.groups )) );
		ctx.lm = new LookupDiscoveryManager( data.groups,
				locators, new DiscoveryListener() {
			public void discarded( DiscoveryEvent ev ) {
				ServiceRegistrar regs[] = ev.getRegistrars();
				for( int i = 0; i < regs.length; ++i ) {
					try {
						log.fine( "discarded LUS: "+regs[i].getLocator() );
					} catch( Exception ex ) {
						log.throwing( PersistentJiniService.class.getName(),
							"startService", ex );
					}
				}
			}
			public void discovered( DiscoveryEvent ev ) {
				ServiceRegistrar regs[] = ev.getRegistrars();
				for( int i = 0; i < regs.length; ++i ) {
					try {
						log.info( "discovered LUS: "+regs[i].getLocator() );
					} catch( Exception ex ) {
						log.throwing( PersistentJiniService.class.getName(),
							"startService", ex );
					}
				}
			}
		});
		
		if( log.isLoggable(Level.FINE) ) {
			Class ints[] = exported.getClass().getInterfaces();
			for( int i = 0; i < ints.length; ++i ) {
				log.fine("svc implements["+i+"]: "+ints[i].getName());
			}
		}

		// If we have a service ID just join otherwise use a listener
		// that will write out our new ID once we learn it from an LUS.
		if( id != null ) {
			ctx.haveServiceID = true;
			log.info( "starting service with id: "+id );
			ctx.join = new JoinManager( exported, items, id, 
				ctx.lm, null, app.conf );
		} else {
			ctx.haveServiceID = false;
			log.info( "waiting for service id" );
			ctx.join = new JoinManager( exported, items, 
				new IDListener(log,ctx), ctx.lm, null, app.conf );
		}

		log.fine("creating JiniAdmin for: "+ctx.join+", "+ctx.lm );
		ctx.appio = appio;
		JiniAdmin adm = new JiniAdmin( ctx.join, new PersistenceIO(){
				public PersistentData readState()
						throws IOException,ClassNotFoundException {
					return readObjState(log, ctx.curFile, ctx.appio);
				}
				public void writeState(
						PersistentData state ) throws IOException {
					log.fine("Check if state writable: "+ctx.appio.isWriteable() );
					if( ctx.appio.isWriteable() ) {
						log.finer("Writing ObjState: "+state+" to "+ctx.curFile+" using: "+ctx.appio );
						writeObjState( log, state, ctx.curFile, ctx.appio );
					}
				}
				public void setFile( String file ) throws IOException {
//					setObjFile( file );
					ctx.curFile = file;
				}
				public String getFile() {
					return ctx.curFile;
				}
			}, ctx.lm, false, app.conf );
		ctx.adm = adm;
//		Exporter admexp = getExporter( (NameableObject)ctx.adm, true );
//		log.finer("Exporting "+ctx.adm+" with: "+admexp );
//		ctx.expadm = (JiniAdmins)admexp.export(ctx.adm);

		log.fine("Registering shutdown hook to terminate join manager" );
		// If the VM terminates, cleanup the Join
		Runtime.getRuntime().addShutdownHook( 
				new Thread("Jini shutdown - "+exported ) {
			public void run() {
				ctx.join.terminate();
			}
		});
		ctx.state = data;
		writeObjState( log, ctx, appio );
		return ctx;
	}

	/**
	 *  Start the service registration process using the passed data.
	 *  This method will construct a new {@link ConfigurableJiniApplication}
	 *  instance to use for {@link Configuration} access.  It will then defer
	 *  to {@link #startService(String, String, Object, boolean, ConfigurableJiniApplication, AppPersistenceIO)}
	 *  to continue starting the service
	 *
	 *  @param name the default initial name for the service.
	 *  @param serFile the file to store the serialized data
	 *  for service config into.
	 *  @param exported the exported, or serializable service object.
	 *  @param addName true to add "name" as a net.jini.core.lookup.Name() value
	 *  @param conf a configuration to use
	 *  @param component the configuration component's entry value to use
	 *  @param appio an implementation of appio for persistence
	 */
	public static JoinContext startService( String name, 
			String serFile, Object exported, boolean addName,
			Configuration conf, final String component,
			AppPersistenceIO appio )
			throws IOException, ConfigurationException {

		if( name == null ) {
			name = exported.getClass().getName().substring(
				exported.getClass().getName().lastIndexOf('.') + 1 );
		}

		final String fname = name.replace(' ','_');
		ConfigurableJiniApplication app = new ConfigurableJiniApplication( conf ) {
			public String getConfigComp() {
				return component;
			}
		};
		return startService( fname, serFile, exported, addName,
			app, appio );
	}
	
	protected static void dumpItems( Logger log, Entry[] items ) {
		// Do a debugging dump of all Entry objects we 
		// are using/advertising
		if( log.isLoggable( Level.FINE ) ) {
			log.log( Level.FINEST, "Dump context", new Throwable("Dumping items context") );
			for( int i = 0; i < items.length; ++i ) {
				log.finer( "entry["+i+"]: "+items[i].getClass().getName() );
				Class c = items[i].getClass();
				Field f[] = c.getDeclaredFields();
				for( int j = 0; j < f.length;++j ) {
					if( (f[j].getModifiers() & Modifier.PUBLIC) == 0 )
						continue;
					try {
						log.finest("         "+f[j].getName()+
							": "+f[j].get(items[i])+"");
					} catch( IllegalAccessException ex ) {
						log.throwing( PersistentJiniService.class.getName(),
							"startService", ex );
						log.fine("        "+f[j].getName()+": <unknown>");
					}
				}
			}
		}
	}

	/**
	 *  Unexport the object when all references have been released
	 */
	protected boolean unexportObject() 
			throws IOException, ConfigurationException {
		return unexportObject( false );
	}

	/**
	 *  Override this to serialize any additional data in the
	 *  serFile passed to the constructor
	 */
	public void writeAppState( ObjectOutputStream os ) throws IOException {
	}
	
	/**
	 *  Override this to deserialize any additional data to the
	 *  persistant state of the service.
	 */
	public void readAppState( ObjectInputStream is ) 
			throws IOException,ClassNotFoundException {
	}

	/**
	 *  Writes out the complete serialized state including the
	 *  <code>state</code> parameter and calling
	 *  <code>writeAppState()</code>.
	 */
	public void writeObjState( PersistentData state ) throws IOException {
		log.fine("writeObjState: "+state );
		if( isWriteable() )
			ctx.state = writeObjState( state, ctx.curFile, this );
	}

	public static void writeObjState( JoinContext ctx, AppPersistenceIO io ) throws IOException {
		writeObjState( Logger.getLogger( PersistentJiniService.class.getName() ),
			ctx, io );
	}
	public static void writeObjState( Logger log, JoinContext ctx, AppPersistenceIO io ) throws IOException {
		ctx.state = writeObjState( log, ctx.state, ctx.curFile, io );
	}

	protected static Object readObjectEntry( ObjectInputStream is 
				) throws IOException,ClassNotFoundException {
		if( is.readInt() == 1 )
			return null;
		return is.readObject();
	}
	
	private static void dumpLoaders( Logger log, Class cls ) {
		ClassLoader ld = cls.getClassLoader();
		if( ld instanceof java.net.URLClassLoader ) {
			java.net.URL[]arr = ((java.net.URLClassLoader)ld).getURLs();
			for( java.net.URL u :arr ) {
				log.info("cls("+cls.getName()+" url: "+u );
			}
		} else {
			log.info("cls ("+cls.getName()+") with loader: "+ld+", ctxld: "+Thread.currentThread().getContextClassLoader() );
		}
		ld = Thread.currentThread().getContextClassLoader();
		if( ld instanceof java.net.URLClassLoader ) {
			java.net.URL[]arr = ((java.net.URLClassLoader)ld).getURLs();
			for( java.net.URL u :arr ) {
				log.info("ctxld ("+ld+") url: "+u );
			}
		}
	}

	protected static void writeObjectEntry( ObjectOutputStream os, Object maybeNull 
				) throws IOException {
		os.writeInt( maybeNull == null ? 1 : 2 );
		if( maybeNull != null )
			os.writeObject( maybeNull );
	}

	/**
	 *  Writes out the complete serialized state including the
	 *  <code>state</code> parameter and calling <code>writeAppState()</code>.
	 */
	public static PersistentData writeObjState( PersistentData state,
			String curFile, AppPersistenceIO appio ) throws IOException {
		return writeObjState( Logger.getLogger( PersistentJiniService.class.getName() ),
			state, curFile, appio );
	}

	/**
	 *  Writes out the complete serialized state including the
	 *  <code>state</code> parameter and calling <code>writeAppState()</code>.
	 */
	public static PersistentData writeObjState( Logger log, PersistentData state,
			String curFile, AppPersistenceIO appio ) throws IOException {
		log.fine("writing state to: "+curFile );
		FileOutputStream fs = new FileOutputStream( new File( curFile ) );
		try {
			ObjectOutputStream os = new ObjectOutputStream( fs );
			try {
				os.writeInt(2);
				if( state == null )
					throw new IllegalArgumentException("request to write null state");
				log.finer("write attributes: "+state.attrs );
				writeObjectEntry( os, state.attrs );
				log.finer("write groups: "+state.groups);
				writeObjectEntry( os, state.groups );
				log.finer("write locators: "+state.locators );
				writeObjectEntry( os, state.locators );
				log.finer("letting app write other state");
				appio.writeAppState( os );
			} finally {
				os.close();
			}
		} finally {
			fs.close();
		}
		
		log.fine("Writing service id: "+state.id+" to "+curFile+".id");
		// In version 2 of the persistence, we store the serviceID separately so that
		// the entry/group/locator data can be reset independently of the serviceID.
		fs = new FileOutputStream( new File( curFile+".id" ) );
		try {
			ObjectOutputStream os = new ObjectOutputStream( fs );
			try {
				os.writeInt(1);
				if( state == null )
					throw new IllegalArgumentException("request to write null state");
				os.writeObject( state.id );
			} finally {
				os.close();
			}
		} finally {
			fs.close();
		}
		if(log.isLoggable(Level.FINEST) )
			log.log(Level.FINEST,"returning written state: "+state, new Throwable("write completes") );
		return state;
	}
	
	/**
	 *  Reads in the complete serialized state returning the
	 *  PersistantData passed to <code>writeObjState()</code>,
 	 *  also calling <code>readAppState()</code>.
	 */
	public PersistentData readObjState(AppPersistenceIO appio
			) throws IOException,ClassNotFoundException {
		ctx.state = readObjState( log, ctx.curFile, this );
		return ctx.state;
	}

	/**
	 *  Reads in the complete serialized state returning the
	 *  PersistantData passed to <code>writeObjState()</code>,
 	 *  also calling <code>readAppState()</code>.
	 */
	private static PersistentData readObjState(Logger log, String curFile,
			AppPersistenceIO appio ) 
				throws IOException,ClassNotFoundException {
		PersistentData data = new PersistentData();
		int ver = 2;
		try {
			File f =  new File( curFile );
			FileInputStream fs = new FileInputStream( f );
			try {
				ObjectInputStream is = new ObjectInputStream( fs );
				ver = is.readInt();
				if(log.isLoggable(Level.FINER) ) log.finer
					("Reading version="+ver+" object state from "+f);
				if( ver == 1 ) {
					data = (PersistentData)is.readObject();
					if( data == null )
						throw new NullPointerException( "Unexpected null data read from stream");
				} else {
					data = new PersistentData();
					data.attrs = (Entry[])readObjectEntry( is );
					data.groups = (String[])readObjectEntry( is );
					data.locators = (LookupLocator[])readObjectEntry( is );
				}
				if(log.isLoggable(Level.FINER) ) log.finer("Read data: "+data );
				if(log.isLoggable(Level.FINER) ) log.finer("Reading app state" );
				appio.readAppState( is );
				is.close();
			} finally {
				fs.close();
			}
		} catch( FileNotFoundException ex ) {
			log.log( Level.FINE, ex.toString(), ex );
		}
		// In version 2 we store the serviceID separately so that the Entry codebases
		// can be reset, but we don't have to worry about the serviceID being reset.
		if( ver >= 2 ) {
			File f = new File( curFile+".id" );
			FileInputStream fs = new FileInputStream( f );
			try {
				ObjectInputStream is = new ObjectInputStream( fs );
				ver = is.readInt();
				if(log.isLoggable(Level.FINER) ) log.finer
					("Reading version="+ver+" serviceID (have="+data.id+") state from "+f);
				data.id = (ServiceID)is.readObject();
				if( log.isLoggable( Level.FINER) ) log.finer
					("found old id: "+data.id );
				is.close();
			} finally {
				fs.close();
			}
		}
		log.fine( "readObjState read: "+data );
		return data;
	}

	public TrustVerifier getProxyVerifier() {
		try {
			// Get the class implementing verification
			Class ver = getProxyTrustVerifier();
			if( ver == null )
				return null;
			// Get Constructor for an instance with our proxy
			Constructor c = ver.getConstructor( 
				new Class[]{ Object.class } );
			// Return instance with reference to this proxy
			return (TrustVerifier)c.newInstance(
				new Object[]{ getExportedObject() } );
		} catch( IOException ex ) {
			reportException(ex);
			return null;
		} catch( ConfigurationException ex ) {
			reportException(ex);
			return null;
		} catch( NoSuchMethodException ex ) {
			reportException(ex);
			return null;
		} catch( InstantiationException ex ) {
			reportException(ex);
			return null;
		} catch( IllegalAccessException ex ) {
			reportException(ex);
			return null;
		} catch( InvocationTargetException ex ) {
			reportException(ex);
			return null;
		}
	}

//	private Logger svnlog = Logger.getLogger( getClass().getName()+".seven" );
//	public void failureDetected(JSCFailure jSCFailure) {
//		log.warning("failure of JSC container: "+jSCFailure );
//	}
//
//	public Entry[] getAttributes(Entry[] entry) throws JSCException {
//		if( entry == null || entry.length == 0 ) {
//			try {
//				Entry[] arr= getInitialEntrys();
//				svnlog.info("using initial attributes: "+(arr != null ? Arrays.toString(arr) : "<null>") );
//			} catch( ConfigurationException ex ) {
//				throw (JSCException)new JSCException(ex.toString()).initCause(ex);
//			} catch( IOException ex ) {
//				throw (JSCException)new JSCException(ex.toString()).initCause(ex);
//			}
//		} else {
//			svnlog.info("getting attributes, using existing "+entry.length+" attributes: "+
//				Arrays.toString(entry) );
//		}
//		return entry;
//	}
//
//	public Remote[] getServiceProviders() throws JSCException {
//		svnlog.info("getServiceProviders returning this="+this );
//		return new Remote[]{ this };
//	}
//
//	public Object getServiceProxy(Object object) throws JSCException {
//		svnlog.info("getServiceProxy passes in: "+object);
//		return object;
//	}
//
//	protected ServiceContext jscContext;
//	public void init(ServiceContext serviceContext) throws JSCException {
//		svnlog.info( "Got serviceContext: "+serviceContext );
//		jscContext = serviceContext;
//	}
//
//	public void stateChanged(ServiceState serviceState, ServiceState serviceState0) {
//		svnlog.info("JSC State changed: "+serviceState+" was "+serviceState0 );
//	}
}



