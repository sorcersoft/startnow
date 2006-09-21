package org.wonderly.jini2;

import java.io.*;
import org.wonderly.util.jini2.*;
import org.wonderly.jini.serviceui.*;
import net.jini.config.*;
import java.util.logging.*;
import javax.security.auth.login.*;
import javax.security.auth.*;
import java.net.*;
import java.security.*;
import net.jini.url.httpmd.HttpmdUtil;
import com.sun.jini.start.*;
import net.jini.config.Configuration;

/**
 *  This class provides a service that is an HTTP server information
 *  provider.  It does not implement an HTTP server, but is pointed at
 *  the directory that an http server is providing service from.  It
 *  allows a service to discover what the digest is for its codebase
 *  jar file(s).
 *
 *  @author Gregg Wonderly - gregg@wonderly.org
 */
public class HttpServerService
		extends PersistentJiniService
		implements HttpServerAccess, RemoteAdministrable {

	protected String filesURL = "file:/c:/classes";

	/**
	 *  Service Starter compatible constructor.
	 */
	public HttpServerService( String args[], LifeCycle life ) 
			throws ConfigurationException, IOException {
		super(args);
		log.fine("constructed for JTSK starter support");
		try {
			startup();
		} catch( LoginException ex ) {
			ex.printStackTrace();
		} catch( PrivilegedActionException ex ) {
			ex.printStackTrace();
		}
	}

	/**
	 *  Constructor for integrating this service into
	 *  another existing application as just a simple
	 *  class instantiation.
	 */
	public HttpServerService( Configuration config ) 
			throws ConfigurationException, IOException {
		super(config);
		log.fine("constructed with configuration: "+config );
		try {
			startup();
		} catch( LoginException ex ) {
			ex.printStackTrace();
		} catch( PrivilegedActionException ex ) {
			ex.printStackTrace();
		}
	}

	/**
	 *  main method to start service as a separate JVM process
	 *  and test its operation.
	 */
	public static void main( String args[] )
			throws LoginException, PrivilegedActionException,
				ConfigurationException, IOException {
		// Disable use of any class load spi for testing
		System.getProperties().remove(
			"java.rmi.server.RMIClassLoaderSpi");
//		System.out.println("codebase: "+System.getProperty("java.rmi.server.codebase"));
		try {
			HttpServerService sv = new HttpServerService( args );
			try {
				sv.log.fine("calling startup");
				sv.startup();
				sv.log.fine("creating annotation provider");
				
				// For testing, try and get an annotation
				DynamicClassAnnotationProvider cl = 
					new DynamicClassAnnotationProvider();
				sv.log.fine("Asking for annotation for: "+
					sv.getClass().getName() );
				sv.log.fine("cl anno: "+
					cl.getClassAnnotation(sv.getClass()));
			} catch( LoginException ex ) {
				ex.printStackTrace();
			} catch( PrivilegedActionException ex ) {
				ex.printStackTrace();
			}
		} catch( ConfigurationException ex ) {
			ex.printStackTrace();
		} catch( IOException ex ) {
			ex.printStackTrace();
		} catch( Throwable ex ) {
			ex.printStackTrace();
		}
	}

	/**
	 *  Constructor for creating an instance of the service from
	 *  another class.
	 */
	public HttpServerService( String args[] )
			throws IOException, ConfigurationException {
		super( args );
		log.fine("Constructed with "+args.length+" arguments" );
	}
	
	/**
	 *  Must be invoked to start the Jini service registration.
	 */
	public void startup()
			throws LoginException, PrivilegedActionException,
				ConfigurationException, IOException {

		filesURL = (String)conf.getEntry(
			getClass().getName(),
			"filesDir", String.class, "." );

		LoginContext ctx = (LoginContext)conf.getEntry( 
			getClass().getName(),
			"loginContext",
			LoginContext.class, null );
		if( ctx != null ) {
			log.fine("Starting service with context: "+ctx );
			ctx.login();
			Subject sb = ctx.getSubject();
			log.fine("Logged in as: "+sb);
			Subject.doAs( sb, new  PrivilegedExceptionAction() {
				public Object run() throws Exception {
					startService();
					return null;
				}
			});
		} else {
			log.fine("Starting service with no login");
			startService();
		}
	}

	/**
	 *  If you know the sum and the path, you can ask the server
	 *  for the stream to the file with getFile().
	 *  @param path URL path
	 *  @param sum the sum that we know
	 */
//	public InputStream getFile( String path,
//			String sum ) throws java.io.IOException {
//		return null;
//	}
	/**
	 *  If you know the sum and the path, you can ask the server
	 *  if it can serve that version of the indicated path
	 *  @param path URL path.
	 *  @param type the type of the digest
	 *  @param digest the digest that we know
	 */
	public boolean canServe( String path, 
			String type,
			String digest ) throws java.io.IOException {
		URL u = new URL( new URL(filesURL), path);
		try {
			u.openStream().close();
			String sum = sumFor( path, type );
			return sum.equals(digest);
		} catch( IOException ex ) {
			log.log( Level.WARNING, ex.toString(), ex );
		}
		return false;
	}
	/**
	 *  If you just know the path, you can ask the server for
	 *  the sum of the version that it has.
	 *  @param path URL path.
	 *  @param type the type of sum desired as in "md5"
	 */
	public String sumFor( String path, 
			String type ) throws IOException {
		try {
			URL u1 = new URL(filesURL);
			URL um = new URL( u1, path);
			log.fine( "filesURL: "+u1+", adding path: "+path );
			log.fine( "with path: "+um);
			return HttpmdUtil.computeDigest( um, type );
		} catch( NoSuchAlgorithmException ex ) {
			IOException ioe = new IOException( ex.toString() );
			throw (IOException)ioe.initCause(ex);
		}
	}
}