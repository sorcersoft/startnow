package org.wonderly.jini2.config;

import net.jini.config.*;
import java.io.*;
import java.util.*;

/**
 *  This class is used to store a configuration that is passed
 *  between the configuration service and the client service
 */
public class ConfigurationSet 
		implements Configuration,Serializable {

	protected String name;
	protected String cfgContents;
	protected ConfigurableId id;
	/**
	 *  Eventually this class should be abstract, and the
	 *  getConfiguration method would be implemented by a
	 *  ConfigurationFileSet sub class.
	 */
	protected transient ConfigurationFile cf;
	private static final long serialVersionUID = 1;

	public ConfigurableId getConfigurableId() {
		return id;
	}
	public String getConfigContents() {
		return cfgContents;
	}
	public String getName() {
		return name;
	}
	public boolean equals( Object obj ) {
		if( obj instanceof ConfigurationSet == false )
			return false;
		return id.equals( ((ConfigurationSet)obj).id );
	}
	public String toString() {
		return name+": "+id;
	}

	public void writePersist( OutputStream os ) throws IOException {
		os.write( getConfigContents().getBytes() );
	}

	/**
	 *  @param name the name of this configuration set, e.g.
	 *  com.wonderly.jini2.JiniDesktop
	 *  @param cfgContent a ConfigurationFile compatible
	 *   configuration file content
	 */
	public ConfigurationSet( String name, 
			ConfigurableId id, String cfgContent ) {
		if( id == null )
			throw new IllegalArgumentException( 
				"ConfigurableId can not be null" );
		this.name = name;
		this.id = id;
		this.cfgContents = cfgContent;
	}
	
	public void setContents( String str ) {
		cfgContents = str;
	}
	
	public ConfigurationSet( String name, ConfigurableId id, 
			InputStream is, long size ) throws IOException {
		this.name = name;
		this.id = id;
		byte[]arr = new byte[ (int)size ];
		DataInputStream ds = new DataInputStream( is );
		ds.readFully( arr );
		this.cfgContents = new String( arr );
	}

	/**
	 *  @return a ConfigurationFile instance created once using the
	 *  parameters that the instance is constructed with.
	 *  @throws ConfigurationException if there is an error creating
	 *  the ConfigurationFile instance.
	 */
	protected Configuration getConfiguration(String[] data)
			throws ConfigurationException {
		if( cf == null ) {
			cf = new ConfigurationFile( 
				new StringReader( cfgContents ), data );
		}
		return cf;
	}

	public Object getEntry( String name, String entry, Class type,
			Object def, Object data ) throws ConfigurationException {
		Configuration cf = getConfiguration(null);
		return cf.getEntry( name, entry, type, def, data );
	}

	public Object getEntry( String name, String entry, Class type,
		Object def) throws ConfigurationException {
		Configuration cf = getConfiguration(null);
		return cf.getEntry( name, entry, type, def );
	}
	public Object getEntry( String name, String entry, Class type )
			throws ConfigurationException {
		Configuration cf = getConfiguration(null);
		return cf.getEntry( name, entry, type );
	}
}