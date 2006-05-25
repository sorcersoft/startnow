package org.wonderly.jini2.browse;

import java.awt.event.*;
import javax.help.*;
import java.net.*;
import javax.swing.*;
import org.wonderly.swing.*;
import java.util.*;
import java.util.logging.*;

/**
 *  Display help for EOI Desktop applications.
 *  <p>
 *  There is a logger under the class name that will log all Map.ID values under FINE.
 */
public class DesktopHelp extends LabeledAction {
	/** The action listener to invoke when action requested */
	private ActionListener lis;
	private String helpHS = "desktop.hs";
	private HelpSet hs;
	private HelpBroker hb;
	private String id;
	Logger log = Logger.getLogger( getClass().getName() );
	volatile boolean isPrep;
	
	public DesktopHelp( String name, String helpset, String id ) 
			throws MalformedURLException, HelpSetException {
		super(name);
		this.id = id;
		helpHS = helpset;
	}

	public DesktopHelp( String name, String id ) 
			throws MalformedURLException, HelpSetException {
		super( name );
		this.id = id;
	}
		
	private synchronized void prep() 
			throws MalformedURLException, HelpSetException {

		ClassLoader cl = getClass().getClassLoader();
		String hd = System.getProperty("user.dir");
		if( hd != null && hd.length() > 0 && hd.charAt(0) != '/' && hd.charAt(0) != '\\' )
			hd = "/"+hd.replace('\\','/');
		hs = new HelpSet( cl,
			new URL("file:"+hd+
				"/docs/help/"+helpHS));

		// Log all available help IDs to help user configure one that works.
		Enumeration e = hs.getLocalMap().getAllIDs();
		while( e.hasMoreElements() ) {
			log.fine("Help ID: "+e.nextElement() );
		}

		// Create a HelpBroker object:
		hb = hs.createHelpBroker();			
		lis = new CSH.DisplayHelpFromSource( hb );
		isPrep = true;
	}
	
	public void actionPerformed( ActionEvent ev ) {
		synchronized(this) {
			if( !isPrep ) {
				try {
					prep();
				} catch( Exception ex ) { 
					log.log( Level.SEVERE, ex.toString(), ex );
				}
			}
		}

		lis.actionPerformed(ev);
		if( id != null )
			hb.setCurrentID( id );
	}
}