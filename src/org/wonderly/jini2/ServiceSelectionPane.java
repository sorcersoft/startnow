package org.wonderly.jini2;

import javax.swing.*;
import java.awt.*;
import org.wonderly.awt.*;
import org.wonderly.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import net.jini.jeri.*;
import net.jini.jeri.tcp.*;

import org.wonderly.util.jini.*;
import org.wonderly.util.jini2.ServiceLookup;
import org.wonderly.util.jini2.ServiceLookupHandler;
import org.wonderly.util.jini2.RemoteListener;
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
import net.jini.export.*;
import java.util.logging.*;

import java.util.*;
import java.net.*;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.NoSuchEntryException;
import net.jini.security.ProxyPreparer;
import java.util.logging.Logger;
import org.wonderly.log.*;

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

 *  For more advanced applications, multiple configurations 
 *  might be presented to the user by doing:
 *  <pre>

LookupEnv env1 = new LookupEnv( "Public Administrable" );
env1.setServiceTemplate( new ServiceTemplate( null, 
	new Class[]{Administrable.class}, null ) );
env1.addLookupLocator( "host2.domain.com");

LookupEnv env2 = new LookupEnv( "Public Services" );
env2.setServiceTemplate( new ServiceTemplate( null, null, null ) );
env2.addLookupLocator( "host1.domain.com" );

