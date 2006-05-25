package org.wonderly.util.jini2;

import net.jini.core.lookup.*;
import java.util.*;
import org.wonderly.util.jini.*;
import org.wonderly.util.jini2.RemoteListener;
import org.wonderly.jini.*;
import java.io.*;
import java.net.*;
import net.jini.core.event.*;
import java.util.logging.*;
import net.jini.export.*;
import net.jini.jeri.*;
import net.jini.jeri.ssl.*;
import net.jini.constraint.*;
import net.jini.discovery.*;
import net.jini.lease.*;
import net.jini.core.constraint.*;
import net.jini.core.lease.*;
import java.rmi.*;
import net.jini.core.entry.Entry;
import net.jini.lookup.entry.*;
import net.jini.core.lookup.*;
import net.jini.export.*;
import net.jini.config.*;
import org.wonderly.jini2.*;
import net.jini.security.*;
import java.security.*;
import net.jini.core.discovery.*;
import net.jini.core.entry.*;
import net.jini.entry.*;
import net.jini.admin.*;
import net.jini.lookup.*;
import net.jini.lookup.ui.factory.*;

/**
 *  This class provides a service lookup with callback
 *  facilities that encapsulates all the lookup activities
 *  into a LookupEnv object.
    <b>Configuration:</b>
    <br>
    The following table shows the configuration entries
    referenced by this class.  The Configuration namespace
    is either the fully qualified name of this class, or the
    passed namespace depending on the constructor used.
 *  <table border="1">
 	<tr><th>Entry<th>Type<th>Usage<th>Default
 	<tr><td>lusPreparer<td>ProxyPreparer<td>Used to prepare 
 	the lookup servers proxy before use.<td><b>none</b>
 	<tr><td>lusExporter<td>Exporter<td>Used to export a
 	RemoteListener to the lookup server.<td><b>none</b>, must be provided
 *	</table>
 */
public class ServiceLookup {
	protected ServiceLookupHandler sl;
	protected LookupEnv env;
	protected Configuration cfg;
	protected Logger log;
	protected Hashtable<ServiceRegistrar,Hashtable<
		ServiceID,ServiceLookupHandler>> lookups;
	protected Hashtable<ServiceRegistrar,RemoteEventListener> rems;
	protected Hashtable<Lease,ServiceRegistrar> leaseToObject;
	protected LookupEnv lastEnv;
	protected LookupDiscoveryManager ldm;
	protected Listener lis;
	protected ProxyPreparer prep;

	/**
	 *  Shutdown lookup operations
	 */
    public void shutdown() {
    	log.fine("Shutting down lookup");
    	if( ldm != null ) {
    		try {
    			log.finer("clearing discovery listener");
    			ldm.removeDiscoveryListener(lis);
    			ldm.terminate();
    		} catch( Throwable ex ) {
    			log.log( Level.WARNING, ex.toString(), ex );
    		}
    		ldm = null;
    	}
    	if( lis != null ) {
    		try {
    			log.finer("stopping discovery listener");
	    		lis.stop();
    		} catch( Exception ex ) {
    			log.log( Level.WARNING, ex.toString(), ex );
    		}
    		lis = null;
    	}
    }

	/**
	 *  @param env the lookup env to use for lookup
	 *  @param hand the handler to callback to
	 *  @param log the logger to log with
	 *  @param config the configuration to pass off to Jini core
	 *  A default preparer will be used for compatibility in Jini
	 *  2.0 environments.
	 */
	public ServiceLookup( LookupEnv env, 
			ServiceLookupHandler hand, 
			Logger log,
			Configuration config ) throws ConfigurationException {
		this( env, hand, log, config, 
			ServiceLookup.class.getName() );
	}
	
	/**
	 *  @param env the lookup env to use for lookup
	 *  @param hand the handler to callback to
	 *  @param log the logger to log with
	 *  @param config the configuration to pass off to Jini core
	 *  @param cfgName the name of the configuration component to use
	 *         to retrieve the ProxyPreparer (lusPreparer) to use for
	 *         ServiceRegistrar preparation to call notify() with.
	 */
	public ServiceLookup( LookupEnv env, 
			ServiceLookupHandler hand, 
			Logger log,
			Configuration config, String cfgName ) throws ConfigurationException {
		sl = hand;
		cfg = config;
		prep = (ProxyPreparer)config.getEntry( cfgName, "lusPreparer",
			ProxyPreparer.class, null );
		if( log != null )
			this.log = log;
		else
			this.log = Logger.getLogger( getClass().getName() );
		log.fine("ServiceLookup Constructed: "+env );
		this.env = env;
	}
	
