package org.wonderly.jini.serviceui;

import org.wonderly.awt.*;
import org.wonderly.swing.*;
import java.lang.reflect.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.rmi.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.*;

import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.entry.*;
import net.jini.admin.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.lookup.ui.factory.*;
import com.sun.jini.admin.*;
import org.wonderly.swing.*;
import java.util.logging.*;
import java.net.UnknownHostException;
import org.wonderly.util.jini2.RemoteActionThread;

/**
 *  Provides a UI Component for JoinAdmin implementations to manage
 *  the LookupLocator instances configured for the service.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class LookupLocatorsAdminUI extends UIJPanel {
	JList lList;
	VectorListModel<String> lMod;
	JButton lAdd, lRmv;
	String curLocator;
	Logger log = Logger.getLogger( getClass().getName() );

	public LookupLocatorsAdminUI( final JoinAdmin serviceInst ) {
		
		Packer lpk = new Packer( this );
		lpk.pack( new JScrollPane( lList = new JList( 
			lMod = new VectorListModel<String>() ) 
			) ).fillboth().gridh(3).gridx(0).gridy(0);
		lList.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent ev ) {
				curLocator = (String)lList.getSelectedValue();
				lRmv.setEnabled( curLocator != null );
			}
		});
		lList.setToolTipText( "No Lookup Locators are set" );
		lpk.pack( lAdd = new JButton( "Add" ) ).gridx(1).fillx().weightx(0).gridy(0).inset(2,2,2,2);
		lpk.pack( lRmv = new JButton( "Remove" ) ).gridx(1).fillx().weightx(0).gridy(1).inset(2,2,2,2);
		lpk.pack( new JPanel() ).gridx(1).filly().gridy(2);
		lRmv.setEnabled(false);

		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if( ev.getSource() == lRmv) {
					final String str = (String)lList.getSelectedValue();
					int idx = JOptionPane.showConfirmDialog( getTopLevelAncestor(), 
						"Really Remove Lookup Locator \""+str+"\"?", 
						"Locator Removal Confirmation",
						JOptionPane.OK_CANCEL_OPTION );
					if( idx == JOptionPane.CANCEL_OPTION )
						return;

					idx = str.indexOf( ':' );
					int port = 0;
					String host = str;
					if( idx >= 0 ) {
						host = str.substring(0,idx);
						port = Integer.parseInt( str.substring( idx+1 ) );
					}
					final String fhost = host;
					final int fport = port;
					log.fine("remove locator is \""+fhost+"\", port: "+fport );
					new RemoteActionThread<Object>( lRmv) {
						public Object construct() throws Exception {
							serviceInst.removeLookupLocators( 
								new LookupLocator[]{ new LookupLocator( fhost, fport ) }
								);
							loadLookups( serviceInst );
							return null;
						}
					}.start();
				} else if( ev.getSource() == lAdd) {
					final String str = JOptionPane.showInputDialog( getTopLevelAncestor(),
						"New Locator (jini://host:port):", 
						"Add Unicast Locator",
							JOptionPane.OK_CANCEL_OPTION );
					if( str == null )
						return;
					if( lMod.contains(str) == true ) {
//						lMod.addElement( str );
//					} else {
						JOptionPane.showMessageDialog( getTopLevelAncestor(), 
							"Locator already specified:\n"+str,
							"Duplicate Locator", 
							JOptionPane.ERROR_MESSAGE );
						return;
					}
//					int idx = str.indexOf( ':' );
//					int port = 0;
//					String host = str;
//					if( idx >= 0 ) {
//						host = str.substring(0,idx);
//						port = Integer.parseInt( str.substring( idx+1 ) );
//					}
//					final String fhost = host;
//					final int fport = port;

					new RemoteActionThread<LookupLocator>( lAdd ) {
						public LookupLocator construct() throws Exception {
							final LookupLocator loc = new LookupLocator( str );
							log.fine("Adding Locator: "+loc );
							try {
								serviceInst.addLookupLocators(
									new LookupLocator[] { loc } );
							} catch( RemoteException ex ) {
								log.log( Level.SEVERE, ex.toString(), ex );
								throw ex;
							}
							return loc;
						}
						public void finished( final LookupLocator loc ) {
							lMod.addElement( loc.getHost()+":"+loc.getPort() );
						}
					}.start();
				}
			}
		};
		lAdd.addActionListener(lis);
		lRmv.addActionListener(lis);
		configForService( serviceInst );
	}
 	
	public void configForService( JoinAdmin adm ) {
		enableAll( true );
		try {
			loadLookups( adm );		
			// Just consider the first ServiceItem found
		} catch( Exception ex ) {
			reportException( ex );
		}
	}
	
	void enableAll( boolean how ) {
		lList.setEnabled( how );
		lAdd.setEnabled( how );
		lRmv.setEnabled( false );
	}

	private void loadLookups( final JoinAdmin adm ) throws RemoteException {
		new RemoteActionThread<LookupLocator[]>( lList, lAdd, lRmv ) {
			public void setup() {
				lMod.removeAllElements();
			}
			public LookupLocator[] construct() {
				try {
					return adm.getLookupLocators();
				} catch( Exception ex ) {
					reportException( ex );
				}
				return null;
			}
			public void finished(LookupLocator[]lArr) {
				try {
					if( lArr == null )
						return;
					if( lArr.length > 0 )
						lList.setToolTipText( "List of Configured Locators" );
					for( int i = 0; i < lArr.length; ++i ) {
						String loc = lArr[i].getHost()+":"+lArr[i].getPort();
						log.fine("found loc: "+lArr[i] );
						if( lMod.contains( loc ) == false ) {
							lMod.addElement( loc );
						}
					}
				} finally {
					lRmv.setEnabled( curLocator != null );
				}
			}
		}.start();
	}
}