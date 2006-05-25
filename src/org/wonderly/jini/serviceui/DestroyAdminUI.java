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
 *  Provides a UI Component for DestroyAdmin implementations.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class DestroyAdminUI extends UIJPanel {
	JButton destroy;

	public DestroyAdminUI( final DestroyAdmin serviceInst ) {
		setBorder( BorderFactory.createTitledBorder( "Destroy Admin" ) );
		Packer dpk = new Packer( this );
		destroy = new JButton( "Destroy Service" );
		dpk.pack( destroy ).gridx(0).gridy(0).west();

		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if( ev.getSource() == destroy) {
					int idx = JOptionPane.showConfirmDialog( getTopLevelAncestor(),
						"Really stop this service?",
						"Service Stop Confirm",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE );
					if( idx == JOptionPane.OK_OPTION ) {
						runRemoteAction( destroy, new RemoteAction() {
							public Object construct() throws Exception {
								serviceInst.destroy();
								return null;
							}
						});
					}
				}
			}
		};
		destroy.addActionListener( lis );
	}
}