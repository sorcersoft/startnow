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
 *  Provides a UI Component for the StorageLocationAdmin interface
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class StorageLocationAdminUI extends UIJPanel {
	JButton setStore;
	JTextField storeLoc;

	public StorageLocationAdminUI( final StorageLocationAdmin serviceInst ) {
		Packer adpk = new Packer( this );

		setStore = new JButton("Set");
		JPanel sp = new JPanel();
		Packer spk = new Packer( sp );
		sp.setBorder( BorderFactory.createTitledBorder( "Storage Location Admin") );
		spk.pack( storeLoc = new JTextField() ).gridx(0).gridy(0).fillx();
		spk.pack( setStore ).gridx(1).gridy(0);
		adpk.pack( sp ).gridx(0).gridy(0).fillx();

		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if( ev.getSource() == setStore) {
					runRemoteAction( setStore, new RemoteAction() {
						public Object construct() throws Exception {
							serviceInst.setStorageLocation( storeLoc.getText() );
							return null;
						}
					});
				}
			}
		};
		setStore.addActionListener( lis );
		runRemoteAction( storeLoc, new RemoteAction() {
			public Object construct() throws RemoteException {
				return serviceInst.getStorageLocation();
			}
			public void finished(Object obj) {
				String val = (String)obj;
				if( val != null )
					storeLoc.setText(val);
			}
		});
	}
}