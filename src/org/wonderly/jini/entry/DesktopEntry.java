package org.wonderly.jini.entry;

import net.jini.core.entry.*;

/**
 *  This Entry is used to specify an icon that would appear
 *  on a 'desktop' to activate a particular serviceUI role.
 *  it identifies a grouping, the role, the icon, and an
 *  indication of whether the service is transient to help
 *  a desktop understand whether it can depend on this mapping
 *  to be around for any length of time.
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class DesktopEntry implements Entry {
	public String group;
	public Boolean istrans;
	public String iconUrl;
	public String iconRole;
	
	public String toString() {
		return "DesktopEntry("+group+","+iconRole+","+iconUrl+","+istrans+")";
	}
	
	/**  No args serialization support. */
	public DesktopEntry() {
	}

	/**
	 *  Construct an instance.
	 *  @param group the group to lump this role/icon into
	 *  @param role the role this mapping applies to
	 *  @param iconUrl the URL of an icon to display for this role.
	 *  @param trans true if this is a transient service
	 */
	public DesktopEntry( String group, String role,
			String iconUrl, boolean trans ) {
		this.iconUrl = iconUrl;
		this.iconRole = role;
		this.group = group;
		istrans = new Boolean(trans);
	}

	public String getGroup() {
		return group;
	}
	
	public String getIconUrl() {
		return iconUrl;
	}
	
	public String getRole() {
		return iconRole;
	}

	public boolean isTransient() {
		return istrans.booleanValue();
	}
}