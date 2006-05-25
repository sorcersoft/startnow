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

import java.net.UnknownHostException;

/**
 *  Provides a UI Component for DiscoveryAdmin implementations to manage
 *  the discovery groups configured for the service.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class DiscoveryGroupsAdminUI extends UIJPanel {
	private final JTextField uniport;
	private final JList dgList;
	private final VectorListModel<String> dgMod;
	private final JButton uniSet;
	private final JButton dgAdd, dgRmv;
	private String curDGroup;
	private String curGroup;

	public DiscoveryGroupsAdminUI( final DiscoveryAdmin serviceInst ) {
		Packer dgpk = new Packer( this );
		dgpk.pack( new JScrollPane( dgList = new JList(
			dgMod = new VectorListModel<String>() )
			) ).fillboth().gridh(3).gridx(0).gridy(0);
		dgList.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent ev ) {
				curDGroup = (String)dgList.getSelectedValue();
				dgRmv.setEnabled( curDGroup != null );
			}
		});
		dgList.setToolTipText( "No Discovery Groups are set" );
		dgpk.pack( dgAdd = new JButton( "Add" ) ).gridx(1).fillx().weightx(0).gridy(0).inset(2,2,2,2);
		dgpk.pack( dgRmv = new JButton( "Remove" ) ).gridx(1).fillx().weightx(0).gridy(1).inset(2,2,2,2);
		dgpk.pack( new JPanel() ).gridx(1).filly().gridy(2);
		dgRmv.setEnabled(false);
		dgpk.pack( new JSeparator() ).gridx(0).gridy(3).gridw(2).fillx().inset(3,3,3,3);
		JPanel up = new JPanel();
		up.setBorder( BorderFactory.createTitledBorder( "Unicast Port" ) );
		Packer upk = new Packer( up );
		uniport = new JTextField();
		uniSet = new JButton( "Set Unicast Port" );
		uniport.getDocument().addDocumentListener( new DocumentListener() {
			public void insertUpdate( DocumentEvent ev ) {
				check();
			}
			public void removeUpdate( DocumentEvent ev ) {
				check();
			}
			public void changedUpdate( DocumentEvent ev ) {
				check();
			}
			void check() {
				try {
					Integer.parseInt( uniport.getText() );
					uniSet.setEnabled(true);
					uniSet.setToolTipText( "Select to Configure Unicast Discovery Port" );
				} catch( NumberFormatException ex ) {
					uniSet.setEnabled(false);
					uniSet.setToolTipText( "Enter a Unicast Discovery Port" );
				}
			}
		});
		upk.pack( uniport ).gridx(0).gridy(0).fillx();
		upk.pack( uniSet ).gridx(1).gridy(0);
		dgpk.pack( up ).gridx(0).gridy(4).gridw(2).fillx();

		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if( ev.getSource() == dgRmv) {
					int idx = JOptionPane.showConfirmDialog( getTopLevelAncestor(), 
						"Really Remove Lookup Service from Group \""+curGroup+"\"?", 
						"Group Removal Confirmation",
						JOptionPane.OK_CANCEL_OPTION );
					if( idx == JOptionPane.CANCEL_OPTION )
						return;
					final String str = (String)dgList.getSelectedValue();
					runRemoteAction( dgRmv, new RemoteAction() {
						public Object construct() throws Exception {
							serviceInst.removeMemberGroups( new String[]{ str } );
							return null;
						}
						public void finished() {
							loadGroups( serviceInst );
						}
					});
				} else if( ev.getSource() == dgAdd) {
					final String str = JOptionPane.showInputDialog( getTopLevelAncestor(),
						"New Group Name:",
						"Add new Discovery Group",
						JOptionPane.OK_CANCEL_OPTION );
					if( dgMod.contains(str) == false ) {
						dgMod.addElement( str );
					} else {
						JOptionPane.showMessageDialog( getTopLevelAncestor(),
							"Group already specified:\n"+str, 
							"Duplicate Group",
							JOptionPane.ERROR_MESSAGE );
					}
					runRemoteAction( dgAdd, new RemoteAction() {
						public Object construct() throws Exception {
							serviceInst.addMemberGroups( new String[]{ str } );
							return null;
						}
					});
				} else if( ev.getSource() == uniSet) {
					runRemoteAction( uniSet, new RemoteAction() {
						public Object construct() throws Exception {
							serviceInst.setUnicastPort( Integer.parseInt( uniport.getText() ) );
							return null;
						}
					});
				}
			}
		};
		uniSet.addActionListener( lis );
		dgAdd.addActionListener(lis);
		dgRmv.addActionListener(lis);
		loadGroups(serviceInst);
	}


	public void configForService( DiscoveryAdmin svc ) throws RemoteException {
		dgAdd.setEnabled(true);
		dgList.setEnabled(true);
		loadGroups( svc );
		uniport.setText( svc.getUnicastPort()+"" );
		uniport.setEnabled(true);
		uniSet.setEnabled(false);
		dgAdd.setToolTipText( "Click to add a Discovery Group" );
		dgRmv.setToolTipText( "Select a Discovery Group to Delete First" );
		dgList.setToolTipText( "Configured Discovery Groups" );
		uniport.setToolTipText( "Configured Unicast Discovery Port" );
		uniSet.setToolTipText( "Select to Configure Unicast Discovery Port" );
	}
	
	private void enableAll( boolean how ) {
		dgList.setEnabled( how );
		dgAdd.setEnabled( how );
		dgRmv.setEnabled( false );
	}

	private void loadGroups( final DiscoveryAdmin adm ) {
		runRemoteAction( new JComponent[] { dgList, dgAdd, dgRmv },
		new RemoteAction() {
			public void setup() {
				dgMod.removeAllElements();
			}
			public Object construct() throws Exception {
				return adm.getMemberGroups();
			}
			public void finished(Object val) {
				String[]gArr = (String[])val;
				if( gArr == null )
					return;
				if( gArr.length > 0 )
					dgList.setToolTipText( "List of Configured Groups" );
				for( int i = 0; i < gArr.length; ++i ) {
					if( dgMod.contains( gArr[i] ) == false ) {
						dgMod.addElement( gArr[i] );
					}
				}
				dgList.repaint();
			}
			public void after() {
				if( curDGroup != null ) {
					dgRmv.setEnabled( true );
					dgRmv.setToolTipText( "Select to Remove the Selected Discovery Group" );
				} else {
					dgRmv.setEnabled( false );
					dgRmv.setToolTipText( "Select a Discovery Group to Delete First" );
				}
			}
		});
	}
}