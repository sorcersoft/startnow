package org.wonderly.util.jini;

import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;

import java.io.*;

/**
 *  Service persistance data saved to disk to indicate the services configuration
 *  that it should apply to LookupService registration.
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class PersistentData implements Serializable {
	public ServiceID id;
	public Entry[] attrs;
	public String[] groups;
	public LookupLocator[] locators;
	public PersistentData() {}
}