	/**
	 *  Start lookup activities
	 */
	public void start() throws IOException {
		try {
			log.fine("starting with env: "+env);
			activateEnv( env );
		} catch( Exception ex ) {
			reportException(ex);
		}
	}
	
	/**
	 *  Can be overridden in subclass to provide handling
	 *  of failed lookups, such as scheduling subsequent 'lookup'
	 *  in environments where notify() might not work because
	 *  of one way networks created due to NAT or other tunneling
	 */
	protected void lookupFailed( LookupEnv env, 
			ServiceRegistrar reg ) {
		log.fine("Lookup failed with: "+env );
	}

	protected void reportException( Throwable ex, boolean show ) {
		log.log(Level.SEVERE, ex.toString(), ex );
	}

	protected void reportException( Throwable ex ) {
		log.log(Level.SEVERE, ex.toString(), ex );
	}

    protected void activateEnv( LookupEnv env ) 
    			throws ConfigurationException,IOException {

    	log.finest("Resetting all structures");
    	lookups = new Hashtable<ServiceRegistrar,
    		Hashtable<ServiceID,ServiceLookupHandler>>();
    	rems = new Hashtable<ServiceRegistrar,RemoteEventListener>();
    	leaseToObject = new Hashtable<Lease,ServiceRegistrar>();

    	log.finest("Removing all services from list");
    	lastEnv = env;
    	log.fine("ServiceLookup: Activating ("+ldm+") lookup env: "+
    		env );
		log.fine("ServiceLookup: Create Service template: "+
			descTemplate( env.getServiceTemplate()) );
		if( lis != null ) {
			log.fine("ServiceLookup: Stopping old listener: "+lis );
			lis.stop();
		}
		log.fine( "ServiceLookup: Creating new "+
			"listener for this environment: "+env );
		lis = new Listener(env, null);
		if( ldm != null ) {
			log.fine(
				"ServiceLookup: terminating old LookupDiscoveryManager: "+ldm );	
			ldm.terminate();
		}
		log.fine("ServiceLookup: Creating LookupDiscoveryManager "+
			"with our listener");
			
		LookupLocator locs[] = env.getLookupLocators();
		for( int i = 0; locs != null && i < locs.length; ++i ) {
			log.fine("ServiceLookup: locator["+i+"]: "+locs[i]);
		}
		ldm = new LookupDiscoveryManager( env.getGroups(),
			env.getLookupLocators(), lis, cfg );
		log.fine("ServiceLookup: created LookupDiscoveryManager: "+
			ldm+", listener: "+lis );
	}
	
	/**
	 *  A class for listening to discovery events, and then
	 *  looking up the appropriate services and getting 
	 *  those services setup in the other structures.
	 */
	protected class Listener implements DiscoveryListener {
		// Lease renewal manager for our use.
		private LeaseRenewalManager lrm = 
			new LeaseRenewalManager();
		// the lookup environment to use as registrars show up.
		private LookupEnv env;
		boolean busy;
		Object lock = new Object();
		boolean done[] = new boolean[1];
		List<EventRegistration> act = new ArrayList<EventRegistration>();
//		DiscoveryManagement disco;

		public void stop() {
			log.fine(this+": stop called");
			done[0] = true;
			synchronized( lock ) {
				if( busy ) {
					try {
						lock.wait(40000);
					} catch( Exception ex ) {
					}
				}
			}
			for( EventRegistration evr : act ) {
				try {
					Lease l = evr.getLease();
					log.fine("Cancel lease: "+l );
					l.cancel();
				} catch( Exception ex ) {
					log.log( Level.WARNING, ex.toString(), ex );
				}
			}
			log.fine("Clearing LeaseRenewalManager");
			lrm.clear();
		}

		/**
		 *  Construct a listener for the passed environment
		 */
		public Listener( LookupEnv env,
				DiscoveryManagement disco ) {
			this.env = env;
		}

		/**
		 *  Called by 'disco' or 'ldisco' instances created, when
		 *  registrars are discovered
		 */
		public void discovered( DiscoveryEvent ev ) {
//			new Throwable("Got discovered event").printStackTrace();
			log.fine(this+": discovery event: "+ev );

			final ServiceRegistrar[] newregs = ev.getRegistrars();
			for( int i = 0; i < newregs.length; ++i ) {
				try {
					ServiceRegistrar reg = newregs[i];
					if( prep != null )
						reg = (ServiceRegistrar)prep.prepareProxy(newregs[i]);
					processRegistrar( reg, env );
				} catch( java.rmi.ConnectException ex ) {
					reportException(ex);
				} catch( RemoteException ex ) {
					reportException(ex);
				}
			}
		}

