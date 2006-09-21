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
import java.net.*;

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
 *  The user will then be provided a JComboBox that they can use to select a lookup environment from.
 *  This allows multiple servers to be queried for different interfaces as needed.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class ServiceSelector extends JDialog {
	protected JDialog condlg;
	protected JFrame parent;
	protected Vector<ActionListener> listeners = new Vector<ActionListener>(7);
	protected LookupEnv defaultEnv;
	protected ServiceSelectionPane sp;

	public static void main( String args[] ) throws Exception {
		JFrame f = new JFrame("test");
		f.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev ) {
				System.exit(0);
			}
		});
		f.setLocation( 200, 200 );
		f.setVisible(true);
		LookupEnv env = new LookupEnv( "Public Administrable" );
		env.addGroup("");
		String loc = System.getProperty("org.wonderly.locator.host");
		if( loc != null ) {
			String[] locs = loc.split(",");
			for( int i = 0; i < locs.length; ++i ) {
				env.addLookupLocator( locs[i] );
			}
		}
		env.setServiceTemplate( new ServiceTemplate(
			null, new Class[]{ net.jini.admin.Administrable.class }, null ));
		ServiceSelector sel = new ServiceSelector( f, "Select Service", env );
		sel.setLocationRelativeTo( f );
		sel.setVisible(true);		
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector( JFrame parent, String title, ServiceTemplate templ, String groups[] ) throws IOException {
		super(parent,title, true );
		this.parent = parent;
		defaultEnv = new LookupEnv( "Default" );
		defaultEnv.setServiceTemplate( templ );
		if( groups != null ) {
			for( int i = 0; i < groups.length; ++i ) {
				defaultEnv.addGroup(groups[i]);
			}
		}
		sp = new ServiceSelectionPane( parent, templ, groups );
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector( JFrame parent, String title, LookupEnv env ) throws IOException {
		super(parent,title, true );
		this.parent = parent;
		sp = new ServiceSelectionPane( parent, env );
	}

	/**
	 *  Called to construct an instance
	 */
	public ServiceSelector( JFrame parent, String title, LookupEnv envs[] ) throws IOException {
		super(parent,title, true );
		this.parent = parent;
		sp = new ServiceSelectionPane( parent, envs );
		activateJini(sp);
	}
	
	public void addActionListener( ActionListener lis ) {
		listeners.addElement(lis);
	}
	
	public void removeActionListener( ActionListener lis ) {
		listeners.removeElement( lis );
	}
	
	private void deliverAction(ActionEvent ev ) {
//		System.out.println(this+": delivering to "+listeners.size()+" listeners" );
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
	private void activateJini( ServiceSelectionPane ssp ) throws IOException {
 		try {
 			if( System.getSecurityManager() == null ) {
 				System.out.println(this+": Activating RMISecurityManager" );
				System.setSecurityManager( new RMISecurityManager() );
 			}
		} catch( Error ex ) {
		}

 		condlg = this;
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
 				System.out.println(this+": Exit selected");
 				setVisible(false);
 			}
 		});
 		condlg.pack();
 		condlg.setLocationRelativeTo(parent);

 		inited = true;
    }
    
    public void setVisible( boolean how ) {
    	if( !how ) {
    		shutdown();
    	} else {
    		try {
    			activateJini(sp);
    		} catch( IOException ex ) {
    			ex.printStackTrace();
    		}
    	}
    	super.setVisible(how);
    }

    protected void shutdown() {
    	sp.shutdown();
    }
}
