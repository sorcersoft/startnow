package org.wonderly.jini2;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.config.ConfigurationException;
import net.jini.config.NoSuchEntryException;
import net.jini.export.Exporter;
import net.jini.security.proxytrust.ProxyTrustVerifier;
import org.wonderly.jini.browse.JiniExplorer;
import net.jini.core.lookup.ServiceItem;
import net.jini.security.ProxyPreparer;
import net.jini.jeri.ServerEndpoint;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import net.jini.security.BasicProxyPreparer;
import java.rmi.*;
import net.jini.export.*;
import net.jini.jeri.*;
import net.jini.jeri.ssl.*;
import net.jini.constraint.*;
import net.jini.core.constraint.*;

import org.wonderly.log.StdoutLogger;
import org.wonderly.jini.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.beans.*;
import org.wonderly.util.jini.*;
import org.wonderly.swing.*;
import org.wonderly.awt.*;
import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.core.event.*;
import net.jini.core.lease.*;
import net.jini.core.transaction.server.*;
import net.jini.entry.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import java.io.*;
import net.jini.lease.*;
import net.jini.event.*;
import net.jini.space.*;
import net.jini.admin.*;
import net.jini.lookup.ui.factory.*;
import net.jini.lookup.ui.*;
import java.util.*;
import java.text.*;
import org.wonderly.jini.serviceui.*;
import org.wonderly.log.*;
import java.awt.*;
import java.rmi.RMISecurityManager;
import net.jini.config.ConfigurationException;

