package org.wonderly.jini2.browse;

import net.jini.core.lookup.*;

/** 
 *  This interface provides the control functions for JiniDesktop resident
 *  applications.  The JiniDesktop provides object implementing this interface
 *  for the JDesktopComponentFactory implementing serviceUIs to use.
 *
 *  Use of this interface is not completed yet.
 */
public interface DesktopControlHandler {
	/** User is done with this desktop item */
	public void exiting( ServiceID id );
	/** Move application from JInternalFrame to JFrame */
	public void detach( ServiceID id );
	/** Move application from JFrame to JInternalFrame */
	public void reAttach( ServiceID id );
}