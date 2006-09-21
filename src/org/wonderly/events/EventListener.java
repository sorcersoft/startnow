package org.wonderly.events;

import java.net.*;
import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.logging.*;
import net.jini.export.*;
import java.security.*;
import java.util.logging.*;

/**
 *  This class provides the remote, per connection implementation
 *  of the subscription mechanism.  EventManager creates an instance
 *  of this class for each register'd subscriber.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class EventListener implements IEventListener,IEventSync,Runnable, Serializable, Remote {
	transient QueueEntry free;
	transient QueueEntry qHead, qTail;
	transient ServerSocket sock;
	transient Socket toSock;
	transient Object queueLock;
	transient Thread queueThread;
	transient Vector<Subscription> subs;
	transient IEventSync me = this;
	transient IEventSource src;
	transient int freeLen;
	transient int pendLen;
	transient boolean done = false;
	transient Remote fac;
	// The socket address
	transient private String addr;
	transient Logger log;
	transient int highWater = 20;
	static final long serialVersionUID = 1l;
	  
	public EventListener( Exporter exporter, ServerSocket s, 
			IEventSource source, String addr
			) throws RemoteException {
		log = Logger.getLogger( getClass().getName() );
		fac = exporter.export(this);
		sock = s;
		subs = new Vector<Subscription>();
		src = source;
		this.addr = addr;
		highWater = 20;
		queueLock = new Object();
		queueThread = new Thread(this);
		queueThread.start();
	}
	
	/**
	 *  Set the highwater mark for event discarding on slow links.
	 *  @param val The maximum number of queued events, or 0 for no limit.
	 */
	public void setHighWater( int val ) {
		highWater = val;
	}
	
	/** Get current Highwater mark value */
	public int getHighWater() {
		return highWater;
	}

	/** Stop and disconnect the client */
	public void stopping() {
		done = true;
		try {
			toSock.close();
		} catch( Exception ex ) {
			reportException(ex);
		}
	}

	/**
	 *  Set the subscription to all of the passed Subscription(s)
	 *  in the Vector.
	 */
	public void setSubscription( Vector<Subscription> v ) throws IOException {
		try {
			for( int i = 0; i < v.size(); ++i ) {
				Subscription sub = (Subscription)v.elementAt(i);
				AccessController.checkPermission( new
					SubscriptionPermission( sub.getTopic().toString() ) );
			}
			subs = v;
		} catch( RuntimeException ex ) {
			reportException(ex);
			throw ex;
		}
	}
	
	protected void reportException( Throwable ex ) {
		log.log( Level.SEVERE, ex.toString(), ex );
	}

	public void run() {
		Socket s = null;
		ObjectOutputStream out;
		try {
			// Create a thread to close the socket after 40 seconds of waiting
			// for a client connection.  We also notify this thread to wake up
			// and close the socket after we accept the first connection from
			// it, so conveniently we can use this thread in both cases.
			Thread th = new Thread() {
				public synchronized void run() {
					log.finer("Wait for timeout" );
					String sdelay = System.getProperty("org.wonderly.event.sync.timeout");
					long delay = 60000;
					if( sdelay != null )
						delay = Long.parseLong( sdelay );
					try { this.wait(delay); } catch( Exception ex ) {}
					log.finer( "Close socket" );
					try {
						sock.close();
					} catch( Exception ex ) {
						log.log( Level.FINER, "Closed socket failed: "+ex, ex );
					}
				}
			};
			th.start();
			log.finer( "Wait for connect" );
			s = sock.accept();
			out = new ObjectOutputStream( s.getOutputStream() );
			log.finer( "Got connection: "+s );
			synchronized( th ) {
				th.notify();
			}
		} catch( Exception ex ) {
			reportException(ex);
			return;
		}
		toSock = s;
		done = false;
		try {
			final InputStream is = s.getInputStream();
			final Socket tsock = s;
			new Thread() {
				public void run() {
					try {
						log.finer( "Wait for lost connection" );
						is.read();
						log.finer( "Connection lost" );
					} catch( Exception ex ) {
						log.finer( this+": shutdown at EOF" );
						try { tsock.close(); } catch( Exception exx ) {}
					} finally {
						done = true;
						synchronized( queueLock ) {
							queueLock.notify();
						}
						src.disconnect( me );
					}
				}
			}.start();
		} catch( Exception ex ) {
			reportException(ex);
			src.disconnect(me);
			return;
		}				
					
		while( !done ) {
			QueueEntry ent;
			PublishedEvent pub;
			synchronized( queueLock ) {
				while( qHead == null && !done ) {
					try {
						log.finer( "Waiting for events" );
						queueLock.wait();
						log.finer( "Wokeup" );
					} catch( Exception ex ) {
						reportException(ex);
					}
					// If we are woke up and need to exit, do that now.
					if( done ) {
						try {
							out.close();
						} catch( IOException ee ) {	}
						return;
					}
				}
				ent = qHead;
				qHead = qHead.next;
				if( qHead == null )
					qTail = null;
				pub = ent.event;
				if( freeLen < highWater ) {
					ent.event = null;
					ent.next = free;
					free = ent;
					++freeLen;
				}
				--pendLen;
			}
			if( shouldHear( pub.getTopic() ) == false )
				continue;
			log.finer( "Got event: "+pub.getTopic()+", data: "+pub.getEvent() );
			try {								
				out.reset();
				out.writeObject( pub );				
				out.flush();
				log.finer( "Wrote event: "+pub.getTopic()+", data: "+pub.getEvent() );
			} catch( Exception e ) {
				try {
					out.close();
				} catch( IOException ee ) {	}
				reportException( e );
				src.disconnect( me );
				return;
			}
			log.finer( "sent event" );
		}
	}

	static class QueueEntry {
		QueueEntry next;
		PublishedEvent event;
		public QueueEntry( PublishedEvent ev ) {
			event = ev;
		}
	}

	public String toString() {
		return "EventListener (pend="+pendLen+", free="+freeLen+") to: "+toSock;
	}

	/**
	 *  Enqueue and event to be delivered if the highwater mark
	 *  has not been reached for outstanding events.
	 */
	public void enqueue( PublishedEvent ev ) {
		synchronized( queueLock ) {
			if( pendLen > highWater && highWater != 0 ) {
				log.finer( this+": too many pending events: "+pendLen+", qHead: "+qHead );
				if( qHead == null )
					pendLen = 0;
				queueLock.notify();
				return;
			}
			QueueEntry ent;
			if( free == null ) {
				ent = new QueueEntry( ev );
			} else {
				--freeLen;
				ent = free;
				free = free.next;
				ent.event = ev;
			}
			ent.next = null;
			if( qTail == null ) {
				log.finer( "put first entry in queue" );
				qHead = ent;
				qTail = ent;
			} else {
				log.finer( "put entry in queue tail" );
				qTail.next = ent;
				qTail = ent;
			}
			++pendLen;
			log.finest( "qHead: "+qHead );
			log.finest( "qTail: "+qTail );
			queueLock.notifyAll();
		}
	}

	public void subscribe( Subscription sub ) throws IOException {
		AccessController.checkPermission( new
			SubscriptionPermission( sub.getTopic().toString() ) );
		subs.addElement( sub );
	}

	public void subscribe( String topic, boolean listenTo ) throws IOException {
		subscribe( new Subscription( topic, listenTo ) );
	}

	public boolean unsubscribe( Subscription sub ) throws IOException {
		subs.removeElement( sub );
		return true;
	}

	public boolean shouldHear( Topic ttopic ) {
		try {
			Enumeration e = subs.elements();
			while( e.hasMoreElements() ) {
				Subscription s = (Subscription)e.nextElement();
				log.finer( "Check \""+s.getTopic()+"\" matches \""+ttopic+"\"" );
				if( s.getTopic().matches( ttopic ) ) {
					return s.listening;
				}
			}
			log.finer( "ignoring: "+ttopic );
		} catch( ArrayIndexOutOfBoundsException ex ) {
		} catch( Exception ex ) {
			reportException( ex );
		}
		return false;
	}

	public int getPort() {
		return sock.getLocalPort();
	}

	public String getHost() throws IOException {
		return addr;
	}
}