/**
 *  This class provides the basic infrastructure for using a Configuration
 *  object in an application.  A Configuration object is constructed using
 *  ConfigurationProvider, in this classes constructor.  The methods
 *  provided here have information in their documentation pertaining to
 *  which Configuration entries they read from to get the values that they
 *  return.
 *
 *  <h2>Configuration Values Recognized</h2>
 *	<table>
 *	<tr><th>Value Name<th>Type<th>Usage
 *	<tr><td>exporter<td>Exporter<td>Exporter for all proxies
 *	<tr><td>loggingConfig<td>String<td>The contents of a 
 *			logging.properties file that will be loaded to initialize
 * 			logging configuration instead of setting 
 			<code>-Djava.util.logging.config.file</code></td>
 *	<tr><td>logLevel<td>Level<td>Default log level for all loggers referenced
 *	<tr><td>logger<td>Logger<td>Logger to use instead of the classname
 *	<tr><td>entries<td>Entry[]<td>The set of Entry objects that will initially
 *		be used at registration.  If there is a serialized store already
 *		in place, that will always be used instead.  That file can be removed
 *		to reset the Entry[] set to that specified here.
 *	<tr><td>wireProtocol<td>String<td>JRMP or JERI are recognized and default 
 *		setup for SSL Jeri endpoints with no authentication is used if JERI is
 *		specified.
 *	<tr><td>template<td>ServiceTemplate<td>Used to do any lookups for services.
 *  <td><td>locators<td>LookupLocator[]<td>Locators for service lookup etc.
 *	<tr><td>lookupenvs<td>LookupEnv[]<td>The unicast locators to use for registration
 *	<tr><td>groups<td>String[]<td>The groups to use for registration
 *	<tr><td>proxyTrustVerifier<td>ProxyTrustVerifier<td>Used to verify proxy of LUS to perform notification with.
 *	<tr><td>preparer<td>Preparer<td>Used to prepare the proxy of the LUS for registration.
 *  <tr><td>otherLoggers<td>String[]<td>The names of other logging instances that should
 *		be manipulated by the <code>setLogLevel()</code> method to keep all of the
 *		associated loggers operating at the same log level.  This makes in unnecessary
 *		to specify or manipulate a logging configuration file too.
 *	</table>
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class ConfigurableJiniApplication extends NameableObjectImpl {
	/** The Configuration object returned by ConfigurationProvider */
	protected volatile Configuration conf;
	/** The Exporter cache */
	protected Hashtable<Object,Exporter> exporters;
	/** The Verifiers cache */
	protected Hashtable<Object,Class> verifiers;
	/** The ServiceTemplate cache */
	protected Hashtable<Object,ServiceTemplate> templates;
	/** The LookupEnv[] cache */
	protected Hashtable<Object,LookupEnv[]> lookupenvs;
	/** The ProxyPreparer cache */
	protected Hashtable<Object,ProxyPreparer> preparers;
	/** The Group name cache */
	protected Hashtable<Object,String[]> groups;
	/** The Entry cache */
	protected Hashtable<Object,Entry[]> entries;
	/** The LookupLocator cache */
	protected Hashtable<Object,LookupLocator[]> locators;
	/** The Loggers cache */
	protected Hashtable<Object,Logger> loggers;
	/** The security manager singleton, if any, that was created */
	protected SecurityManager sm;

	protected Logger log;

	/**
	 *  Returns a cached instance of RMISecurityManager.
	 *
	 *  @see java.rmi.RMISecurityManager
	 */
	public SecurityManager getSecurityManager() {
		if( sm != null )
			return sm;
		return sm = new RMISecurityManager();
	}
	
	/**
	 *  @throws ConfigurationException if a Configuration can not be constructed
	 *  @throws IOException if a configuration file access or other I/O based error occurs
	 */
	public Configuration getConfiguration() throws ConfigurationException,IOException {
		return conf;
	}
	
	/** Returns the name of the configuration component to use for this
	 *  application.  The default implementation is
	 *  <pre>
	 *	return getPackage()+"."+getName();
	 *  </pre>
	 */
	public String getConfigComp() {
		return getPackage()+"."+getName();
	}

	/**
	 *  The default wire protocol to use.  "JRMP" by default
	 *  is returned.  "JERI" can be configured to turn that
	 *  protocol on and a default exporter and preparer.
	 *
	 *  <br>Configuration entry name: <b>wireProtocol</b>
	 */
	public String getDefaultWireProtocol() 
			throws IOException,ConfigurationException {
		try {
			return (String)conf.getEntry( getConfigComp(),
				"wireProtocol", String.class, "JRMP" );
		} catch( NoSuchEntryException ex ) {
			log.throwing( getConfigComp(),
				"getDefaultWireProtocol", ex );
		}
		return "JRMP";
	}

	/**
	 *  The default wire protocol to use.  "JRMP" by default
	 *  is returned.  "JERI" can be configured to turn that
	 *  protocol on and a default exporter and preparer.
	 *
	 *  <br>Configuration entry name: <b>wireProtocol</b>
	 */
	public String getDefaultWireProtocol(NameableObject ctx) 
			throws IOException,ConfigurationException {
		try {
			return (String)conf.getEntry( ctx.getPackage()+"."+ctx.getName(),
				"wireProtocol", String.class, "JRMP" );
		} catch( NoSuchEntryException ex ) {
			log.throwing( ctx.getPackage()+"."+ctx.getName(),
				"getDefaultWireProtocol", ex );
		}
		return "JRMP";
	}

	/**
	 *  The setof LookupEnv objects for this application using
	 *  its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>lookupenvs</b>
	 */
	public LookupEnv[] getLookupEnvs() 
			throws IOException,ConfigurationException {
		return getLookupEnvs( (Object)null, false );
	}

	/**
	 *  The setof LookupEnv objects for the passed NameableObject 
	 *  using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>lookupenvs</b>
	 */
	public LookupEnv[] getLookupEnvs(NameableObject no)
			throws IOException,ConfigurationException {
		return getLookupEnvs(no,false);
	}

	/**
	 *  The setof LookupEnv objects for the passed NameableObject
	 *  using its package and name.
	 *  The value returned <b>is</b> cached based on the value
	 *  passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>lookupenvs</b>
	 */
	public LookupEnv[] getLookupEnvs( final NameableObject no, 
			final boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,LookupEnv[]>().get( lookupenvs, no,
			new ObjectCreator<LookupEnv[]>() {
				public LookupEnv[] create() 
						throws IOException,ConfigurationException {
					return (LookupEnv[]) conf.getEntry(
						no.getPackage()+"."+no.getName(),
						"lookupenvs", (new LookupEnv[]{}).getClass() );
				}
			}, cache );
	}

	/**
	 *  The setof LookupEnv objects for this application
	 *  using its package and name.
	 *  The value returned <b>is</b> cached based on the
	 *  value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>lookupenvs</b>
	 */
	public LookupEnv[] getLookupEnvs( Object obj,
			boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,LookupEnv[]>().get( lookupenvs, obj,
			new ObjectCreator<LookupEnv[]>() {
				public LookupEnv[] create() 
						throws IOException,ConfigurationException {
					return (LookupEnv[]) conf.getEntry(
						getConfigComp(),
						"lookupenvs", (new LookupEnv[]{}).getClass() );
				}
			}, cache );
	}

	/**
	 *  The setof group names for this application using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>groups</b>
	 */
	public String[] getGroups() throws IOException,ConfigurationException {
		return getGroups( (Object)null, false );
	}

	/**
	 *  The setof group names for the passed NameableObject
	 *  using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>groups</b>
	 */
	public String[] getGroups(NameableObject no) 
			throws IOException,ConfigurationException {
		return getGroups(no,false);
	}

	/**
	 *  The setof group names for the passed NameableObject
	 *  using its package and name.
	 *  The value returned <b>is</b> cached based on
	 *  the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>groups</b>
	 */
	public String[] getGroups( final NameableObject no,
			final boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,String[]>().get( groups, no,
			new ObjectCreator<String[]>() {
				public String[] create()
						throws IOException,ConfigurationException {
					return (String[])conf.getEntry( no.getPackage()+"."+no.getName(),
						"groups", (new String[]{}).getClass(), new String[]{""} );				
				}
			}, cache );
	}

	/**
	 *  The setof group names for this application using its package and name.
	 *  The value returned <b>is</b> cached based on the value passed
	 *  in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>groups</b>
	 */
	public String[] getGroups( Object obj, boolean cache )
			throws IOException,ConfigurationException {
		return new CacheAcces<Object,String[]>().get( groups, obj,
			new ObjectCreator<String[]>() {
				public String[] create()
						throws IOException,ConfigurationException {
					return (String[])conf.getEntry( getConfigComp(),
						"groups", (new String[]{}).getClass(), new String[]{""} );
				}
			}, cache );
	}
	
	/**
	 *  The setof group names for this application using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>entries</b>
	 */
	public Entry[] getInitialEntrys() 
			throws IOException,ConfigurationException {
		return getInitialEntrys( (Object)null, false );
	}

	/**
	 *  The setof group names for the passed
	 *  NameableObject using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>entries</b>
	 */
	public Entry[] getInitialEntrys(NameableObject no) 
			throws IOException,ConfigurationException {
		return getInitialEntrys(no,false);
	}

	/**
	 *  The setof group names for the passed 
	 *  NameableObject using its package and name.
	 *  The value returned <b>is</b> cached based on
	 *  the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>entries</b>
	 */
	public Entry[] getInitialEntrys( final NameableObject no, 
			final boolean cache )
				throws IOException,ConfigurationException {
		return new CacheAcces<Object,Entry[]>().get( entries, no,
			new ObjectCreator<Entry[]>() {
				public Entry[] create()
						throws IOException,ConfigurationException {
					if( log.isLoggable(Level.FINE) ) {
						log.fine("Looking for \"entries\" in "+no.getPackage()+"."+no.getName() );
					}
					return (Entry[])conf.getEntry( no.getPackage()+"."+no.getName(),
						"entries", (new Entry[]{}).getClass(),
						new Entry[]{new Name(no.getName())} );				
				}
			}, cache );
	}

	/**
	 *  The setof group names for this
	 *  application using its package and name.
	 *  The value returned <b>is</b> cached based on 
	 *  the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>entries</b>
	 */
	public Entry[] getInitialEntrys( Object obj,
			boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,Entry[]>().get( entries, obj,
			new ObjectCreator<Entry[]>() {
				public Entry[] create() 
						throws IOException,ConfigurationException {
					if( log.isLoggable(Level.FINE) ) {
						log.fine("Looking for \"entries\" in "+getConfigComp() );
					}
					return (Entry[])conf.getEntry( getConfigComp(),
						"entries", (new Entry[]{}).getClass(), 
						new Entry[] { new Name(getName()) } );
				}
			}, cache );
	}

	/**
	 *  The setof group names for this 
	 *  application using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>locators</b>
	 */
	public LookupLocator[] getLocators()
			throws IOException,ConfigurationException {
		return getLocators( (Object)null, false );
	}

	/**
	 *  The setof group names for the passed 
	 *  NameableObject using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>locators</b>
	 */
	public LookupLocator[] getLocators(NameableObject no) 
			throws IOException,ConfigurationException {
		return getLocators(no,false);
	}

	/**
	 *  The setof group names for the passed NameableObject
	 *  using its package and name.
	 *  The value returned <b>is</b> cached 
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>locators</b>
	 */
	public LookupLocator[] getLocators( final NameableObject no,
			final boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,LookupLocator[]>().get( locators, no,
			new ObjectCreator<LookupLocator[]>() {
				public LookupLocator[] create()
						throws IOException,ConfigurationException {
					return (LookupLocator[])conf.getEntry( no.getPackage()+"."+no.getName(),
						"locators", (new LookupLocator[]{}).getClass() );
				}
			}, cache );
	}

	/**
	 *  The setof group names for this application
	 *  using its package and name.
	 *  The value returned <b>is</b> cached
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>locators</b>
	 */
	public LookupLocator[] getLocators( Object obj,
			boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,LookupLocator[]>().get( locators, obj,
			new ObjectCreator<LookupLocator[]>() {
				public LookupLocator[] create() 
						throws IOException,ConfigurationException {
					return (LookupLocator[])conf.getEntry( getConfigComp(),
						"locators", (new LookupLocator[]{}).getClass() );
				}
			}, cache );
	}

	/**
	 *  The setof group names for this 
	 *  application using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>template</b>
	 */
	public ServiceTemplate getTemplate()
			throws IOException,ConfigurationException {
		return getTemplate( (Object)null, false );
	}

	/**
	 *  The setof group names for the passed 
	 *  NameableObject using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>template</b>
	 */
	public ServiceTemplate getTemplate(NameableObject no) 
			throws IOException,ConfigurationException {
		return getTemplate(no,false);
	}

	/**
	 *  The setof group names for the passed NameableObject
	 *  using its package and name.
	 *  The value returned <b>is</b> cached 
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>template</b>
	 */
	public ServiceTemplate getTemplate( final NameableObject no,
			final boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,ServiceTemplate>().get( templates, no,
			new ObjectCreator<ServiceTemplate>() {
				public ServiceTemplate create()
						throws IOException,ConfigurationException {
					return (ServiceTemplate)conf.getEntry( no.getPackage()+"."+no.getName(),
						"template", ServiceTemplate.class );
				}
			}, cache );
	}

	/**
	 *  The setof group names for this application
	 *  using its package and name.
	 *  The value returned <b>is</b> cached
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>template</b>
	 */
	public ServiceTemplate getTemplate( Object obj,
			boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,ServiceTemplate>().get( templates, obj,
			new ObjectCreator<ServiceTemplate>() {
				public ServiceTemplate create() 
						throws IOException,ConfigurationException {
					return (ServiceTemplate)conf.getEntry( getConfigComp(),
						"template", ServiceTemplate.class );
				}
			}, cache );
	}

	protected interface ObjectCreator<T> {
		public T create() throws IOException,ConfigurationException;
	}

	static class CacheAcces<K, T> {
		Logger log = Logger.getLogger(getClass().getName());
		public CacheAcces() {
		}
		protected T get( Hashtable<K,T> h, K key,
				ObjectCreator<T> crt, boolean cache )
				throws IOException, ConfigurationException {
	
			if( log.isLoggable( Level.FINER ) ) {
				log.finer( "handleCacheable type [cached="+cache+
					"]: cache="+h+" for "+key );
			}
			if( cache && key != null ) {
				if( h.get( key ) != null ) {
					if( log.isLoggable( Level.FINEST ) ) {
						log.finest("handleCacheable: using "+
							h.get(key)+" cached value for "+key );
					}
					return h.get( key );
				} else if( log.isLoggable(Level.FINEST ) ) {
					log.finest("handleCacheable: no match for "+key+" in "+h );
				}
			}
	
			try {
				T obj = crt.create();
				if( cache && key != null ) {
					if( log.isLoggable( Level.FINEST ) ) {
						log.finest( "handleCacheable: Caching "+obj+" in "+h+" with key="+key );
					}
					h.put( key, obj );
				}
				if( log.isLoggable( Level.FINE ) ) {
					log.fine( "handleCacheable: Returning Object: "+obj );
				}
				return obj;
			} catch( NoSuchEntryException ex ) {
				log.log(Level.FINE, "handleCacheable: "+ex, ex);
				return null;
			}
		}
	}

	protected Object handleCacheable( Hashtable<Object,Object> h, Object key,
			ObjectCreator crt, boolean cache )
			throws IOException, ConfigurationException {

		if( log.isLoggable( Level.FINER ) ) {
			log.finer( "handleCacheable type [cached="+cache+"]: cache="+h+" for "+key );
		}
		if( cache && key != null ) {
			if( h.get( key ) != null ) {
				if( log.isLoggable( Level.FINEST ) ) {
					log.finest("handleCacheable: using "+h.get(key)+" cached value for "+key );
				}
				return h.get( key );
			} else if( log.isLoggable(Level.FINEST ) ) {
				log.finest("handleCacheable: no match for "+key+" in "+h );
			}
		}

		try {
			Object obj = crt.create();
			if( cache && key != null ) {
				if( log.isLoggable( Level.FINEST ) ) {
					log.finest( "handleCacheable: Caching "+obj+" in "+h+" with key="+key );
				}
				h.put( key, obj );
			}
			if( log.isLoggable( Level.FINE ) ) {
				log.fine( "handleCacheable: Returning Object: "+obj );
			}
			return obj;
		} catch( NoSuchEntryException ex ) {
			logException( Level.FINE, "handleCacheable", ex );
			return null;
		}
	}

	protected void logException( Level l, String msg, Throwable ex ) {
		log.log( l, msg, ex );
		logException( msg, ex );
	}

	protected void logException( String msg, Throwable ex ) {
		log.throwing( getClass().getName(), msg, ex );
	}

	/**
	 *  The Exporter for this application using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>exporter</b>
	 */
	public Exporter getExporter() throws IOException,ConfigurationException {
		return getExporter( (Object)null, false );
	}

	/**
	 *  The Exporter for the passed NameableObject using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>exporter</b>
	 */
	public Exporter getExporter(NameableObject no) 
			throws IOException,ConfigurationException {
		return getExporter(no,false);
	}

	/**
	 *  The Exporter for the passed NameableObject using its package and name.
	 *  The value returned <b>is</b> cached 
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>exporter</b>
	 */
	public Exporter getExporter( final NameableObject no,
			final boolean cache ) throws IOException,ConfigurationException {
		return getExporter( no, cache, new net.jini.jrmp.JrmpExporter() );
	}

	/**
	 *  The Exporter for the passed NameableObject using its package and name.
	 *  The value returned <b>is</b> cached 
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>exporter</b>
	 */
	public Exporter getExporter( final NameableObject no,
			final boolean cache, final Exporter defaultExp ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,Exporter>().get( exporters, no,
			new ObjectCreator<Exporter>() {
				public Exporter create()
						throws IOException,ConfigurationException {
					if( log.isLoggable( Level.FINEST ) ) {
						log.finest("Getting new Exporter["+cache+"] for: "+
							no.getPackage()+"."+no.getName()+", default: "+defaultExp );
					}
					return (Exporter)conf.getEntry( no.getPackage()+"."+no.getName(),
						"exporter", Exporter.class,
							defaultExp );				}
			}, cache );
	}
	
	public static class AccessPermission extends java.security.BasicPermission {
		public AccessPermission( String arg ) {
			super(arg);
		}
		public AccessPermission( String arg, String acts ) {
			super( arg, acts );
		}
	}

	public Class getJeriPermissionClass() {
		return AccessPermission.class;
	}
	
	/**
	 *  Returns the default set of constraints to use for
	 *  the default exporter.
	 */
	public InvocationConstraints getJeriInvocationConstraints() {
		return new InvocationConstraints(
				new InvocationConstraint[]{},
				new InvocationConstraint[]{} 
			);
	}
	
	public ServerEndpoint getDefaultServerEndpoint() {
		return SslServerEndpoint.getInstance(0);
	}

	/**
	 *  The Exporter for this application using its package and name.
	 *  The value returned <b>is</b> cached
	 *  based on the value passed in <code>cache</code>.
	 * <p>
	 *  The {@link #getDefaultWireProtocol()} method is called
	 *  to establish which default exporter to provide if none
	 *  is specified in the configuration.  For "JERI", the
	 *  default is
	 *  <pre>
			new BasicJeriExporter( 
					getDefaultServerEndpoint(),
					new BasicILFactory(
						new BasicMethodConstraints(
							getJeriInvocationConstraints() ),
						getJeriPermissionClass()
					)
				);
		</pre>
	 *
	 *  <br>Configuration entry name: <b>exporter</b>
	 */
	public Exporter getExporter( Object obj,
			boolean cache ) throws IOException,ConfigurationException {
		Exporter exp;
		if( getDefaultWireProtocol().equals( "JERI" ) ) {
			exp = new BasicJeriExporter( 
					getDefaultServerEndpoint(),
					new BasicILFactory(
						new BasicMethodConstraints(
							getJeriInvocationConstraints() ),
						getJeriPermissionClass()
					)
				);
		} else {
			exp = new net.jini.jrmp.JrmpExporter();
		}
		return getExporter( obj, cache, exp );
	}

	/**
	 *  The Exporter for this application using its package and name.
	 *  The value returned <b>is</b> cached
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>exporter</b>
	 */
	public Exporter getExporter( final Object obj,
		final boolean cache, final Exporter defaultExp ) throws IOException,ConfigurationException {
		if( log.isLoggable(Level.FINEST)) {
			log.finest("Getting ("+getConfigComp()+
				") Exporter["+cache+"] for: "+obj+", default: "+defaultExp );
		}
		return new CacheAcces<Object,Exporter>().get( exporters, obj,
			new ObjectCreator<Exporter>() {
				public Exporter create()
						throws IOException,ConfigurationException {
					if( log.isLoggable( Level.FINEST ) ) {
						log.finest("Getting ("+getConfigComp()+
							")new Exporter["+cache+"] for: "+obj+", default: "+defaultExp );
					}
					return (Exporter)conf.getEntry( getConfigComp(),
						"exporter", Exporter.class,
						defaultExp );
				}
			}, cache );
	}

	/**
	 *  When no key is provided by calling exportObject() without
	 *  args, there is no cached exporter.  So, this method
	 *  will just use the config exporter to unexport.
	 */
	public boolean unexportObject( boolean force )
			throws IOException, ConfigurationException {
		Exporter exp = getExporter((Object)null,false);
		exporters.remove(this);
		return exp.unexport( force);
	}

	public boolean unexportNameableObject(NameableObject ctx, boolean force)
			throws IOException, ConfigurationException {
		Exporter exp = getExporter(ctx,force);
		exporters.remove(ctx);
		return exp.unexport(force);
	}

	public boolean unexportObject(Object obj, boolean force) 
			throws IOException, ConfigurationException {
		Exporter exp = getExporter( obj, force );
		exporters.remove(obj);
		return exp.unexport(force);
	}

	/**
	 *  The ProxyTrustVerifier for this 
	 *  application using its package and name .
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>proxyTrustVerifier</b>
	 */
	public Class getProxyTrustVerifier() 
			throws IOException,ConfigurationException {
		return getProxyTrustVerifier( (Object)null, false );
	}

	/**
	 *  The ProxyTrustVerifier for the passed
	 *  NameableObject using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>proxyTrustVerifier</b>
	 */
	public Class getProxyTrustVerifier(NameableObject no)
			throws IOException,ConfigurationException {
		return getProxyTrustVerifier(no,false);
	}

	/**
	 *  The ProxyTrustVerifier for the passed
	 *  NameableObject using its package and name.
	 *  The value returned <b>is</b> cached
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>proxyTrustVerifier</b>
	 */
	public Class getProxyTrustVerifier( final NameableObject no, 
			final boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,Class>().get( verifiers, no,
			new ObjectCreator<Class>() {
				public Class create() 
						throws IOException,ConfigurationException {
					return (Class) conf.getEntry( no.getPackage()+"."+no.getName(),
						"proxyTrustVerifier", Class.class,
						com.sun.jini.proxy.BasicProxyTrustVerifier.class );	
				}
			}, cache );
	}

	/**
	 *  The ProxyTrustVerifier for this
	 *  application using its package and name.
	 *  The value returned <b>is</b> cached based
	 *  on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>proxyTrustVerifier</b>
	 */
	public Class getProxyTrustVerifier( Object obj,
			boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,Class>().get( verifiers, obj,
			new ObjectCreator<Class>() {
				public Class create()
						throws IOException,ConfigurationException {
					return (Class)conf.getEntry( getConfigComp(),
						"proxyTrustVerifier", Class.class );
				}
			}, cache );
	}

	/**
	 *  The Exporter for this application using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>preparer</b>
	 */
	public ProxyPreparer getProxyPreparer()
			throws IOException,ConfigurationException {
		return getProxyPreparer( (Object)null, false );
	}

	/**
	 *  The ProxyPreparer for the passed
	 *  NameableObject using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>preparer</b>
	 */
	public ProxyPreparer getProxyPreparer(NameableObject no) 
			throws IOException,ConfigurationException {
		return getProxyPreparer(no,false);
	}

	/**
	 *  The ProxyPreparer for the passed 
	 *  NameableObject using its package and name.
	 *  The value returned <b>is</b> cached
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>preparer</b>
	 */
	public ProxyPreparer getProxyPreparer( final NameableObject no,
			final boolean cache ) throws IOException,ConfigurationException {
		ProxyPreparer rprep = null;
		if( getDefaultWireProtocol().equals( "JERI" ) ) {
			rprep = new BasicProxyPreparer();
		} else {
			rprep = null;
		}
		final ProxyPreparer prep = rprep;

		return new CacheAcces<Object,ProxyPreparer>().get( preparers, no,
			new ObjectCreator<ProxyPreparer>() {
				public ProxyPreparer create()
						throws IOException,ConfigurationException {
					if(log.isLoggable(Level.FINE)) {
						log.fine("getting \"preparer\" from "+no.getPackage()+"."+no.getName() );
					}
					if( prep == null ) {
						return (ProxyPreparer)
							conf.getEntry( no.getPackage()+"."+no.getName(),
							"preparer", ProxyPreparer.class );	
					} else {
						return (ProxyPreparer)
							conf.getEntry( no.getPackage()+"."+no.getName(),
							"preparer", ProxyPreparer.class, prep );
					}
				}
			}, cache );
	}

	/**
	 *  The ProxyPreparer for this application using its package and name.
	 *  The value returned <b>is</b> cached 
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>preparer</b>
	 */
	public ProxyPreparer getProxyPreparer( Object obj, 
			boolean cache ) throws IOException,ConfigurationException {
		ProxyPreparer rprep = null;
		if( getDefaultWireProtocol().equals( "JERI" ) ) {
			rprep = new BasicProxyPreparer();
		} else {
			rprep = null;
		}
		final ProxyPreparer prep = rprep;

		return new CacheAcces<Object,ProxyPreparer>().get( preparers, obj,
			new ObjectCreator<ProxyPreparer>() {
				public ProxyPreparer create()
						throws IOException,ConfigurationException {
					if(log.isLoggable(Level.FINE)) {
						log.fine("getting \"preparer\" from "+getConfigComp() );
					}
					if( prep == null ) {
						return (ProxyPreparer)conf.getEntry( getConfigComp(),
							"preparer", ProxyPreparer.class );
					} else {
						return (ProxyPreparer)conf.getEntry( getConfigComp(),
							"preparer", ProxyPreparer.class, prep );
					}
				}
			}, cache );
	}

	/**
	 *  The Exporter for this application using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>logger</b>
	 */
	public Logger getLogger() throws IOException,ConfigurationException {
		return getLogger( (Object)null, false );
	}

	/**
	 *  The ProxyPreparer for the passed
	 *  NameableObject using its package and name.
	 *  The value returned <b>is not</b> cached by default.
	 *
	 *  <br>Configuration entry name: <b>logger</b>
	 */
	public Logger getLogger(NameableObject no) 
			throws IOException,ConfigurationException {
		return getLogger(no,false);
	}

	/**
	 *  The Logger for the passed NameableObject using its package and name.
	 *  The value returned <b>is</b> cached
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>logger</b>
	 */
	public Logger getLogger( final NameableObject no, 
			final boolean cache ) throws IOException,ConfigurationException {
		return new CacheAcces<Object,Logger>().get( loggers, no,
			new ObjectCreator<Logger>() {
				public Logger create() 
						throws IOException,ConfigurationException {
					Logger l = (Logger) conf.getEntry( no.getPackage()+"."+no.getName(),
						"logger", Logger.class, log );
					log.fine("returning ("+log+") logger: "+l );
					return l;
				}
			}, cache );
	}

	/**
	 *  The Logger for this application using its package and name.
	 *  The value returned <b>is</b> cached 
	 *  based on the value passed in <code>cache</code>.
	 *
	 *  <br>Configuration entry name: <b>logger</b>
	 */
	public Logger getLogger( Object obj,
			boolean cache ) throws IOException,ConfigurationException {
		log.fine("getLogger("+obj+", "+cache+")" );
		return new CacheAcces<Object,Logger>().get( loggers, obj,
			new ObjectCreator<Logger>() {
				public Logger create() 
						throws IOException,ConfigurationException {
					Logger l = (Logger)conf.getEntry( getConfigComp(),
						"logger", Logger.class, log );
					log.fine("returning ("+log+") logger: "+l );
					return l;
				}
			}, cache );
	}
	
	/**
	 *  No args constructor provided for more complex initialization.  If you use this
	 *  constructor, you have to call <code>configure()</code>.
	 */
	protected ConfigurableJiniApplication() {
	}

	/**
	 *  Establishes the Configuration to 
	 *  use based on <code>args</code> and initializes
	 *  the caches, and sets the security manager to the value returned 
	 *  from <code>getSecurityManager</code>
	 *  only if a security manager is not already set.
	 */
	public ConfigurableJiniApplication( String args[] )
			throws ConfigurationException, IOException {
		Configuration conf = ConfigurationProvider.getInstance(
			args, getClass().getClassLoader() );
		configure( conf );
	}

	/**
	 *  Establishes the Configuration to 
	 *  use based on <code>args</code> and initializes
	 *  the caches, and sets the security manager to the value returned 
	 *  from <code>getSecurityManager</code>
	 *  only if a security manager is not already set.
	 */
	public ConfigurableJiniApplication( String args[], ClassLoader ldr )
			throws ConfigurationException, IOException {
		configure( ConfigurationProvider.getInstance(args, ldr) );
	}

	/**
	 *  Establishes the Configuration using the passed value and initializes
	 *  the caches, and sets the security manager to the value returned 
	 *  from <code>getSecurityManager</code>
	 *  only if a security manager is not already set.
	 */
	public ConfigurableJiniApplication( Configuration conf )
			throws ConfigurationException, IOException  {
		configure( conf );
	}

	static protected Hashtable hndlrs = new Hashtable();
	/**
	 *  Attach the default console handler that logs to stdout.
	 *  The application can override this method to attach a different
	 *  handler.
	 *
	 *  This method uses a cache of attached handlers so that
	 *  multiple instances of this class or any subclass will
	 *  only attach the handler once.  If you want different
	 *  handlers for different instances, you should override
	 *  this method to provide the appropriate logic.
	 *
	 *  @return the handler attached to the passed logger.
	 */
	protected Handler attachDefaultHandler( Logger log ) throws ConfigurationException {
		Handler h = null;
//			if( (h = (Handler)hndlrs.get( getConfigComp() ) ) == null ) {
//	//			System.out.println("got logger: "+log.getName() );
//				log.setLevel( getInitialLogLevel( log ) );
//				h = getDefaultHandler( log );
//				if( h != null ) {
//					h.setLevel( getInitialLogLevel( log ) );
//					log.addHandler( h );
//	//				System.out.println("final logger: "+log.getName() );
//					hndlrs.put( getConfigComp(), h );
//				}
//			}
		return h;
	}
	
	protected InputStream getLoggingConfigStream() throws ConfigurationException, IOException {
		try {
			String logConfig = (String)conf.getEntry( getConfigComp(),
				"loggingConfig", String.class );
			return new StringBufferInputStream( logConfig );
		} catch( NoSuchEntryException ex ) {
			log.log( Level.CONFIG, ex.toString(), ex );
		}
		return null;
	}
	
	/**
	 *  Returns the default Handler implementation to use
	 *  for the service.  This is a {@link PrintStreamHandler}
	 *  by default that is pointed at System.out, logging Level.ALL
	 */
	protected Handler getDefaultHandler( Logger log ) {
		return new PrintStreamHandler( log.getName(), System.out, Level.ALL );
	}

	/**
	 *  This method must be called by subclasses to configure things that are
	 *  established from the configuration.
	 */
	protected void configure( Configuration conf )
			throws ConfigurationException, IOException {
		this.conf = conf;
		if( conf == null )
			throw new NullPointerException( "non-null Configuration required");
		loggers = new Hashtable<Object,Logger>();
		log = Logger.getLogger(getConfigComp(), null );
		Logger mlog = getLogger();
		if( mlog != null ) {
			log = mlog;
			log.config("using application provided logger");
		}

		InputStream logProps = getLoggingConfigStream();
		if( logProps != null ) {
			log.warning("Reading new logging config");
			LogManager lm = LogManager.getLogManager();
			lm.readConfiguration( logProps );
			log = Logger.getLogger(getConfigComp(), null );
			mlog = getLogger();
			if( mlog != null ) {
				log = mlog;
				log.config("using application provided logger");
			}
			log.warning("Logging Configuration read");
		} else {
			attachLoggersToHandler( log, attachDefaultHandler( log ) );
		}
		
		log.info("Logging setup with logger assigned");

		exporters = new Hashtable<Object,Exporter>();
		preparers = new Hashtable<Object,ProxyPreparer>();
		lookupenvs = new Hashtable<Object,LookupEnv[]>();
		locators = new Hashtable<Object,LookupLocator[]>();
		groups = new Hashtable<Object,String[]>();

		// Don't attach the security manager till here so that
		// we can do anything needed, above, without
		// security active.
		if( System.getSecurityManager() == null ) {
			SecurityManager sec = getSecurityManager();
			if( sec != null ) {
				if( log.isLoggable( Level.CONFIG ) ) {
					log.config("Attaching security Manager: "+sec);
				}
				System.setSecurityManager( sec );
			}
		}
	}
	
	protected void attachLoggersToHandler( Logger log, Handler h ) throws ConfigurationException {
		if( h == null )
			return;
		String[] logs = (String[]) conf.getEntry( 
			getConfigComp(),
			"otherLoggers", new String[]{}.getClass(),
			new String[]{} );
		otherLoggers = new Vector<Logger>();	
		for( int i = 0; i < logs.length; ++i ) {
			log.config("Attaching Handler to "+logs[i] );
			Logger l = Logger.getLogger( logs[i] );
			// Only if there is a default handler,
			// will we attach it.
			if( h != null ) {
				l.config( "adding handler: "+h );
				l.addHandler( h );
			}
			otherLoggers.addElement(l);
			l.config("Setting log level to: "+log.getLevel());
			l.setLevel( log.getLevel() );
		}
	}
	
	protected Vector<Logger> otherLoggers;

	/**
	 *  Specifies the logging level to use at application startup.
	 *  @return Level.ALL
	 */
	public Level getInitialLogLevel() throws ConfigurationException {
		return getInitialLogLevel( log );
	}

	/**
	 *  @param log the logger to get the level for.
	 */
	public Level getInitialLogLevel(Logger log) throws ConfigurationException {
		try {
			return (Level)conf.getEntry( 
				getConfigComp(), "logLevel",
				Level.class, 
				log.getLevel() != null ? log.getLevel() : Level.ALL );
		} catch( NoSuchEntryException ex ) {
			// Go ahead and report this case, and then return Level.ALL.
			log.log( Level.CONFIG, "getInitialLogLevel for "+log.getName()+": "+ex, ex );
		} catch( ConfigurationException ex ) {
			ex.printStackTrace();
			throw ex;
		}
		return Level.ALL;
	}

	/**
	 *  Set the log level for this applications logger
	 */
	public void setLogLevel( Level level ) {
		log.setLevel( level );
		for( int i = 0;	 otherLoggers != null && i < otherLoggers.size(); ++i ) {
			((Logger)otherLoggers.elementAt(i)).setLevel( level );
		}
	}
	
	public static String templateToString( ServiceTemplate tmpl ) {
		String str = null;
		if( tmpl.serviceID != null )
			str = tmpl.serviceID.toString();
		if( tmpl.serviceTypes != null ) {
			if( str != null )
				str += ", Class={";
			else
				str = "Class={";
			for( int i = 0 ;i < tmpl.serviceTypes.length; ++i ) {
				if( i > 0 )
					str += ",";
				str += tmpl.serviceTypes[i].getName();
			}
			str += "}";
		}
		if( tmpl.attributeSetTemplates == null ) {
			if( str != null )
				str += ", Entry={";
			else
				str = "Entry={";
			for( int i = 0; i < tmpl.attributeSetTemplates.length; ++i ) {
				if( i > 0 )
					str += ",";
				str += tmpl.attributeSetTemplates[i].
					getClass().getName()+"=[";
				str += fieldsForEntry( tmpl.attributeSetTemplates[i] );
				str += "]";
			}
		}

		return str;
	}
	
	static class Test extends ConfigurableJiniApplication implements Remote {
		public String getPackage() {
			return "config";
		}
		public String getName() {
			return "Test";
		}
		public Test( String[]args ) throws Exception {
			super(args);
		}
	}
