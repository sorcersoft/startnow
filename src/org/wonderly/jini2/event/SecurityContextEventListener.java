package org.wonderly.jini2.event;

import java.security.*;
import javax.security.auth.*;
import java.util.EventListener;

/**
 *  This class is the base class of this package's event listener
 *  implementation.  It encapsulates the work of getting the
 *  context needed to recreate the execution environment that
 *  the constructor is called in when events are delivered.
 */
public class SecurityContextEventListener implements EventListener {
	protected ClassLoader ctxLoader;
	protected Subject subj;
	protected AccessControlContext ctx;

	public SecurityContextEventListener() {
		ctxLoader = Thread.currentThread().getContextClassLoader();
		subj = Subject.getSubject( ctx = AccessController.getContext() );
	}
}