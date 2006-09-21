package org.wonderly.events;

import org.wonderly.events.PublishedEvent;
import org.wonderly.events.EventListener;
import java.util.*;
import java.net.*;
import java.io.*;
import java.rmi.*;
import java.util.logging.*;
import net.jini.export.*;
import net.jini.config.*;

/**
 *  The EventManager provides an implementation of IEventManager for
 *  use in applications that have a centralized source of events.
 *  The application should create an instance of this object and
 *  make it visible to the users.  It can export the object and
 *  advertise it via RMI/Jini etc.  Or, can include the IEventManager
 *  interface as part of the signature of another exported class,
 *  and then delegate to this object.
 *
 *  The starting() method should be called to initialize and start
 *  the processing of the object.
 *
 *  The deliverEvent() method is then used to send out events to
 *  the subscribers.
 *
 *  When this object is not needed anylonger, stopping() should be
 *  called to disconnect all subscribers.
 *
 *  <table>
 *  <tr><th width="3">Logging through class name
 *  <tr><th>Level<th>What Logging
 *  <tr><td>FINER<th>General Events of processing
 *  </table>
 *  <p>
 *  <table>
 *  <tr><th width="3">Configuration Entries recognized
 *  <tr><th>Entry<th>Use<th>Default
 *  <tr><td>Exporter<td>Used to export EventListener(s)<td>None
 *  <tr><td>highWater<td>Maximum outstanding events<td>20
 *  <tr><td>bindTo<td>SocketAddress[] to bind to<td>"0.0.0.0", port=0
 *  <tr><td>returnName<td>Hostname/address to return to clients<td>InetAddress.getLocalHost().getHostName()
 *  </table>
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class EventManager implements IEventManager {
	transient Hashtable<EventListener,EventListener> syncs;
	transient String bindaddr;
	transient boolean sendName;
	transient int backlog = 10;
	transient Logger log;
	transient Configuration conf;
	transient int port;
	transient IEventSource src;
	transient Exporter exporter;
	transient String retAddr;
	transient int highWater = 20;
	transient SocketAddress ports[];

	/**
	 *  @param conf the configuration to use for getting an exporter
	 *  @param inst the name of the configuration instance entry to use
	 *  @param src where the events will be comming from so that
	 *         a callback can be made with the destination is closed
	 */
	public EventManager(Configuration conf, String inst,
			IEventSource src) throws java.net.UnknownHostException,
				RemoteException,ConfigurationException {
		Logger log = Logger.getLogger( getClass().getName() );
		this.conf = conf;
		this.src = src;
		exporter = (Exporter)conf.getEntry( inst, "exporter",
			Exporter.class );
		highWater = ((Integer)conf.getEntry( inst, "highWater",
			Integer.class, new Integer(20) )).intValue();
		ports = (SocketAddress[])conf.getEntry( inst, "bindTo",
			(new SocketAddress[]{}).getClass(), null );
		retAddr = (String)conf.getEntry( inst, "returnAddr",
			String.class, InetAddress.getLocalHost().getHostName() );
		log.config("exporter: "+exporter);
		log.config("highWater: "+highWater);
		log.config("ports: "+ 
			ports == null ? "null" : ports.length+"" );
		log.config("returnAddr: "+retAddr );
	}

	public int getBackLog() {
		return backlog;
	}

	public void setBackLog( int val ) {
		backlog = val;
	}

	public boolean getSendHostName() {
		return sendName;
	}

	public void setSendHostName( boolean how ) {
		sendName = how;
	}

	public void setPort(int port ) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public String getInterfaceAddr() {
		return bindaddr;
	}
	
	public void setInterfaceAddr( String addr ) {
		bindaddr = addr;
	}

	/**
	 *  Called by the framework to start up the object and
	 *  prepare it for use.
	 */
	public void starting() throws IOException {
		log.finer(this+": starting object: "+super.toString() );
		syncs = new Hashtable<EventListener,EventListener>(20);
		if( bindaddr == null )
			bindaddr = "0.0.0.0";
	}

	/**
	 *  Returns the number of connected listeners
	 */
	public int getConnectedCount() {
		if( syncs == null )
			return 0;
		return syncs.size();
	}

	/**
	 *  Called by the framework to shutdown the object.
	 *  We unsubscribe from the broker, call super.stopping()
	 *  to get the server socket shutdown, and then go through
	 *  all the remote client sockets and close them down.
	 */
	public void stopping() throws IOException {
		log.finer(this+": stopping object: "+super.toString() );
		Enumeration i = syncs.elements();
		while( i.hasMoreElements() ) {
			EventListener evl = (EventListener)i.nextElement();
			try {
				evl.stopping();
			} catch( Exception ex ) {
				reportException(ex);
			}
		}			
	}

	/**
	 *  Handle report exceptions via the Logger
	 */
	protected void reportException( Throwable ex ) {
		log.log( Level.SEVERE, ex.toString(), ex );
	}

	/**
	 *  This method is remotely access by clients wishing to initiate a
	 *  subscription.  They are returned an IEventListener which they
	 *  can then use to perform the subscription management and to
	 *  connect to the server to get the matching events.
	 */
	public IEventListener register() throws IOException {
		log.finer( "registering new client" );
		ServerSocket srv = null;
		if( ports == null ) {
			InetAddress addr = InetAddress.getByName( bindaddr );
			srv = new ServerSocket(0,backlog,addr);
		} else {
			srv = new ServerSocket();
			for( int i = 0; i < ports.length; ++i ) {
				srv.bind( ports[i], backlog );
			}
		}
		log.fine( "bound to: "+srv );
		EventListener evl = new EventListener( exporter,
			srv, src, retAddr );
		evl.setHighWater( highWater );
		syncs.put( evl, evl );

		return evl;			
	}

	/**
	 *  Process received events.
	 *
	 *  @param event the event to process
	 *  @param topic the topic the event was published with
	 */
	public void deliverEvent( EventObject event, Topic topic ) {
		log.finest( "delivering Event ("+topic+"): "+event );
		PublishedEvent pub = new PublishedEvent( event, topic );
		Enumeration e = syncs.elements();
	 	while( e.hasMoreElements() ) {
			IEventSync s = (IEventSync)e.nextElement();
			s.enqueue( pub );
		}
	}
}