package org.wonderly.jini2.browse;

import org.wonderly.jini.*;
import org.wonderly.jini.browse.JiniExplorer;
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
import net.jini.config.ConfigurationException;
import java.awt.*;
import java.io.*;

/**
 *  This is a serviceUI JFrameFactory that instantiates the JiniBrowse class.
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */

public class BrowserFactory implements JFrameFactory {
	JiniBrowse br;
	public JFrame getJFrame( Object svcItem ) {
		Object svc = ((ServiceItem)svcItem).service;
		try {
			LookupEnv[]envs = ((JiniExplorer)svc).getLookups();
			br = new JiniBrowse(null);
			if( envs == null ) {
				envs = new LookupEnv[] {
					new LookupEnv( "Public serviceUI enabled",
						new ServiceTemplate( null, null, new Entry[]{new UIDescriptor()} ) )
				};
			}
			br.showSelector(envs);
			br.f.pack();
			br.f.setSize( 400, 800 );
			return br.f;
		} catch( ConfigurationException ex ) {
			ex.printStackTrace();
		} catch( IOException ex ) {
			ex.printStackTrace();
		} catch( RuntimeException ex ) {
			ex.printStackTrace();
			throw ex;
		}
		return null;
	}
}
