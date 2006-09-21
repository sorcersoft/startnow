package org.wonderly.jini2.config;

import org.wonderly.awt.*;
import org.wonderly.swing.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;
import net.jini.id.*;

import net.jini.config.*;
import java.io.*;
import org.wonderly.jini2.*;
import org.wonderly.util.jini.LookupEnv;
import org.wonderly.util.jini2.*;
import org.wonderly.jini2.config.*;
import org.wonderly.jini2.config.constraints.*;
import net.jini.core.lookup.*;
import net.jini.core.discovery.*;
import net.jini.core.entry.*;
import net.jini.lookup.entry.*;
import net.jini.core.lookup.*;
import java.rmi.*;
import java.net.*;
import net.jini.admin.*;
import org.wonderly.util.jini2.ServiceLookup;

public class JiniConfigEditor 
		extends ConfigurableJiniApplication {
	JFrame frm;
	URL configImg = new URL( "file:///"+System.getProperty(
		"user.dir")+"/images/config.jpg" );
	URL nonConfigImg = new URL( "file:///"+System.getProperty(
		"user.dir")+"/images/noconfig.jpg");
	URL unConfigImg = new URL( "file:///"+System.getProperty(
		"user.dir")+"/images/unconfig.jpg");
	ResourceBundle rb = ResourceBundle.getBundle("jiniconfig");
	ServiceLookup sl, cl;
	Vector configs = new Vector();
	Vector<ManagedConfiguration> confServers = new Vector<ManagedConfiguration>();

	public static void main( String args[] ) throws Exception {
		String laf = UIManager.getSystemLookAndFeelClassName();
		System.out.println("system laf: "+UIManager.getSystemLookAndFeelClassName() );
		try {
		  	UIManager.setLookAndFeel(laf);
//		    // If you want the Cross Platform L&F instead, comment out the
//			// above line and uncomment the following:
//
			Object obj[] = UIManager.getInstalledLookAndFeels();
			for( int i = 0; i < obj.length; ++i ) {
				System.out.println("lnf["+i+"]: "+obj[i]);
			}	
			obj = UIManager.getAuxiliaryLookAndFeels();
			for( int i = 0; obj != null && i < obj.length; ++i ) {
				System.out.println("aux lnf["+i+"]: "+obj[i]);
			}
			System.out.println("cross platform: "+
				UIManager.getCrossPlatformLookAndFeelClassName());
			System.out.println("system laf: "+
				UIManager.getSystemLookAndFeelClassName() );
//		    UIManager.setLookAndFeel( 
//		  		UIManager.getCrossPlatformLookAndFeelClassName());

		} catch (UnsupportedLookAndFeelException exc) {
		    System.err.println("Warning: UnsupportedLookAndFeel: " + laf);
		} catch (Exception exc) {
			exc.printStackTrace();
		    System.err.println("Error loading " + laf + ": " + exc);
		}
		final JiniConfigEditor ed = new JiniConfigEditor( args );
		ed.frm.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev ) {
				ed.log.warning(Thread.currentThread()+": main window closed");
				System.out.println("Window closed");
				System.exit(1);
			}
		});
		final Object lock = new Object();
		try {
			ed.log.fine(Thread.currentThread()+": Waiting on forever lock");
			synchronized( lock ) {
				lock.wait();
			}
			ed.log.fine(Thread.currentThread()+": Woke on forever lock");
		} catch( Exception ex ){
			ed.log.log( Level.SEVERE, ex.toString(), ex );
		}
		ed.log.severe(Thread.currentThread()+": Leaving main, goodbye...");
	}
	
	ServiceEntry curService;
	void setConfigs( final ServiceEntry se ) {
		curService = se;
		confs.repaint();
	}

	JComboBox endPoints, exporters, preparers;
	ChoicePane<ConstraintSet> srvConstraints, clientConstraints;
	ChoicePane<ServiceEntry> known;
	ChoicePane<ConfigurationSet> confs;
