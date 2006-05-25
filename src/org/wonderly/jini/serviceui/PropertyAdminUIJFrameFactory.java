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

/**
 *  The class should be passed to the UIDescriptor constructor, in
 *  a MarshalledObject.  It provides an interface to constructing a
 *  Service-UI for Administrative purposes.  It will create a JComponent
 *  containing an instance of he AdminUIManager class, as well as a
 *  PropertiesPanel to return to the calling application, wrapped
 *  inside of the appropriate container, based on the factory component
 *  type used.
 *
 *  @see org.wonderly.awt.PropertiesPanel
 *  @see AdminUIManager
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class PropertyAdminUIJFrameFactory
		extends
			ServiceUIFactory
		implements
			JComponentFactory,
			JWindowFactory,
			JFrameFactory,
			JDialogFactory {

	static final long serialVersionUID = 5024429951981897437l;

	public PropertyAdminUIJFrameFactory() {
	}

	protected JFrame buildJFrame( ServiceItem svcItem ) 
		throws RemoteException,
			IllegalAccessException,
			InvocationTargetException,
				IntrospectionException {
		final JFrame f = new JFrame( "Admin Service" );
		Packer pk = new Packer( f.getContentPane() );
		int y = -1;
		pk.pack( buildJComponent(svcItem) ).fillboth().gridx(0).gridy(++y);
		final JButton cancel = new JButton("Close");
		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(4,4,4,4);
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
	protected JWindow buildJWindow( ServiceItem svcItem, Component owner ) 
		throws RemoteException,
			IllegalAccessException,
			InvocationTargetException,
				IntrospectionException {
		JWindow ff = null;
		if( owner instanceof Window )
			ff = new JWindow( (Window)owner );
		else if( owner instanceof Frame )
			ff = new JWindow( (Frame)owner );
		else if( owner == null )
			ff = new JWindow();
		final JWindow f = ff;
		Packer pk = new Packer( f.getContentPane() );
		int y = -1;
		pk.pack( buildJComponent(svcItem) ).fillboth().gridx(0).gridy(++y);
		final JButton cancel = new JButton("Close");
		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(4,4,4,4);
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

	protected JDialog buildJDialog( ServiceItem svcItem, Component parent, boolean lock ) 
		throws RemoteException,
			IllegalAccessException,
			InvocationTargetException,
				IntrospectionException {
		JDialog ff = null;
		if( parent instanceof JDialog )
			ff = new JDialog( (JDialog)parent, "Admin Service", lock );
		else if( parent instanceof JFrame )
			ff= new JDialog( (JFrame)parent, "Admin Service", lock );
		else if( parent instanceof JComponent ) {
			return buildJDialog( svcItem,
				((JComponent)parent).getTopLevelAncestor(), lock );
		} else {
			ff= new JDialog( (JFrame)null, "Admin Service", lock );
		}
			
		final JDialog f = ff;
		Packer pk = new Packer( f.getContentPane() );
		int y = -1;		
		pk.pack( buildJComponent(svcItem) ).fillboth().gridx(0).gridy(++y);
		
		final JButton cancel = new JButton("Close");
		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(4,4,4,4);
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
		JComponent tp = null;
		Packer tpk = null;
		tp = new JPanel();
		tpk = new Packer( (JPanel)tp );
		if( svcObject instanceof PropertiesAccess ) {
			JPanel jp = buildPropertiesAccess( svcItem );
			jp.setPreferredSize( new Dimension( 400, 300 ) );
			tpk.pack( jp ).fillboth();
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
		final PropertiesPanel p = new PropertiesPanel( fd, cur, defaults );
		JPanel jp = new JPanel();
		Packer pk = new Packer( jp );
		int y = -1;
		pk.pack( p ).fillboth().gridx(0).gridy(++y);
		final JButton okay = new JButton("Save");
		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(4,4,4,4);
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
