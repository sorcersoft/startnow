package org.wonderly.jini2.demo;

import org.wonderly.jini2.PersistentJiniService;
import org.wonderly.jini.serviceui.RemoteAdministrable;

/**
 *  This is the simplest service possible with the
 *  startnow project classes.  The PersistentJiniService
 *  class does all the dirty work for you by subclassing
 *  ConfigurableJiniApplication, and delegating the
 *  net.jini.admin.* interfaces to an instance of the
 *  JiniAdmin class.
 *
 *  @see org.wonderly.util.jini2.JiniAdmin
 */
public class SimpleService 
		extends PersistentJiniService 
		implements RemoteAdministrable {
			
	/**
	 *  Entry point as an application.
	 */
	public static void main( String args[] ) throws Exception {
		new SimpleService( args, "simple.ser" );
	}

	/**
	 *  Construct the object
	 *  @param args the arguments for Configuration intialization
	 *  @param persistStore the name of the file to use for
	 *         serialization of our persistent state.
	 */
	public SimpleService(String []args, String persistStore ) throws Exception {
		super(args);
		startService( "Simple Service", persistStore );
	}
}