//	JList confs, known;
	HashMap cons = new HashMap(13);
	Vector<ServiceEntry> svcs = new Vector<ServiceEntry>();
	ConfigurationEditor confEdit;

	public JiniConfigEditor( String args[] ) 
			throws ConfigurationException, IOException {
		super(args);
		frm = new JFrame(rb.getString("frameTitle"));
		confEdit = new ConfigurationEditor( frm, log );
		Packer pk = new Packer( frm.getContentPane() );
		JPanel bp = new JPanel();
		Packer bpk = new Packer( bp );
		bpk.pack( confs = new ChoicePane<ConfigurationSet>( 
			rb.getString("configPane") )
				).gridx(0).gridy(0).fillboth();
		confs.setEnabled(false);
		JSplitPane jsp = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
		pk.pack( jsp ).gridx(0).gridy(0).fillboth();
		jsp.setBottomComponent(bp);
		confs.setUseMake(false);
		jsp.setMinimumSize( new Dimension( 400, 500 ) );
		known = new ChoicePane<ServiceEntry>(	rb.getString("servicesBorder")); //JList();
		known.setUseEdit(false);
		known.setUseMake(false);
		final ListCellRenderer orend = known.getCellRenderer();
		final ListCellRenderer ocrend = confs.getCellRenderer();
		known.setCellRenderer( new ListCellRenderer() {
			public Component getListCellRendererComponent(
				JList list, Object value, int idx, boolean sel, boolean focus ) {
				Component c = orend.getListCellRendererComponent(
					list, value, idx, sel, focus );
				JLabel l = (JLabel)c;
				ServiceEntry en = (ServiceEntry)value;
				if( en.isConfigurable() ) {
					ConfigurableId id = findConfigurableId( en );
					String name = id+"";
					if( id != null )
						name = id.getName();
					l.setText( en.getName()+" - ("+
						name+")" );
					l.setIcon( new ImageIcon(configImg) );
				} else {
					try {
						if( en.isAdministrable() ) {
							l.setText( en.getName() +" - (not configurable)");
							l.setIcon( new ImageIcon(nonConfigImg) );
						} else {
							l.setIcon( new ImageIcon(unConfigImg) );
							l.setText( en.getName() +" - (not administrable)");
						}
					} catch( RemoteException ex ) {
						log.log(Level.SEVERE, ex.toString(), ex);
						l.setIcon( new ImageIcon(nonConfigImg) );
					}
				}
//				if( !sel && !focus ) {
//					l.setForeground( Color.black );
//					l.setOpaque(false);
//				}
				return l;
			}
		});
		confs.setCellRenderer( new ListCellRenderer() {
			public Component getListCellRendererComponent(
				JList list, Object value, int idx, boolean sel, boolean focus ) {
				Component c = orend.getListCellRendererComponent(
					list, value, idx, sel, focus );
				JLabel l = (JLabel)c;
				ConfigurationSet cs = (ConfigurationSet)value;
				ConfigurableId cid = findConfigurableId( curService );
				if( cid != null ) {
					if( cid.equals( cs.id ) ) {
						l.setForeground( Color.blue );
					} else {
						l.setForeground( Color.gray );
					}
				} else {
					l.setForeground( Color.gray );
				}
				return l;
			}
		});
		known.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent ev ) {
				final ServiceEntry se = known.getSelectedValue();
				final ConfigurationSet cs = (ConfigurationSet)confs.getSelectedValue();
				if( ev.getValueIsAdjusting() )
					return;

				log.fine( "known selected: "+se+", cs: "+cs );
				if( se != null ) {
					log.finer("enabling known selection");
//					known.setMakeEnabled(findConfigurableId(se) == null); 
					known.setAddEnabled(cs != null && findConfigurableId(se) == null); 
					known.setRemoveEnabled(findConfigurableId(se) != null); 
//					known.setEditEnabled(findConfigurableId(se) != null); 
					setConfigs(se);
				} else {
					log.finer("disabling known selection");
					known.setAddEnabled(false);
					known.setRemoveEnabled(false);
					known.setEditEnabled(false);
				}
			}
		});
		confs.addAddListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				new ComponentUpdateThread( known ) {
					public Object construct() {
						try {
							ConfigurationSet set = 
								confEdit.editConfigurationSet(null);
							if( set != null ) {
								storeConfigurationSet( set );
							}
						} catch( Exception ex ) {
							reportException(ex);
						}
						return null;
					}
					public void finished() {
						super.finished();
//						known.configWithConfigSelected(true);
						confs.clearSelection();
						confs.setEditEnabled(false);
						confs.setAddEnabled(true);
						confs.setRemoveEnabled(false);
					}
				}.start();
			}
		});
		known.setAddTitle( "Set Config");
		known.setRemoveEnabled(false);