ServiceSelector os = new ServiceSelector( f,
	"Server Selection", new LookupEnv[]{env1,env2} );

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
 *  The user will then be provided a JComboBox that they 
 *  can use to select a lookup environment from.
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
	protected Vector<ActionListener> listeners =
		new Vector<ActionListener>(7);
	protected LookupEnv defaultEnv;
	protected JMenuBar bar;
	protected Configuration config;
	protected Logger log;
	protected ServiceLookup lookup;
	protected Hashtable<ServiceID,ServiceItem> services =
		new Hashtable<ServiceID,ServiceItem>(5);
	protected WrappedActionAccess matched;
    protected LookupEnv lastEnv;
    protected LookupDiscoveryManager ldm;

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

	/**
	 *  @return getClass().getName().substring(
	 *     getClass().getName().lastIndexOf('.')+1 );
	 */
	public String getName() {
		String nm = getClass().getName();
		int nmi = nm.lastIndexOf('.');
		return nm.substring(nmi+1);
	}

	/**
	 *  @return getClass().getName().substring(0,
	 *		getClass().getName().lastIndexOf('.') );
	 */
	public String getPackage() {
		String nm = getClass().getName();
		int nmi = nm.lastIndexOf('.');
		return nm.substring(0,nmi);
	}
	
	private Logger myLogger() throws ConfigurationException {
		Logger log = null;
		try {
			log = (Logger)config.getEntry( getPackage(),
				getName(), Logger.class );
		} catch( NoSuchEntryException ex ) {
			log = new StdoutLogger( getName(), null );
		}
		if( log == null )
			log = new StdoutLogger( getName(), null );

		return log;
	}

	public ServiceSelectionPane( Component parent, 
			ServiceTemplate templ, 
			String groups[], Configuration conf ) 
				throws IOException, ConfigurationException {
		this( parent, templ, groups, conf, null );
	}

	public ServiceSelectionPane( Component parent, 
			ServiceTemplate templ,
			String groups[], Configuration conf, Logger log )
				throws IOException, ConfigurationException {
		this.parent = parent;
		config = conf;
		log = myLogger();
		defaultEnv = new LookupEnv( "Default" );
		defaultEnv.setServiceTemplate( templ );
		if( groups != null ) {
			for( int i = 0; i < groups.length; ++i ) {
				defaultEnv.addGroup(groups[i]);
			}
		}
		activateJini(new LookupEnv[]{defaultEnv});
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelectionPane( Component parent,
			LookupEnv env, Configuration conf ) 
				throws IOException, ConfigurationException {
		this( parent, env, conf, null );
	}

	public ServiceSelectionPane( Component parent,
			LookupEnv env, Configuration conf, Logger log )
				throws IOException, ConfigurationException {
		this.parent = parent;
		config = conf;
		this.log = log;
		if( log == null )
			this.log = myLogger();
		
		activateJini(new LookupEnv[]{env});
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelectionPane( JFrame parent,
			LookupEnv envs[], Configuration conf ) 
				throws IOException, ConfigurationException {
		this( parent, envs, conf, null );
	}

	public ServiceSelectionPane( JFrame parent,
			LookupEnv envs[], Configuration conf, Logger log )
			throws IOException, ConfigurationException {
		this.parent = parent;
		config = conf;
		this.log = log;
		if( log == null )
			this.log = myLogger();

		activateJini(envs);
	}
	
	public void addActionListener( ActionListener lis ) {
		listeners.addElement(lis);
	}
	
	public void removeActionListener( ActionListener lis ) {
		listeners.removeElement( lis );
	}
	
	private void deliverAction(ActionEvent ev ) {
		log.fine(this+": delivering to "+
			listeners.size()+" listeners" );
		for( int i = 0; i < listeners.size(); ++i ) {
			((ActionListener)listeners.elementAt(i)
				).actionPerformed( ev );
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
	private void activateJini( LookupEnv envset[] ) 
			throws ConfigurationException, IOException {
		if( log == null )
			log = new StdoutLogger( getName(), null );


 		try {
 			if( System.getSecurityManager() == null ) {
 				log.fine(this+": Activating RMISecurityManager" );
				System.setSecurityManager( new RMISecurityManager() );
 			}
		} catch( Error ex ) {
			log.throwing( getClass().getName(), "activateJini", ex );
		}

 		final JPanel p = this;
 		final Packer pk = new Packer( p );
 		svclist = new JList(new DefaultListModel());
 		final ListCellRenderer rnd = svclist.getCellRenderer();
 		final JLabel cl = new JLabel();
 		JPanel sp = new JPanel();
 		sp.setBorder(
 			BorderFactory.createTitledBorder(
 			"Registered Servers"));
 		Packer spk = new Packer( sp );
 		spk.pack( new JScrollPane( svclist ) ).fillboth();
 		final JPanel selp = new JPanel();
 		Packer slpk = new Packer( selp );
 		envmod = new DefaultComboBoxModel();
 		// mirror image of envmod to check for presence.
 		envs = new Vector<LookupEnv>(); 
 		envbox = new JComboBox(envmod);
		slpk.pack( new JLabel( "LUS Env:") 
			).gridx(0).gridy(0).inset(0,4,0,4);
		slpk.pack( envbox ).gridx(1).gridy(0).fillx();
 		envbox.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				LookupEnv env = (LookupEnv)envbox.getSelectedItem();
 				if( env == null )
 					return;
 				try {
 					log.finer("entry ("+env+
 						") selected, inited? "+inited );
 					if( inited ) {
 						log.finest("activating: "+env );
 						activateEnv( env );
 						log.finest("activated: "+env);
 					}
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
 		bar.setBorder(BorderFactory.createEtchedBorder());
 		JMenu m;
 		m = new JMenu("Select");
 		bar.add(m);
 		m.add(conn);
 		JMenuItem mi;

 		m = new JMenu("Show");
 		bar.add(m);
 		final JCheckBoxMenuItem luscb = 
 			new JCheckBoxMenuItem("LUS Configs");
 		final JCheckBoxMenuItem msgcb = 
 			new JCheckBoxMenuItem("Progress Messages");
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
			 		pk.pack( msg ).gridw(2).gridx(0
			 			).gridy(4).fillx();
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
 		final JMenuItem byName = 
 			new JCheckBoxMenuItem("Names/ServiceInfo");
 		grp.add(byName);
 		byName.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				svclist.repaint();
 			}
 		});
 		m.add( byName );
 		byName.setSelected(true);
 		
 		final JMenuItem byClass = 
 			new JCheckBoxMenuItem("Class Names");
 		grp.add(byClass);
 		byClass.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				svclist.repaint();
 			}
 		});
 		m.add( byClass );
 		
 		final JMenuItem bySvcID = 
 			new JCheckBoxMenuItem("Service IDs");
 		grp.add(bySvcID);
 		bySvcID.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				svclist.repaint();
 			}
 		});
 		m.add( bySvcID );
 		
 		svclist.setCellRenderer( new ListCellRenderer() {
 			public Component getListCellRendererComponent(
 					JList list, Object value,
 					int index, boolean isSelected,
 						boolean cellHasFocus) {
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
 						boolean isRemote =
 							intf[i].getName().equals(
 								"java.rmi.Remote");
 						boolean isSerializable =
 							intf[i].getName().
 								equals("java.io.Serializable");
 						if( isRemote == false &&
 								isSerializable == false ) {
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
 		msg = new JLabel(" ");
 		msg.setBorder( BorderFactory.createLoweredBevelBorder() );
 		msgcb.setSelected(false);

 		svclist.addListSelectionListener( new ListSelectionListener() {
 			public void valueChanged( ListSelectionEvent ev ) {
 				if( ev.getValueIsAdjusting() )
 					return;
 				conn.setEnabled(true);
 				conn.setToolTipText( 
 					"Click to Connect to the Selected Server");
 				log.fine( svclist.getSelectedValue()+" selected" );
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
    	log.fine("shutting down, ldm: "+ldm );
    	if( ldm != null ) {
    		try {
//    			ldm.removeDiscoveryListener(lis);
//    			ldm.terminate();
    			lookup.shutdown();
    		} catch( Throwable ex ) {
    		}
    		ldm = null;
    	}
    }

    /**
     *  Active the indicated LookupEnv environment to find
     *  services to select from
     *
     *  @throws IOException if an error occurs in ServiceLookup
     *  @throws ConfigurationException if an error occurs in ServiceLookup
     *  @throws NullPointerException if env is null
     */
    protected void activateEnv( LookupEnv env ) 
    		throws IOException, NullPointerException, ConfigurationException {
    	if( lastEnv == env ) {
    		throw new NullPointerException("null LookupEnv" );
    	}

    	log.finer("Shutdown current discovery");
    	if( lookup != null ) {
    		try {
    			lookup.shutdown();
    		} catch( Exception ex ) {
    			log.log( Level.SEVERE, ex.toString(), ex );
    		}
    	}
    	log.finest("Resetting all structures");

    	lookup = new ServiceLookup( env,
			new ServiceLookupHandler() {
				public void serviceLost( ServiceID id,
						ServiceRegistrar reg ) {
					log.fine("serviceLost("+id+", "+reg+" )" );
				}
				public void updateItem( ServiceItem item, 
						ServiceRegistrar reg ) {
					log.fine("updateItem("+
						descItem(item)+", "+reg+" )" );
				}
				public void processItem( ServiceItem item, 
						ServiceRegistrar reg ) {
					log.fine("processItem("+
						descItem(item)+", "+reg+" )" );
					try {
						addItem( reg, item );
					} catch( ConfigurationException ex ) {
						reportException(ex);
					} catch( IOException ex ) {
						reportException(ex);
					}
				}
				}, log, config );

    	log.finest("Removing all services from list");
    	((DefaultListModel)svclist.getModel()).removeAllElements();
    	lastEnv = env;
    	log.fine("\n"+this+": Activating ("+ldm+
    		") lookup env: "+env );
		log.fine(this+": Create Service template: "+
			env.getServiceTemplate());

		lookup.start();
	}

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
		log.fine("Launching Selected Service: "+
			svclist.getSelectedValue());
		msg.setText(" ");
		msg.repaint();
		new ComponentUpdateThread( svclist ) {
			public Object construct() {
				try {
					int sidx = svclist.getSelectedIndex();
					if( sidx >= 0 ) {
						WrappedActionAccess db = 
							(WrappedActionAccess)svclist.getSelectedValue();
						db.actionPerformed(
							new ActionEvent(this, 0, db.toString()) );
					}
				} catch( IllegalArgumentException ex ) {
					log.throwing(getClass().getName(),
						"launchSelectedService", ex );
					int sidx = svclist.getSelectedIndex();
					if( sidx >= 0 ) {
						WrappedActionAccess db = 
							(WrappedActionAccess)svclist.getSelectedValue();
						log.fine(this+": discarding LUS: "+db.lookup );
						ldm.discard(db.lookup);
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

	protected void reportException(
			final Throwable ex, boolean showUser ) {
//		log.throwing( getClass().getName(), "reportException", ex );
		log.log( Level.SEVERE, ex.toString(), ex );
		if( showUser ) {
			runInSwing( new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog( parent, ex,
						"Exception", JOptionPane.ERROR_MESSAGE );
				}
			});
		}
	}

	protected void runInSwing( final Runnable r ) {
		if( SwingUtilities.isEventDispatchThread() ) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(r);
			} catch( Exception ex ) {
				ex.printStackTrace();
			}
		}
	}

	public void addLookupServer( String srvr ) 
				throws MalformedURLException {
		if( defaultEnv == null )
			throw new NullPointerException(
				"No default lookup environment exists");
		defaultEnv.addLookupLocator( srvr );
		log.fine(this+": Add unicast server: "+srvr );
		// Do unicast lookup too...
	}
	
	protected String descItem(ServiceItem si) {
		if( si == null )
			return "<no service Item>";
		Entry[]ents = si.attributeSets;
		String name = si.service.getClass().getName();
		for( int j = 0; j < ents.length; ++j ) {
			if( ents[j] instanceof Name )
				name = ((Name)ents[j]).name;
			else if( ents[j] instanceof ServiceInfo )
				name = ((ServiceInfo)ents[j]).name;
		}
		return name;
	}

	protected ButtonGroup migrp = new ButtonGroup();
	protected void addItem( ServiceRegistrar lookup,
			ServiceItem si )
				throws ConfigurationException, IOException {
		if( si == null || si.service == null ) {
			log.severe( "null "+((si==null) ? 
				"item" : "service")+" reference!" );
			return;
		}
		log.fine( "Found an instance: "+
			si.service.getClass().getName()+": "+si );
		Entry[]ents = si.attributeSets;
		Object name = si.service.getClass().getName();
		Object ent = null;
		log.fine("svc class: "+name);
		for( int j = 0; j < ents.length; ++j ) {
			ent = ents[j];
			log.fine("ents["+j+"]: "+
				(ents[j] == null ? "null" :
					ents[j].getClass().getName()));
			if( ent instanceof Name && 
					((Name)ent).name != null && name == null ) 
				name = ((Name)ents[j]).name;
			if( ent instanceof ServiceInfo && 
					((ServiceInfo)ent).name != null ) {
				name = ((ServiceInfo)ent).name;
			}
		}
		
		if( haveService( si.serviceID ) == false ) {
			log.fine( "Don't have svc "+descItem(si)+" yet" );
			services.put( si.serviceID, si );
			Object fac = si.service;

			// Prepare the proxy now.
 			ProxyPreparer pp = getProxyPreparer(si);
 			log.finer( "ProxyPreparer: "+pp );
 			if( pp != null ) {
 				try {
 					pp.prepareProxy( fac );
 				} catch( SecurityException ex ) {
 					log.log( Level.SEVERE, 
 						"Service Proxy Prepare Failed", ex );
 					return;
 				}
 			}
 
			WrappedActionAccess acc = 
				new WrappedActionAccess( name.toString(), fac );
			JCheckBoxMenuItem com = acc.getMenuItemCheckBox();
			migrp.add(com);
			acc.setServiceInfo( lookup, si, com );
			if( ((DefaultListModel)svclist.getModel()
					).contains(acc) == false ) {
				((DefaultListModel)svclist.getModel()
						).addElement(acc);
			}
			ActionEvent aev = new ActionEvent(
				new Object[]{si,lookup,com},
				SERVICE_FOUND, si.serviceID.toString() );
			deliverAction( aev );
			
			// All setup, enable it now
			svclist.setEnabled(true);
			svclist.setToolTipText("Select a Server to Connect to");
		} else {
			services.put( si.serviceID, si );
		}
	}
		
	protected ProxyPreparer getProxyPreparer( 
			ServiceItem item ) throws ConfigurationException {
		try {
			return (ProxyPreparer) config.getEntry(
		    	getPackage()+"."+getName(), 
		    		"preparer", ProxyPreparer.class);
		} catch( ClassCastException ex ) {
			log.throwing( getClass().getName(), 
				"getProxyPreparer", ex );
		} catch( NoSuchEntryException ex ) {
			log.throwing( getClass().getName(), 
				"getProxyPreparer", ex );
		}
		return null;
	}

	protected boolean haveService( ServiceID id ) {
		return services.get( id ) != null;
	}

	public ServiceItem getSelectedService() {
		return matched.item;
	}

	protected class WrappedActionAccess extends AbstractAction 
			implements ServiceStateManager {
		String name;
		Object access;
		Component comp;
		ServiceItem item;
		ServiceRegistrar lookup;
		JCheckBoxMenuItem mic;
		
		public boolean equals( Object obj ) {
			if( obj instanceof WrappedActionAccess == false )
				return false;
			return item.serviceID.equals( 
				((WrappedActionAccess)obj).item.serviceID );
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
		
		public void setServiceInfo(
				ServiceRegistrar reg, ServiceItem si, 
					Component c ) {
			comp = c;
			lookup = reg;
			item = si;
		}
		
		public void setServiceInfo( ServiceRegistrar reg, 
				ServiceItem si) {
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
			log.fine("Deactivating: "+this);
			((DefaultListModel)svclist.getModel()).removeElement(this);
			svclist.repaint();
			mic.setEnabled(false);
			ActionEvent aev = new ActionEvent(
				new Object[]{item,lookup},
					SERVICE_LOST, item.serviceID.toString() );
			deliverAction( aev );
		}

		public void actionPerformed( ActionEvent ev ) {
			log.fine("performing selection for: "+
				this+", mic: "+mic.isSelected() );
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