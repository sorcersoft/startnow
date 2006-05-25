package org.wonderly.awt;

import javax.security.auth.Subject;

/**
 *  This class provides a simple implementation of ThreadContextMap.
 *  This implementation allows a single Subject and ClassLoader to
 *  be used for all action in the associated {@link SecureEventQueue}.
 *
 *  @see SecureEventQueue
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class SingleIdentityContextMap
		implements ThreadContextMap {
	protected Subject subject;
	protected ClassLoader loader;

	/**
	 *  Initializes with the passed subject and the
	 *  contextClassLoader of the current Thread.
	 *  @param subj the subject to return on {@link #getSubject(Object)}
	 *  callbacks.
	 */
	public SingleIdentityContextMap( Subject subj ) {
		subject = subj;
		loader = Thread.currentThread().getContextClassLoader();
	}

	/**
	 *  Initializes with the passed subject and loader
	 *  @param subj the subject to return on {@link #getSubject(Object)}
	 *  callbacks.
	 *  @param loader the loader to return on {@link #getClassLoader(Object)}
	 *  callbacks.
	 */
	public SingleIdentityContextMap( Subject subj, ClassLoader loader ) {
		subject = subj;
		this.loader = loader;
	}

	/**
	 *  returns the Subject passed when this instance was constructed.
	 */
	public Subject getSubject( Object source ) {
		return subject;		
	}

	/**
	 *  returns the ClassLoader established when this instance was constructed.
	 */
	public ClassLoader getClassLoader( Object source ) {
		return loader;
	}
}