//		known.setMakeEnabled(false);
		known.setAddEnabled(false);
		confs.setAddEnabled(true);
		confs.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent ev ) {
				ConfigurationSet cs = 
					(ConfigurationSet)confs.getSelectedValue();
				final ServiceEntry se = (ServiceEntry)known.getSelectedValue();
				log.fine("confs selected: "+cs+", se: "+se );
				known.setAddEnabled( cs != null && se != null && 
					findConfigurableId( se ) == null );
				known.setRemoveEnabled( cs != null && se != null && 
					findConfigurableId( se ) != null );
				confs.setEditEnabled( cs != null );
				confs.setRemoveEnabled( cs != null );
			}
		});
//			known.addMakeListener( new ActionListener() {
//				public void actionPerformed( ActionEvent ev ) {
//					ServiceEntry se = (ServiceEntry)known.getSelectedValue();
//					if( se == null ) {
//	//					known.setMakeEnabled(false);
//						return;
//					}
//					log.fine("known-make: "+se+", se.itm: "+se.itm );
//					if( se == null || se.itm == null || 
//							se.itm.service instanceof Administrable == false ) {
//						reportError(rb.getString("noAdmin"));
//						return;
//					}
//					makeServiceConfigurable( se );
//					known.clearSelection();
//				}
//			});
		known.addRemoveListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				ServiceEntry se = (ServiceEntry)known.getSelectedValue();
				if( se.itm.service instanceof Administrable == false ) {
					reportError(rb.getString("noAdmin"));
					return;
				}
				removeServiceConfigurable( se );
			}
		});
		known.addAddListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				ServiceEntry se = (ServiceEntry)known.getSelectedValue();
				if( se.itm.service instanceof Administrable == false ) {
					reportError(rb.getString("noAdmin"));
					return;
				}
				ConfigurationSet cs = (ConfigurationSet)
					confs.getSelectedValue();
				setServiceConfigurable( se, cs.id );
			}
		});
