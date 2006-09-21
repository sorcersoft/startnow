package org.wonderly.jini.browse;

import org.wonderly.jini.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.beans.*;
import org.wonderly.util.jini.*;
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
 *  @see BrowserFactory
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */

public class JiniExplorerImpl extends PersistentJiniService implements RemoteAdministrable,JiniExplorer {
	String file;

	public static void main( String args[] ) throws IOException {
		new JiniExplorerImpl(args);
	}
	
	public String toString() {
		return "JiniExplorer";
	}
	
	public LookupEnv[] getLookups() throws IOException {
		try {
			LookupEnvList evl = new LookupEnvList(file);
			try {
				evl.loadEnvs();
			} catch( SAXException ex ) {
				IOException ioex = new IOException( "File Format Error?" );
				ioex.initCause( ex );
				throw ioex;
			} catch( ParserConfigurationException ex ) {
				IOException ioex = new IOException( "XML Parse Error?" );
				ioex.initCause( ex );
				throw ioex;
			} finally {
			}
			return (LookupEnv[])(evl.toArray(new LookupEnv[evl.size()]));
		} catch( Exception ex ) {
			ex.printStackTrace();
		}
		LookupEnv env1 = new LookupEnv( "Public ServiceUI enabled",
			new ServiceTemplate( null, null, new Entry[]{new UIDescriptor()} ) );
			
		LookupEnv[] envs = new LookupEnv[]{ env1 };
		if( System.getProperty("org.wonderly.jini.locator.host") != null )
			env1.addLookupLocator( System.getProperty("org.wonderly.jini.locator.host") );
		return envs;
	}

	public JiniExplorerImpl(String args[]) throws IOException {
		file = args.length > 0 ? args[0] : "lookups.xml";
		String lus[] = null;
		if( System.getProperty("org.wonderly.jini.locator.host") != null ) {
			lus = new String[] {
				"jini://"+System.getProperty("org.wonderly.jini.locator.host")
			};
		}
		startService( "Service UI Explorer",
			"serviceBrws.cfg", new Entry[] {
				new Name("Service UI Explorer"),
				new AdminDescriptor(),
				new BrowserDescriptor()
				//new PropertyAdminDescriptor()
			}, new String[]{""}, lus, null );
	}
}
