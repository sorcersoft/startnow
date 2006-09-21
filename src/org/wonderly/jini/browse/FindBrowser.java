package org.wonderly.jini.browse;

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
import  org.wonderly.jini.serviceui.*;
import java.awt.*;
import java.rmi.*;

/**
 *  This is a Java application that uses the SDM to for a JiniExplorer
 *  implementation as a serviceUI, and then instantiates it.
 *
 *  @see JiniExplorer
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */

public class FindBrowser {
	static LookupDiscoveryManager ldm;
	public static void main( String args[] ) throws Exception {
		if( System.getSecurityManager() == null )
			System.setSecurityManager( new RMISecurityManager() );

		String group[] = {""};
		if( System.getProperty("org.wonderly.jini.group") != null ) {
			group = System.getProperty("org.wonderly.jini.group").split(",");
		}
		UIDescriptor uid = new UIDescriptor();
		uid.role = MainUI.ROLE;
		// We will be doing a lookup for JiniExplorer implementations
		final ServiceTemplate templ = new ServiceTemplate( null,
			new Class[] {
				JiniExplorer.class
			}, new Entry[]{ uid } );
		String loc = System.getProperty("org.wonderly.jini.locator.host");
		LookupLocator[]ll = null;
		if( loc != null ) {
			ll = new LookupLocator[] {
				new LookupLocator( "jini://"+loc )
			};
		}
		ldm = new LookupDiscoveryManager(
			group,
			ll,
			new DiscoveryListener() {
			public void discarded( DiscoveryEvent ev ) {
				System.out.println("discarded: "+ev );
			}
			public void discovered( DiscoveryEvent ev ) {
				System.out.println("discovered: "+ev );
				ServiceRegistrar regs[] = ev.getRegistrars();
				for( int i = 0; i < regs.length; ++i ) {
					try {
						regs[i].notify( templ, 
							regs[i].TRANSITION_MATCH_MATCH|
							regs[i].TRANSITION_MATCH_NOMATCH |
							regs[i].TRANSITION_NOMATCH_MATCH,
							new RemoteListener( new RegistrarTransitionListener() {
						public void removeInstance(ServiceEvent ev,ServiceRegistrar reg) {
						}
						public void addInstance(ServiceEvent ev,ServiceRegistrar reg) {
							ServiceItem itm = ev.getServiceItem();
							processItem( itm );
						}
						public void updateInstance(ServiceEvent ev,ServiceRegistrar reg) {
						}
						}, regs[i] ), null, Lease.FOREVER );
					} catch( RemoteException ex ) {
						JOptionPane.showMessageDialog( null, ex );
					}
					try {
						ServiceMatches mt = regs[i].lookup( templ,1 );
						ServiceItem itms[] = mt.items;
						for( int j = 0; j < itms.length; ++j ) {
							processItem( itms[j] );
						}
					} catch( Exception ex ) {
						JOptionPane.showMessageDialog( null, ex );
					}
				}
			}
		});
		Object lock = new Object();
		synchronized( lock ) {
			lock.wait();
		}
	}
	
	static protected void processItem( ServiceItem item ) {
		Entry ent[] = item.attributeSets;
		Object svc = item.service;
		String name = item.service.getClass().getName();
		String vs[] = name.split("\\.");
		name = vs[vs.length-1];
		for( int i = 0; i < ent.length; ++i ) {
			if( ent[i] instanceof Name ) {
				name = ((Name)ent[i]).name;
			} else if( ent[i] instanceof ServiceInfo ) {
				name = ((ServiceInfo)ent[i]).name;
			}
		}
		for( int i = 0; i < ent.length; ++i ) {
			if( ent[i] instanceof UIDescriptor && ((UIDescriptor)ent[i]).role.equals(MainUI.ROLE) ) {
				openRole( (UIDescriptor)ent[i], svc, name );
			}
		}
	}

	static protected void openRole( UIDescriptor uid, Object svc, String name ) {
		Object fobj = null;
		try {
			fobj = uid.getUIFactory( svc.getClass().getClassLoader() );
		} catch( Exception ex ) {
			ex.printStackTrace();
			try {
				fobj = uid.getUIFactory( uid.getClass().getClassLoader() );
			} catch( Exception exx ) {
				exx.printStackTrace();
				JOptionPane.showMessageDialog( null, exx );
				return;
			}
		}
		
		if( fobj instanceof JFrameFactory ) {
			JFrame f = ((JFrameFactory)fobj).getJFrame(svc);
			f.setSize( 400, 300 );
			f.setVisible(true);
			f.addWindowListener( new WindowAdapter() {
				public void windowClosing( WindowEvent ev ) {
					System.exit(1);
				}
			});
		} else if( fobj instanceof JComponentFactory ) {
			JFrame f = new JFrame( name );
			f.setContentPane( ((JComponentFactory)fobj).getJComponent(svc) );
			f.pack();
			f.setLocation( 100, 100 );
			f.setVisible(true);
			f.addWindowListener( new WindowAdapter() {
				public void windowClosing( WindowEvent ev ) {
					System.exit(1);
				}
			});
		} else {
			throw new IllegalArgumentException( "Don't know how to "+
				"process this factory: "+fobj.getClass().getName() );
		}
	}
}