//		known.addMakeListener( new ActionListener() {
//			public void actionPerformed( ActionEvent ev ) {
//				ServiceEntry se = (ServiceEntry)known.getSelectedValue();
//				if( se.itm.service instanceof Administrable == false ) {
//					reportError(rb.getString("noAdmin"));
//					return;
//				}
//				ConfigurationSet cs = (ConfigurationSet)
//					confs.getSelectedValue();
//				removeServiceConfigurable( se );
//				known.clearSelection();
//			}
//		});
		confs.addRemoveListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				final ConfigurationSet cs = (ConfigurationSet)
					confs.getSelectedValue();
				log.fine("removing config set: "+cs );
				new ComponentUpdateThread( confs ) {
					public Object construct() {
						try {
							removeConfigurationSet( cs );
						} catch( Exception ex ) {
							reportException(ex);
						}
						return null;
					}
					public void finished() {
						super.finished();
						confs.clearSelection();
						confs.setEditEnabled(false);
						confs.setAddEnabled(true);
						confs.setRemoveEnabled(false);
					}
				}.start();
			}
		});
		confs.addEditListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				final ConfigurationSet cs = (ConfigurationSet)
					confs.getSelectedValue();
				new ComponentUpdateThread( confs ) {
					public Object construct() {
						try {
							ConfigurationSet set = 
								confEdit.editConfigurationSet(cs);
							if( set != null ) {
								storeConfigurationSet(set );
							}
						} catch( Exception ex ) {
							reportException(ex);
						}
						return null;
					}
					public void finshed() {
						super.finished();
						confs.clearSelection();
						confs.setEditEnabled(false);
						confs.setAddEnabled(true);
						confs.setRemoveEnabled(false);
					}
				}.start();
			}
		});
		jsp.setTopComponent( known );
		
		log.fine("Final open of window frame");
		frm.pack();
		jsp.setDividerLocation(.5);
		jsp.revalidate();
		
		frm.setLocationRelativeTo( null );
		frm.setVisible(true);
		log.fine( "Starting service lookup...");
		known.setMinimumSize( known.getPreferredSize() );
		confs.setMinimumSize( confs.getPreferredSize() );
		sl = new ServiceLookup( new LookupEnv(
				rb.getString("AllServices"),
				new ServiceTemplate( null, null, null
//				new Entry[] {
//					new ConfigurableId( null )
//				}
				),
				new LookupLocator[] {
					new LookupLocator("jini://localhost")
				}
			), 
			new ServiceLookupHandler() {
				public synchronized void serviceLost( ServiceID id, ServiceRegistrar reg ) {
					log.fine(rb.getString("serviceLost")+": "+id );
					ServiceEntry en = new ServiceEntry( id.toString(), 
						new ServiceItem( id, null, null ) );
					if( svcs.contains(en) == true )
						svcs.removeElement(en);
					known.setListData( svcs );
				}
				public synchronized void updateItem( ServiceItem item, ServiceRegistrar reg ) {
					log.fine(rb.getString("updateService")+": "+sl.descItem(item) );
					String name = ServiceEntry.getName(item);
					ServiceEntry en = new ServiceEntry( name, item );
					int idx = svcs.indexOf( en );
					if( idx == -1 ) {
						svcs.addElement(en);
					} else {
						en = (ServiceEntry)svcs.elementAt(idx);
						en.updateItem(item);
					}
					runInSwing( new Runnable() {
						public void run() {
							known.setListData( svcs );
						}
					});
				}
				public synchronized void processItem(
						ServiceItem item, ServiceRegistrar reg ) {
					log.fine(rb.getString("processService")+": "+sl.descItem(item) );
					String name = ServiceEntry.getName(item);
					ServiceEntry en = new ServiceEntry( name, item );
					if( svcs.contains(en) == false ) {
						log.finer("Adding initial entry for service to svcs list");
						svcs.addElement(en);
					} else {
						log.finer("updating ServiceItem to new value" );
						int idx = svcs.indexOf( en );
						ServiceEntry se = (ServiceEntry)svcs.elementAt(idx);
						se.itm = en.itm;
					}
					runInSwing( new Runnable() {
						public void run() {
							known.setListData( svcs );
						}
					});
				}
			}, 
			getLogger(),
			conf);
		sl.start();

		cl = new ServiceLookup( new LookupEnv(
				"Config Services",
				new ServiceTemplate( null, new Class[] {
					ManagedConfiguration.class
					}, null
//				new Entry[] {
//					new ConfigurableId( null )
//				}
				),
				new LookupLocator[] {
					new LookupLocator("jini://localhost")
				}
			), 
			new ServiceLookupHandler() {
				public synchronized void serviceLost( ServiceID id, ServiceRegistrar reg ) {
					log.fine(rb.getString("serviceLost")+": "+id );
					ServiceEntry en = new ServiceEntry( id.toString(), 
						new ServiceItem( id, null, null ) );
					if( svcs.contains(en) == true )
						svcs.removeElement(en);
					known.setListData( svcs );
				}
				public synchronized void updateItem( 
						ServiceItem item, ServiceRegistrar reg ) {
					log.fine(rb.getString("updateService")+": "+sl.descItem(item) );
					final ManagedConfiguration mc =
						(ManagedConfiguration)item.service;
					confs.removeAllElements();
					new ComponentUpdateThread<Vector<ConfigurationSet>>( confs ) {
						public Vector<ConfigurationSet> construct() {
							try {
								log.fine( "Get Configuration keys at: "+mc );
								List cids = mc.getConfigurationKeys();
								log.fine(mc+": returns keys: "+cids);
								Iterator i = cids.iterator();
								Vector<ConfigurationSet> a = 
									new Vector<ConfigurationSet>();
								while( i.hasNext() ) {
									ConfigurableId id = (ConfigurableId)i.next();
									log.fine("Got ID for: "+id );
									ConfigurationSet cs = mc.getConfiguration( id );
									a.addElement(cs);
								}
								return a;
							} catch( Throwable ex ) {
								log.log(Level.SEVERE, ex.toString(),ex);
								reportException(ex);
							}
							return null;
						}

						public void finished() {
							try {
								Vector<ConfigurationSet> csv = getValue();
								if( csv != null ) {
									Iterator<ConfigurationSet> i = csv.iterator();
									while( i.hasNext() ) {
										ConfigurationSet cs = i.next();
										log.fine("Got Configuration Set for: "+cs );
										if( confs.contains( cs ) == false )
											confs.add(cs);
									}
								}
								if( confServers.contains( mc ) == false )
									confServers.add( mc );
							} finally {
								super.finished();
								confs.clearSelection();
								confs.setEditEnabled(false);
								confs.setRemoveEnabled(false);
							}
						}
					}.start();
				}
				public synchronized void processItem(
						ServiceItem item, ServiceRegistrar reg ) {
					log.fine("Config "+rb.getString("processService")+": "+sl.descItem(item) );
					String name = ServiceEntry.getName(item);
//					if( configs.contains( name ) == false ) {
//						configs.addElement( name );
//						confs.setListData( configs );
//					}
					final ManagedConfiguration mc =
						(ManagedConfiguration)item.service;
					new ComponentUpdateThread<Vector<ConfigurationSet>>( confs ) {
						public Vector<ConfigurationSet> construct() {
							try {
								log.fine( "Get Configuration keys at: "+mc );
								List cids = mc.getConfigurationKeys();
								log.fine(mc+": returns keys: "+cids);
								Iterator i = cids.iterator();
								Vector<ConfigurationSet> a = new Vector<ConfigurationSet>();
								while( i.hasNext() ) {
									ConfigurableId id = (ConfigurableId)i.next();
									log.fine("Got ID for: "+id );
									ConfigurationSet cs = mc.getConfiguration( id );
									a.addElement(cs);
								}
								return a;
							} catch( Throwable ex ) {
								log.log(Level.SEVERE, ex.toString(),ex);
								reportException(ex);
							}
							return null;
						}

						public void finished() {
							try {
								Vector<ConfigurationSet> csv = getValue();
								if( csv != null ) {
									Iterator<ConfigurationSet> i = csv.iterator();
									while( i.hasNext() ) {
										ConfigurationSet cs = (ConfigurationSet)i.next();
										log.fine("Got Configuration Set for: "+cs );
										if( confs.contains( cs ) == false )
											confs.add(cs);
									}
								}
								if( confServers.contains( mc ) == false )
									confServers.add( mc );
							} finally {
								super.finished();
								confs.clearSelection();
								confs.setEditEnabled(false);
								confs.setRemoveEnabled(false);
							}
						}
					}.start();
//					ServiceEntry en = new ServiceEntry( name, item );
//					if( svcs.contains(en) == false ) {
//						log.finer("Adding initial entry for service to svcs list");
//						svcs.addElement(en);
//					} else {
//						log.finer("updating ServiceItem to new value" );
//						int idx = svcs.indexOf( en );
//						ServiceEntry se = (ServiceEntry)svcs.elementAt(idx);
//						se.itm = en.itm;
//					}
//					runInSwing( new Runnable() {
//						public void run() {
//							known.setListData( svcs );
//						}
//					});
				}
			}, 
			getLogger(),
			conf);
		cl.start();
		log.fine("Service lookup started...");
	}
	
	ConfigurableId findConfigurableId( ServiceEntry se ) {
		ConfigurableId cid = null;
		if( se == null || se.itm == null )
			return null;
		Entry[] ents = se.itm.attributeSets;
		for( int i = 0; i < ents.length; ++i ) {
			if( ents[i] instanceof ConfigurableId ) {
				cid = (ConfigurableId)ents[i];
			}
		}
		
		return cid;
	}

	/**
	 *  Attempt to make the passed service Configurable.  The
	 *  User will see visible feedback via reportError if
	 *  this is not possible.  If it happens, the service will
	 *  be updated and a ServiceRegistrar notification should
	 *  occur to any listeners.
	 */
	private void makeServiceConfigurable( final ServiceEntry se ) {

		log.fine("Handling Make Configurable");
		setServiceConfigurable( se, 
				new ConfigurableId( "unknown", net.jini.id.UuidFactory.generate())
			);
	}

	/**
	 *  Attempt to make the passed service Configurable.  The
	 *  User will see visible feedback via reportError if
	 *  this is not possible.  If it happens, the service will
	 *  be updated and a ServiceRegistrar notification should
	 *  occur to any listeners.
	 */
	private void setServiceConfigurable( final ServiceEntry se,
			final ConfigurableId id ) {

		log.fine("Handling Set Configurable");
		new ComponentUpdateThread( known ) {
			public Object construct() {
				try {
					Object sobj = ((Administrable)se.itm.service).getAdmin();
					if( sobj instanceof JoinAdmin == false ) {
						reportError(rb.getString("noJoinAdmin"));
						return null;
					}
					log.fine( "setting up for JoinAdmin.addLookupAttributes()" );
					JoinAdmin ja = (JoinAdmin)sobj;
					log.fine( "Setting ConfigurableId entry to service: "+id );
					ja.addLookupAttributes( new Entry[] { id } );
				} catch( Exception ex ) {
					reportException(ex);
				}
				return null;
			}
			public void finished() {
				super.finished();
				known.clearSelection();
				known.setRemoveEnabled(false);
				known.setAddEnabled(false);
			}
		}.start();
	}

	private void removeServiceConfigurable( final ServiceEntry se ) {

		log.log(Level.FINE,"Handling Remove Configurable",new Throwable("removeServiceConfigurable"));
		new ComponentUpdateThread( known ) {
			public Object construct() {
				try {
					Object sobj = ((Administrable)se.itm.service).getAdmin();
					if( sobj instanceof JoinAdmin == false ) {
						reportError(rb.getString("noJoinAdmin"));
						return null;
					}
					log.fine( "setting up for JoinAdmin.modifyLookupAttributes()" );
					JoinAdmin ja = (JoinAdmin)sobj;
					log.fine( "Removing ConfigurableId entry to service" );
					ja.modifyLookupAttributes( new Entry[] {
						new ConfigurableId( null ) },
						new Entry[]{null} );
				} catch( Exception ex ) {
					reportException(ex);
				}
				return null;
			}
			public void finished() {
				super.finished();
				known.clearSelection();
				known.setRemoveEnabled(false);
				known.setAddEnabled(false);
			}
		}.start();
	}
	
	private void runInSwing( final Runnable r ) {
		if( SwingUtilities.isEventDispatchThread() ) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait( r );
			} catch( Exception ex ) {
			}
		}
	}

	private void storeConfigurationSet( final ConfigurationSet set ) 
			throws IOException {

		final Uuid uuid = UuidFactory.generate();
		new ComponentUpdateThread( new JComponent[] {
			confs, known } ) {
			public Object construct() {
				int cnt = 0;
				try {
					for( int i = 0; i < confServers.size(); ++i ) {
						ManagedConfiguration mc = 
							(ManagedConfiguration)confServers.elementAt(i);
						try {
							log.fine("Storing Config to: "+mc );
							mc.storeConfiguration( set );
							++cnt;
						} catch( Exception ex ) {
							log.log( Level.SEVERE, ex.toString(), ex );
						}
					}
				} catch( Exception ex ) {
					log.log(Level.SEVERE, ex.toString(), ex );
				}
				// Return stores that worked.
				return new Integer(cnt);
			}
		}.start();
	}

	private void removeConfigurationSet( final ConfigurationSet set ) 
			throws IOException {

		final String name = set.getName();
		new ComponentUpdateThread( new JComponent[] {
			confs, known } ) {
			public Object construct() {
				int cnt = 0;
				try {
					for( int i = 0; i < confServers.size(); ++i ) {
						ManagedConfiguration mc = 
							(ManagedConfiguration)confServers.elementAt(i);
						try {
							log.fine("Removing Config: "+set.id+", at: "+mc );
							mc.removeConfiguration( set.id );
							++cnt;
						} catch( Exception ex ) {
							log.log( Level.SEVERE, ex.toString(), ex );
						}
					}
				} catch( Exception ex ) {
					log.log(Level.SEVERE, ex.toString(), ex );
				}
				// Return stores that worked.
				return new Integer(cnt);
			}
		}.start();
	}

	private void reportError( String str ) {
		JOptionPane.showMessageDialog( frm, str, 
			rb.getString("errorTitle"), JOptionPane.ERROR_MESSAGE );
	}
	
	private void reportException( Throwable ex ) {
		log.log(Level.SEVERE, ex.toString(), ex);
		JOptionPane.showMessageDialog( frm, ex );
	}
}