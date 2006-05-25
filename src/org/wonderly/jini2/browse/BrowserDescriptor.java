package org.wonderly.jini2.browse;

import net.jini.lookup.ui.factory.*;
import net.jini.lookup.ui.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import java.io.*;
import java.rmi.*;
import java.util.*;
import org.wonderly.jini2.browse.BrowserFactory;

/**
 *  This class provides the UIDescriptor for accessing the JiniBrowse
 *  class from BrowserFactory.
 *
 *  @see BrowserFactory
 *  @see JiniBrowse
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class BrowserDescriptor extends UIDescriptor {
	/**
	 *   Creates a JFrameFactory descriptor for the AdminUI.ROLE.
	 */
	public BrowserDescriptor() throws IOException {
		this( MainUI.ROLE );
	}

	/**
	 *   Creates a JFrameFactory descriptor for the specified role.
	 */
	public BrowserDescriptor( String role) throws IOException {
		this( role, new HashSet() );
	}

	/**
	 *   Creates a JFrameFactory descriptor for the specified role.
	 */
	public BrowserDescriptor( String role, HashSet set) throws IOException {
		super( role, JFrameFactory.TOOLKIT, set, 
			new MarshalledObject( new BrowserFactory() ) );
	}
}
