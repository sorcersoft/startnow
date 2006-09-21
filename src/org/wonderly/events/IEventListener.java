package org.wonderly.events;

import org.wonderly.events.PublishedEvent;
import java.util.*;
import java.rmi.*;
import java.io.*;

/**
 *  This interface is implemented by classes that provide a
 *  subscription service.  The subscription is controlled by the
 *  methods here-in.  The EventManager is the primary user of 
 *  this interface.  The EventManager is used by the 
 *  components of the System to listen to specified topics as
 *  events matching those topics are published in the system.
 *
 *  @see org.wonderly.events.EventManager
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public interface IEventListener extends Remote {
	/** Add a subscription element */
	public void subscribe( Subscription sub ) throws IOException;
	/**
	 *  Set the subscription list to the passed set of
	 *  Subscription objects
	 */
	public void setSubscription( Vector<Subscription> v ) throws IOException;
	/**
	 *  Unsubscribe from the passed subscription.  The passed subscription
	 *  information which may either deny reporting of a topic, or require
	 *  reporting of a topic will be removed from the current subscription
	 *  list.
	 *  @return true if found. false if topic was not found.
	 */
	public boolean unsubscribe( Subscription sub ) throws IOException;
	/** Get the port to connect to to receive data */
	public int getPort() throws IOException;
	/** Get the host to connect to to receive data */
	public String getHost() throws IOException;
}