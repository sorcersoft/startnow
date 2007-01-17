/*
 * StartServiceDeployer.java
 *
 * Created on May 16, 2006, 4:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.wonderly.jini2.start;

import java.security.AccessController;
import java.security.SecurityPermission;
import javax.security.auth.login.LoginContext;
import net.jini.config.*;
import java.util.*;
import java.util.logging.*;
import net.jini.export.Exporter;
import net.jini.id.*;
import java.io.*;
import java.net.*;
import net.jini.security.*;
import org.wonderly.jini2.*;
import com.sun.jini.start.*;
import java.rmi.*;

/**
 * @see com.sun.jini.start.ServiceStarter
 * @author gregg
 */
public class StartServiceDeployer extends com.sun.jini.start.ServiceStarter {
	static Logger logger = Logger.getLogger( StartServiceDeployer.class.getName() );
	static JiniServiceDeployment depl;

    /**
     * The main method for the <code>ServiceStarter</code> application.
     * The <code>args</code> argument is passed directly to 
     * <code>ConfigurationProvider.getInstance()</code> in order to 
     * obtain a <code>Configuration</code> object. This configuration 
     * object is then queried for the 
     * <code>com.sun.jini.start.serviceDescriptors</code> entry, which
     * is assumed to be a <code>ServiceDescriptor[]</code>.
     * The <code>create()</code> method is then called on each of the array
     * elements.
     * @param args <code>String[]</code> passed to 
     *             <code>ConfigurationProvider.getInstance()</code> in order
     *             to obtain a <code>Configuration</code> object.
     * @see com.sun.jini.start.ServiceDescriptor
     * @see com.sun.jini.start.SharedActivatableServiceDescriptor
     * @see com.sun.jini.start.SharedActivationGroupDescriptor
     * @see com.sun.jini.start.NonActivatableServiceDescriptor
     * @see net.jini.config.Configuration
     * @see net.jini.config.ConfigurationProvider
     */
    public static void main(String[] args) {
       ensureSecurityManager();
       try {
           logger.entering(ServiceStarter.class.getName(),
	       "main", (Object[])args);
           Configuration config = ConfigurationProvider.getInstance(args);
           ServiceDescriptor[] descs =  (ServiceDescriptor[])
	           config.getEntry(START_PACKAGE, "serviceDescriptors",
	               ServiceDescriptor[].class, null );

		   Exporter exp =  (Exporter)
	           config.getEntry( START_PACKAGE+".deploy", "exporter",
	               Exporter.class, null );

           if( (descs == null || descs.length == 0 ) && exp == null ) {
               logger.warning("service.config.empty");
			   return;
		   }

		   Object expSvc = exp.export( depl = new ServiceDeployer() );

		   String serviceName = "Service Deployer";
		   String serFile = "startsvc";
		   PersistentJiniService.JoinContext sc = PersistentJiniService.startService(
				serviceName, serFile, 
				expSvc, false,config, START_PACKAGE+".deploy", null );

           LoginContext loginContext =  (LoginContext)
			   config.getEntry(START_PACKAGE, "loginContext", 
				   LoginContext.class, null);
		   Result[] results = null;
		   if (loginContext != null)
			   results = createWithLogin(descs, config, loginContext);
		   else
			   results = create(descs, config);
           checkResultFailures(results);	       
           maintainNonActivatableReferences(results);	       
       } catch (ConfigurationException cex) {
		   logger.log(Level.SEVERE, "service.config.exception", cex);
       } catch (Exception e) {
           logger.log(Level.SEVERE, "service.creation.exception", e);
       }
       logger.exiting(ServiceStarter.class.getName(), "main");
	}

	private static class ServiceDeployer implements JiniServiceDeployment, Remote {

		private String stringList( List<String> args, String sep ) {
			String str = "";
			for( String s : args ) {
				if( str.length() > 0 )
					str += sep;
				str += s;
			}
			return str;
		}
		public Uuid createNonActivatableServiceDeployment(
				String className, String[] configs, List<String> codebase, 
				List<String>classpath, String policy,
				String[]args ) throws IOException, ConfigurationException {
			return createNonActivatableServiceDeployment( className, configs, 
					codebase, classpath, policy, args, null);
		}

		public Uuid createNonActivatableServiceDeployment( 
				String className, String[] configs, List<String> codebase, 
				List<String>classpath, String policy,
				String[]args, LifeCycle lifeCycle ) throws IOException, ConfigurationException {

			AccessController.checkPermission( new SecurityPermission( className ) );

			Configuration config = ConfigurationProvider.getInstance( configs );
			Uuid ui = UuidFactory.generate();
			ServiceDescriptor[]descs = new ServiceDescriptor[] {
				new NonActivatableServiceDescriptor(
					stringList( codebase, System.getProperty("path.separator") ),
					policy, stringList( classpath, System.getProperty("path.separator")),
					className, args, lifeCycle )
				 };
			Result[] results = null;
			try {
				LoginContext loginContext =  (LoginContext)
					config.getEntry(START_PACKAGE, "loginContext",
						LoginContext.class, null);
				if( loginContext != null )
					results = createWithLogin( descs, config, loginContext );
				else
					results = create( descs, config );
				checkResultFailures(results);	       
				maintainNonActivatableReferences(results);	       
			} catch( ConfigurationException ex ) {
				throw ex;
			} catch( IOException ex ) {
				throw ex;
			} catch( Exception e ) {
				logger.log(Level.SEVERE, "service.creation.exception", e);
				throw (IOException)new IOException(e.toString()).initCause(e);
			}
			if( results != null && results.length == 1 ) {
				return svcToUuid.get( results[0] );
			}
			throw new IOException( "Service not created?, null entry" );
		}
	}
}
