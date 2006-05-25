package org.wonderly.util.jini2.entry;

import java.net.URL;
	
/**
 *  This is the base level interface for providing Help
 *  documentation with a service for a desktop environment to
 *  provide access for to the user.
 */
public class HtmlHelpEntry extends HelpEntry {
	/** Public storage required for Entry implementation, use access method
	 *  to get this value.
	 */
	public URL url;
	/** The title for the user to read */
	public String title;

	 static final long serialVersionUID = 1;

	/** Construct an instance */
	public HtmlHelpEntry( String name, String title, URL help ) {
		super(name, HTML_HELP_SET);
		url = help;
		this.title = title;
	}

	public HtmlHelpEntry() {
	}

	public String getDescr() {
		return title;
	}

	/**
	 *  @return the help URL for this HTML based Help Set instance.
	 */
	public URL getHelpURL() {
		return url;
	}
}
