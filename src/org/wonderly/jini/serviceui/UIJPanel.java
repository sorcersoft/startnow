package org.wonderly.jini.serviceui;

import org.wonderly.awt.*;
import org.wonderly.swing.*;
import java.lang.reflect.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.rmi.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.*;

import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.entry.*;
import net.jini.admin.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.lookup.ui.factory.*;
import com.sun.jini.admin.*;
import org.wonderly.swing.*;

import java.net.UnknownHostException;

/**
 *  This is a convenience subclass for all the UI Components implemented
 *  in this package.  It provides some basic methods that are an integral
 *  part of this implementation of ServiceUI
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public abstract class UIJPanel extends JPanel {

	/**
	 *  Report the passed exception to the user via System.out, and
	 *  via a JOptionPane
	 */
	protected void reportException( final Throwable ex ) {
		ex.printStackTrace();
		runInSwing( new Runnable() {
			public void run() {
				JOptionPane.showMessageDialog( getTopLevelAncestor(), ex,
					"Exception Message", JOptionPane.ERROR_MESSAGE );
			}
		});
	}

	/**
	 *  Make sure the passed Runnable is executed by a Swing dispatch Thread
	 */
	protected void runInSwing( Runnable r ) {
		if( SwingUtilities.isEventDispatchThread() ) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait( r );
			} catch( Exception ex ) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 *  Class runRemoteAction(new JComponent[]{c},r );
	 */
	protected void runRemoteAction( JComponent c, final RemoteAction r ) {
		runRemoteAction( new JComponent[]{c}, r );
	}

	/**
	 * Convenience method that handles all exception processing for 
	 *  remote actions
	 */
	protected void runRemoteAction( JComponent[] c, final RemoteAction r ) {
		new ComponentUpdateThread( c ) {
			public void setup() {
				super.setup();
				try {
					r.setup();
				} catch( Exception ex ) {
					reportException(ex);
				}
			}
			public Object failed() {
				return null;
			}
			public Object construct() {
				try {
					return r.construct();
				} catch( Exception ex ) {
					reportException(ex);
				}
				return failed();
			}
			public void finished() {
				try {
					r.finished(getValue());
				} catch( Exception ex ) {
					reportException(ex);
				} finally {
					super.finished();
					r.after();
				}
			}
		}.start();
	}
	
	/**
	 *  Inner class that provides a simple layer for
	 *  deferring common Exception handling to the 
	 *  convenience methods in this class
	 */
	protected static abstract class RemoteAction {
		public void setup() throws Exception {};
		public void finished(Object val) throws Exception {};
		public abstract Object construct() throws Exception;
		public void after() {};
	}
}