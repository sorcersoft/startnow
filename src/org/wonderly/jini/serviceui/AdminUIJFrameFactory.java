package org.wonderly.jini.serviceui;

import net.jini.lookup.ui.factory.*;
import javax.swing.*;
import java.beans.*;
import java.util.*;
import org.wonderly.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.lang.reflect.*;
import java.awt.*;
import org.wonderly.swing.*;
import net.jini.admin.*;
import net.jini.core.lookup.*;
import java.util.logging.*;

/**
 *  The class should be passed to the UIDescriptor
 *  constructor, in a MarshalledObject.  It provides 
 *  an interface to constructing a Service-UI for
 *  Administrative purposes.  It will create an 
 *  instance of the AdminUIManager class to return
 *  to the calling application, wrapped inside of 
 *  the appropriate container, based on the factory
 *  component type used.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class AdminUIJFrameFactory
		extends
			ServiceUIFactory
		implements
			JComponentFactory,
			JWindowFactory,
			JFrameFactory,
			JDialogFactory {
	static final long serialVersionUID = 5024429951981897437l;
	transient Logger log = Logger.getLogger( getClass().getName() );

	private void readObject( ObjectInputStream is ) 
			throws IOException,ClassNotFoundException {
		is.defaultReadObject();
		if( log == null ) {
			log = Logger.getLogger( getClass().getName() );
		}
	}

	public AdminUIJFrameFactory() {
	}

	protected JFrame buildJFrame( ServiceItem svcItem ) 
		throws RemoteException,
			IllegalAccessException,
			InvocationTargetException,
				IntrospectionException {
		int y = -1;
		final JFrame f = new JFrame( "Admin Service" );
		log.fine("Build component into: "+f);
		Packer pk = new Packer( f.getContentPane() );
	
		pk.pack( buildJComponent(svcItem) 
			).fillboth().gridx(0).gridy(++y);

		final JButton cancel = new JButton("Close");
		pk.pack( new JSeparator() 
			).gridx(0).gridy(++y).fillx().inset(4,4,4,4);
		pk.pack( cancel ).gridx(0).gridy(++y);
		final boolean cancelled[] = new boolean[1];
		cancel.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				f.setVisible(false);
			}
		});
		f.pack();
		f.setSize( 500, 350 );
		return f;
	}
	
	
	protected JDialog buildJDialog( ServiceItem svcItem, Dialog parent, boolean modal ) throws Exception {
		return buildJDialog( svcItem, (Component)parent, modal );
	}
	
	protected JDialog buildJDialog( ServiceItem svcItem, Frame parent, boolean modal ) throws Exception {
		return buildJDialog( svcItem, (Component)parent, modal );
	}

	protected JWindow buildJWindow( ServiceItem svcItem,
			Component owner ) 
		throws RemoteException,
			IllegalAccessException,
			InvocationTargetException,
				IntrospectionException {
		JWindow ff = null;
		if( owner instanceof Window ) {
			ff = new JWindow( (Window)owner );
		} else if( owner instanceof Frame ) {
			ff = new JWindow( (Frame)owner );
		} else if( owner == null ) {
			ff = new JWindow();
		} else {
			// No parent type can be determined yet
		}
		final JWindow f = ff;
		log.fine("Build component into: "+f);
		int y = -1;
		Packer pk = new Packer( f.getContentPane() );
		pk.pack( buildJComponent( svcItem ) 
			).fillboth().gridx(0).gridy(++y);
		final JButton cancel = new JButton("Close");
		pk.pack( new JSeparator() 
			).gridx(0).gridy(++y).fillx().inset(4,4,4,4);
		pk.pack( cancel ).gridx(0).gridy(++y);
		final boolean cancelled[] = new boolean[1];
		cancel.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				f.setVisible(false);
			}
		});
		f.pack();
		f.setSize( 500, 350 );
		return f;
	}

	protected JDialog buildJDialog( ServiceItem svcItem,
			Component parent, boolean lock ) 
		throws RemoteException,
			IllegalAccessException,
			InvocationTargetException,
				IntrospectionException {

		JDialog ff = null;
		if( parent instanceof JDialog ) {
			ff = new JDialog( (JDialog)parent, 
				"Admin Service", lock );
		} else if( parent instanceof JFrame ) {
			ff= new JDialog( (JFrame)parent, 
				"Admin Service", lock );
		} else if( parent instanceof JComponent ) {
			// Defer to using the passed component to locate a
			// top level ancestor to use for parenting.
			return buildJDialog( svcItem,
				((JComponent)parent).getTopLevelAncestor(), lock );
		} else {
			ff= new JDialog( (JFrame)null,
				"Admin Service", lock );
		}
		final JDialog f = ff;
		log.fine("Build component into: "+f);
			
		Packer pk = new Packer( f.getContentPane() );
		int y = -1;
		pk.pack( buildJComponent(svcItem)
			).fillboth().gridx(0).gridy(++y);

		final JButton cancel = new JButton("Close");
		pk.pack( new JSeparator()
			).gridx(0).gridy(++y).fillx().inset(4,4,4,4);
		pk.pack( cancel ).gridx(0).gridy(++y);
		final boolean cancelled[] = new boolean[1];
		cancel.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				f.setVisible(false);
			}
		});
		f.pack();
		f.setSize( 500, 350 );
		return f;
	}

	protected JComponent buildJComponent( ServiceItem svcItem ) 
		throws RemoteException,
			IllegalAccessException,
			InvocationTargetException,
			IntrospectionException {
		Object svcObject = svcItem.service;
		
		if( svcObject == null ) {
			throw new NullPointerException(
				"no service found in "+svcItem );
		}
		JComponent tp = null;
		Packer tpk = null;
		JTabbedPane tabs = null;
		AdminUIManager mgr = null;
		log.fine("Build component svcObject: "+
			svcObject.getClass().getName() );
		Class ints[] = svcObject.getClass().getInterfaces();
		for( int i = 0; i < ints.length; ++i ) {
			log.fine("svc implements: "+ints[i].getName() );
		}
		if( svcObject instanceof Administrable ) {
			log.fine("service is Administrable");
			mgr = new AdminUIManager((Administrable)svcObject);
		}
		if( svcObject instanceof PropertiesAccess && 
				svcObject instanceof Administrable) {
			log.fine("service is PropertiesAccess");
			tabs = new JTabbedPane(JTabbedPane.BOTTOM);
			tp = tabs;
			tabs.add( "Admin", mgr );
		} else {
			tp = new JPanel();
			tpk = new Packer( (JPanel)tp );
			if( svcObject instanceof Administrable ) {
				log.fine("Putting Adminstrable pane into UI");
				tpk.pack( mgr ).fillboth();
			}
		}
		if( svcObject instanceof PropertiesAccess ) {
			log.fine("Building PropertiesAccess");
			JPanel jp = buildPropertiesAccess( svcItem );
			jp.setPreferredSize( new Dimension( 400, 300 ) );
			if( svcObject instanceof Administrable ) {
				tabs.add( "Properties", jp );
			} else {
				tpk.pack( jp ).fillboth();
			}
		}
		if( svcObject instanceof Administrable == false &&
				svcObject instanceof PropertiesAccess == false ) {
			tpk.pack( 
				new JLabel(
					"No Known Adminstration Interfaces Implemented",
					JLabel.CENTER ) 
				).inset( 10, 10, 10, 10 ).fillboth();
			tp.setBorder( BorderFactory.createEtchedBorder() );
		}
		return tp;
	}

	protected JPanel buildPropertiesAccess( ServiceItem svcItem ) 
		throws RemoteException,
			IllegalAccessException,
			InvocationTargetException,
			IntrospectionException {	
		Object svcObject = svcItem.service;
		final PropertiesAccess acc = (PropertiesAccess)svcObject;
		FeatureDescriptor fd[] = acc.descriptors();
		Properties cur = acc.currentValues();
		Properties defaults = acc.defaultValues();
		final PropertiesPanel p = 
			new PropertiesPanel( fd, cur, defaults );
		JPanel jp = new JPanel();
		Packer pk = new Packer( jp );
		int y = -1;
		pk.pack( p ).fillboth().gridx(0).gridy(++y);
		final JButton okay = new JButton("Save");
		pk.pack( new JSeparator()
			).gridx(0).gridy(++y).fillx().inset(4,4,4,4);
		pk.pack( okay ).gridy(++y);
		final boolean cancelled[] = new boolean[1];
		okay.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				cancelled[0] = false;
				final Properties pp = new Properties();
				p.getPropertyValues(pp);
				new ComponentUpdateThread( okay ) {
					public Object construct() {
						try {
							acc.setProperties( pp );
						} catch( Exception ex ) {
							ex.printStackTrace();
						}
						return null;
					}
				}.start();
			}
		});
		return jp;
	}
}
