package org.wonderly.util.jini;

import net.jini.core.lookup.*;

/**
 *  A simple interface for letting external entities listen to Jini
 *  LUS state changes
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public interface RegistrarTransitionListener {
	public void removeInstance( ServiceEvent ev, ServiceRegistrar reg );
	public void addInstance( ServiceEvent ev, ServiceRegistrar reg );
	public void updateInstance( ServiceEvent ev, ServiceRegistrar reg );
}