		/**
		 *  Do a lookup in the passed environment, ignoring 
		 *  the state of connectivity do the LUS.  An LUS that
		 *  can not do EventRegistration, is not a problem, if
		 *  we can rediscover the LUS later.
		 */
		private void lookupUsing( ServiceRegistrar reg,
				LookupEnv env ) throws java.rmi.ConnectException {
			try {
				// this might throw an exception if we've 
				// already lost access to the registrar
				log.fine(this+": found lookup service at: "+
					reg.getLocator()  );
				// registrar still in view, do lookup
				if( lookForService( env, reg ) == false ) {
					lookupFailed( env, reg );
				}
			} catch( java.security.AccessControlException ex ) {
				reportException(ex,false);
			} catch( java.rmi.ConnectException ex ) {
				throw ex;
			} catch( RemoteException ex ) {
				reportException(ex);
			} catch( Exception ex ) {
				reportException(ex);
			} finally {
				synchronized( lock ) {
					busy = false;
					lock.notify();
				}
			}
		}

		/**
		 *  Handle the passed registrar and do the lookup
		 *  using the LookupEnv
		 */
		public void processRegistrar( final ServiceRegistrar reg, 
				final LookupEnv env ) 
					throws RemoteException, java.rmi.ConnectException {
			log.fine("processing registrar: "+reg);
			lookupUsing( reg, env );
			log.fine("Register with registrar");
			registerWithLus( reg );
			log.fine("Lookup again after registration");
			lookupUsing( reg, env );
		}

		/**
		 *  Register for EventRegistration processing with the 
		 *  passed LUS.  If we can not do notify(), we will 
		 *  discard the LUS, and it will come back later
		 *  and then we'll reprocess the LUS and find any of
		 *  the new services.
		 */
		private void registerWithLus( ServiceRegistrar reg ) {
			try {

				RemoteEventListener rem;
				// Create our remote listener instance that 
				// will handle notifications from the registrar
				rem = new RemoteListener( 
					(RegistrarTransitionListener)transHand, reg );
				log.finer(this+": Creating remote listener ("+
					rem+") for LUS: "+reg );
				Exporter exp = (Exporter)cfg.getEntry( 
					ServiceLookup.this.getClass().getName(), 
					"lusExporter", Exporter.class,
					new BasicJeriExporter( 
						SslServerEndpoint.getInstance(0),
						new BasicILFactory(
							new BasicMethodConstraints(
								new InvocationConstraints(
								new InvocationConstraint[] {},
								new InvocationConstraint[] {} )
							),
							null
						)
					)
				);
				RemoteEventListener rrem = 
					(RemoteEventListener)exp.export(rem);
				// Notify the registrar of who to send service 
				// changes to.
				log.finer(this+
					": calling ServiceRegistrar.notify()");
				final EventRegistration evr = reg.notify(
					env.getServiceTemplate(),
					ServiceRegistrar.TRANSITION_MATCH_MATCH|
					ServiceRegistrar.TRANSITION_MATCH_NOMATCH|
					ServiceRegistrar.TRANSITION_NOMATCH_MATCH,
					rrem, null,
					Lease.FOREVER );
				log.finer(this+": ServiceRegistrar.notify() returns");

				log.fine("notify tracking lease ("+new Date(evr.getLease().getExpiration())+
					": "+evr.getLease()+", with: "+act );
				act.add( evr);

				// If we can do the notify to the registrar, then
				// set up a lease renewel failure notification too.
				lrm.renewUntil( evr.getLease(), Lease.FOREVER,
					new LeaseListener() {
					public void notify( LeaseRenewalEvent evv ) {
						ServiceRegistrar reg = (ServiceRegistrar)
							leaseToObject.remove( evv.getLease() );
						log.fine(this+": lost registrar: "+reg );
						rems.remove( reg );
						ldm.discard(reg);
						act.remove( evr.getLease() );
					}
				});

				log.finer(this+": asked for lease renewal of "+
					"notify registration");
				// Got notification and lease renewal, so remember
				// all the information that we'll need to tell us
				// when there is a problem and we need to
				// discard the LUS.
				log.fine( this+": registration of listener "+
					"completed!");
				leaseToObject.put( evr.getLease(), reg );
				rems.put( reg, rrem );
			} catch( java.rmi.ServerException ex ) {
				String svname = reg.toString();
				try {
					svname = reg.getLocator().toString();
				} catch( Exception xex ) {
					log.log( Level.WARNING, xex.toString(), xex );
				}
				log.warning(env+": "+this+": Can't perform notify to: "+svname);
				log.log(Level.SEVERE, ex.toString(), ex );
				final ServiceRegistrar freg = reg;
				new Thread(env+": "+this+": discard in 10 minutes") {
					public void run() {
						try {
							Thread.sleep(600000);
						} catch( Exception ex ) {
						ldm.discard( freg );
						}
					}
				}.start();
			} catch( RemoteException ex ) {
				log.warning(env+": "+this+": Can't connect" );
				final ServiceRegistrar freg = reg;
				new Thread(env+": "+this+": discard in 10 minutes") {
					public void run() {
						try {
							Thread.sleep(600000);
						} catch( Exception ex ) {
						ldm.discard( freg );
						}
					}
				}.start();
			} catch( Exception ex ) {
				log.fine(env+": "+this+": Can't connect" );
				log.log(Level.SEVERE, ex.toString(), ex );
//				ldm.discard( reg );
			}
		}

