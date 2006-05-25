package org.wonderly.jini.serviceui;

import java.util.Properties;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;
import java.lang.reflect.*;

/**
 *  This class provides a level of indirection for property value management.
 *  The introspection code in PropertyAdminDelegate, looks for the passed
 *  object to implement this interface, and will then call these methods,
 *  instead of doing its own introspection.  This allows the set of properties
 *  to be dynamically controlled, and have no tie to methods at all, let alone
 *  being implemented in a single object.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public interface PropertyValueManager {
	/** 
	 *  Get the descriptors for this objects properties.
	 */
	public PropertyDescriptorImpl []propertyDescriptors() throws IntrospectionException,NoSuchMethodException;
	/**
	 *  Gets the name-value pairs of all properties.
	 */
	public Properties propertyValues() throws IntrospectionException, InvocationTargetException,IllegalAccessException;
	/**
	 *  Gets the name-value pairs of all properties.
	 */
	public Properties propertyDefaults() throws IntrospectionException, InvocationTargetException,IllegalAccessException;
	/**
	 *  Called to set zero or more property values using the
	 *  passed Properties object to carry those name-value
	 *  pairs.
	 */
	public void putPropertyValues( Properties props );
}
