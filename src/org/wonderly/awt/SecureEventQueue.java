package org.wonderly.awt;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.security.PrivilegedAction;
import javax.security.auth.Subject;

/**
 *  This class was first suggested by Mike in
 *  <a href="http://davis.jini.org/servlets/ReadMsg?list=discuss&msgNo=1097">this
 *  post</a>.  This is an extension of that version to include
 *  a mechanism for multiple subject use to help manage multiple
 *  client sources.
 *
 *  To use this...
 *  <pre>
 SubjectMap subjectMap = ...;

 Toolkit toolkit = Toolkit.getDefaultToolkit();
 EventQueue queue = toolkit.getSystemEventQueue();
 queue.push(new SecureEventQueue(subjectMap));

 ... // awt away...
 
 </pre>
 *  This is not really a complete solution yet,  There are
 *  some thoughts here that need more investigation.
 */
public class SecureEventQueue extends EventQueue {
	private ThreadContextMap map;

	public SecureEventQueue( ThreadContextMap subjectMap){
		this.map = subjectMap;
	}

	private void dispatchEventInternal(AWTEvent e){
		ClassLoader old = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader( 
				map.getClassLoader( e.getSource() ) );
			super.dispatchEvent(e);
		} finally {
			Thread.currentThread().setContextClassLoader( old );
		}
	}

	protected void dispatchEvent(final AWTEvent e){
		Subject.doAs( map.getSubject( e.getSource() ),
			new PrivilegedAction() {
				public Object run() {
					dispatchEventInternal(e);
					return null;
				}
			}
		);
	}
}