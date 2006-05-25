package org.wonderly.jini2.browse;

import org.wonderly.jini2.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.beans.*;
import org.wonderly.util.jini2.*;
import org.wonderly.util.jini.LookupEnv;
import org.wonderly.util.jini.LookupEnvList;
import org.wonderly.swing.*;
import org.wonderly.awt.*;
import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.core.event.*;
import net.jini.core.lease.*;
import net.jini.core.transaction.server.*;
import net.jini.entry.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.lease.*;
import net.jini.event.*;
import net.jini.space.*;
import net.jini.admin.*;
import net.jini.lookup.ui.factory.*;
import net.jini.lookup.ui.*;
import java.util.*;
import  org.wonderly.jini.serviceui.*;
import java.awt.*;
import java.io.*;
import java.rmi.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import org.wonderly.jini.browse.JiniExplorer;
import org.wonderly.jini2.browse.BrowserDescriptor;
import net.jini.config.*;

/**
 *  This is an example service that implements the JiniExplorer
 *  interface.  It loads a LookupEnvList from the file provided
 *  on the commandline, or if none is provided, or the provided
 *  name does not exist, it returns a lookup environment for
 *  serviceUI implementations, and uses the org.wonderly.jini.locator.host
 *  property to indicate an lookup locator that should be used.
 *
 *  @see JiniExplorer
 *  @see BrowserDescriptor
 *  @see org.wonderly.jini.browse.BrowserFactory
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */

public class JiniExplorerImpl 
		extends org.wonderly.jini2.PersistentJiniService 
		implements RemoteAdministrable,JiniExplorer {

	protected String file;

	public static void main( String args[] ) throws IOException, ConfigurationException {
		new JiniExplorerImpl(args);
	}
	
	public String toString() {
		return "JiniExplorer";
	}
	
	public LookupEnv[] getLookups() throws IOException {
		try {
			log.fine("lookups requested");
			LookupEnv[] l = getLookupEnvs();
			log.fine( "lookups from config: "+
				(l == null ? "null" : l.length+" entries") );
			for( int i = 0; l != null && i < l.length; ++i ) {
				log.finer( "   ["+i+"] = "+l[i].printable() );
			}
			return l;
		} catch( ConfigurationException ex ) {
			IOException ioex = new IOException( "Configuration Error" );
			ioex.initCause(ex);
			throw ioex;
		}
	}

	public JiniExplorerImpl(String args[]) throws IOException, ConfigurationException {
		super(args);
		String cfg = null;
		String name = getName();
		if( args.length > 0 )
			cfg = args[0]+".ser";
		else
			cfg = name+".cfg.ser";
		startService( "Service UI Explorer", cfg );
	}
}
