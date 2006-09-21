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

import org.wonderly.jini2.ServiceSelectionPane;
import org.wonderly.util.jini.*;
import org.wonderly.util.jini2.ServiceLookup;
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
import java.net.*;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.config.ConfigurationException;
import java.util.logging.*;

import org.wonderly.log.*;

/**
 *  This class provides a mechanism to allow specific services to be
 *  selected for use by users via a JDialog.  The typical use is to construct an instance
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
		if( ev.getID() == ServiceSelectionPane.SERVICE_SELECTED ) {
			Object[]arr = (Object[])ev.getSource();
			ServiceItem it = (ServiceItem)arr[0];
			ServiceRegistrar reg = (ServiceRegistrar)arr[1];
			System.out.println(this+": Selected Service: "+
				it.serviceID+" from: "+reg );
			... do something with it...
		} else if( ev.getID() == ServiceSelectionPane.SERVICE_LOST ) {
		} else if( ev.getID() == ServiceSelectionPane.SERVICE_FOUND ) {
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
 *  The user will then be provided a JComboBox that they can use to select a lookup environment from.
 *  This allows multiple servers to be queried for different interfaces as needed.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class ServiceSelector extends ConfigurableJiniApplication {
	protected JDialog condlg;
	protected JFrame parent;
	protected Vector listeners = new Vector(7);
	protected LookupEnv defaultEnv;
	protected ServiceSelectionPane sp;
	protected LookupEnv[] publicEnvs = new LookupEnv[] {
		new LookupEnv("Public Services", new ServiceTemplate(
			null, null, null), new String[]{ "" } )
	};
 
	public static void main( String args[] ) throws Exception {
		final JFrame f = new JFrame("test");
		f.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev ) {
				System.exit(0);
			}
		});
		JLabel l = new JLabel( "Initializing...." );
		l.setFont( new Font( "serif", Font.PLAIN, 36 ) );
		f.getContentPane().add( l );
		f.pack();
		f.setLocation( 200, 200 );
		f.setVisible(true);
		final ServiceSelector sel = new ServiceSelector( args, f, "Select Service" );
		sel.condlg.setLocationRelativeTo( f );
		sel.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				if( ev.getID() == ServiceSelectionPane.SERVICE_LOST ) {
					sel.log.fine( "lost: "+ev );
				} else if( ev.getID() == ServiceSelectionPane.SERVICE_FOUND ) {
					sel.log.fine( "found: "+ev );
				} else if( ev.getID() == ServiceSelectionPane.SERVICE_SELECTED ) {
//					sel.log.fine( "selected: "+ev );
					Object[]arr = (Object[])ev.getSource();
					ServiceItem it = (ServiceItem)arr[0];
					ServiceRegistrar reg = (ServiceRegistrar)arr[1];
					String regdesc = reg.toString();
					try {
						regdesc = reg.getLocator().toString();
					} catch( RemoteException ex ) {
						ex.printStackTrace();
					}
					sel.log.info( "Selected \""+ServiceLookup.descItem(it)+"\" at "+regdesc );
					f.setVisible(false);
				}
			}
		});
		l.setText( "Selector Test" );
		f.pack();
		sel.setVisible(true);
		f.setVisible(false);
		System.exit(1);
	}
	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector(  Configuration args, JFrame parent, String title, 
				ServiceTemplate templ, String groups[] ) throws IOException, ConfigurationException {
		super(args);
		log.fine("cons with: "+templ+", groups: "+groups );
		condlg = new JDialog(parent,title, true );
		this.parent = parent;
		defaultEnv = new LookupEnv( "Default" );
		defaultEnv.setServiceTemplate( templ );
		log.fine( groups.length+" groups" );

		if( groups != null ) {
			for( int i = 0; i < groups.length; ++i ) {
				log.finer( "adding group: "+groups[i]);
				defaultEnv.addGroup(groups[i]);
			}
		}
		sp = new ServiceSelectionPane( parent, templ, groups, conf, log );
//		activateJini(sp);
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector(  Configuration args, JFrame parent, String title, LookupEnv env ) throws IOException, ConfigurationException {
		super(args);
		log.fine("cons with env: "+env );
		condlg = new JDialog(parent,title, true );
		this.parent = parent;
		sp = new ServiceSelectionPane( parent, env, conf, log );
//		activateJini(sp);
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector( Configuration args, JFrame parent, String title, LookupEnv envs[] ) throws IOException, ConfigurationException {
		super(args);
		log.fine("cons with envs: "+envs );
		condlg = new JDialog(parent,title, true );
		this.parent = parent;
		sp = new ServiceSelectionPane( parent, envs, conf, log );
//		activateJini(sp);
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector( Configuration args, JFrame parent, String title ) throws IOException, ConfigurationException {
		super(args);
//		new Throwable("cons with cfg envs").printStackTrace();
		LookupEnv envs[] = getLookupEnvs();
		if( envs == null ) {
			envs = publicEnvs;
		}
		log.fine("cons with config envs: "+envs );
		condlg = new JDialog(parent,title, true );
		this.parent = parent;
		sp = new ServiceSelectionPane( parent, envs, conf, log );
//		activateJini(sp);
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector( String args[], JFrame parent, String title, 
				ServiceTemplate templ, String groups[] ) throws IOException, ConfigurationException {
		super(args);
		log.fine("cons with: "+templ+", groups: "+groups );
		condlg = new JDialog(parent,title, true );
		this.parent = parent;
		defaultEnv = new LookupEnv( "Default" );
		defaultEnv.setServiceTemplate( templ );
		log.fine( groups.length+" groups" );

		if( groups != null ) {
			for( int i = 0; i < groups.length; ++i ) {
				log.finer( "adding group: "+groups[i]);
				defaultEnv.addGroup(groups[i]);
			}
		}
		sp = new ServiceSelectionPane( parent, templ, groups, conf, log );
//		activateJini(sp);
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector( String args[], JFrame parent, String title, LookupEnv env ) throws IOException, ConfigurationException {
		super(args);
		log.fine("cons with env: "+env );
		condlg = new JDialog(parent,title, true );
		this.parent = parent;
		sp = new ServiceSelectionPane( parent, env, conf, log );
//		activateJini(sp);
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector( String args[], JFrame parent, String title, LookupEnv envs[] ) throws IOException, ConfigurationException {
		super(args);
		log.fine("cons with envs: "+envs );
		condlg = new JDialog(parent,title, true );
		this.parent = parent;
		sp = new ServiceSelectionPane( parent, envs, conf, log );
//		activateJini(sp);
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector( String args[], JFrame parent, String title ) throws IOException, ConfigurationException {
		super(args);
		LookupEnv envs[] = getLookupEnvs();
		if( envs == null ) {
			envs = publicEnvs;
		}
		log.fine("cons with string envs: "+envs );
		condlg = new JDialog(parent,title, true );
		this.parent = parent;
		sp = new ServiceSelectionPane( parent, envs, conf, log );
//		activateJini(sp);
	}

	public void addActionListener( ActionListener lis ) {
		sp.addActionListener(lis);
//		log.fine("adding listener: "+lis );
//		listeners.addElement(lis);
//		log.finer("listeners now has: "+listeners.size()+" elements");
//		if( log.isLoggable( Level.FINEST ) )
//			log.finest("listeners are: "+listeners );
	}

	public void removeActionListener( ActionListener lis ) {
		sp.removeActionListener(lis);
//		log.fine("remove listener: "+lis );
//		listeners.removeElement( lis );
	}

//	private void deliverAction(ActionEvent ev ) {
//		log.fine("delivering to "+listeners.size()+" listeners" );
//		if( log.isLoggable(Level.FINEST) )
//			log.finest("listeners Object: "+listeners );
//		for( int i = 0; i < listeners.size(); ++i ) {
//			log.finer("deliver "+ev+" to "+listeners.elementAt(i) );
//			((ActionListener)listeners.elementAt(i)).actionPerformed( ev );
//		}
//	}

	protected DefaultComboBoxModel envmod;
	protected Vector<LookupEnv> envs;
	protected JComboBox envbox;
	public void addLookupEnv( LookupEnv env ) {
		if( envs.contains(env) == false ) {
			log.finer( "adding LookupEnv: "+env );
			envs.addElement(env);
			envmod.addElement( env );
		} else {
			log.finer( env+" already known" );
		}
	}

	protected boolean inited;
	private void activateJini( ServiceSelectionPane ssp ) throws IOException {
//		new Throwable("activate jini").printStackTrace();
 		final JPanel p = new JPanel();
 		condlg.setContentPane(p);
 		final Packer pk = new Packer( p );
 		int y = -1;
 		pk.pack( ssp ).gridx(0).gridy(++y).fillboth();
 		pk.pack( new JSeparator()).gridx(0).gridy(++y).fillx().gridw(2);
 		condlg.setJMenuBar( sp.getJMenuBar() );
 		final JButton exit = new JButton("Cancel");
 		pk.pack( exit ).gridy(++y).inset(8,8,8,8);
 		exit.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				log.fine(this+": Exit selected");
 				setVisible(false);
 			}
 		});
 		condlg.pack();
 		condlg.setLocationRelativeTo(parent);

 		inited = true;
    }
    
    /**
     *  Overidden to check for how == false and do a dispose()
     *  on the dialog.
     */
    public void setVisible( boolean how ) {
    	if( !how ) {
    		log.fine("Deactivating Jini environment");
    		shutdown();
    	} else {
    		try {
    			log.fine("Activating Jini environment");
    			activateJini(sp);
    		} catch( IOException ex ) {
    			ex.printStackTrace();
    		}
    	}
    	condlg.setVisible(how);
    	if( !how ) {
    		condlg.dispose();
    	}
    }

    protected void shutdown() {
    	log.fine("Shutting down Jini Environment");
    	sp.shutdown();
    }
}