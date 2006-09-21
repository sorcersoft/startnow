package org.wonderly.jini2.config.test;

import org.wonderly.jini2.config.*;
import org.wonderly.jini2.*;
import net.jini.config.*;
import java.io.*;

public class ConfigProviderTest
		extends PersistentJiniService 
		implements ConfigTestInterface {

	public void showConfig() {
		log.info( "config: "+conf );
	}

	public static void main( String args[] ) 
			throws IOException, ConfigurationException {
		System.out.println("Creating test object");
		if( ConfigProviderTest.class.getClassLoader().getResource("META-INF/services/net.jini.config.Configuration") == null ) {
			throw new NullPointerException("META-INF/services/net.jini.config.Configuration resource not found");
		}
		new ConfigProviderTest( args );
	}

	public ConfigProviderTest( String args[] )
			throws IOException, ConfigurationException {
		super( args );
		log.info("Starting service");
		startService( "ConfigTest", "cfgtest.ser" );
	}
}