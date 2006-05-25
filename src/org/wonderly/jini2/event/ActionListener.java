package org.wonderly.jini2.event;

import java.security.*;
import javax.security.auth.*;
import java.util.EventListener;
import java.awt.event.*;

/**
 *  This class is the base class of this package's event listener
 *  implementation.  It encapsulates the work of getting the
 *  context needed to recreate the execution environment that
 *  the constructor is called in when events are delivered.
 */
public abstract class ActionListener 
		extends SecurityContextEventListener 
		implements java.awt.event.ActionListener {
			
	public abstract void doActionPerformed( ActionEvent ev );

	public final void actionPerformed( final ActionEvent ev ) {
		final Thread th = Thread.currentThread();
		final ClassLoader cur = th.getContextClassLoader();
		try {
			th.setContextClassLoader( ctxLoader );
			if( subj == null ) {
				doActionPerformed(ev);
			} else {
				Subject.doAsPrivileged( subj, new PrivilegedAction() {
					public Object run() {
						doActionPerformed(ev);
						return null;
					}
				}, ctx );
			}
		} finally {
			th.setContextClassLoader( cur );
		}
	}		
}