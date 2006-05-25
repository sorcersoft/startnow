package org.wonderly.jini2.browse;

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

import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.config.ConfigurationException;
import net.jini.export.Exporter;
import org.wonderly.jini.browse.JiniExplorer;
import org.wonderly.jini2.ConfigurableJiniApplication;
import org.wonderly.util.jini.RemoteListener;

/**
 *  This is a Java application that uses the SDM to for a JiniExplorer
 *  implementation as a serviceUI, and then instantiates it.
 *
 *  @see JiniExplorer
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */

public class FindBrowser extends ConfigurableJiniApplication {
	static LookupDiscoveryManager ldm;
	private String groups[];

	public static void main( String args[] ) throws Exception {
		String laf = UIManager.getSystemLookAndFeelClassName();
		try {
		  	UIManager.setLookAndFeel(laf);
		    // If you want the Cross Platform L&F instead, comment out the
			// above line and uncomment the following:

		    // UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName());

		} catch (UnsupportedLookAndFeelException exc) {
		    System.err.println("Warning: UnsupportedLookAndFeel: " + laf);
		} catch (Exception exc) {
		    System.err.println("Error loading " + laf + ": " + exc);
		}
		new FindBrowser( args );
	}
	
	public FindBrowser( String args[] ) throws ConfigurationException,IOException,InterruptedException {
		super(args);
	
		// Get the groups to confine the search to.
		groups = getGroups();

		// Look for MainUI objects
		UIDescriptor uid = new UIDescriptor();
		uid.role = MainUI.ROLE;

		// We will be doing a lookup for JiniExplorer implementations
		final ServiceTemplate templ = new ServiceTemplate( null,
			new Class[] {
				JiniExplorer.class
			}, new Entry[]{ uid } );

		// Get the locators needed.
		LookupLocator[]ll = getLocators();
		
		// Create the LookupDiscoveryManager using the above parameters
		ldm = new LookupDiscoveryManager(
			groups,
			ll,
			// Provide a DiscoveryListener that we can track events with.
			new DiscoveryListener() {
				public void discarded( DiscoveryEvent ev ) {
					log.fine("discarded: "+ev.getRegistrars().length+" registrar(s)" );
				}
				public void discovered( DiscoveryEvent ev ) {
					log.fine("discovered: "+ev.getRegistrars().length+" registrar(s)" );
					ServiceRegistrar regs[] = ev.getRegistrars();
					for( int i = 0; i < regs.length; ++i ) {
						try {
							log.fine("processing: "+regs[i].getLocator() );
							log.finer("asking for notify from: "+regs[i].getLocator() );
							RegistrarTransitionListener reglis = new RegistrarTransitionListener() {
								public void removeInstance(ServiceEvent ev,ServiceRegistrar reg) {
									log.fine( "notified of remove: "+reg+", ev: "+ev );
								}
								public void addInstance(ServiceEvent ev,ServiceRegistrar reg) {
									log.fine( "notified of: "+reg+", ev: "+ev );
									ServiceItem itm = ev.getServiceItem();
									processItem( itm );
								}
								public void updateInstance(ServiceEvent ev,ServiceRegistrar reg) {
									log.fine( "notified of update: "+reg+", ev: "+ev );
								}
							};
							RemoteEventListener rl;
							rl = new org.wonderly.util.jini2.RemoteListener( reglis, regs[i] );
							Exporter exp = getExporter(rl,false,null);
							if( exp == null )
								exp = getExporter( FindBrowser.this, false );
							rl = (RemoteEventListener)exp.export(rl);

							regs[i].notify( templ, 
								regs[i].TRANSITION_MATCH_MATCH|
								regs[i].TRANSITION_MATCH_NOMATCH |
								regs[i].TRANSITION_NOMATCH_MATCH,
								rl, null, Lease.FOREVER );
							log.finer("notify establised from: "+regs[i].getLocator() );
						} catch( ConfigurationException ex ) {
							logException( "discovered: "+regs[i], ex );
						} catch( IOException ex ) {
							logException( "discovered: "+regs[i], ex );
						}
						try {
							log.fine( "Lookup using: "+templateToString(templ) );
							ServiceMatches mt = regs[i].lookup( templ,1 );
							ServiceItem itms[] = mt.items;
							log.fine( "Lookup returns "+itms.length+" matches" );
							for( int j = 0; j < itms.length; ++j ) {
							log.fine( "processing ["+i+"]: "+itms[i] );
								processItem( itms[j] );
							}
						} catch( RemoteException ex ) {
							logException( "processItem: "+regs[i], ex );
						} catch( RuntimeException ex ) {
							logException( "processItem: "+regs[i], ex );
						}
					}
				}
			}
		);
		Object lock = new Object();
		synchronized( lock ) {
			log.finest( "Waiting for application termination" );
			lock.wait();
		}
		// Should never get here!
		log.finest( "Application terminating" );
	}

	protected void processItem( ServiceItem item ) {
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
				log.finer( "Opening role or: "+name+", role: "+((UIDescriptor)ent[i]).role );
				try {
					openRole( (UIDescriptor)ent[i], svc, name );
				} catch( Exception ex ) {
					log.throwing( getClass().getName(), "processItem", ex );
				}
			}
		}
	}

	/**
	 *  Open the found serviceUI, MainUI.ROLE.
	 */
	protected void openRole( UIDescriptor uid, Object svc, String name ) {
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
		
		// Favor the existing JFrameFactory
		if( fobj instanceof JFrameFactory ) {
				log.finer( "Opening using JFrame factory for: "+name+", role: "+uid.role );
			JFrame f = ((JFrameFactory)fobj).getJFrame(svc);
			f.setSize( 800, 400 );
			f.setVisible(true);
			f.addWindowListener( new WindowAdapter() {
				public void windowClosing( WindowEvent ev ) {
					System.exit(1);
				}
			});
		} else if( fobj instanceof JDialogFactory ) {
				log.finer( "Opening using JFrame factory for: "+name+", role: "+uid.role );
			JDialog f = ((JDialogFactory)fobj).getJDialog(svc);
			f.setSize( 800, 400 );
			f.setVisible(true);
			f.addWindowListener( new WindowAdapter() {
				public void windowClosing( WindowEvent ev ) {
					System.exit(1);
				}
			});
		} else if( fobj instanceof JComponentFactory ) {
				log.finer( "Opening using JComponent factory for: "+name+", role: "+uid.role );
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
