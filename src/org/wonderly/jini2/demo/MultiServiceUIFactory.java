package org.wonderly.jini2.demo;

import net.jini.lookup.ui.*;
import net.jini.lookup.ui.factory.*;
import net.jini.lookup.entry.*;

import javax.swing.*;
import java.rmi.*;
import java.awt.*;
import net.jini.core.lookup.*;
import org.wonderly.swing.*;

public class MultiServiceUIFactory implements JComponentFactory {
	Color color;
	public MultiServiceUIFactory( Color c ) {
		color = c;
	}
	public JComponent getJComponent( Object svcItem ) {
		Object svc = ((ServiceItem)svcItem).service;
		final DemoInterface dif = (DemoInterface)svc;
		final JLabel l = new JLabel("Service-00-00");
		new ComponentUpdateThread(l) {
			public Object construct() {
				try {
					return( "Service-"+dif.getGroup()+" = "+dif.getInstance());
				} catch( Exception ex ) {
					return( ex.toString() );
				}
			}
			public void finished() {
				try {
					l.setText( (String)getValue() );
					l.setForeground( color );
					l.setBackground( Color.black );
					l.setOpaque(true);
					l.repaint();
				} finally {
					super.finished();
				}
			}
		}.start();
		l.setFont( new Font( "serif", Font.PLAIN, 36 ) );
		return l;
	}
}