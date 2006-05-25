package org.wonderly.awt;

import javax.security.auth.Subject;
import java.util.*;

/**
 *  This class provides a dymamic implementation of ThreadContextMap.
 *  This implementation allows the application to draw on the values in
 *  the AWTEvent source field to use particular Subject and ClassLoader
 *  values.  <b>This mechanism is exploitable by errant code which lies
 *  in the source field about the source of the event</b>.
 *
 *  @see SecureEventQueue
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class DynamicAWTEventSourceContextMap implements ThreadContextMap {
	/**
	 *  The map from source Object to Subject
	 */
	protected Hashtable<Object,Object> subjMap = 
		new Hashtable<Object,Object>();
	/**
	 *  The map from source Object to ClassLoader
	 */
	protected Hashtable<Object,ClassLoader> ldMap = 
		new Hashtable<Object,ClassLoader>();

	/**
	 *  Initializes with the passed subject and the
	 *  contextClassLoader of the current Thread.
	 *  @param source The source field of the AWTEvent.
	 *  @param subj the subject to return on {@link #getSubject(Object)}
	 *  callbacks.
	 */
	public DynamicAWTEventSourceContextMap( Object source, Subject subj ) {
		addSubjectMap( source, subj );
	}

	/**
	 *  Initializes with the passed subject and loader
	 *  @param source The source field of the AWTEvent.
	 *  @param subj the subject to return on {@link #getSubject(Object)}
	 *  callbacks.
	 *  @param loader the loader to return on {@link #getClassLoader(Object)}
	 *  callbacks.
	 */
	public DynamicAWTEventSourceContextMap( Object source, Subject subj, ClassLoader loader ) {
		addSubjectMap( source, subj, loader );
	}

	/**
	 *  Add a mapping from a particular AWTEvent source to a subject
	 *  @param source The source field of the AWTEvent.
	 *  @param subj the subject to return on {@link #getSubject(Object)}
	 *  callbacks.
	 */
	public void addSubjectMap( Object source, Subject subj ) {
		subjMap.put( source, subj );
		ldMap.remove( source );
	}

	/**
	 *  Add a mapping from a particular AWTEvent source to a subject
	 *  and classloader.
	 *  @param source The source field of the AWTEvent.
	 *  @param subj the subject to return on {@link #getSubject(Object)}
	 *  callbacks.
	 *  @param loader the loader to return on {@link #getClassLoader(Object)}
	 *  callbacks.
	 */
	public void addSubjectMap( Object source, Subject subj, ClassLoader loader ) {
		subjMap.put( source, subj );
		ldMap.put( source, loader );
	}

	/**
	 *  Get the Subject associated with the passed source.
	 *  @param source The source field of the AWTEvent.
	 *  @return the Subject passed when this instance was constructed.
	 */
	public Subject getSubject( Object source ) {
		return (Subject)subjMap.get( source );
	}

	/**
	 *  Get the classloader to use for the indicated source object.
	 *  @param source The source field of the AWTEvent.
	 *  @return the ClassLoader of the passed <code>source</code> Object.
	 */
	public ClassLoader getClassLoader( Object source ) {
		return (ClassLoader)ldMap.get( source );
	}
}