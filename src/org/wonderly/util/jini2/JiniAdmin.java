package org.wonderly.util.jini2;

import java.net.InetAddress;
import java.rmi.*;
import java.lang.reflect.*;
import java.rmi.server.*;

import net.jini.discovery.*;
import net.jini.admin.*;
import com.sun.jini.admin.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.entry.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.lease.*;
import net.jini.core.lease.*;
import com.sun.jini.lookup.entry.LookupAttributes;
import java.util.logging.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.util.*;
import org.wonderly.log.*;

import org.wonderly.jini2.*;
import org.wonderly.util.jini2.*;
import org.wonderly.util.jini.*;
import net.jini.export.*;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.config.ConfigurationException;
import net.jini.security.proxytrust.ServerProxyTrust;
import net.jini.security.proxytrust.ProxyTrustVerifier;
import net.jini.security.TrustVerifier;
import net.jini.export.ProxyAccessor;
import net.jini.jeri.ssl.SslTrustVerifier;

/**
 *  This class provides a remoteable class that implements the Jini administration interfaces
 *  which allow the inheriting class to be treated as an Administrable service.  The DestroyAdmin
 *  interface is enabled via the constructor argument or the use of the setDestroyAdminEnabled()
 *  method.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class JiniAdmin 
		extends org.wonderly.util.jini.JiniAdmin 
		implements NameableObject,Remote,ServerProxyTrust {
	protected Configuration config;

	public Remote exportObject( Object obj ) throws RemoteException {
		throw new IllegalArgumentException("Unsupported API interface" );
	}
	
	public TrustVerifier getProxyVerifier() throws RemoteException {
		try {
			return (TrustVerifier)config.getEntry( getPackage()+"."+getName(),
				"proxyVerifier", TrustVerifier.class );
		} catch( ConfigurationException ex ) {
			log.log( Level.CONFIG, ex.toString(), ex );
			return null;
		}
	}

	public Remote getExportedObject() throws RemoteException {
		throw new IllegalArgumentException("Unsupported API interface" );
	}

	/**
	 *  @return getClass().getName().substring( getClass().getName().lastIndexOf('.')+1 );
	 */
	public String getName() {
		String nm = getClass().getName();
		int nmi = nm.lastIndexOf('.');
		return nm.substring(nmi+1);
	}

	/**
	 *  @return getClass().getName().substring(0, getClass().getName().lastIndexOf('.') );
	 */
	public String getPackage() {
		String nm = getClass().getName();
		int nmi = nm.lastIndexOf('.');
		return nm.substring(0,nmi);
	}

	/**
	 *  Constructs an instance with the passed parameters.
	 *  Delegates to JiniAdmin( mgr, io, dm, false );
	 */
	public JiniAdmin( JoinManager mgr, PersistenceIO io,
			LookupDiscoveryManager dm, String args[] ) throws IOException, ConfigurationException {
		this( mgr, io, dm, false, args );
	}

	/**
	 *  Constructs an instance using the passed parameters to control
	 *  interactions with Jini and the filesystem and JVM.
	 */
	public JiniAdmin( JoinManager mgr, PersistenceIO io,
			LookupDiscoveryManager dm, boolean enableDestroy, String args[] ) throws IOException, ConfigurationException {
		super( mgr, io, dm, enableDestroy );
		config = ConfigurationProvider.getInstance( args );
	}
	
	/**
	 *  Constructs an instance using the passed parameters to control
	 *  interactions with Jini and the filesystem and JVM.
	 */
	public JiniAdmin( JoinManager mgr, PersistenceIO io,
			LookupDiscoveryManager dm, boolean enableDestroy, Configuration conf ) throws IOException, ConfigurationException {
		super( mgr, io, dm, enableDestroy );
		config = conf;
	}
}