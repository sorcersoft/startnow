package org.wonderly.jini.serviceui;

import net.jini.lookup.ui.factory.*;
import java.rmi.*;
import java.rmi.server.*;
import java.beans.*;
import java.util.*;
import java.lang.reflect.*;
import net.jini.admin.*;

/**
 *  This interface defines the methods needed to get, set and describe the set of
 *  properties that a service wants to export for administration.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public interface PropertiesAccess extends Remote {
	PropertyDescriptorImpl[]descriptors() throws RemoteException,IntrospectionException;
	Properties currentValues() throws RemoteException,InvocationTargetException,IllegalAccessException,IntrospectionException;
	Properties defaultValues() throws RemoteException,IntrospectionException;
	void setProperties( Properties props ) throws RemoteException,PropertyVetoException,InvocationTargetException,IllegalAccessException,IntrospectionException;
}