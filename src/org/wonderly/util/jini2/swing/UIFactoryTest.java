package org.wonderly.util.jini2.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.*;
import org.wonderly.util.jini2.*;
import net.jini.config.*;
import org.wonderly.awt.*;
import org.wonderly.util.jini.LookupEnv;
import net.jini.core.lookup.*;
import net.jini.core.discovery.*;
import org.wonderly.util.jini2.ServiceLookup;

public abstract class UIFactoryTest {
	final Logger log = Logger.getLogger( getClass().getName() );
	protected ServiceLookup lu;

	public abstract JDesktopComponentFactory getFactory();

	public UIFactoryTest( String title, String args[], LookupEnv env ) throws Exception {
		System.setSecurityManager( new java.rmi.RMISecurityManager() );
		Configuration config = ConfigurationProvider.getInstance( args );

		final JFrame d = new JFrame( title );
		final JPanel p = new JPanel();
		final Packer pk = new Packer(p);
		d.setContentPane(p);

		final JDesktopComponentFactory fa = getFactory();
		final Object st = new Object();
		lu = new ServiceLookup(
			env,
			new ServiceLookupHandler() {
				public void serviceLost( ServiceID id, ServiceRegistrar reg ) {
				}
				public void updateItem( ServiceItem item, ServiceRegistrar reg ) {
				}
				public void processItem( ServiceItem item, ServiceRegistrar reg ) {
					p.removeAll();
					pk.pack( fa.getJComponent( item ) ).fillboth();
					d.pack();
					d.setLocationRelativeTo(null);
					d.setVisible(true);
					synchronized(st) {
						st.notify();
					}
				}
			}, log, config );
		lu.start();
		d.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev ) {
				d.dispose();
				log.info("Shuting down lookup");
				lu.shutdown();
			}
		});
		synchronized(st) {
			 st.wait();
		}
		log.info("exiting startup");
	}
}