//	
//	public static void main( String args[] ) throws Exception {
//		Test test = new Test(args);
//		Object exp = test.getExporter( test, true );
//		test.log.config("exporters.get("+exp+"): "+test.exporters.get(exp) );
//		test.log.config("exporters.get("+test+"): "+test.exporters.get(test) );
//	}

	public static String fieldsForEntry( Entry ent ) { 
		Class c = ent.getClass();
		java.lang.reflect.Field f[] = c.getFields();
		String str = "";
		for( int i = 0; i < f.length; ++i ) {
			if( i > 0 )
				str += ",";
			str += f[i].getName()+"("+f[i].getType().getName()+")=";
			try {
				str += f[i].get(ent);
			} catch( IllegalAccessException ex ) {
				str += "<Value inaccessible>";
			}
		}
		
		return str;
	}

	/** 
	 *  Convenience method to get a configuration entry for the current package.name
	 *  associated with this object.
	 */
	public Object getConfigData( String entry, Class entryClass ) throws IOException,ConfigurationException {
		return getConfigData( this, entry, entryClass );	
	}

	/** 
	 *  Convenience method to get a configuration entry for the passed NameableObject.
	 */
	public Object getConfigData( final NameableObject no,
			String entry, Class entryClass ) throws IOException,ConfigurationException {
		return conf.getEntry( no.getPackage()+"."+no.getName(),
						entry, entryClass );	
	}

	/** 
	 *  Get the application resource bundle for logging and other use.
	 *  @return default returns null.
	 */
	public ResourceBundle getResourceBundle() {
		return null;
	}
}