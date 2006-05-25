package org.wonderly.jini.serviceui;

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
public abstract class PropertyAdminDelegate {

	public abstract void debugln( String msg );
	public abstract void println( String msg );
	public abstract void reportException( Throwable ex );

	public PropertyDescriptorImpl[]descriptors( Object svc ) throws IntrospectionException,RemoteException {
		if( svc instanceof PropertyValueManager ) {
			try {
				return ((PropertyValueManager)svc).propertyDescriptors();
			} catch( NoSuchMethodException ex ) {
				reportException(ex);
			} catch( IntrospectionException ex ) {
				reportException(ex);
			}
		}
		BeanInfo inf = Introspector.getBeanInfo( svc.getClass() );
		PropertyDescriptor[]d = inf.getPropertyDescriptors();
		Vector<PropertyDescriptorImpl> v = new Vector<PropertyDescriptorImpl>();
		for( int i = 0; i < d.length; ++i ) {
			if( isProperty( d[i].getName() ) ) {
				v.addElement(new PropertyDescriptorImpl( d[i] ));
			}
		}
		PropertyDescriptorImpl pd[] = new PropertyDescriptorImpl[v.size()];
		v.copyInto( pd );
		return pd;
	}
	
	/** Default property value filter.  Will return false for 
	 *  known properties.
	 *  <ul>
	 *  <li>class
	 *  <li>properties
	 *  <li>exportedObject
	 *  <li>objFile
	 *  <li>admin
	 *  </ul>
	 */
	public boolean isProperty( String name ) {
		/** Filter the things that aren't really properties */
		if( name.equals("class") )
			return false;
		if( name.equals("properties") )
			return false;
		if( name.equals("exportedObject") )
			return false;
		if( name.equals("objFile") )
			return false;
		if( name.equals("admin") )
			return false;			
		return true;
	}


	public Properties currentValues( Object svc ) throws RemoteException,IllegalAccessException,IntrospectionException,InvocationTargetException {
		Properties p = getPropertiesForObject( svc, false );
		
		return p;
	}

	public Properties defaultValues( Object svc ) throws RemoteException,IntrospectionException {
		return new Properties();
	}
	/** 
	 *  This method uses introspection to find the properties for the
	 *  passed object and turn them into a Properties object.
	 *
	 *  @param cls the object to get the properties for
	 *  @param defer true to defer to a PropertyValueManager implementation
	 *
	 *  @exception IntrospectionException if there are problems accessing the object
	 *  @exception InvocationTargetException if the property reader can not be invoked
	 *  @exception IllegalAccessException if the object is not accessible,
	 *
	 *  @see #setViaReflection
	 */
	public Properties getPropertiesForObject( Object cls, boolean defer )
    		throws IntrospectionException,
    			RemoteException,
    			InvocationTargetException, 
    			IllegalAccessException {
    
    	if( defer && cls instanceof PropertyValueManager ) {
    		return ((PropertyValueManager)cls).propertyValues();
    	}

    	Properties props = new Properties();
    
//        PropertyDescriptor pds[] = getDescriptors(cls);
		BeanInfo inf = Introspector.getBeanInfo( cls.getClass() );
		PropertyDescriptor[]pds = inf.getPropertyDescriptors();
        if( pds == null ) {
        	// no properties from beaninfo
        	return props;
        }

    	// Process all properties advertised via beaninfo
   		for( int i = 0; i < pds.length; ++i ) {
            PropertyDescriptor pd = pds[i];
           	String arg = pd.getName();
        	if( isProperty(arg) == false ) {
        		continue;
        	}

        	// Nothing to set, skip this property
            Class typ = pd.getPropertyType();
            Method m = pd.getReadMethod();
 
        	// No read method, try next property.
        	if( m == null ) {
        		continue;
        	}

//        	System.out.println(arg+": check exceptions: "+m);
        	Class excpts[] = m.getExceptionTypes();
        	for( int j = 0; j < excpts.length; ++j ) {
//        		System.out.println( arg+": Consider: "+excpts[j]+" vs "+RemovedPropertyException.class.getName() );
        		if( excpts[j].getName().equals(RemovedPropertyException.class.getName()) ) {
        			m = null;
        			break;
        		}
        	}

         	// No read method, try next property.
        	if( m == null ) {
//        		System.out.println("Skipping property: "+arg);
        		continue;
        	}

       		Object ret = null;
   			try {
   				ret = m.invoke( cls, (Object[])null );
   			} catch( InvocationTargetException ex ) {
   				if( ex.getTargetException() instanceof RemovedPropertyException == false )
   					reportException(ex.getTargetException());
   			} catch( Throwable ex ) {
   				reportException(ex);
   			}
 
        	// May return null if read is not possible
        	if( ret == null )
        		continue;
        	props.put( arg, ret.toString() );
   		}
    	return props;
	}
	public void setProperties( Object svc, Properties props ) 
		throws IntrospectionException, 
    			PropertyVetoException, 
    			InvocationTargetException, 
    			IllegalAccessException {
		setViaReflection( svc, props );
	}

