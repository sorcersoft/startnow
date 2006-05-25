package org.wonderly.jini2.proxy;

import net.jini.security.ProxyPreparer;
import net.jini.core.lookup.ServiceItem;
import java.rmi.RemoteException;
import net.jini.config.*;

/**
 *  This interface provides a more extensive set up parameters to
 *  proxy preparation.  It provides a method that allows the
 *  ServiceItem of the service to be provided as well.  This can allow
 *  specific characteristics of the service such as serviceID or
 *  values in the attributeList array to be used to control
 *  how preparation is performed.
 *
 *  The specific type of deployment this is intended to deal with is
 *  an environment where many, services are hosted off a single machine.
 *  This might happen on a Surrogate host as an example.  There are
 *  other situations such as a household server or other environments
 *  where a single server might provide access to a wide number of
 *  services that need to be treated differently, yet might have the
 *  same codebase.
 */
public interface ServiceProxyPreparer extends ProxyPreparer {
	/**
	 *  @param proxy the proxy to prepare
	 *  @param item the services service item to find Entry's in.
	 *  @param conf the clients configuration to match
	 *    Entry field name values with for <code>ref</code> use.
	 */
	public Object prepareProxy( Object proxy, ServiceItem item, Configuration conf ) throws RemoteException;
}