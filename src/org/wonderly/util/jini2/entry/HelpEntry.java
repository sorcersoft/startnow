package org.wonderly.util.jini2.entry;

import net.jini.core.entry.Entry;
import java.net.URL;

/**
 *  This is the base level interface for providing Help
 *  documentation with a service for a desktop environment to
 *  provide access for to the user.
 */
public abstract class HelpEntry implements Entry {
	public static final int JAVA_HELP_SET = 1;
	public static final int HTML_HELP_SET = 2;
	public Integer helpSetType;
	public String name;
	 static final long serialVersionUID = 1;

	public String toString() {
		return getDescr();
	}

	public int hashCode() {
		return name.hashCode();
	}

	public boolean equals( Object obj ) {
		if( obj instanceof HelpEntry == false )
			return false;
		return ((HelpEntry)obj).name.equals(name);
	}

	public HelpEntry() {
		helpSetType = JAVA_HELP_SET;
	}

	public HelpEntry( String name, int type ) {
		this.name = name;
		helpSetType = type;
	}
	
	public String getName() {
		return name;
	}

	/** @return the description that the user sees of this help set */
	public abstract String getDescr();
	/** @return the URL for accessing the help information */
	public abstract URL getHelpURL();
}