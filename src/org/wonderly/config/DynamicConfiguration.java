package org.wonderly.config;

import net.jini.config.*;
import java.util.*;
import java.io.*;
import java.beans.*;

/**
 *  Uses a ConfigurationFile, or the passed Configuration as
 *  the readonly source of initial values and then provides
 *  a text based configuration extensibility using
 *  ConfigurationFile to parse passed configuration data.
 *
 *  @author Gregg Wonderly - gregg@wonderly.org
 */
public class DynamicConfiguration 
		implements Configuration,RemotelyModifiableConfigurationFile {
	/** The property change listeners */
	private Vector<PropertyChangeListener> listeners =
		new Vector<PropertyChangeListener>();
	/** The configuration Strings for overrides */
	private Hashtable<String,String> cfgs =
		new Hashtable<String,String>();
	/** The initial configuration */
	private Configuration cfg;
	/** Assocatiated class loader */
	private ClassLoader cl;
	/** The command line overrides */
	private String[]options;
	/** The overrides of values */
	private Hashtable<String,Object> overs =
		new Hashtable<String,Object>();

	/**
	 *  Provide a dynamicly updateable configuration using
	 *  the passed Configuration as the readonly initial
	 *  values.
	 */
	public DynamicConfiguration( Configuration cfg )
			throws ConfigurationException {
		this.cfg = cfg;
	}

	public DynamicConfiguration( String[] options )
			throws ConfigurationException {
		cfg = new ConfigurationFile( options );
		this.options = options;
	}

	public DynamicConfiguration( String[] options,
			ClassLoader cl ) throws ConfigurationException {
		cfg = new ConfigurationFile( options, cl );
		this.options = options;
		this.cl = cl;
	}

	public DynamicConfiguration( Reader reader,
			String[] options )throws ConfigurationException {
		cfg = new ConfigurationFile( reader, options );
		this.options = options;
	}

	public DynamicConfiguration( Reader reader,
			String[] options, ClassLoader cl ) 
				throws ConfigurationException {
		cfg = new ConfigurationFile( reader, options, cl );
		this.options = options;
		this.cl = cl;
	}

	public void addPropertyChangeListener( PropertyChangeListener lis ) {
		listeners.add( lis );
	}

	/**
	 *  @param comp the Configuration component
	 *  @param name the name of the configuration entry
	 *  @param val the value of the entry
	 *  @param cfgText a string to use for help telling the user 
	 *         about problems with the entry
	 */
	public void setEntry( String comp, String name, 
			Object val, String cfgText ) throws ConfigurationException {
		String ename = comp+"."+name;
		// get the old entry
		Object ent = cfg.getEntry( comp, name, val.getClass(), val );
		if( ent != val ) {
			// Cross check type to make sure its not changing
			if( val.getClass().equals( ent.getClass() ) == false ) {
				throw new ClassCastException( val.getClass().getName()+
					" <> "+ent.getClass().getName()+": "+cfgText );
			}
		}
		Object old = overs.get(ent);

		cfgs.put( ename, cfgText );
		overs.put( ename, val );

		PropertyChangeEvent pe = new PropertyChangeEvent(
			comp, name,	old != null ? old : ent, val );

		Enumeration<PropertyChangeListener>e = listeners.elements();
		while( e.hasMoreElements() ) {
			PropertyChangeListener pcl = e.nextElement();
			pcl.propertyChange( pe );
		}
	}

	public Object getEntry( String comp, String name,
				Class type, Object defVal,
					Object data ) throws ConfigurationException {
		Object o = overs.get( comp + "." + name );
		if( o == null )
			return cfg.getEntry( comp, name, type, defVal, data );
		return o;
	}

	public Object getEntry( String comp, String name, 
				Class type, Object defVal )
					throws ConfigurationException {
		Object o = overs.get( comp + "." + name );
		if( o == null )
			return cfg.getEntry( comp, name, type, defVal );
		return o;
	}

	public Object getEntry( String comp, 
				String name, Class type )
					throws ConfigurationException {
		Object o = overs.get( comp + "." + name );
		if( o == null )
			return cfg.getEntry( comp, name, type  );
		return o;
	}
}