	/**
	 *  This method takes the passed object and a set of properties and
	 *  uses reflection to set the property values for the object to the
	 *  values inside the property object.
	 *
	 *  @param cls an object to set properties on
	 *  @param props the property values to set on cls.
	 *
	 *  @exception IntrospectionException if an error occurs during introspection.
	 *  @exception PropertyVetoException if a boolean property value is not one of
	 *	           (0,1,"yes","no","true","false")
	 *  @exception InvocationTargetException if there is a problem invoking the
	 *             property setting method.
	 *  @exception IllegalAccessException if the property setting method can not
	 *             be accessed.
	 *
	 *  @see #getPropertiesForObject
	 */
    public void setViaReflection( Object cls, Properties props ) 
    		throws IntrospectionException, 
    			PropertyVetoException, 
    			InvocationTargetException, 
    			IllegalAccessException {   	
    	if( cls instanceof PropertyValueManager ) {
    		println("PropertyValueManager: "+cls+", props: "+props );
    		((PropertyValueManager)cls).putPropertyValues( props );
    		return;
    	}

    	BeanInfo inf = Introspector.getBeanInfo( cls.getClass() );
        PropertyDescriptor pds[] = inf.getPropertyDescriptors();
        if( pds == null ) {
        	return;
        }
        String name = cls.toString();
        debugln( name+": set properties" );
        for( int i = 0; i < pds.length; ++i ) {
            PropertyDescriptor pd = pds[i];
           	String arg = props.getProperty(pd.getName());
            // Nothing to set, skip this property
            if( arg == null ) {
            	continue;
            }
            // don't let through non-exposed properties
            if( isProperty(arg) == false )
            	continue;

            Class typ = pd.getPropertyType();
            Method m = pd.getWriteMethod();
        	
        	// Check if this property is writable.
        	if( m == null ) {
        		// No write method, do not update.
        		continue;
        	}
            Object args[] = new Object[1];
            if( typ == String.class ) {
            	args[0] = arg;
        	} else if( typ == Integer.class || typ == int.class ) {
            	args[0] = new Integer( arg );
        	} else if( typ == Long.class || typ == long.class ) {
            	args[0] = new Long( arg );
            } else if( typ == Boolean.class || typ == boolean.class ) {
				if( arg.equals("true") || arg.equals("yes") || arg.equals("1") ) {
                	args[0] = new Boolean(true);
                } else if (arg.equals("false") || arg.equals( "no" ) || arg.equals( "0" ) ) {
                	args[0] = new Boolean(false);
                } else {
                	reportException( new PropertyVetoException( cls+" Invalid boolean value for "+pd.getName()+" property",
                    	new PropertyChangeEvent(cls, pd.getName(), null, arg ) ) );
                    continue;
                }
            } else if( typ == Float.class || typ == float.class ) {
            	args[0] = new Float( arg );
            } else if( typ == Double.class || typ == double.class ) {
            	args[0] = new Double( arg );
            } else {
            	println( "   "+cls+": property value type of "+typ+", not supported" );
            	continue;
            }
        	try {
        		m.invoke( cls, args );
        	} catch( InvocationTargetException ex ) {
        		reportException(ex.getTargetException());
        	} catch( Exception ex ) {
        		reportException( ex);
        	}
   		}
	}

}
