package org.wonderly.jini.serviceui;

import net.jini.lookup.ui.factory.*;
import net.jini.lookup.ui.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import java.io.*;
import java.rmi.*;
import java.util.*;

/**
 *  This class provides the UIDescriptor for getting a complete
 *  AdminUI that includes recognition of all
 *  {@link org.wonderly.util.jini.JiniAdmins} and PropertiesAccess
 *  implementations.
 *
 *  @see PropertiesAccess
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class AdminDescriptor extends UIDescriptor {
	/**
	 *   Creates a JFrameFactory descriptor for the AdminUI.ROLE.
	 */
	public AdminDescriptor() throws IOException {
		this( AdminUI.ROLE );
	}

	/**
	 *   Creates a JFrameFactory descriptor for the specified role.
	 */
	public AdminDescriptor( String role) throws IOException {
		this( role, new HashSet() );
	}

	/**
	 *   Creates a JFrameFactory descriptor for the specified role.
	 */
	public AdminDescriptor( String role, HashSet set) throws IOException {
		super( role, JFrameFactory.TOOLKIT, set, 
			new MarshalledObject( new AdminUIJFrameFactory() ) );
	}
}