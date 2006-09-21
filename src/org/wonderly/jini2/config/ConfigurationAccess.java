package org.wonderly.jini2.config;

import net.jini.config.*;

/**
 *  This interface is used to receive notification
 *  of Configuration content changes.  The <code>apply()</code>
 *  method is called to indicated that the passed
 *  Configuration instance is now applicable.
 */
public interface ConfigurationAccess {
	/**
	 *  This method is called when the passed configuration
	 *  should now be applied.  Note that this will likely be
	 *  a new instance of Configuration for each call and that
	 *  using <code>conf</code> as a key to anything is not a
	 *  good idea.
	 *  @param conf the new Configuration that should be applied
	 */
	public void apply( Configuration conf ) throws ConfigurationException;
}