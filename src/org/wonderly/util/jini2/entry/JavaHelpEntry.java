package org.wonderly.util.jini2.entry;

import java.net.URL;
	
/**
 *  This is the Entry used to specify information about a JavaHelp HelpSet
 *  that should be associated with a particular service..
 */
public class JavaHelpEntry extends HelpEntry {
	/** Public storage required for Entry implementation, use access method
	 *  to get this value.
	 */
	public URL url;
	/** The title for the user to read */
	public String title;
	/** The starting ID */
	public String id;
	 static final long serialVersionUID = 1;

	public String getDescr() {
		return title;
	}
	
	public JavaHelpEntry() {
	}

	/**
	 *  Construct an instance
	 *	@param name a unique name that won't conflict with other names visible
	 *  in the system. Typically, this should resemble a java package name using
	 *  a domain name to qualify the space and the follow that with a particular
	 *  help set name.  Another choice would be to just use the URL string that
	 *  points at the helpset to make it unique.
	 *  @param title a title to use in the UI to describe this entry
	 *  @param startId the starting help set map id to go to when this entry is selected.
	 *  @param help the URL to the helpset file.
	 */
	public JavaHelpEntry( String name, String title, String startId, URL help ) {
		super(name, JAVA_HELP_SET);
		url = help;
		this.id = startId;
		this.title = title;
	}
	public String getStartID() {
		return id;
	}
	/**
	 *  @return the help URL for this Java Help Set instance.
	 */
	public URL getHelpURL() {
		return url;
	}
}
