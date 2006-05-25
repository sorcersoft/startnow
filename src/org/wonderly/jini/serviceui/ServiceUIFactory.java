package org.wonderly.jini.serviceui;

import javax.swing.*;
import java.awt.*;
import java.rmi.*;
import java.lang.reflect.*;
import java.beans.*;
import net.jini.core.lookup.*;

/**
 *  This is a base class for all ServiceUI Factories.  It implements methods
 *  for all Swing related component retrievals.  The subclass can declare the
 *  needed Factory interfaces to attach those factories to the implementation.
 *  The subclass can then implement <code>buildJComponent</code> to build
 *  the needed component that will be encapsulated by all the other container
 *  types.
 *  <p>
 *  Subclasses can also override the methods here to add toolbar or menu components
 *  to the toplevel containers if needed.
 *
 *  @author Gregg Wonderly - gregg.wonderly@pobox.com
 */
public abstract class ServiceUIFactory {

	public JFrame getJFrame( Object svcItem ) {
		try {
			return buildJFrame( (ServiceItem)svcItem );
		} catch( Exception ex ) {
			throw new RuntimeException("Remote Error", ex );
		}
	}

	public JWindow getJWindow( Object svcItem ) {
		return getJWindow( (ServiceItem)svcItem, null );
	}

	public JWindow getJWindow( Object svcItem, Frame owner ) {
		return getJWindow( (ServiceItem)svcItem, owner );
	}

	public JWindow getJWindow( Object svcItem, Window owner ) {
		try {
			return buildJWindow( (ServiceItem)svcItem, null );
		} catch( Exception ex ) {
			throw new RuntimeException("Remote Error", ex );
		}
	}

	public JDialog getJDialog( Object svcItem ) {
		return getJDialog( (ServiceItem)svcItem, (JFrame)null, true );
	}


	public JDialog getJDialog( Object svcItem, Dialog parent ) {
		return getJDialog( (ServiceItem)svcItem, parent, true );
	}

	public JDialog getJDialog( Object svcItem, Dialog parent, boolean lock ) {
		try {
			return buildJDialog( (ServiceItem)svcItem, parent, lock );
		} catch( Exception ex ) {
			throw new RuntimeException("Remote Error", ex );
		}
	}

	public JDialog getJDialog( Object svcItem, Frame parent ) {
		return getJDialog( (ServiceItem)svcItem, parent, true );
	}

	public JDialog getJDialog( Object svcItem, Frame parent, boolean lock ) {
		try {
			return buildJDialog( (ServiceItem)svcItem, parent, lock );
		} catch( Exception ex ) {
			throw new RuntimeException("Remote Error", ex );
		}
	}

	public JComponent getJComponent( Object svcItem ) {
		try {
			return buildJComponent( (ServiceItem)svcItem );
		} catch( Exception ex ) {
			throw new RuntimeException("Remote Error", ex );
		}
	}
	
	protected JFrame buildJFrame( ServiceItem svcItem ) throws Exception {
		return null;
	}
	
	protected JWindow buildJWindow( ServiceItem svcItem, Window parent ) throws Exception {
		return null;
	}
	
	protected JDialog buildJDialog( ServiceItem svcItem, Dialog parent, boolean modal ) throws Exception {
		return null;
	}
	
	protected JDialog buildJDialog( ServiceItem svcItem, Frame parent, boolean modal ) throws Exception {
		return null;
	}
	
	protected abstract JComponent buildJComponent( ServiceItem svcItem ) throws Exception;
}
