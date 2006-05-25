package org.wonderly.util.jini2;

import java.io.IOException;
import java.net.MalformedURLException;
import net.jini.loader.pref.PreferredClassProvider;
import net.jini.url.httpmd.HttpmdUtil;
import net.jini.config.*;
import java.util.logging.*;
import java.util.*;
import java.net.*;
import java.rmi.*;

import org.wonderly.jini2.*;
import org.wonderly.util.jini.LookupEnv;
import org.wonderly.util.jini2.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;

/**
 *  This class is an extension of the PreferredClassProvider
 *  which uses an HttpServerAccess implementing service 
 *  to dynamically establish the codebase to annotate a class with.
 *
 *  <table border>
 *  <tr><th colspan="3">System Properties Used</td></tr>
 *  <tr><th>Property<th>Type<th>Description</tr>
 *  <tr><td>codeserver.path<td>Director Name</td>Path
 *      to file store for validating signatures</tr>
 *  <tr><td>codeserver.config.file<td>File Name<td>Name 
 *      of ConfigurationFile compatible configuration file</tr>
 *  </table>
 *
 *  The ConfigurationFile contents can specified one or more
 *  the the following entries under the following key
 *  <code>org.wonderly.util.jini2.DynamicClassAnnotationProvider</code>
 *  <table border>
 *  <tr><th colspan="4">Configuration Entries</th></tr>
 *  <tr><th>Entry Name<th>Type<th>Default<th>Description</tr>
 *  <tr><td>lookupenv<td><td><td></tr>
 *  <tr><td>sumtype<td>String<td>"md5"
 *		<td>Minimal required sumtype for all exported 
 *		secure (secureCodebase == true) codebases.</tr>
 *  <tr><td>secureCodeBase<td>Boolean<td>true
 *		<td>Is a secure codebase required for
 *		all exported codebases.  This property causes
 *      exclusive behavior.  When false, all used
 *      codebases will be those not using httpmd:
 *      as the protocol.  When true, all used codebases
 *      must use httpmd: as the protocol.</tr>
 *  </table>
 *
 *  @see HttpServerAccess
 *  @see HttpServerInfo
 *  @see HttpServerService
 *  @author <a href="mailto:gregg.wonderly@pobox.com">
 *		Gregg Wonderly</a>.
 */
