package org.wonderly.jini2.lookup;

import java.io.*;
import net.jini.lookup.*;
import net.jini.core.lookup.*;
import net.jini.discovery.*;
import net.jini.config.*;
import java.rmi.*;
import net.jini.core.event.*;
import net.jini.core.discovery.*;
import net.jini.export.*;
import net.jini.jeri.*;
import net.jini.jeri.tcp.*;
import net.jini.core.lease.*;
import net.jini.lease.*;
import java.util.*;
import net.jini.core.entry.*;
import net.jini.lookup.entry.*;
import java.util.logging.*;

/**
 *  This is a demonstration example class that is not useful yet.
 */
public class CachingDiscoveryManager {
	LookupDiscoveryManager ldm;
	protected Configuration config;
	protected LookupLocator lookups[];
	protected String[]groups;
	protected long timeout;
	protected ServiceTemplate templ;
	protected Logger log = Logger.getLogger( getClass().getName() );

	public static void main( String args[] ) throws Exception {
		System.setSecurityManager( new RMISecurityManager() );
		Configuration conf = ConfigurationProvider.getInstance( args );
		CachingDiscoveryManager cdm = new CachingDiscoveryManager(
			conf, new ServiceTemplate( null, null, new Entry[]{ new UIDescriptor() } ),
				new LookupLocator[]{ new LookupLocator("jini://localhost") },
				new String[]{""}, 15000 );	
		synchronized( cdm ) {
			cdm.wait();
		}
	}

	public CachingDiscoveryManager( Configuration conf, ServiceTemplate tmpl, 
				LookupLocator[] locs, String[]grps, long timeout ) throws
						IOException, ConfigurationException {
		this.config = conf;
		this.lookups = locs;
		this.groups = grps;
		this.timeout = timeout;
		this.templ = tmpl;
		activateLookup( config, lookups, grps );
	}

	public void activateLookup( Configuration conf, LookupLocator[] locs, String[]grps )
			throws IOException,ConfigurationException {
		ldm = new LookupDiscoveryManager( grps, locs, new DiscoveryListener() {
			public void discovered( DiscoveryEvent e ) {
				ServiceRegistrar[]regs = e.getRegistrars();
				for( ServiceRegistrar r: regs ) {
					try {
						log.info("Discovered: "+r);
						doDiscover( r );
					} catch( Exception ex ) {
						log.log( Level.SEVERE, ex.toString(), ex );
					}
				}
			}
			public void discarded( DiscoveryEvent e ) {
				ServiceRegistrar[]regs = e.getRegistrars();
				for( ServiceRegistrar r: regs ) {
					try {
						log.info("Discarding: "+r );
						doDiscarded( r );
					} catch( Exception ex ) {
						log.log( Level.SEVERE, ex.toString(), ex );
					}
				}
			}
		}, conf );
	}
	
	Hashtable<Object,Exporter> exports = new Hashtable<Object,Exporter>();
	protected Remote export( Remote obj, String type ) throws ConfigurationException, IOException {
		Exporter exp = (Exporter)config.getEntry( getClass().getName(), 
			type, Exporter.class, new BasicJeriExporter(
				TcpServerEndpoint.getInstance(0), new BasicILFactory() ) );
		Remote ol = exp.export(obj);
		exports.put( obj, exp );
		return ol;
	}
	
	private void doDiscarded( final ServiceRegistrar r ) {
	}
	
	LeaseRenewalManager lrm;
	Set<EventRegistration>erls = new HashSet<EventRegistration>();
	private void doDiscover( final ServiceRegistrar r ) throws UnknownLeaseException,
			ConfigurationException, IOException  {
		RemoteEventListener rl = new RemoteEventListener() {
			public void notify( RemoteEvent ev ) {
			}
		};
		RemoteEventListener erl = (RemoteEventListener)export(rl,"notifyExporter");
		int trans = r.TRANSITION_MATCH_NOMATCH |
			r.TRANSITION_NOMATCH_MATCH |
			r.TRANSITION_MATCH_MATCH;
		MarshalledObject handback = null;
		log.info("Trying notify for: "+timeout+" millis" );
		EventRegistration er = r.notify( templ, trans, erl, handback, timeout );  //5 minute lease on notify
		erls.add( er );
		final Lease l = er.getLease();
		lrm = new LeaseRenewalManager();
		log.info("lease class: "+l.getClass().getName() );

		lrm.renewFor( l, timeout, new LeaseListener() {
			public void notify( LeaseRenewalEvent e ) {
				try {
					log.info("Lease event: "+e );
					doDiscover( r );
				} catch( Exception ex ) {
					log.log( Level.SEVERE, ex.toString(), ex );
				}
			}
		});

		new Thread() {
			public void run() {
				while(true) {
					try {
						log.info(l+" expires "+new Date(lrm.getExpiration(l)) );
						Thread.sleep(2500);
					} catch( UnknownLeaseException ex ) {
						log.log( Level.FINE, ex.toString(),ex );
						while( true ) {
						try {
							doDiscover( r );
							return;
						} catch( Exception exx ) {
							log.log( Level.WARNING, exx.toString(), exx );
							try {
							Thread.sleep(30000);
							} catch( Exception exxx ) {}
						}
						}
					} catch( Exception ex ) {
						log.log(Level.SEVERE,ex.toString(),ex);
						
					}
				}
			}
		}.start();
		ServiceMatches m = r.lookup( templ, Integer.MAX_VALUE );
		checkMatches( m );
	}

	Hashtable<ServiceID,ServiceItem> svcMap = new Hashtable<ServiceID,ServiceItem>();

	protected boolean haveService( ServiceID id ) {
		return svcMap.get(id) != null;
	}

	protected void addService( ServiceItem si ) {
		svcMap.put( si.serviceID, si );
	}

	protected void checkService( ServiceItem si ) {
		ServiceItem oi = svcMap.get(si.serviceID);
		svcMap.put( si.serviceID, si );
	}

	protected void checkMatches( ServiceMatches m ) {
		for( ServiceItem si : m.items ) {
			if( haveService( si.serviceID ) == false ) {
				addService( si );
			} else {
				checkService( si );
			}
		}
	}
}