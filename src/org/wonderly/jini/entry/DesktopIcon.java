package org.wonderly.jini.entry;

import net.jini.core.entry.*;

/**
 *  This Entry is used to store the String value of an image URL that will
 *  be used with java.awt.ImageIcon to create an Icon.
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */

public class DesktopIcon implements Entry {
	public String iconUrl;
	public String iconRole;
	
	public DesktopIcon() {
	}
	public DesktopIcon( String url ) {
		this( url, null);
	}
	public DesktopIcon( String url, String role ) {
		iconUrl = url;
		iconRole = role;
	}
}
