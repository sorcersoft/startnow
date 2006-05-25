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
import org.wonderly.util.jini2.RemoteActionThread;

import java.net.UnknownHostException;

/**
 *  Provides a UI Component for JoinAdmin implementations to manage
 *  the join groups configured for the service.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class JoinGroupsAdminUI extends UIJPanel {
	JList gList;
	VectorListModel<String> gMod;
	JButton gAdd, gRmv;
	String curGroup;
	Logger log = Logger.getLogger( getClass().getName() );

	public JoinGroupsAdminUI( final JoinAdmin serviceInst ) {
		Packer gpk = new Packer( this );
		gMod = new VectorListModel<String>();
		gList = new JList( gMod );
		gpk.pack( new JScrollPane( gList ) ).fillboth().gridh(3).gridx(0).gridy(0);
		gList.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent ev ) {
				curGroup = (String)gList.getSelectedValue();
				gRmv.setEnabled( curGroup != null );
			}
		});
		gList.setToolTipText( "No Join Groups are set" );
		gpk.pack( gAdd = new JButton( "Add" ) ).gridx(1).fillx().weightx(0).gridy(0).inset(2,2,2,2);
		gpk.pack( gRmv = new JButton( "Remove" ) ).gridx(1).fillx().weightx(0).gridy(1).inset(2,2,2,2);
		gpk.pack( new JPanel() ).gridx(1).filly().gridy(2);
		gRmv.setEnabled(false);

		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if( ev.getSource() == gRmv) {
					int idx = JOptionPane.showConfirmDialog( 
						getTopLevelAncestor(),
						"Really Remove Service from Group \""+curGroup+"\"?",
						"Group Removal Confirmation", JOptionPane.OK_CANCEL_OPTION );
					if( idx == JOptionPane.CANCEL_OPTION )
						return;
					JoinAdmin adm = null;
					new RemoteActionThread<Object>( gRmv ) {
						public Object construct() throws Exception {
							try {
								serviceInst.removeLookupGroups( new String[]{ curGroup } );
							} catch( RemoteException ex ) {
								log.log(Level.SEVERE, ex.toString(), ex );
								throw ex;
							}
							return null;
						}
						public void finished(Object val) throws Exception {
							loadGroups( serviceInst );
						}
					}.start();
				} else if( ev.getSource() == gAdd) {
					final String str = JOptionPane.showInputDialog(
						getTopLevelAncestor(),
						"New Group Name:", 
						"Add new Join Group",
						JOptionPane.OK_CANCEL_OPTION );
					if( str == null ) {
						return;
					} else if( gMod.contains(str) == true ) {
						JOptionPane.showMessageDialog( 
							getTopLevelAncestor(),
							"Group already specified:\n"+str,
							"Duplicate Group",
							JOptionPane.ERROR_MESSAGE );
					}
					new RemoteActionThread<Object>( gAdd ) {
						public Object construct() throws Exception {
							try {
								serviceInst.addLookupGroups( new String[]{ str } );
							} catch( RemoteException ex ) {
								log.log(Level.SEVERE, ex.toString(), ex );
								throw ex;
							}
							return null;
						}
						public void finshed(Object val) {
							gMod.addElement( str );
						}
					}.start();
				}
			}
		};
		gAdd.addActionListener(lis);
		gRmv.addActionListener(lis);
	}
  	
	public void configForService( JoinAdmin adm ) throws RemoteException {
		enableAll( true );
		try {
			loadGroups( adm );			
		} catch( Exception ex ) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog( this, ex );
		}
	}
	
	private void enableAll( boolean how ) {
		gList.setEnabled( how );
		gAdd.setEnabled( how );
		gRmv.setEnabled( false );
	}

	private void loadGroups( final JoinAdmin adm ) throws RemoteException {
		new RemoteActionThread<String[]>( gList, gAdd, gRmv ) {
			public void setup() throws Exception {
				gMod.removeAllElements();
			}
			public String[] construct() throws Exception {
				return adm.getLookupGroups();
			}
			public void finished(String[] gArr) throws Exception {
				if( gArr == null )
					return;
				if( gArr.length > 0 ) {
					gList.setToolTipText( "List of Configured Groups" );
				} else {
					gList.setToolTipText( "No Join Groups are set" );
				}
				for( int i = 0; i < gArr.length; ++i ) {
					if( gMod.contains( gArr[i] ) == false ) {
						gMod.addElement( gArr[i] );
					}
				}
				gList.repaint();
			}
			public void after() {
				gRmv.setEnabled( curGroup != null );
			}
		}.start();
	}
}