		protected boolean lookForService( LookupEnv env,
				ServiceRegistrar lookup ) throws RemoteException {
			ServiceMatches o = null;
			ServiceTemplate template = env.getServiceTemplate();
			log.fine("  looking up "+template+" at "+lookup );
			if( template == null ) {
				log.finer("  initilizing null template" );
				template = new ServiceTemplate(null,null,null);
			}
			String loc = lookup.getLocator().toString();
			try {
				log.finer("  lookup starts: "+loc );
				o = lookup.lookup(template,20);
				if( done[0] )
					return false;
				busy = true;
				log.finer("  lookup returns" );
			} catch( Exception ex ) {
				reportException(ex);
				return false;
			}
			
			if( o == null || o.items.length == 0 ) {
				log.finer("  no services found");
				return false;
			}
			
			log.finer("  Checking "+o.items.length+" matches" );
			for( int i  = 0;i < o.items.length; ++i) {
				if( done[0] )
					return false;
				log.finer( "Adding item["+i+"]: "+o.items[i]);
				try {
					sl.processItem( o.items[i], lookup );
				} catch( Exception ex ) {
					reportException(ex);
				}
			}
			log.fine("Registrar processed: "+loc );
			return true;
		}

		/**
		 *  Called as services are discarded.
		 */
		public void discarded( DiscoveryEvent ev ) {
			log.fine(this+": discarded event: "+ev );
			ServiceRegistrar[] newregs = ev.getRegistrars();
			for( int i = 0; i < newregs.length; ++i ) {
				discardReg( newregs[i] );
			}
		}
		
		void discardReg( ServiceRegistrar reg ) {
			log.finer(this+": lost lookup service: "+
				reg.getServiceID()+": "+reg );
			deregisterInstance( reg );
			ldm.discard( reg );
			rems.remove( reg );
		}
	}
	/**
	 *  When a service is updated, refresh the information 
	 *  that we have about it.
	 */
	private void updateInstance( ServiceRegistrar lookup, 
				ServiceItem si ) {
		log.fine(this+": updating: "+si+" from "+lookup );
		Hashtable<ServiceID,ServiceLookupHandler> h = lookups.get( lookup );
		if( h == null ) {
			h = new Hashtable<ServiceID,ServiceLookupHandler>(3);
			lookups.put(lookup,h);
		}
		
		log.fine(this+": Updating: "+si.serviceID );
		ServiceLookupHandler hand = 
			(ServiceLookupHandler)h.get( si.serviceID );
	}

	/**
	 *  register located services with our infrastructure.
	 */
	private void registerInstance( ServiceRegistrar lookup, 
			ServiceItem si ) {
		log.fine("registering: "+si+" from "+lookup );
		Hashtable<ServiceID,ServiceLookupHandler> h = lookups.get( lookup );
		if( h == null ) {
			h = new Hashtable<ServiceID,ServiceLookupHandler>(3);
			lookups.put(lookup,h);
		}
		
		log.finer("Registering: "+si.serviceID );
		h.put( si.serviceID, sl );
	}
	
	/**
	 *  Remove service registrations from our infrastructure.
	 */
	private void deregisterInstance( ServiceRegistrar lookup ) {
		log.fine("deregistering: all from "+lookup );
		Hashtable<ServiceID,ServiceLookupHandler> h = lookups.get( lookup );
		if( h == null ) {
			return;
		}
		Enumeration e = h.keys();
		while( e.hasMoreElements() ) {
			ServiceID item = (ServiceID)e.nextElement();
			ServiceLookupHandler hand = 
				(ServiceLookupHandler)h.get(item);
		}
	}

