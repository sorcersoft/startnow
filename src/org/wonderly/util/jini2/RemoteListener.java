package org.wonderly.util.jini2;

import net.jini.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.entry.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.core.event.*;
import net.jini.core.lease.*;
import net.jini.lease.*;

import java.rmi.*;
import java.rmi.server.*;
import org.wonderly.util.jini.RegistrarTransitionListener;
import net.jini.export.Exporter;
import org.wonderly.jini2.*;
import net.jini.security.proxytrust.*;
import net.jini.security.*;

/**
 *  A class the uses the RegistrarTransitionListener to call specific
 *  methods for management of state.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class RemoteListener extends org.wonderly.util.jini.RemoteListener implements Remote, NameableObject,ServerProxyTrust  {
	TrustVerifier verif;

	public RemoteListener(RegistrarTransitionListener pane, ServiceRegistrar reg ) throws RemoteException {
		super( pane,reg,false);
	}

	public RemoteListener(RegistrarTransitionListener pane, ServiceRegistrar reg, TrustVerifier verifier ) throws RemoteException {
		super( pane,reg,false);
		verif = verifier;
	}
	/**
	 *  @return getClass().getName().substring( getClass().getName().lastIndexOf('.')+1 );
	 */
	public String getName() {
		String nm = getClass().getName();
		int nmi = nm.lastIndexOf('.');
		return nm.substring(nmi+1);
	}

	public TrustVerifier getProxyVerifier() throws RemoteException {
		return verif;
	}

	/**
	 *  @return getClass().getName().substring(0, getClass().getName().lastIndexOf('.') );
	 */
	public String getPackage() {
		String nm = getClass().getName();
		int nmi = nm.lastIndexOf('.');
		return nm.substring(0,nmi);
	}
}
