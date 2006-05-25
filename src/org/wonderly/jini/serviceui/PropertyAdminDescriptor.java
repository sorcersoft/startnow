package org.wonderly.jini.serviceui;

import net.jini.lookup.ui.factory.*;
import net.jini.lookup.ui.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import java.io.*;
import java.rmi.*;
import java.util.*;

/**
 *  This class is the UIDescriptor definition that will provide only a properties
 *  based AdminUI.  An application can use this descriptor in concert with implementing
 *  PropertiesAccess to provide a complete Properties based administration UI.
 *
 *  @see PropertiesAccess
 *  @see PropertyValueManager
 *  @see PropertyAdminDelegate
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class PropertyAdminDescriptor extends UIDescriptor {
	/**
	 *   Creates a JFrameFactory descriptor for the AdminUI.ROLE.
	 */
	public PropertyAdminDescriptor() throws IOException {
		this( AdminUI.ROLE );
	}

	/**
	 *   Creates a JFrameFactory descriptor for the specified role.
	 */
	public PropertyAdminDescriptor( String role ) throws IOException {
		this( role, new HashSet() );
	}

	/**
	 *   Creates a JFrameFactory descriptor for the specified role.
	 */
	public PropertyAdminDescriptor( String role, HashSet set ) throws IOException {
		super( role, JFrameFactory.TOOLKIT, set, 
			new MarshalledObject( new PropertyAdminUIJFrameFactory() ) );
	}
}