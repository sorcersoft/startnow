package org.wonderly.util.jini;

import net.jini.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.entry.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.core.event.*;
import net.jini.core.lease.*;
import net.jini.lease.*;
import java.util.logging.*;

import java.rmi.*;
import java.rmi.server.*;

/**
 *  A class the uses the RegistrarTransitionListener to call specific
 *  methods for management of state.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class RemoteListener implements RemoteEventListener, Remote {
	RegistrarTransitionListener pane;
	ServiceRegistrar reg;
	Logger log = Logger.getLogger(getClass().getName());

	public RemoteListener( RegistrarTransitionListener pane, ServiceRegistrar reg ) throws RemoteException {
		this( pane, reg, true );
	}
	public RemoteListener( RegistrarTransitionListener pane, ServiceRegistrar reg, boolean export ) throws RemoteException {
		this.pane = pane;
		this.reg = reg;
		if( export )
			UnicastRemoteObject.exportObject( this );
	}
	public void notify( RemoteEvent ev ) throws RemoteException {
		log.fine( "notify( "+ev+") " );
		if( ev instanceof ServiceEvent ) {
			switch( ((ServiceEvent)ev).getTransition() ) {
			case ServiceRegistrar.TRANSITION_MATCH_NOMATCH:
				pane.removeInstance( (ServiceEvent)ev, reg );
				break;
			case ServiceRegistrar.TRANSITION_NOMATCH_MATCH:
				pane.addInstance( (ServiceEvent)ev, reg );
				break;
			case ServiceRegistrar.TRANSITION_MATCH_MATCH:
				pane.updateInstance( (ServiceEvent)ev, reg );
				break;
			}
		}
	}
}
