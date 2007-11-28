package org.wonderly.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides a way for tracking the loss of reference of one type of
 * object to allow a secondary reference to be used to perform some cleanup
 * activity.  The most common use of this is with one object which might
 * contain or refer to another object that needs some cleanup performed
 * when the referer is no longer referenced.
 * <p>
 * An example might be an object of type Holder, which refers to or uses a
 * Socket connection.  When the reference is lost, the socket should be
 * closed.  Thus, an instance might be created as in
 * <pre>
 *	ReferenceTracker<Holder,Socket> trker = ReferenceTracker<Holder,Socket>() {
 *		public void released( Socket s ) {
 *			try {
 *				s.close();
 *			} catch( Exception ex ) {
 *				log.log( Level.SEVERE, ex.toString(), ex );
 *			}
 *		}
 *  };
 * </pre>
 * Somewhere, there might be calls such as the following.
 * <pre>
 *		interface Holder<T> {
 *			public T get();
 *		}
 *		class SocketHolder implements Holder<Socket> {
 *			Socket s;
 *			public SocketHolder( Socket sock ) {
 *				s = sock;
 *			}
 *			public Socket get() {
 *				return s;
 *			}
 *		}
 * </pre>
 * This defines an implementation of the Holder interface which holds
 * a reference to Socket objects.  The use of the <code>trker</code>
 * object, above, might then include the use of a method for creating
 * the objects and registering the references as shown below.
 * <pre>
 *	public SocketHolder connect( String host, int port ) throws IOException {
 *		Socket s = new Socket( host, port );
 *		SocketHolder h = new SocketHolder( s );
 *		trker.trackReference( h, s );
 *		return h;
 *	}
 * </pre>
 * Software wishing to use a socket connection, and pass it around would
 * use SocketHolder.get() to reference the Socket instance, in all cases.
 * then, when all SocketHolder references are dropped, the socket would
 * be closed by the <code>released(java.net.Socket)</code> method shown
 * above.
 * @author gregg wonderly <gregg@wonderly.org>
 */
public abstract class ReferenceTracker<T,K> {
	/**
	 * The thread instance that is removing entries from the reference queue, refqueue, as they appear.
	 */
	private volatile RefQueuePoll poll;
	/**
	 * The Logger instance used for this instance.  It will include the name as a suffix 
	 * if that constructor is used.
	 */
	private final Logger log;
	/**
	 * The name indicating which instance this is for logging and other separation of
	 * instances needed.
	 */
	private final String which;

	/**
	 * Creates a new instance of ReferenceTracker using the passed name to differentiate
	 * the instance in logging and toString() implementation.
	 * @param which The name of this instance for differentiation of multiple instances in logging etc.
	 */
	public ReferenceTracker( String which  ) {
		this.which = which;
		log = Logger.getLogger( getClass().getName()+"."+which );
	}

	/**
	 * Creates a new instance of ReferenceTracker with no qualifying name.
	 */
	public ReferenceTracker( ) {
		this.which = null;
		log = Logger.getLogger( getClass().getName() );
	}

	/**
	 * Provides access to the name of this instance.
	 * @return The name of this instance.
	 */
	public String toString() {
		if( which == null )
			return super.toString();
		return "ReferenceTracker["+which+"]";
	}

	/**
	 * Subclasses must implement this method.  It will be called when all references to the
	 * associated holder object are dropped.
	 * @param val The value passed as the second argument to a corresponding call to {@link #trackReference(T,K)}
	 */
	public abstract void released( K val );

	/** The reference queueu for references to the holder objects */
	private final ReferenceQueue<T>refqueue = new ReferenceQueue<T>();
	/**
	 * The count of the total number of threads that have been created and then destroyed as entries have
	 * been tracked.  When there are zero tracked references, there is no queue running.
	 */
	private final AtomicInteger tcnt = new AtomicInteger();
	/**
	 * A Thread implementation that polls {@link #refqueue} to subsequently call {@link released(K)}
	 * as references to T objects are dropped.
	 */
	private class RefQueuePoll extends Thread {
		/**
		 * The thread number associated with this instance.  There might briefly be two instances of
		 * this class that exists in a volatile system.  If that is the case, this value will
		 * be visible in some of the logging to differentiate the active ones.
		 */
		private final int mycnt;
		/**
		 * Creates an instance of this class.
		 */
		public RefQueuePoll() {
			setDaemon( true );
			mycnt = tcnt.incrementAndGet();
		}
		/**
		 * This method provides all the activity of performing <code>refqueue.remove()</code>
		 * calls and then calling <code>released(K)</code> to let the application release the
		 * resources needed.
		 */
		public void run() {
			boolean done = false;
			while( !done ) {
				try {
					Reference<? extends T> ref = refqueue.remove();
					K ctl;
					synchronized( refmap ) {
						ctl = refmap.remove( ref );
						done = actCnt.decrementAndGet() == 0;
						log.info("current act refs="+actCnt.get());
					}
					log.finer("reference released for: "+ref+", dep="+ctl );
					if( ctl != null ) {
						try {
							released( ctl );
							log.fine("dependant object released: "+ctl );
						} catch( Exception ex ) {
							log.log( Level.SEVERE, ex.toString(), ex );
						}
					}
				} catch( Exception ex ) {
					log.log( Level.SEVERE, ex.toString(), ex );
				}
			}
			log.info("poll thread "+mycnt+" shutdown for "+this );
		}
	}

	/**
	 * A count of the active references.
	 */
	private final AtomicInteger actCnt = new AtomicInteger();
	/**
	 * Map from T References to K objects to be used for the released(K) call
	 */
	private final ConcurrentHashMap<Reference<? extends T>,K>refmap = new ConcurrentHashMap<Reference<? extends T>,K>();
	/**
	 *  Adds a tracked reference.  dep should not refer to ref in any way except possibly
	 *  a WeakReference.  dep is almost always something referred to by ref.
	 * @throw IllegalArgumentException of ref and dep are the same object.
	 * @param dep The dependent object that needs cleanup when ref is no longer referenced.
	 * @param ref the object whose reference is to be tracked
	 */
	public void trackReference( T ref, K dep ) {
		if( ref == dep ) {
			throw new IllegalArgumentException( "Referenced object and dependent object can not be the same" );
		}
		PhantomReference<T> p = new PhantomReference<T>( ref, refqueue );
		synchronized( refmap ) {
			refmap.put( p, dep );
			if( actCnt.getAndIncrement() == 0 ) {
				poll = new RefQueuePoll();
				poll.start();
				log.info( "poll thread #"+tcnt.get()+" created for "+this );
			}
		}
	}

	/**
	 *  This method can be called if the JVM that the tracker is in, is being
	 *  shutdown, or someother context is being shutdown and the objects tracked
	 *  by the tracker should now be released.  This method will result in 
	 *  {@link #released(K)} being called for each outstanding refernce.
	 */
	public void shutdown() {
		List<K>rem;
		// Copy the values and clear the map so that released
		// is only ever called once, incase GC later evicts references
		synchronized( refmap ) {
			rem = new ArrayList<K>( refmap.values() );
			refmap.clear();
		}
		for( K dep : rem ) {
			try {
				released( dep );
			} catch( Exception ex ) {
				log.log( Level.SEVERE, ex.toString(), ex );
			}
		}
	}
}
