package org.wonderly.events;

import java.io.*;
import net.jini.export.*;
import java.util.*;
import java.rmi.*;

/**
 *  A remote interface to getting an event stream from the associated
 *  object(s).
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public interface IEventManager extends Remote {
	/**
	 *  Register and create initial server socket to connect to.
	 *  as a new Source of events to this manager.
	 */
	public IEventListener register() throws IOException;
}