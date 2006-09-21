package org.wonderly.util.jini;

import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;

import java.io.*;

/**
 *  Service persistance data saved to disk to indicate the services configuration
 *  that it should apply to LookupService registration.
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 *
 *  @deprecated - Use PersistentData now
 */
public class PersistantData extends PersistentData {
	/**
	 *
	 *  @deprecated - Use PersistentData now
	 */
	public PersistantData() {
		super();
	}
}
