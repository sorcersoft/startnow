package org.wonderly.util.jini2;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceID;
import java.io.IOException;
import net.jini.config.ConfigurationException;

public interface ServiceLookupHandler {
	public void processItem( ServiceItem item, ServiceRegistrar reg ) throws ConfigurationException,IOException;
	public void updateItem( ServiceItem item, ServiceRegistrar reg ) throws ConfigurationException,IOException;
	public void serviceLost( ServiceID id, ServiceRegistrar reg ) throws ConfigurationException,IOException;
}