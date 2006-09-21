package org.wonderly.jini2.config;

import net.jini.config.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import java.io.*;
import org.wonderly.log.*;
import org.wonderly.util.jini2.*;
import org.wonderly.util.jini.LookupEnv;
import java.util.logging.*;
import java.rmi.*;
import java.util.*;
import org.wonderly.util.jini2.ServiceLookup;

/**
 *  This class is a Configuration instance that can
 *  be used as a ConfigurationProvider compatible 
 *  implementation. It uses service lookup to find 
 *  ManagedConfiguration services and queries them for
 *  a configuration under the indicated ConfigurableId. 
 *  <p>
 *  When the last ServiceRegistrar that hold the
 *  service registry for a ManagedConfiguration instance
 *  that provides access to the needed ConfigurationSet
 *  is no longer accessible, this class silently
 *  ignores that situation in an attempt to maintain
 *  service.  Technically the loss of the reference
 *  doesn't present any loss to this class since it
 *  has a copy of the ConfigurationSet locally.
 *  <p>
 *  <b>Logger: </b><i>This classname is used for the logger name</i>
 *  <p>
    <b>Configuration:</b>
    <br>
    The following table shows the configuration entries
    referenced by this class.  The Configuration namespace
    is either this classes fully qualified classname, or
    the string value in the system property 
    <code>org.wonderly.jini2.config.configName</code>.  This class also
    uses an {@link org.wonderly.util.jini2.ServiceLookup} instance that
    will also use a particular set of Configuration entries under the
    same Configuration name.
 *  <table border="1">
 	<tr><th>Entry<th>Type<th>Usage<th>Default
 	<tr><td>configid<td>String<td>The Uuid.toString() representation of the ConfigurationSet to use.<td><b>none</b>
 	<tr><td>waitTime<td>long<td>Time in millis for a ConfigurationSet to appear, before failing<td>60,000
 	<tr><td>logFail<td>boolean<td>A Level.WARNING log entry will be generated if a ConfigurationSet is never found.<td>false
 	<tr><td>lookupEnv<td>org.wonderly.util.jini.LookupEnv<td>This will be the
 	 environment used to find the ManagedConfiguration
 	 instances used for locating ConfigurationSets.</td><td>
 	 <pre>
new LookupEnv(
    "ManagedConfigurations",
    new ServiceTemplate( null,
        new Class[] {
            ManagedConfiguration.class
            }, null ),
    new LookupLocator[] {
        // Force local lookup when multicast is missing
        new LookupLocator("jini://localhost")
    }
)
	</pre>
 *	</table>
 *  @see ConfigurableId
 *  @see net.jini.config.ConfigurationProvider
 *  @author Gregg Wonderly - gregg.wonderly@pobox.com
 */
