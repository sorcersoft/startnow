package org.wonderly.jini;

import javax.swing.*;
import java.awt.*;
import org.wonderly.awt.*;
import org.wonderly.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;

import org.wonderly.util.jini.*;
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
import net.jini.lease.*;
import net.jini.event.*;
import net.jini.space.*;
import net.jini.admin.*;

import java.util.*;
import java.util.logging.*;
import java.net.*;

/**
 *  This class provides a mechanism to allow specific services to be
 *  selected for use by users.  The typical use is to construct an instance
 *  and use it as shown here:
 *  <pre>

ServiceSelector os = new ServiceSelector( f,"Server Selection",
	new ServiceTemplate(
		null,
		new Class[] { ...interfaces... },
		Entry[] {...entries...} ), new String[]{ ...groups...} );

// Use a specific unicast locator if needed
os.setLookupServer( System.getProperty("jini.locator.host") );
// Receive events about what the system and the user is doing
os.addActionListener( new ActionListener() {
	public void actionPerformed( ActionEvent ev ) {
		if( ev.getID() == os.SERVICE_SELECTED ) {
			Object[]arr = (Object[])ev.getSource();
			ServiceItem it = (ServiceItem)arr[0];
			ServiceRegistrar reg = (ServiceRegistrar)arr[1];
			System.out.println(this+": Selected Service: "+
				it.serviceID+" from: "+reg );
			... do something with it...
		} else if( ev.getID() == os.SERVICE_LOST ) {
		} else if( ev.getID() == os.SERVICE_FOUND ) {
		}
	}
});
os.setVisible(true);
// Control returns to here when dialog is closed.

</pre>

 *  For more advanced applications, multiple configurations might be presented to the
 *  user by doing:
 *  <pre>

LookupEnv env1 = new LookupEnv( "Public Administrable" );
env1.setServiceTemplate( new ServiceTemplate( null, new Class[]{Administrable.class}, null ) );
env1.addLookupLocator( "host2.domain.com");

LookupEnv env2 = new LookupEnv( "Public Services" );
env2.setServiceTemplate( new ServiceTemplate( null, null, null ) );
env2.addLookupLocator( "host1.domain.com" );

ServiceSelector os = new ServiceSelector( f,"Server Selection", new LookupEnv[]{env1,env2} );

// Receive events about what the system and the user is doing
os.addActionListener( new ActionListener() {
	public void actionPerformed( ActionEvent ev ) {
		if( ev.getID() == os.SERVICE_SELECTED ) {
			Object[]arr = (Object[])ev.getSource();
			ServiceItem it = (ServiceItem)arr[0];
			ServiceRegistrar reg = (ServiceRegistrar)arr[1];
			System.out.println(this+": Selected Service: "+
				it.serviceID+" from: "+reg );
			... do something with it...
		} else if( ev.getID() == os.SERVICE_LOST ) {
		} else if( ev.getID() == os.SERVICE_FOUND ) {
		}
	}
});

os.setVisible(true);
// Control returns to here when dialog is closed.

</pre>
 *
 *  The user will then be provided a JComboBox that they can
 *  use to select a lookup environment from.
 *  This allows multiple servers to be queried for 
 *  different interfaces as needed.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class ServiceSelectionPane extends JPanel {
	protected JList svclist;
	protected JMenuItem conn;
	protected Component parent;
	protected JLabel msg;
//	protected String lookupServer;
	protected Vector<ActionListener> listeners = new Vector<ActionListener>(7);
	protected LookupEnv defaultEnv;
	protected Logger log = Logger.getLogger( getClass().getName() );
//	protected String groups[] = { "" };
	JMenuBar bar;

	/**
	 *  Service was selected by user, source is
	 *  Object[]{ServiceItem,ServiceRegistrar}
	 */
	public static final int SERVICE_SELECTED = 0;
	/**
	 *  Service was lost by user, source is
	 *  Object[]{ServiceItem,ServiceRegistrar}
	 */
	public static final int SERVICE_LOST = 1;
	/**
	 *  Service was found by user, source is
	 *  Object[]{ServiceItem,ServiceRegistrar,JCheckBoxMenuItem}
	 *  The JCheckBoxMenuItem usable by your UI.
	 */
	public static final int SERVICE_FOUND = 2;

	public String toString() {
		return getClass().getName()+"@"+hashCode();
	}
	
	public JMenuBar getJMenuBar() {
		return bar;
	}

	private ServiceSelectionPane( Component parent ) {
		this.parent = parent;
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelectionPane( Component parent,
			ServiceTemplate templ, String groups[] )
				throws IOException {
		this( parent );
		defaultEnv = new LookupEnv( "Default" );
		defaultEnv.setServiceTemplate( templ );
		if( groups != null ) {
			for( int i = 0; i < groups.length; ++i ) {
				defaultEnv.addGroup(groups[i]);
			}
		}
//		template = templ;
//		this.groups = groups;
		activateJini(new LookupEnv[]{defaultEnv});
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelectionPane( Component parent,LookupEnv env ) throws IOException {
		this( parent );
//		template = env.getServiceTemplate();
//		this.groups = env.getGroups();
		activateJini(new LookupEnv[]{env});
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelectionPane( JFrame parent, LookupEnv envs[] ) throws IOException {
		this( parent );
//		template = envs[0].getServiceTemplate();
//		this.groups = envs[0].getGroups();
		activateJini(envs);
	}
	
	public void addActionListener( ActionListener lis ) {
		listeners.addElement(lis);
	}
	
	public void removeActionListener( ActionListener lis ) {
		listeners.removeElement( lis );
	}
	
	private void deliverAction(ActionEvent ev ) {
		log.fine("delivering to "+listeners.size()+" listeners" );
		for( int i = 0; i < listeners.size(); ++i ) {
			((ActionListener)listeners.elementAt(i)).actionPerformed( ev );
		}
	}
	
	protected DefaultComboBoxModel envmod;
	protected Vector<LookupEnv> envs;
	protected JComboBox envbox;
	public void addLookupEnv( LookupEnv env ) {
		if( envs.contains(env) == false ) {
			envs.addElement(env);
			envmod.addElement( env );
		}
	}

	protected boolean inited;
	private void activateJini( LookupEnv envset[] ) throws IOException {
 		try {
 			if( System.getSecurityManager() == null ) {
 				log.warning("activating RMISecurityManager" );
				System.setSecurityManager( new RMISecurityManager() );
 			}
		} catch( Error ex ) {
		}

 		final JPanel p = this;
 		final Packer pk = new Packer( p );
 		svclist = new JList(new DefaultListModel());//svcmod = new DefaultListModel());
 		final ListCellRenderer rnd = svclist.getCellRenderer();
 		final JLabel cl = new JLabel();
 		JPanel sp = new JPanel();
 		sp.setBorder(BorderFactory.createTitledBorder("Registered Servers"));
 		Packer spk = new Packer( sp );
 		spk.pack( new JScrollPane( svclist ) ).fillboth();
 		final JPanel selp = new JPanel();
 		Packer slpk = new Packer( selp );
 		envmod = new DefaultComboBoxModel();
 		envs = new Vector<LookupEnv>(); // mirror image of envmod to check for presence.
 		envbox = new JComboBox(envmod);
		slpk.pack( new JLabel( "LUS Env:") ).gridx(0).gridy(0).inset(0,4,0,4);
		slpk.pack( envbox ).gridx(1).gridy(0).fillx();
 		envbox.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				LookupEnv env = (LookupEnv)envbox.getSelectedItem();
 				if( env == null )
 					return;
 				try {
 					if( inited )
 						activateEnv( env );
 				} catch( Exception ex ) {
 					ex.printStackTrace();
 					JOptionPane.showMessageDialog( parent, ex );
 				}
 			}
 		});
 
 		if( envset != null ) {
 			for( int i = 0; i < envset.length; ++i ) {
 				envmod.addElement(envset[i]);
 			}
 		}
 		pk.pack( new JSeparator() ).gridx(0).gridy(1).fillx().gridw(2);
 		pk.pack( sp ).gridx(0).gridy(2).fillboth().gridw(2);
 		pk.pack( new JSeparator()).gridx(0).gridy(1).fillx().gridw(2);
 		conn = new JMenuItem("Service");
 		bar = new JMenuBar();
// 		bar.setBackground( sp.getBackground() );
 		bar.setBorder(BorderFactory.createEtchedBorder());
 		JMenu m;
 		m = new JMenu("Select");
 		bar.add(m);
 		m.add(conn);
// 		m.addSeparator();
 		JMenuItem mi;
// 		m.add( mi = new JMenuItem("Close") );
// 		mi.addActionListener( new ActionListener() {
// 			public void actionPerformed( ActionEvent ev ) {
// 				setVisible(false);
// 			}
// 		});
// 		bar.add(m);
 		m = new JMenu("Show");
 		bar.add(m);
 		final JCheckBoxMenuItem luscb = new JCheckBoxMenuItem("LUS Configs");
 		final JCheckBoxMenuItem msgcb = new JCheckBoxMenuItem("Progress Messages");
 		m.add( luscb );
 		luscb.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				if( luscb.isSelected() ) {
			 		pk.pack( selp ).gridx(0).gridy(0).fillx().gridw(2);
 				} else {
 					p.remove(selp);
 				}
  				p.revalidate();
 				p.repaint();
			}
 		});
  		if( envset.length > 1 ) {
	 		pk.pack( selp ).gridx(0).gridy(0).fillx().gridw(2);
  			luscb.setSelected(true);
  		}
 		m.add( msgcb );
 		msgcb.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				if( msgcb.isSelected() ) {
			 		pk.pack( msg ).gridw(2).gridx(0).gridy(4).fillx();
 				} else {
 					p.remove(msg);
 				}
 				p.revalidate();
 				p.repaint();
 			}
 		});
 		m = new JMenu("View");
 		bar.add(m);
 		ButtonGroup grp = new ButtonGroup();
 		final JMenuItem byName = new JCheckBoxMenuItem("Names/ServiceInfo");
 		grp.add(byName);
 		byName.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				svclist.repaint();
 			}
 		});
 		m.add( byName );
 		byName.setSelected(true);
 		
 		final JMenuItem byClass = new JCheckBoxMenuItem("Class Names");
 		grp.add(byClass);
 		byClass.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				svclist.repaint();
 			}
 		});
 		m.add( byClass );
 		
 		final JMenuItem bySvcID = new JCheckBoxMenuItem("Service IDs");
 		grp.add(bySvcID);
 		bySvcID.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				svclist.repaint();
 			}
 		});
 		m.add( bySvcID );
 		
 		svclist.setCellRenderer( new ListCellRenderer() {
 			public Component getListCellRendererComponent(
 					JList list, Object value, int index,
 					boolean isSelected, boolean cellHasFocus) {
 				
 				JLabel cl = (JLabel)rnd.getListCellRendererComponent(
 					list, value, index, isSelected, cellHasFocus );
 				WrappedActionAccess acc = (WrappedActionAccess)value;
 				ServiceItem si = acc.item;
 				String str = null;
 				if( byName.isSelected() ) {
 					str = descItem( si );
 				} else if( byClass.isSelected() ) {
 					Class cls = si.service.getClass();
 					Class[] intf = cls.getInterfaces();
 					str = "";
 					for( int i = 0; i < intf.length; ++i ) {
 						if( intf[i].getName().equals("java.rmi.Remote") == false &&
 								intf[i].getName().equals("java.io.Serializable") == false ) {
	 						if( i > 0 )	str += ", ";
 							str += intf[i].getName();
 						}
 					}
 				} else /* if( bySvcID.isSelected() ) */ {
 					str = si.serviceID.toString();
 				}
 				cl.setText(str);
 				return cl;
 			}
 		});
 		conn.setEnabled(false);
