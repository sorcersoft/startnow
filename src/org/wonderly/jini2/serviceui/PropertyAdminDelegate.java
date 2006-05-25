package org.wonderly.jini2.serviceui;

import net.jini.lookup.ui.factory.*;
import javax.swing.*;
import java.beans.*;
import java.util.*;
import org.wonderly.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.lang.reflect.*;
import org.wonderly.jini.serviceui.PropertyDescriptorImpl;
import org.wonderly.jini.serviceui.PropertyValueManager;
import org.wonderly.jini.serviceui.RemovedPropertyException;

/**
 *  Property setting via remote method calls that use introspection would require
 *  the class hierachy of the existing service to be altered, without some form
 *  of delegation.  We have the methods for setting and clearing properties to be managed
 *  via a simple interface (PropertiesAccess).
 *  <p>
 *  This class should be subclassed, and its abstract methods implemented to provide
 *  debugging and logging facilities specific to the application.
 *	<p>
 *  The service should implement PropertiesAccess, and then delegate those calls
 *  to its implementatio of this class.  It should pass itself as the argument to
 *  the calls, or the approriate object that provides the properties for the
 *  application.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public abstract class PropertyAdminDelegate extends org.wonderly.jini.serviceui.PropertyAdminDelegate {
	
	/** Default property value filter.  Will return false for 
	 *  known properties.
	 *  <ul>logger
	 *  <li>lookupEnvs
	 *  <li>name
	 *  <li>package
	 *  <li>proxyPreparer
	 *  <li>resourceBundle
	 *  <li>securityManager
	 *  <li>logLevel
	 *  <li>locators
	 *  <li>initialLogLevel
	 *  <li>initialEntrys
	 *  <li>groups
	 *  <li>exporter
	 *  <li>proxyTrustVerifier
	 *  </ul>
	 */
	public boolean isProperty( String name ) {
		/** Filter the things that aren't really properties */
		if( name.equals("logger") )
			return false;
		if( name.equals("lookupEnvs") )
			return false;
		if( name.equals("name") )
			return false;
		if( name.equals("package") )
			return false;
		if( name.equals("proxyPreparer") )
			return false;
		if( name.equals("resourceBundle") )
			return false;
		if( name.equals("securityManager") )
			return false;
		if( name.equals("logLevel") )
			return false;
		if( name.equals("locators") )
			return false;
		if( name.equals("initialLogLevel") )
			return false;
		if( name.equals("initialEntrys") )
			return false;
		if( name.equals("groups") )
			return false;
		if( name.equals("exporter") )
			return false;
		if( name.equals("proxyTrustVerifier") )
			return false;
		
		return super.isProperty( name );
	}
}