public class ConfigurationProvider
		implements Configuration {
	protected ServiceLookup sl;
	protected Logger log = Logger.getLogger( getClass().getName() );
	protected Configuration conf;
	protected Hashtable<ConfigurableId,ConfigurationSet> configs = new Hashtable<ConfigurableId,ConfigurationSet>();
	protected Vector<ConfigurationAccess> listeners = new Vector<ConfigurationAccess>();

	public void addAccess( ConfigurationAccess acc ) {
		if( listeners.contains(acc) == false )
			listeners.addElement(acc);
	}
	public void removeAccess( ConfigurationAccess acc ) {
		listeners.removeElement(acc);
	}
	protected void notifyAccess() {
		for( int i = 0; i < listeners.size(); ++i ) {
			try {
				((ConfigurationAccess)listeners.elementAt(i)).apply(this);
			} catch( ConfigurationException ex ) {
				log.log( Level.SEVERE, ex.toString(), ex );
			}
		}
	}
	public Object getEntry( String name, String ent, 
			Class type, Object def,
			Object data ) throws ConfigurationException {
		log.fine( "getEntry( "+name+", "+ent+", "+type.getName()+", "+def+", "+data+");");
		return conf.getEntry( name, ent, type, def, data );
	}

	public Object getEntry( String name, String ent, 
			Class type, Object def ) 
				throws ConfigurationException {
		log.fine( "getEntry( "+name+", "+ent+", "+type.getName()+", "+def+");");
		return conf.getEntry( name, ent, type, def );
	}

	public Object getEntry( String name, String ent,
			Class type ) throws ConfigurationException {
		log.fine( "getEntry( "+name+", "+ent+", "+type.getName()+");");
		return conf.getEntry( name, ent, type );
	}

	private void testLoggingSetup() {
// 		ConsoleHandler h;
//		h = new ConsoleHandler();
//		h.setFormatter( new StreamFormatter() );
//		h.setLevel( Level.ALL );
//		Handler[]hs = log.getHandlers();
//		for( int i = 0; i < hs.length; ++i ) {
//			log.removeHandler(hs[i]);
//		}
//		log.addHandler(h);
//		log.setUseParentHandlers(false);
//		h.setLevel(Level.ALL);
//		log.setLevel(Level.ALL);
	}

	public ConfigurationProvider( String[]args, 
			ClassLoader ld ) throws IOException, ConfigurationException {

		if( System.getSecurityManager() == null ) {
			System.setSecurityManager( new RMISecurityManager() );
		}

		if( System.getProperty( "org.wonderly.jini2.config.testConfig") != null )
			testLoggingSetup();

		/** Get the name of the configuration entry to use */
		String name = System.getProperty(
			"org.wonderly.jini2.config.configName");

		if( name == null )
			name = getClass().getName();
		log.fine("Cons("+name+")");
		String file = args[0];
		String[]nargs = new String[args.length-1];
		System.arraycopy( args, 1, nargs, 0, nargs.length );
		FileReader rd = null;
		final Object waitObject = new Object();
		final boolean done[] = new boolean[1];
		try {
			log.fine("opening file: "+file);
			rd = new FileReader( file );
		} catch( FileNotFoundException ex ) {
			ConfigurationException cex = 
				new ConfigurationException( ex.toString() );
			throw (ConfigurationException)cex.initCause(ex);
		}
		ConfigurationFile f = 
			new ConfigurationFile( rd, nargs, ld );
		final ConfigurableId id = (ConfigurableId)
			f.getEntry( name,
			"configid", ConfigurableId.class );
		log.fine( "configId: " + id );
		long waitTime = ((Long)
			f.getEntry( name,
			"waitTime", Long.class, 
				new Long(60000) ) ).longValue();
		log.fine( "waitTime: " + waitTime );

		boolean logFail = ((Boolean)
			f.getEntry( name,
			"logFail", Boolean.class, 
				new Boolean( false ) ) ).booleanValue();
		log.fine( "logFail: "+logFail );

		LookupEnv env = (LookupEnv)f.getEntry( name,
			"lookupEnv", LookupEnv.class,
			new LookupEnv(
			"ManagedConfigurations",
			new ServiceTemplate( null,
				new Class[] {
					ManagedConfiguration.class
				}, null ), new LookupLocator[] {
					new LookupLocator("jini://localhost")
						}));
		
		sl = new ServiceLookup( env, 
			new ServiceLookupHandler() {
				public void serviceLost( ServiceID id,
					ServiceRegistrar reg) {
					notifyAccess();
				}
				public void updateItem( ServiceItem item, 
					ServiceRegistrar reg) {
					Configuration c = null;
					if( (c = checkConfig( id, 
							item, reg ) ) != null ) {
						synchronized( waitObject ) {
							waitObject.notify();
						}
						log.info("updated config: "+conf );
						conf = c;
						notifyAccess();
					}
				}
				public void processItem( ServiceItem item,
						ServiceRegistrar reg) {
							
					log.info("Got Service Item: "+
							ServiceLookup.descItem(item) );
					Configuration c = null;
					if( (c = checkConfig( id, 
							item, reg ) ) != null ) {
						synchronized( waitObject ) {
							waitObject.notify();
						}
						log.info("Found config: "+conf );
						conf = c;
						notifyAccess();
					}
				}
			}, log, f, name );

		log.fine("Starting lookup for ManagedConfiguration instances");
		try {
			sl.start();
		} catch( IOException ex ) {
			ConfigurationException cex =
				new ConfigurationException( ex.toString() );
			throw (ConfigurationException)cex.initCause(ex);
		}

		synchronized( waitObject ) {
			try {
				waitObject.wait( waitTime );
			} catch( InterruptedException ex ) {
				log.log( Level.WARNING, ex.toString(), ex );
			}
		}

		if( conf == null ) {
			ConfigurationException ex =
				new ConfigurationException( 
					"Remote Configuration not Found" );
			if( logFail ) {
				log.log( Level.WARNING, ex.toString(), ex );
				log.log( Level.WARNING,
					"using configuration in \""+file+
					"\" as default configuration" );
				conf = f;
			} else {
				throw ex;
			}
		}
	}

	/**
	 *  Check if the indicated service can provide the
	 *  configuration indicated by the ConfigurableId.
	 *  If an IOException occurs while making the
	 *  remote call, a cached version of the 
	 *  Configuration will be returned if it exists.
	 *
	 *  @return null if no config is found otherwise
	 *  return the found Configuration object.
	 */
	protected Configuration checkConfig( 
			ConfigurableId id, ServiceItem item, 
				ServiceRegistrar reg ) {
		ManagedConfiguration conf = 
			(ManagedConfiguration)item.service;
		try {
			ConfigurationSet cs = conf.getConfiguration( id );
			log.fine("ConfigurationSet found for: "+id+", val: "+cs );
			if( cs != null ) {
				configs.put( id, cs );
				return cs;
			}
		} catch( IOException ex ) {
			log.log( Level.SEVERE, 
				"Error getting configuration from: "+
				ServiceEntry.getName( item ), ex );
			if( configs.get( id ) != null )
				return (ConfigurationSet)configs.get(id);
		}
		
		return null;
	}
}