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
 *  This is a convenience class that will create a complete administrative
 *  component for a Jini service that implements <code>Administrable</code>.
 *  This class will create instances of the following classes to provide
 *  the GUI components for managing the associated parts of the service.
 *  <ul> 
 *  <li>AttributesAdminUI - if JoinAdmin is implemented
 *  <li>JoinGroupsAdminUI - if JoinAdmin is implemented
 *  <li>LookupLocatorsAdminUI - if JoinAdmin is implemented
 *  <li>DiscoveryAdminUI - if DiscoveryAdmin is implemented
 *  <li>DestroyAdminUI - if DestroyAdmin is implemented
 *  <li>StorageLocationAdminUI - if StorageLocationAdmin is implemented
 *  </ul> 
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class AdminUIManager extends JPanel {
	/** The top level component as returned by
	 *  getRootComponent().  By default,this will be
	 *  a JTabbedPane.  Subclass can override getRootComponent()
	 *  and return something else that will be passed as the first
	 *  argument to addItem().
	 *  @see #addItem( String,JComponent )
	 */
	protected JComponent root = new JTabbedPane();
	
	/**
	 *  Should return the desired top level component that
	 *  will embed all contained AdminUI components.
	 */
	protected JComponent getRootComponent() {
		return root;
	}
	/**
	 *  Construct the whole AdminUI.
	 */
	public AdminUIManager( Administrable object ) throws RemoteException {
		buildComponentPane( object );
	}
	/**
	 *  Can be overridden to completely replace the default
	 *  AdminUI construction.  This should be done with
	 *  great care. If you choose to completely ignore this
	 *  default implementation, you will not get any new
	 *  Admin interfaces for free.  You will have to implement
	 *  updates to your code to add support for them.
	 */
	protected void buildComponentPane( Administrable object ) throws RemoteException {
		Object svc = object.getAdmin();
		
		Packer packer = new Packer( this );
		JComponent tp = getRootComponent();
		packer.pack( tp ).fillboth();

		// Add the JoinAdmin specific UIs
		if( svc instanceof JoinAdmin ) {
			addItem( "Attributes",
				new AttributesAdminUI( (JoinAdmin)svc ) );
			addItem( "Join Groups",
				new JoinGroupsAdminUI( (JoinAdmin)svc ) );
			addItem( "Lookup Locators",
				new LookupLocatorsAdminUI( (JoinAdmin)svc ) );
		}
		// Add the DiscoveryAdmin specific UIs
		if( svc instanceof DiscoveryAdmin ) {
			addItem( "Discovery Groups",
				new DiscoveryGroupsAdminUI( (DiscoveryAdmin)svc ) );
		}
		// Add the misc panel for DestroyAdmin and StorageLocationAdmin
		if( svc instanceof DestroyAdmin ||
				svc instanceof StorageLocationAdmin ) {
			JPanel p = new JPanel();
			Packer pk = new Packer(p);
			addItem( "Misc", p );
			int y = -1;
			// Storage location on left
			if( svc instanceof StorageLocationAdmin ) {
				pk.pack( new StorageLocationAdminUI(
					(StorageLocationAdmin)svc )
					).gridx(0).gridy(++y).fillx();
			}
			// Destroy admin button on right, or in middle.
			if( svc instanceof DestroyAdmin ) {
				pk.pack( new DestroyAdminUI(
					(DestroyAdmin)svc )
					).gridx(1).gridy(y);
			}
			// Push remaining components to top!
			pk.pack( new JPanel() ).gridx(0).gridy(++y).filly();
		}
	}
	/**
	 *  Called to add the indicated AdminUI component to the
	 *  collection of components for the <code>root</code>
	 *  component.  Subclasses can recognize the titles passed,
	 *  and replace the titles, or handle the specially to
	 *  reorganize the layout of the controls from the default
	 *  layout.  Subclasses should make note that new Admin
	 *  interfaces may appear and this method will need to make
	 *  sure that it handles those gracefully,
	 */
	protected void addItem( String title, JComponent comp ) {
		root.add( title, comp );
	}
}