// 		final JButton exit = new JButton("Cancel");
// 		pk.pack( conn ).gridx(0).gridy(3).west().inset(8,8,8,8);
// 		pk.pack( exit ).gridx(1).gridy(3).east().inset(8,8,8,8);
 		msg = new JLabel(" ");
 		msg.setBorder( BorderFactory.createLoweredBevelBorder() );
 		msgcb.setSelected(false);
// 		pk.pack( msg ).gridw(2).gridx(0).gridy(4).fillx();
 		svclist.addListSelectionListener( new ListSelectionListener() {
 			public void valueChanged( ListSelectionEvent ev ) {
 				if( ev.getValueIsAdjusting() )
 					return;
 				conn.setEnabled(true);
 				conn.setToolTipText( "Click to Connect to the Selected Server");
 				log( svclist.getSelectedValue()+" selected" );
 			}
 		});
 		svclist.addMouseListener( new MouseAdapter() {
 			public void mouseClicked( MouseEvent ev ) {
 				if( ev.getClickCount() > 1 ) {
					launchSelectedService();
 				}
 			}
 		});
		conn.setToolTipText("Waiting for Servers to Register");
		svclist.setToolTipText("Waiting for Servers to Register");
 		svclist.setEnabled(false);
		
 		conn.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				launchSelectedService();
 			}
 		});

 		inited = true;
		activateEnv( (LookupEnv)envbox.getSelectedItem() );
    }
    
    public void shutdown() {
    	if( log.isLoggable(Level.FINEST) ) {
			Throwable ex = new Throwable( 
				"Shutting down last lookup: disco: "+disco+
				", ldisco: "+ldisco );
			log.log( Level.FINEST, ex.toString(), ex );
    	} else {
    		log.fine("Shutting down last lookup: disco: "+
    			disco+", ldisco: "+ldisco);
    	}
    	if( disco != null ) {
    		try {
    			disco.removeDiscoveryListener(lis);
    			disco.terminate();
    		} catch( Throwable ex ) {
    		}
    		disco = null;
    	}
    	if( ldisco != null ) {
    		try {
    			ldisco.removeDiscoveryListener(lis);
    			ldisco.terminate();
    		} catch( Throwable ex ) {
    		}
    		ldisco = null;
    	}
    }

    LookupEnv lastEnv;
    protected void activateEnv( LookupEnv env ) throws IOException {
    	if( lastEnv == env ) {
    		if( log.isLoggable(Level.FINEST) ) {
				Throwable ex = new Throwable(env+": already activated, returning");
				log.log(Level.FINEST, ex.toString(), ex );
    		}
    		return;
    	}

    	shutdown();
    	lookups = new Hashtable<ServiceRegistrar,Hashtable<ServiceID,WrappedActionAccess>>();
    	services = new Hashtable<ServiceID,ServiceItem>();
    	rems = new Hashtable<ServiceRegistrar,RemoteListener>();
    	leaseToObject = new Hashtable<Lease,ServiceRegistrar>();

    	((DefaultListModel)svclist.getModel()).removeAllElements();
		if( log.isLoggable(Level.FINEST) ) {
			Throwable ex = new Throwable("activating "+env);
			log.log(Level.FINEST, ex.toString(), ex );
		} else {
			log.fine("activating: "+env);
		}
    	lastEnv = env;
    	log.fine("Activating ("+disco+","+ldisco+") lookup env: "+env );
		log.fine("Create Service template: "+env.getServiceTemplate());
		disco = new LookupDiscovery( env.getGroups() );
		log.fine( "create Lookup Discovery: "+disco+", groups: "+arrList(env.getGroups()) );
		if( lis != null )
			lis.stop();
		lis = new Listener(env, disco);
		disco.addDiscoveryListener( lis );
		log.fine(disco+": added discovery Listener (multicast): "+lis );
		LookupLocator[]l = env.getLookupLocators();
		// Start a new one with this set of locators.
		ldisco = new LookupLocatorDiscovery( l );
		if( log.isLoggable(Level.FINER) ) {
			for( int i = 0; i < l.length; ++i ) {
				log.finer("adding listener to unicast locators: "+l[i] );
			}
		}
		ldisco.addDiscoveryListener( lis );
		log.finer("ldisco: "+ldisco+", disco: "+disco );
		if( log.isLoggable(Level.FINEST) ) {
			
			log.finest( "Performing static lookup on all registrars for debugging");
			ServiceRegistrar[]rs = disco.getRegistrars();
			log.finest(disco+": currently have "+rs.length+" registrars" );
			for( int i = 0; i < rs.length; ++i ) {
				try {
					log.finest("processing: "+rs[i].getLocator());
				} catch( Exception ex ) {
					reportException(Level.FINEST, ex);
				}
				lis.processRegistrar( rs[i], env );
			}

			rs = ldisco.getRegistrars();
			log.finest(ldisco+": currently have "+rs.length+" registrars" );
			for( int i = 0; i < rs.length; ++i ) {
				try {
					log.finest("processing: "+rs[i].getLocator());
				} catch( Exception ex ) {
					reportException(Level.FINEST, ex);
				}
				lis.processRegistrar( rs[i], env );
			}
		}
	}
	Listener lis;

	String arrList( Object[] arr ) {
		String vals = arr+":{";
		for( int i = 0;i < arr.length; ++i ) {
			if( i > 0 )
				vals += ",";
			vals += arr[i];
		}
		vals += "}";
		return vals;
	}
	
	void launchSelectedService() {
//		new Throwable("Launching selected service").printStackTrace();
		msg.setText(" ");
		msg.repaint();
		new ComponentUpdateThread( svclist ) {
			public Object construct() {
				try {
					int sidx = svclist.getSelectedIndex();
					if( sidx >= 0 ) {
						WrappedActionAccess db = 
							(WrappedActionAccess)svclist.getModel().getElementAt(sidx);
						db.actionPerformed( new ActionEvent(this, 0, db.toString()) );
					}
				} catch( IllegalArgumentException ex ) {
					int sidx = svclist.getSelectedIndex();
					if( sidx >= 0 ) {
						WrappedActionAccess db = 
							(WrappedActionAccess)svclist.getModel().getElementAt(sidx);
						log.fine("discarding LUS: "+db.lookup );
						disco.discard(db.lookup);
					}
					msg.setText( ex.getMessage() );
					msg.repaint();
				} catch( Exception ex ) {
					reportException(ex);
				}
				return null;
			}
		}.start();
	}

	protected void reportException( Throwable ex ) {
		reportException( ex, true );
	}

	protected void reportException( Level lev, Throwable ex ) {
		reportException( lev, ex, true );
	}

	protected void reportException( Throwable ex, boolean showUser ) {
		reportException( Level.SEVERE, ex, showUser );
	}

	protected void reportException( Level lev, Throwable ex, boolean showUser ) {
		log.log( lev, ex.toString(), ex );
		if( showUser )
			JOptionPane.showMessageDialog( parent, ex );
	}

	/**
	 *  For each lease that covers our knowledge of a registrar, we
	 *  have a lease listener that is waiting for notification of the
	 *  lease being terminated.  When it is terminated, we can use
	 *  this table to find the corresponding registrar, and then
	 *  we can tear down the remaining infrastructure for the lost
	 *  registrar
	 */
	private Hashtable<Lease,ServiceRegistrar> leaseToObject = 
		new Hashtable<Lease,ServiceRegistrar>();
	
	/**
	 *  A class for listening to discovery events, and then looking up
	 *  the appropriate services and getting those services setup in
	 *  the other structures.
	 */
	protected class Listener implements DiscoveryListener {
		/** Lease renewal manager for our use. */
		private LeaseRenewalManager lrm = new LeaseRenewalManager();
		/** the lookup environment to use as registrars show up. */
		private LookupEnv env;
		boolean busy;
		Object lock = new Object();
		boolean done[] = new boolean[1];
		DiscoveryManagement disco;

		public void stop() {
			done[0] = true;
			synchronized( lock ) {
				if( busy ) {
					try {
						lock.wait(40000);
					} catch( Exception ex ) {
					}
				}
			}
		}

		/**
		 *  Construct a listener for the passed environment
		 */
		public Listener( LookupEnv env, DiscoveryManagement disco ) {
			this.env = env;
			this.disco = disco;
			this.done = done;
		}
		/**
		 *  Called by 'disco' or 'ldisco' instances created, when
		 *  registrars are discovered
		 */
		public void discovered( DiscoveryEvent ev ) {
//			new Throwable("Got discovered event").printStackTrace();

			final ServiceRegistrar[] newregs = ev.getRegistrars();
			log.fine("discovery event: "+ev );
			for( int i = 0; i < newregs.length; ++i ) {
				processRegistrar( newregs[i], env );
			}
		}
		
		/**
		 *  Do a lookup in the passed environment, ignoring the state of connectivity
		 *  do the LUS.  An LUS that can not do EventRegistration, is not a problem, if
		 *  we can rediscover the LUS later.
		 */
		private void lookupUsing( ServiceRegistrar reg, LookupEnv env ) {
			try {
				// this might throw an exception if we've already lost access to the registrar
				log.fine("found lookup service at: "+reg.getLocator()  );
				// registrar still in view, do lookup
				if( lookForService( env, reg ) == false ) {
					lookupFailed( env, reg );
				}
			} catch( java.security.AccessControlException ex ) {
				reportException(ex,false);
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
		 *  Handle the passed registrar and do the lookup using the LookupEnv
		 */
		public void processRegistrar( ServiceRegistrar reg, LookupEnv env ) {
			lookupUsing( reg, env );
			registerWithLus( reg );
			lookupUsing( reg, env );
		}

		/**
		 *  Register for EventRegistration processing with the passed LUS.  If we can
		 *  not do notify(), we will discard the LUS, and it will come back later
		 *  and then we'll reprocess the LUS and find any of the new services.
		 */
		private void registerWithLus( ServiceRegistrar reg ) {
			try {

				RemoteListener rem;
				// Create our remote listener instance that will handle notifications from
				// the registrar as 
				rem = new RemoteListener( transHand, reg );
				log.fine("Creating remote listener ("+rem+") for LUS: "+reg );

				// Notify the registrar of who to send service changes to.
				EventRegistration evr = reg.notify(
					env.getServiceTemplate(),
					ServiceRegistrar.TRANSITION_MATCH_MATCH|
					ServiceRegistrar.TRANSITION_MATCH_NOMATCH|
					ServiceRegistrar.TRANSITION_NOMATCH_MATCH,
					rem, null,
					Lease.FOREVER );

				// If we can do the notify to the registrar, then
				// set up a lease renewel failure notification too.
				lrm.renewUntil( evr.getLease(), Lease.FOREVER, new LeaseListener() {
					public void notify( LeaseRenewalEvent evv ) {
						ServiceRegistrar reg = (ServiceRegistrar)
							leaseToObject.remove( evv.getLease() );
						log.fine("lost registrar: "+reg );
						rems.remove( reg );
						disco.discard(reg);
					}
				});

				// Got notification and lease renewal, so remember all the information
				// that we'll need to tell us when there is a problem and we need to
				// discard the LUS.
				log.fine( "registration of listener completed!");
				leaseToObject.put( evr.getLease(), reg );
				rems.put( reg, rem );
			} catch( RemoteException ex ) {
				log.log(Level.WARNING, "can't connect, discard LUS", ex );
				disco.discard( reg );
			} catch( Exception ex ) {
				log.log(Level.WARNING, "can't connect, discard LUS", ex );
				disco.discard( reg );
			}
		}

		protected boolean lookForService( LookupEnv env,
				ServiceRegistrar lookup ) throws RemoteException {
			ServiceMatches o = null;
			ServiceTemplate template = env.getServiceTemplate();
			log("  looking up "+template+" at "+lookup );
			if( template == null ) {
				log("  initilizing null template" );
				template = new ServiceTemplate(null,null,null);
			}
			String loc = lookup.getLocator().toString();
			try {
				log("  lookup starts: "+loc );
				o = lookup.lookup(template,20);
				if( done[0] )
					return false;
				busy = true;
				log("  lookup returns" );
			} catch( Exception ex ) {
				reportException(ex);
				return false;
			}
			
			if( o == null || o.items.length == 0 ) {
				log("  no services found");
				return false;
			}
			
			log("  Checking "+o.items.length+" matches" );
			for( int i  = 0;i < o.items.length; ++i) {
				if( done[0] )
					return false;
				try {
					addItem( lookup, o.items[i] );
				} catch( Exception ex ) {
					reportException(ex);
				}
			}
			log("Registrar processed: "+loc );
			return true;
		}

		/**
		 *  Called as services are discarded.
		 */
		public void discarded( DiscoveryEvent ev ) {
			log.fine("discarded event: "+ev );
			ServiceRegistrar[] newregs = ev.getRegistrars();
			for( int i = 0; i < newregs.length; ++i ) {
				log.finer( "lost lookup service: "+newregs[i].getServiceID()+": "+newregs[i] );
				deregisterInstance( newregs[i] );
				disco.discard(newregs[i]);
				rems.remove( newregs[i] );

			}
		}
	}
	
	/**
	 *  Can be overridden in subclass to provide handling of failed lookups, such
	 *  as scheduling subsequent 'lookup' in environments where notify() might not
	 *  work because of one way networks created due to NAT or other tunneling
	 */
	protected void lookupFailed( LookupEnv env, ServiceRegistrar reg ) {
	}

	/**
	 *  When a service is updated, refresh the information that we have about it.
	 */
	private void updateInstance( ServiceRegistrar lookup, ServiceItem si ) {
		log.fine( "updating: "+si+" from "+lookup );
		Hashtable<ServiceID,WrappedActionAccess> h = 
			lookups.get( lookup );
		if( h == null ) {
			h = new Hashtable<ServiceID,WrappedActionAccess>(3);
			lookups.put(lookup,h);
		}
		
		log.fine("updating: "+si.serviceID );
		WrappedActionAccess acc = (WrappedActionAccess)h.get( si.serviceID );
		if( ((DefaultListModel)svclist.getModel()).contains(acc) == false )
			((DefaultListModel)svclist.getModel()).addElement(acc);
		acc.setServiceInfo( lookup, si );
	}

	/**
	 *  register located services with our infrastructure.
	 */
	private void registerInstance( ServiceRegistrar lookup, ServiceItem si, WrappedActionAccess acc ) {
		log.fine("registering: "+si+" from "+lookup );
		Hashtable<ServiceID,WrappedActionAccess> h =
			lookups.get( lookup );
		if( h == null ) {
			h = new Hashtable<ServiceID,WrappedActionAccess>(3);
			lookups.put(lookup,h);
		}
		
		log.fine("registering: "+si.serviceID );
		h.put( si.serviceID, acc );
		if( ((DefaultListModel)svclist.getModel()).contains(acc) == false )
			((DefaultListModel)svclist.getModel()).addElement(acc);
	}
	
	/**
	 *  Remove service registrations from our infrastructure.
	 */
	private void deregisterInstance( ServiceRegistrar lookup ) {
		log.fine("deregistering: all from "+lookup );
		Hashtable h = (Hashtable)lookups.get( lookup );
		if( h == null ) {
			return;
		}
		Enumeration e = h.keys();
		while( e.hasMoreElements() ) {
			ServiceID item = (ServiceID)e.nextElement();
			services.remove( item );
			WrappedActionAccess acc = (WrappedActionAccess)h.get(item);
			Component comp = acc.comp;

			((DefaultListModel)svclist.getModel()).removeElement(acc);
		}
	}
	
	/**
	 *  @deprecated use addLookupServer
	 */
	public void setLookupServer( String srvr ) throws MalformedURLException {
		addLookupServer(srvr);
	}
	
	/**
	 *  Add a unicast lookup server that will be used for the default environment.
	 */
	public void addLookupServer( String srvr ) throws MalformedURLException {
		if( defaultEnv == null )
			throw new NullPointerException("No default lookup environment exists");
		defaultEnv.addLookupLocator( srvr );
		log.fine( "add unicast server: "+srvr );
		// Do unicast lookup too...
		activateLookupServer( srvr );
	}

	/**
	 *  Activate the passed lookup server by creating a LookupLocator, and a new
	 *  LookupLocatorDiscovery instance.  'ldisco' is update to refer to the new
	 *  discovery instance.
	 */
	protected void activateLookupServer( String lookupServer  ) {
		if( lookupServer != null ) {
			StringTokenizer st = new StringTokenizer( lookupServer, "," );
			Vector<LookupLocator> list = new Vector<LookupLocator>();
			while( st.hasMoreTokens() ) {
				String server = st.nextToken();
				int idx = server.indexOf(':');
				if( idx != -1 ) {
	 				String h ;
	 				int p;
					try {
		 				h = server.substring(0,idx);
		 				p = Integer.parseInt( server.substring(idx+1) );
		 				list.addElement(
		 					new LookupLocator( "jini://"+h+":"+p ));
						log.fine("added Lookup Locator (unicast) "+h+":"+p );   	
					} catch( Exception ex ) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog( parent, ex );
					}
				} else {
					try {
		 				list.addElement(new LookupLocator( "jini://"+server ));
						log.fine("added Lookup Locator (unicast) "+server );   	
					} catch( Exception ex ) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog( parent, ex );
					}
				}
			}
			LookupLocator[] arr = new LookupLocator[ list.size() ];
			list.copyInto(arr);
			if( ldisco != null ) {
				try {
					ldisco.terminate();
				} catch( Throwable ex ) {
				}
				ldisco = null;
			}
			ldisco = new LookupLocatorDiscovery( arr );
			log.fine("adding listener to unicast locators: "+list );
			ldisco.addDiscoveryListener( lis );
//		} else {
//			ldisco.shutdown();
		}
	}
	
	String descItem(ServiceItem si) {
		if( si == null )
			return "<no service Item>";
		Entry[]ents = si.attributeSets;
		String name = "<no service class loaded>: "+si.serviceID;
		if( si.service != null )
			name = si.service.getClass().getName();
		for( int j = 0; j < ents.length; ++j ) {
			if( ents[j] instanceof Name )
				name = ((Name)ents[j]).name;
			else if( ents[j] instanceof ServiceInfo )
				name = ((ServiceInfo)ents[j]).name;
		}
		return name;
	}

	private TransitionHandler transHand = new TransitionHandler();
	protected  class TransitionHandler implements RegistrarTransitionListener {
		public void removeInstance( ServiceEvent ev, ServiceRegistrar reg ) {
			log.fine("removing instance: "+descItem(ev.getServiceItem()) );
			Hashtable h = (Hashtable)lookups.get( reg );
			if( h == null ) {
				return;
			}
			ServiceID item = ev.getServiceID();
			WrappedActionAccess acc = (WrappedActionAccess)h.remove(item);
			if( acc != null )
				acc.deactivate();
			services.remove(item);
		}
	
		public void addInstance( ServiceEvent ev, ServiceRegistrar reg ) {
			log.fine("adding instance: "+descItem(ev.getServiceItem()) );
			addItem( reg, ev.getServiceItem() );
		}
	
		public void updateInstance( ServiceEvent ev, ServiceRegistrar reg ) {
			log.fine("updating instance: "+descItem(ev.getServiceItem()) );
			Hashtable h = (Hashtable)lookups.get( reg );
			if( h == null ) {
				return;
			}
			ServiceItem si = ev.getServiceItem();
			Component c = ((WrappedActionAccess)h.get(si.serviceID)).comp;
			if( c != null ) {
				Entry[]ents = si.attributeSets;
				Object name = si.service.getClass().getName();
				for( int j = 0; j < ents.length; ++j ) {	
					if( ents[j] instanceof Name )
						name = ((Name)ents[j]).name;
					else if( ents[j] instanceof ServiceInfo )
						name = ((ServiceInfo)ents[j]).name;
				}
				((JCheckBoxMenuItem)c).setText( name.toString() );
			} else {
				ServiceSelectionPane.this.updateInstance( reg, si );
			}
		}
	}

	protected Hashtable<ServiceRegistrar,RemoteListener> rems =
		new Hashtable<ServiceRegistrar,RemoteListener>();
	protected LookupDiscovery disco;
	protected LookupLocatorDiscovery ldisco;
	
	public DiscoveryManagement getDiscoveryManager() {
		return disco;
	}
	
	protected void log( String logmsg ) {
		log.fine(logmsg);
		msg.setText( logmsg );
		msg.repaint();
	}


	protected ButtonGroup migrp = new ButtonGroup();
	protected void addItem( ServiceRegistrar lookup, ServiceItem si ) {
		if( si == null || si.service == null ) {
			log.fine( si+": null service reference!" );
			return;
		}
		log.fine( "found an instance: "+si.service.getClass().getName()+": "+si );
		Entry[]ents = si.attributeSets;
		Object name = si.service.getClass().getName();
		Object ent = null;
		log.fine("addItem: svc class: "+name);
		for( int j = 0; j < ents.length; ++j ) {
			ent = ents[j];
			log.finer("ents["+j+"]: "+
				(ents[j] == null ? "null" : ents[j].getClass().getName()));
			if( ent instanceof Name && ((Name)ent).name != null && name == null ) 
				name = ((Name)ents[j]).name;
			if( ent instanceof ServiceInfo && ((ServiceInfo)ent).name != null ) 
				name = ((ServiceInfo)ent).name;
		}
		
		if( haveService( si.serviceID ) == false ) {
			services.put( si.serviceID, si );
			Object fac = si.service;

			WrappedActionAccess acc = new WrappedActionAccess( name.toString(), fac );
			JCheckBoxMenuItem com = acc.getMenuItemCheckBox();
			migrp.add(com);
//			connMenu.add( com );
			acc.setServiceInfo( lookup, si, com );
			registerInstance( lookup, si, acc );
			ActionEvent aev = new ActionEvent(
				new Object[]{si,lookup,com},
				SERVICE_FOUND, si.serviceID.toString() );
			deliverAction( aev );
			
			// All setup, enabled it now
//			connMenu.setEnabled(true);
			svclist.setEnabled(true);
			svclist.setToolTipText("Select a Server to Connect to");
		} else {
			services.put( si.serviceID, si );
		}
	}
	protected Hashtable<ServiceRegistrar,Hashtable<ServiceID,WrappedActionAccess>> lookups = 
		new Hashtable<ServiceRegistrar,Hashtable<ServiceID,WrappedActionAccess>>(5);
	protected Hashtable<ServiceID,ServiceItem> services =
		new Hashtable<ServiceID,ServiceItem>(5);

	protected boolean haveService( ServiceID id ) {
		return services.get( id ) != null;
	}

	public ServiceItem getSelectedService() {
		return matched.item;
	}

	protected WrappedActionAccess matched;
	protected  class WrappedActionAccess extends AbstractAction implements ServiceStateManager {
		String name;
		Object access;
		Component comp;
		ServiceItem item;
		ServiceRegistrar lookup;
		JCheckBoxMenuItem mic;
		
		public boolean equals( Object obj ) {
			if( obj instanceof WrappedActionAccess == false )
				return false;
			return item.serviceID.equals( ((WrappedActionAccess)obj).item.serviceID );
		}
		
		public int hashCode() {
			return item.serviceID.hashCode();
		}

		public JCheckBoxMenuItem getMenuItemCheckBox() {
			if( mic == null )
				mic = new JCheckBoxMenuItem(name);
			mic.removeActionListener(this);
			mic.addActionListener(this);	
			mic.setEnabled(true);
			return mic;
		}
		
		public String toString() {
			return name;
		}
		
		public void setServiceInfo( ServiceRegistrar reg, ServiceItem si, Component c ) {
			comp = c;
			lookup = reg;
			item = si;
		}
		
		public void setServiceInfo( ServiceRegistrar reg, ServiceItem si) {
			lookup = reg;
			item = si;
		}

		public WrappedActionAccess( String name, Object access ) {
			this.name = name;
			this.access = access;
			putValue( Action.NAME, name );
		}

		/**
		 */
		public void deactivate() {
			// Loose sight of the factory
			// Say we are disconnected
			((DefaultListModel)svclist.getModel()
				).removeElement(this);
			svclist.repaint();
			mic.setEnabled(false);
			ActionEvent aev = new ActionEvent(
				new Object[]{item,lookup}, 
				SERVICE_LOST, item.serviceID.toString() );
			deliverAction( aev );
		}

		public void actionPerformed( ActionEvent ev ) {
			log.fine("got action performed for wrapped db access" );
			matched = this;
			if( mic != null && mic.isSelected() == false )
				mic.setSelected(true);
			ActionEvent aev = new ActionEvent(
				new Object[]{ item, lookup },
				SERVICE_SELECTED, item.serviceID.toString() );
			deliverAction( aev );
		}
	}
}
