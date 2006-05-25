package org.wonderly.awt;

import javax.security.auth.Subject;

public interface ThreadContextMap {
	/** Source is AwtEvent.getSource() */
	public Subject getSubject( Object source );
	/** Source is AwtEvent.getSource() */
	public ClassLoader getClassLoader( Object source );
}