package org.wonderly.jini.entry;

import net.jini.core.entry.*;

/**
 *  This Entry is used to specify a desktop group for a
 *  UI to use to separate itself amongst other UIs.  Examples
 *  of DesktopGroup names might be 'accounting', 'developers',
 *  'engineering', 'games', 'office' etc.
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class DesktopGroup implements Entry {
	public String group;
	public Boolean istrans;

	public DesktopGroup() {
	}

	public DesktopGroup( String name) {
		this( name, false );
	}

	public DesktopGroup( String name, boolean trans ) {
		group = name;
		istrans = new Boolean(trans);
	}

	public boolean isTransient() {
		return istrans.booleanValue();
	}
}