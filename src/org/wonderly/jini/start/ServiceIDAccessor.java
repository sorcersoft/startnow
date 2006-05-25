package org.wonderly.jini.start;

import net.jini.core.lookup.ServiceID;

/**
 *  This interface can be used to provide access to the services
 *  ServiceID value.  This might be used by a container for starting
 *  Jini services where the container might not be in charge of ServiceIDs,
 *  or might need to get a preestablished ServiceID for service registration.
 */
public interface ServiceIDAccessor {
	public ServiceID getServiceID();
}