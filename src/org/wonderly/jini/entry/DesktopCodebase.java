package org.wonderly.jini.entry;

import net.jini.core.entry.*;

/**
 *  This Entry is used to specify the codebase associated
 *  with a service.  A serviceui client can show this
 *  to the user who can direct the UI to use a dynamic
 *  policy provider to grant permissions to the codebase.
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class DesktopCodebase implements Entry {
	public String path;

	public DesktopCodebase() {
	}

	public DesktopCodebase( String path ) {
		this.path = path;
	}
}