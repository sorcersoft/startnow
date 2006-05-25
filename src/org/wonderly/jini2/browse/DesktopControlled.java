package org.wonderly.jini2.browse;

import net.jini.core.lookup.*;

/**
 *  This interface should be implemented by the JiniDesktop object so
 *  that DesktopItem objects can register for notifications.  But,
 *  that doesn't seem quite right yet.
 *
 *  Use of this interface is not completed yet.
 */
public interface DesktopControlled {
	public void setHandler( ServiceID id, DesktopControlHandler hand );
}