	private TransitionHandler transHand = new TransitionHandler();
	protected  class TransitionHandler 
			implements RegistrarTransitionListener {
		public void removeInstance( ServiceEvent ev,
				ServiceRegistrar reg ) {
			log.fine(this+": Removing instance: "+
				descItem(ev.getServiceItem()) );
			Hashtable h = (Hashtable)lookups.get( reg );
			if( h == null ) {
				log.finer("no known items for registrar: "+reg );
				return;
			}
			ServiceID item = ev.getServiceID();
			ServiceLookupHandler hand = 
				(ServiceLookupHandler)h.get(item);
			log.finer("handler for id: "+item+" is "+hand );
			if( hand != null ) {
				try {
					log.finer("invoking serviceLost("+item+","+reg+")");
					hand.serviceLost(item, reg);
				} catch( IOException ex ) {
					log.throwing( getClass().getName(), 
						"removeInstance", ex );
				} catch( ConfigurationException ex ) {
					log.throwing( getClass().getName(), 
						"removeInstance", ex );
				}
			}
			log.finer("deregisteringInstance("+reg+")");
			deregisterInstance( reg );
		}
	
		public void addInstance( ServiceEvent ev, 
					ServiceRegistrar reg ) {
			log.fine(this+": Adding instance: "+
				descItem(ev.getServiceItem()) );
			registerInstance( reg, ev.getServiceItem() );
			try {
				sl.processItem( ev.getServiceItem(), reg );
//				addItem( reg, ev.getServiceItem() );
//			} catch( ConfigurationException ex ) {
//				reportException(ex);
			} catch( IOException ex ) {
				log.throwing( getClass().getName(),
					"addInstance", ex );
			} catch( ConfigurationException ex ) {
				log.throwing( getClass().getName(), 
					"addInstance", ex );
			} catch( RuntimeException ex ) {
				reportException(ex);
			}
		}
	
		public void updateInstance( ServiceEvent ev, 
					ServiceRegistrar reg ) {
			log.fine(this+": Updating instance: "+
				descItem(ev.getServiceItem()) );
			ServiceLookup.this.updateInstance( reg,
				ev.getServiceItem() );
//			Hashtable h = (Hashtable)lookups.get( reg );
//			if( h == null ) {
//				log.fine("No entries for registrar: "+reg );
//				return;
//			}
			ServiceItem si = ev.getServiceItem();
			try {
				sl.updateItem( si, reg );
			} catch( IOException ex ) {
				log.throwing( getClass().getName(),
					"updateInstance", ex );
			} catch( ConfigurationException ex ) {
				log.throwing( getClass().getName(),
					"updateInstance", ex );
			}
		}
	}
	
	/**
	 *  A utility method for getting a descriptive version of
	 *  the ServiceItem contents
	 */	
	public static String descItem(ServiceItem si) {
		if( si == null )
			return "<no service Item>";
		Entry[]ents = si.attributeSets;
		String name = "<no data available>";
		if( si.service != null )
			name = si.service.getClass().getName();
		for( int j = 0; ents != null && j < ents.length; ++j ) {
			if( ents[j] instanceof Name )
				name = ((Name)ents[j]).name;
			else if( ents[j] instanceof ServiceInfo )
				name = ((ServiceInfo)ents[j]).name;
		}
		return name;
	}
	
	/**
	 *  A utility method for getting a descriptive version of
	 *  the ServiceTemplate contents
	 */
	public static String descTemplate(ServiceTemplate si) {
		if( si == null )
			return "<no service Item>";
		Entry[]ents = si.attributeSetTemplates;
		ServiceID id = si.serviceID;
		Class[]cls = si.serviceTypes;
		String str = "";
		if( ents != null ) {
			str += "Entry[] { ";
			for( int i = 0; i < ents.length; ++i ) {
				str += ents[i].getClass().getName();
				str += "=["+ents[i]+"]";
				if( i+1 < ents.length )
					str += ", ";
			}
			str += " }";
		}
		if( id != null )
			str += " ServiceID="+id;
		if( cls != null ) {
			str += "Class[] { ";
			for( int i = 0; i < cls.length; ++i ) {
				str += cls[i].getName();
				if( i+1 < cls.length )
					str += ", ";
			}
			str += " }";
		}
		return str;
	}
}