public class DynamicClassAnnotationProvider
    extends PreferredClassProvider
{
	private Logger log = Logger.getLogger(getClass().getName());
	private String paths;
	private String type;
	private Hashtable<String,String> urls = 
		new Hashtable<String,String>();
	private Configuration config;
	private boolean secure;
    private ServiceLookup lu;
    private Hashtable<ServiceID,ServerEntry> srvs =
    	new Hashtable<ServiceID,ServerEntry>();
    private Hashtable<ServerEntry,Hashtable<String,String>> srvurls = 
    	new Hashtable<ServerEntry,Hashtable<String,String>>();
    private String currentCodebase;
    private final Object lock = new Object();
    private long lockWaitTime = 15000;

    public DynamicClassAnnotationProvider()
		throws IOException, MalformedURLException
    {
		super(false);
		log.fine("Constructing "+getClass().getName());
		try {
			paths = System.getProperty("codeserver.path");
			String file = System.getProperty(
				"codeserver.config.file");
			log.fine("paths: "+paths);
			log.fine("configFile: "+file );
			try {
				config = ConfigurationProvider.getInstance(
					new String[]{ file } );
				type = (String)config.getEntry( getClass().getName(),
					"sumtype",
					String.class, "md5" );
				secure = ((Boolean)config.getEntry(
					getClass().getName(),
					"secureCodeBase",
					Boolean.class, new Boolean(true) )
						).booleanValue();
			} catch( ConfigurationException ex ) {
				log.log( Level.SEVERE, ex.toString(), ex );
				IOException ioe = new IOException(ex.toString());
				throw(IOException)ioe.initCause(ex);
			}
			log.fine("setupLookup");
			setupLookup();
		} catch( RuntimeException ex ) {
			log.log( Level.SEVERE, ex.toString(), ex );
		}
    }

    protected void setupLookup() {
    	new Thread( "Lookup Setup Thread") {
    		public void run() {
    			try {
    				log.fine("Starting lookup of web servers");
    				startLookup();
    			} catch( Exception ex ) {
    				log.log(Level.SEVERE, ex.toString(), ex );
    			}
    		}
    	}.start();
    }

    protected void startLookup() throws IOException,
    							ConfigurationException {
    	log.fine("Creating ServiceLookup instance");
    	
    	LookupEnv env = (LookupEnv)config.getEntry(
    		getClass().getName(),
    		"lookupenv", LookupEnv.class,
    		new LookupEnv( "HTTP servers",
	    		new ServiceTemplate( null, 
	    			new Class[] {
	    				HttpServerAccess.class
	    			},
	    			new Entry[] { new HttpServerInfo() }
	    		),
    			new LookupLocator[] {
    				new LookupLocator("jini://localhost")
    			}
	    	)
    	);
    	lu = new ServiceLookup( env, 
			new ServiceLookupHandler() {
				public void serviceLost( ServiceID id, 
									ServiceRegistrar reg ) {
					log.fine("Lost server with id: "+id );
					ServerEntry se = (ServerEntry)srvs.remove(id);
					if( se != null ) {
						Hashtable<String,String> v = 
							srvurls.get(se);
						if( v != null ) {
							Enumeration<String>e = v.keys();
							while( e.hasMoreElements() ) {
								urls.remove( e.nextElement() );
							}
						}
					}
					try {
						recomputeCodebase();
					} catch( Exception ex ) {
						log.log(Level.SEVERE, ex.toString(), ex );
					}
				}
				public void updateItem( ServiceItem item, 
										ServiceRegistrar reg ) {
					log.fine("updating server with id: "+
						item.serviceID);
					updateServer( (HttpServerAccess)item.service,
						item.attributeSets, item.serviceID );
				}
				public void processItem( ServiceItem item,
										ServiceRegistrar reg ) {
					log.fine("adding server with id: "+
						item.serviceID);
					updateServer( (HttpServerAccess)item.service,
						item.attributeSets, item.serviceID );
				}
			}, 
			log,
			config );
		log.fine("Starting lookup");
    	lu.start();
    }

    private static class ServerEntry {
    	private HttpServerAccess acc;
    	private Entry[]ents;
    	private ServiceID id;
    	private HttpServerInfo inf;
    	public ServerEntry( HttpServerAccess acc, 
    			Entry[]ents, ServiceID id ) {
    		this.acc = acc;
    		this.ents = ents;
    		this.id = id;
    		for( int i = 0; i < ents.length; ++i ) {
    			if( ents[i] instanceof HttpServerInfo ) {
    				inf = (HttpServerInfo)ents[i];
    			}
    		}
    		if( inf == null ) {
    			throw new NullPointerException(
    				"No HttpServerInfo present");
    		}
    	}
    }

    
    private void updateServer( HttpServerAccess acc,
    		Entry[]ents, ServiceID id ) {
    	ServerEntry se = (ServerEntry)srvs.get(id);
    	log.fine("updateServer for id: "+id+", is: "+se );
    	if( se == null ) {
    		se = new ServerEntry( acc, ents, id );
    		srvs.put( id, se );
    		// Take first server in list, and do not
    		// act on others.
    		if( srvs.size() > 1 ) {
    			return;
    		}
    	}
    	log.fine("Recompute codebase with all servers");
		try {
			recomputeCodebase();
		} catch( Exception ex ) {
			log.log(Level.SEVERE, ex.toString(), ex );
		}
    }

    private void recomputeCodebase() throws IOException {
    	if( srvs.size() == 0 ) {
    		log.warning("No more codebase servers for: "+
    			currentCodebase);
    		// Do not reset currentCodebase, always fail
    		// gracefully with the currentCodebase set to 
    		// the last known value.
    		return;
    	}
    	log.fine("process servers: "+srvs );
    	Enumeration ese = srvs.elements();
		while( ese.hasMoreElements() ) {
			ServerEntry se = (ServerEntry)ese.nextElement();
			HttpServerInfo linf = se.inf;
	    	log.fine("Entry: "+se+", info: "+linf );
	    	if( secure && linf.proto.equals("httpmd") == false ) {
	    		log.warning( 
	    			"ignoring nonsecure codebase from "+linf );
	    		continue;
	    	} else if( !secure && linf.proto.equals("httpmd") ) {
	    		log.warning( "ignoring secure codebase from "+linf );
	    		continue;
	    	}
	    	StringTokenizer st = new StringTokenizer( paths, " ");
	    	Vector<String> v = new Vector<String>();
	    	while( st.hasMoreTokens() ) {
	    		String elem = st.nextToken();
	    		log.finer("Next Path Element: "+elem);
	    		v.addElement(elem);
	    	}

	    	try {
		    	for( int i = 0; i < v.size(); ++i ) {
		    		String url = (String)v.elementAt(i);
		    		URL u = new URL( linf.formatUrl( 
		    			url, type, "0") );
		    		String path = u.getPath().
		    			substring(1).split(";")[0];
		    		log.finer("sumFor("+path+")" );
		    		String sum = se.acc.sumFor( path, type );
		    		log.finer("sumFor("+path+") sum: "+sum );
		    		if( sum != null ) {
			    		log.fine("url: "+url+", sum is: "+sum);
			    		Hashtable<String,String> urls = srvurls.get(se);
			    		if( urls == null ) {
			    			urls = new Hashtable<String,String>();
			    			srvurls.put(se, urls);
			    		}
			    		urls.put( url, sum );
		    		} else {
		    			log.fine(se+" can't serve: "+path );
		    		}
		    	}

		    	// codebase for each url
		    	Hashtable<String,String> u = 
		    		new Hashtable<String,String>();
		  
		    	Enumeration<ServerEntry> e = srvurls.keys();
		    	while( e.hasMoreElements() ) {
		    		ServerEntry lse = e.nextElement();
		    		HttpServerInfo inf = lse.inf;
		    		// Get the URLS that this server supports
		    		Hashtable<String,String> urls = srvurls.get(lse);
		    		Enumeration<String> eu = urls.keys();
		    		while( eu.hasMoreElements() ) {
		    			String url = eu.nextElement();
		    			if( u.get( url ) != null ) {
		    				log.finer("using "+
		    					u.get(url)+" for "+url );
		    				continue;
		    			}
		    			log.finer("No URL set yet for "+url);
		    			String sum = urls.get(url);
		    			u.put( url, inf.formatUrl( url, type, sum ) );
		    		}
		    	}

		    	log.fine( "have "+u.size()+" of "+
		    		v.size()+" codebase elements handled");
		    	if( u.size() == v.size() ) {
		    		String cb = "";
		    		log.fine("Calculating new codebase URL");
		    		Enumeration ue = v.elements();
		    		while( ue.hasMoreElements() ) {
//		    		for( int i = 0; i < v.size(); ++i ) {
		    			String u1 = (String)ue.nextElement();
		    			String str = (String)u.get(u1);
		    			if( cb.length() > 0 )
		    				cb += " ";
		    			cb += str;
		    		}
		    		currentCodebase = cb;
		    		log.fine("Waking up any waiting threads: \""+
		    			currentCodebase+"\"");
		    		synchronized( lock ) {
		    			lock.notifyAll();
		    		}
		    	}
	    	} catch( RemoteException ex ) {
	    		log.log( Level.SEVERE, ex.toString(), ex );
	    		throw ex;
	    	}
    	}
    }

    protected String getClassAnnotation(ClassLoader loader) {
    	if( currentCodebase == null ) {
    		if( log.isLoggable( Level.FINEST ) ) {
				log.log( Level.FINEST, 
					"Waiting for Codebase", new Throwable() );
    		} else {
				log.fine("Waiting for Codebase for: "+paths );
    		}
    		synchronized( lock ) {
   				while( currentCodebase == null ) {
    				try {
    					log.fine( "Waiting for codebase: "+
    						lockWaitTime);
    					lock.wait(lockWaitTime);
    					log.fine( "Wakeup with codebase: "+
    						currentCodebase);
    				} catch( InterruptedException ex ) {
    					log.log( Level.SEVERE, ex.toString(), ex );
	    			} finally {
    				}
    			}
    		}
    	}
		return currentCodebase;
    }
}