package org.wonderly.util;

import java.io.*;
import java.lang.reflect.*;
import java.util.logging.*;

/**
 *  This class is implemented inside of the native service code
 *  associated with the "jservice" program.  This class can be
 *  used to call into the service routines for NT and thus manage
 *  the ability to log messages to the NT logging subsystem as well
 *  as tell NT that the service should stop
 *	<p>
 *  A logger is created using the full name of this class for
 *  logging information about the startup environment.  If 
 *  the system property, <b>jservice.outputstream</b> is set,
 *  it should be the name of a class that is an
 *  <code>java.io.OutputStream</code>.  System.out and
 *  System.err will be directed to this stream.  Otherwise
 *  other output to these streams will go through the
 *  NT logging mechanism.
 */
public class NTServices {
	/** Sends a message to the NT logging subsystem */
	public native void logMessage( int i, String msg );

	/** Asks the NT service subsystem to shut this service down. */
	public native void exit( int code );

	protected static Logger log = 
		Logger.getLogger( NTServices.class.getName());

	public static void logException( Throwable ex ) {
		log.log( Level.SEVERE, ex.toString(), ex );
	}

	public static void logException( Level level, Throwable ex ) {
		log.log( level, ex.toString(), ex );
	}

	/** Constructs an object to avoid static references if
	 *  desired.
	 */
	public NTServices() {
	}

	public String toString() {
		return "NTServices interface";
	}

	/**
	 *  Invoked by JService to launch the indicated class
	 *  as the service object with the passed arguments
	 *  @param className the name of the class to instantiate
	 *  @param args the set of command line arguments to pass
	 *   to main in the indicated className
	 */
	public static void service( String className,
					String args[] ) throws Exception {
		try {
			NTServices nt = new NTServices();

			// Create a stream to this OutputStream if requested.
			String fn = System.getProperty("jservice.outputstream");
			if( fn != null ) {
				try {
					Class c = Class.forName( fn );
					OutputStream os = (OutputStream)c.newInstance();
					PrintStream ps = new PrintStream( os );
					System.setOut( ps );
					System.setErr( ps );
				} catch( Throwable ex ) {
					logException( ex );
					nt.logMessage( 2, ex.toString() );
				}
			}
			// Create a stream to this file if requested.
			// make sure the class name is correctly stated.
			className = className.replace('/','.');
			String str = nt+": starting main in "+
				className+" with "+args.length+" arguments";
			for( int i = 0; i < args.length; ++i ) {
				str += "\n   arg["+i+"]: "+args[i];
			}
			log.finer( str );
			nt.logMessage( 0, str );
			// Must set the context class loader due to bug in JDK1.4
			Thread.currentThread().setContextClassLoader(
					ClassLoader.getSystemClassLoader() );
			Class cl = Class.forName( className );
			Method m = cl.getMethod( "main",
				new Class[]{ new String[]{}.getClass() } );
			// Static call to main(String[])
			m.invoke( null, new Object[]{ args } );
		} catch( Exception ex ) {
			// Make sure any startup exception is printed out.
			logException(ex);
		}
	}

	/**
	 *  If we invoke ourselves, then show the visible methods
	 */
	public static void main( String args[] ) {
		if( log.isLoggable(Level.FINER ) ) {
			Class c = NTServices.class;
			Method m[] = c.getDeclaredMethods();
			for( int i = 0; i <m.length; ++i ) {
				log.finer("m["+i+"]: "+m[i]);
			}
		}
	}
}