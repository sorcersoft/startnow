package org.wonderly.jini2;

import java.awt.*;
import javax.swing.*;
import net.jini.config.*;
import net.jini.lookup.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.lookup.entry.*;
import java.io.*;
import java.util.logging.*;
import net.jini.lookup.ui.*;
import net.jini.lookup.ui.factory.*;
import java.rmi.*;
import org.wonderly.awt.*;

/**
 *  This class provides support for simple lookup interfaces to
 *  the Jini LUS.  It uses the {@link ServiceDiscoveryManager} which
 *  has some limitations related to callbacks.  The SDM also requires
 *  sdm-dl.jar (or its contents) to be in the codebase.
 */
public class JiniAccess<T> {
	static Logger log = Logger.getLogger( JiniAccess.class.getName() );
	protected boolean launchInProg;
	protected ServiceItem item;
	protected Object itemLock = new Object();
	protected boolean lookupComplete;

	public JiniAccess() {
	}

	/**
	 *  This is a type safe factory method for service lookup.
	 *  @param tmpl the template for service lookup
	 *  @param svcType the class of the service to return for type safety.
	 *         Use Object if you don't care.
	 *  @param conf a configuration for controlling {@link ServiceDiscoveryManager}
	 *  @param timeout the timeout for {@link ServiceDiscoveryManager} timeout.
	 *  @throws IllegalStateException when another operation is already in progress
	 */
	public synchronized T getServiceInstance( 
			ServiceTemplate tmpl, Class<T> svcType,
				Configuration conf, long timeout 	
				) throws IOException, ConfigurationException,
					InterruptedException {
		if( launchInProg ) {
			throw new IllegalStateException("ServiceUI Launch Already in Progress");
		}
		T ref = null;

		checkSecurityManager();

		ServiceDiscoveryManager sdm = new ServiceDiscoveryManager(
			null, null, conf );

		ServiceItem m = sdm.lookup( tmpl, null, timeout );
		if( m == null ) {
			throw new IllegalArgumentException(
				"No service found for: "+svcType.getName() );
		}

		if( log.isLoggable( Level.FINE ) ) {
			log.fine("getInstance Service: "+m.service+" ("+
				(m.service == null ? "unresolved" : 
					m.service.getClass().getName()));
			log.fine("getInstance      ID: "+m.serviceID );
		}
		lookupComplete = true;
		item = m;

		ref = (T)m.service;
		return ref;
	}

	protected void checkSecurityManager() {
		if( System.getSecurityManager() == null ) {
			System.setSecurityManager( new RMISecurityManager() );
			log.config("setSecurityManager to RMISecurityManger");
		}
	}
	
	public ServiceItem getItem() {
		synchronized( itemLock ) {
			while( !lookupComplete ) {
				log.fine("waiting for service discovery to complete");
				try {
					itemLock.wait(10000);
				} catch( Exception ex ) {
				}
			}
		}
		return item;
	}
	
	public void interruptLookup() {
		synchronized( itemLock ) {
			lookupComplete = true;
			itemLock.notifyAll();
		}
	}

	public synchronized void startServiceUIIn( final Class<T> svcType, 
			final String role, final Container par,
			final Configuration conf, final long timeout ) {

		launchInProg = true;

		checkSecurityManager();
		new Thread() {
			public void run() {
				try {
					process();
				} catch( Exception ex ) {
					log.log(Level.SEVERE,ex.toString(),ex);
				} finally {
					launchInProg = false;
					if( lookupComplete == false ) {
						synchronized( itemLock ) {
							lookupComplete = true;
							itemLock.notifyAll();
						}
					}
				}
			}
			
			/**
			 *  @throws IllegalArgumentException if no service found or
			 *  if associated factory does not support JComponentFactory
			 */
			private void process() throws Exception {
				ServiceDiscoveryManager sdm = new ServiceDiscoveryManager(
					null, null, conf
					);
				ServiceItem m = null;
				m = sdm.lookup( new ServiceTemplate( null,
					new Class[]{ svcType }, null ), null, timeout );
				if( m == null ) {
					throw new IllegalArgumentException(
						"No service found for: "+svcType.getName() );
				}
				launchItem( m, role, par );
			}
		}.start();
	}
	
	protected void launchItem( ServiceItem m, String role, 
			Container par ) throws IOException, ClassNotFoundException {
		Entry[]e = m.attributeSets;
		if( log.isLoggable(Level.FINE) ) {
			log.fine("Service: "+m.service);
			log.fine("     ID: "+m.serviceID );
			log.fine("  attrs: "+e.length );
		}
		boolean tried = false;
		for( int i = 0; i < e.length; ++i ) {
			if( log.isLoggable(Level.FINE) ) {
				log.fine("e["+i+"]: "+((e[i] ==null) ? "<unresolved>" : 
					e[i].getClass().getName()) );
			}
			if( e[i] instanceof UIDescriptor && 
				((UIDescriptor)e[i]).role.equals(role) ) {
				UIDescriptor ui = (UIDescriptor)e[i];
				Object f = ui.factory.get();
				tried = true;
				if( checkFactory( f, par, m ) ) {
					item = m;
					synchronized( itemLock ) {
						lookupComplete = true;
						itemLock.notifyAll();
					}
					return;
				}
			}
		}
		throw new IllegalArgumentException( tried ? 
			"Factory does not support required type: " +
				JComponentFactory.class.getName() :
			"Service Attributes have no UIDescriptor");
	}
	
	public String serviceName( ServiceItem m ) {
		Entry[]e = m.attributeSets;
		if( log.isLoggable(Level.FINE) ) {
			log.fine("Service: "+m.service);
			log.fine("     ID: "+m.serviceID );
			log.fine("  attrs: "+e.length );
		}
		String name = null;
		for( int i = 0; i < e.length; ++i ) {
			if( log.isLoggable(Level.FINE) ) {
				log.fine("e["+i+"]: "+((e[i] ==null) ? "<unresolved>" : 
					e[i].getClass().getName()) );
			}
			if( e[i] instanceof Name ) {
				name = ((Name)e[i]).name;
			} else if( e[i] instanceof ServiceInfo && name == null ) {
				name = ((ServiceInfo)e[i]).name;
			}
		}
		if( name == null )
			name = m.serviceID.toString();
		return name;
	}

	
	protected static boolean checkFactory( Object fact, Container par,
			ServiceItem item ) {
		if( log.isLoggable( Level.FINE ) ) {
			Class in[] = fact.getClass().getInterfaces();
			for( int i = 0; i < in.length; ++i ) {
				log.fine(fact+": implements: "+in[i].getName() );
			}
		}
		if( fact instanceof JComponentFactory ) {
			JComponent c = ((JComponentFactory)fact).getJComponent( item );
			Packer pk = new Packer( par );
			pk.pack( c ).gridx(0).gridy(0).fillboth();
			Container top = null;
			if( par instanceof JFrame || par instanceof JDialog ) {
				top = par;
			} else {
				top = ((JComponent)par).getTopLevelAncestor();
			}
			if( top instanceof JFrame ) {
				JFrame t = (JFrame)top;
				t.pack();
			} else if( top instanceof JDialog ) {
				JDialog t = (JDialog)top;
				t.pack();
			}
			return true;
		}
		return false;
	}
}