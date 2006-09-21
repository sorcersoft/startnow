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
import java.io.*;
import net.jini.lease.*;
import net.jini.event.*;
import net.jini.space.*;
import net.jini.admin.*;
import net.jini.lookup.ui.factory.*;
import net.jini.lookup.ui.*;
import java.util.*;
import  org.wonderly.jini.serviceui.*;
import java.awt.*;

/**
 *  This is an example Service browser that uses some of the pieces of the startnow
 * project.
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */

public class JiniBrowse {
	public JFrame f;
//	JList list;
//	VectorListModel lmod;
	JDesktopPane desk;
	JMenu wins;
	int off = 0;
	Packer pk;

	public static void main( String args[] ) throws Exception {
		String laf = UIManager.getSystemLookAndFeelClassName();
		try {
		  	UIManager.setLookAndFeel(laf);
		    // If you want the Cross Platform L&F instead, comment out the
			// above line and uncomment the following:

		    // UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName());

		} catch (UnsupportedLookAndFeelException exc) {
		    System.err.println("Warning: UnsupportedLookAndFeel: " + laf);
		} catch (Exception exc) {
		    System.err.println("Error loading " + laf + ": " + exc);
		}
		JiniBrowse jb = new JiniBrowse();
		jb.showSelector();
		jb.f.pack();
	}

	public JiniBrowse() throws IOException {
		f = new JFrame( "Jini Service Access" );
		f.setLocation(100,100);
		JSplitPane sp = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
		
		desk = new JDesktopPane();
		JPanel p = new JPanel();
		p.setBorder( BorderFactory.createTitledBorder( "Active Services" ) );
		pk = new Packer(p);
//		JList list = new JList( lmod = new VectorListModel() );

//		pk.pack( new JScrollPane( list ) ).gridx(0).gridy(0).fillboth();
		sp.setLeftComponent( p );
		sp.setRightComponent( desk );
//		pk.pack( desk ).gridx(1).gridy(0).fillboth();
		f.setContentPane( sp );
		org.wonderly.url.vhttp.Handler.setParent(f);
		org.wonderly.url.vhttp.Handler.setCacheDir(
			System.getProperty("user.home")+java.io.File.separatorChar+"jarcache" );
		JMenuBar bar = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem mi = new JMenuItem( "Open...");
		file.add(mi);
		bar.add(file);
		f.setJMenuBar( bar );
		mi.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				try {
					openSelector();
				} catch( Exception ex ) {
					reportException(ex);
				}
			}
		});
		f.pack();
		sp.setDividerLocation( .5 );
		f.setSize( 500, 300 );
		f.setVisible(true);
	}
	
	public void showSelector( LookupEnv envs[] ) throws IOException {
		JComponent op = openSelector(envs);
		op.setMinimumSize( new Dimension( 200, 50 ) );
		pk.pack( op ).gridx(0).gridy(0).fillboth();
	}
	
	public void showSelector() throws IOException {
		JComponent op = openSelector();
		op.setMinimumSize( new Dimension( 200, 50 ) );
		pk.pack( op ).gridx(0).gridy(0).fillboth();
	}
	
	void reportException(Throwable ex ){
		ex.printStackTrace();
	}
	
	protected ServiceSelectionPane os;
	protected JComponent openSelector() throws IOException {
		LookupEnv envs[] = new LookupEnv[7];
		LookupEnv env1 = new LookupEnv( "Public serviceUI enabled",
			new ServiceTemplate( null, null, new Entry[]{new UIDescriptor()} ) );
		LookupEnv env0 = new LookupEnv( "Public Administrable",
			new ServiceTemplate( null, new Class[]{Administrable.class}, null ) );
		LookupEnv env2 = new LookupEnv( "Public Lookup Services",
			new ServiceTemplate( null, new Class[]{ServiceRegistrar.class}, null ) );
		LookupEnv env3 = new LookupEnv( "Public Event MailBoxes",
			new ServiceTemplate( null, new Class[]{EventMailbox.class}, null ) );
		LookupEnv env4 = new LookupEnv( "Public JavaSpaces",
			new ServiceTemplate( null, new Class[]{JavaSpace.class}, null ) );
		LookupEnv env5 = new LookupEnv( "Public Transaction Managers",
			new ServiceTemplate( null, new Class[]{TransactionManager.class}, null ) );
		LookupEnv env6 = new LookupEnv( "All Public Services",
			new ServiceTemplate( null, null, null ) );
		envs[0] = env0;
		envs[1] = env1;
		envs[2] = env2;
		envs[3] = env3;
		envs[4] = env4;
		envs[5] = env5;
		envs[6] = env6;
		String loc = System.getProperty("org.wonderly.jini.locator.host");
		if( loc != null ) {
			for( int i = 0; i < envs.length; ++i ) {
				envs[i].addLookupLocator( loc );
			}
		}
		return openSelector(envs);
	}

	protected JComponent openSelector( LookupEnv[]envs ) throws IOException {
		return openSelector( envs, true );
	}

	protected JComponent openSelector( LookupEnv[]envs, boolean useEnvs ) throws IOException {

		if( useEnvs ) {
			os = new ServiceSelectionPane( f, envs );
		} else {
			os = new ServiceSelectionPane( f,
				new ServiceTemplate(
					null,
					null, //new Class[] { net.jini.core.transaction.server.TransactionManager.class },
					null ), new String[]{""} );
			String loc = System.getProperty("org.wonderly.jini.locator.host");
			if( loc != null ) {
				os.addLookupServer( loc );
			}
		}
		os.addActionListener( new ActionListener() {
			public void actionPerformed( final ActionEvent ev ) {
				new ComponentUpdateThread( os ) {
					public Object construct() {
						doAction( ev );
						return null;
					}
				}.start();
			}
			void doAction( ActionEvent ev ) {
				if( ev.getID() == ServiceSelectionPane.SERVICE_SELECTED ) {
					Object[]arr = (Object[])ev.getSource();
					ServiceItem it = (ServiceItem)arr[0];
					JInternalFrame fi;
					if( (fi = (JInternalFrame)frames.get( it.serviceID )) != null ) {
						frameToFront(fi);
						return;
					}
					Entry[]ent = it.attributeSets;
					Vector<UIDWrapper> roles = new Vector<UIDWrapper>();
					String name = "<unknown>";
					for( int i = 0; i < ent.length; ++i ) {
						if( ent[i] instanceof UIDescriptor ) {
							UIDescriptor uid = (UIDescriptor)ent[i];
							roles.addElement( new UIDWrapper( uid ) );
						} else if( ent[i] instanceof Name ) {
							name = ((Name)ent[i]).name;
						} else if( ent[i] instanceof ServiceInfo ) {
							name = ((ServiceInfo)ent[i]).name;
						}
					}
					boolean admin = it.service instanceof Administrable;
					if( roles.size() > 0 ) {
						JComboBox b = new JComboBox( roles );
						int idx =
							JOptionPane.showConfirmDialog( f, b,
								"Select role to open",
								JOptionPane.OK_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE
								);
						if( idx == JOptionPane.CANCEL_OPTION )
							return;
						UIDWrapper uiw = (UIDWrapper)b.getSelectedItem();
						Component c = openRole( uiw, it.service );
						final ServiceInstance inst = new ServiceInstance( name, c, it.service );
//						lmod.addElement( inst );
						if( c instanceof JDialog ) {
							((JDialog)c).addWindowListener( new WindowAdapter() {
								public void windowClosing( WindowEvent ev ) {
//									lmod.removeElement( inst );
//									list.repaint();
								}
							});
						} else if( c instanceof JFrame ) {
							((JFrame)c).addWindowListener( new WindowAdapter() {
								public void windowClosing( WindowEvent ev ) {
//									lmod.removeElement( inst );
								}
							});
						} else if( c instanceof JWindow ) {
							((JWindow)c).addWindowListener( new WindowAdapter() {
								public void windowClosing( WindowEvent ev ) {
//									lmod.removeElement( inst );
								}
							});
						} else {
							c.addComponentListener( new ComponentListener() {
								public void componentResized( ComponentEvent ev ) {
								}
								public void componentMoved( ComponentEvent ev ) {
								}
								public void componentShown( ComponentEvent ev ) {
								}
								public void componentHidden( ComponentEvent ev ) {
//									lmod.removeElement( inst );
								}
							});
						}
					} else if( admin ) {
						openAdminFrame( (Administrable)it.service, it.serviceID, name );
					}
					ServiceRegistrar reg = (ServiceRegistrar)arr[1];
				}
			}
		});
//		JDialog dlg = new JDialog( f, "Select Service", true );
//		dlg.setContentPane( os );
//		dlg.setLocationRelativeTo( f );
//		dlg.pack();
//		dlg.addWindowListener( new WindowAdapter() {
//			public void windowClosing( WindowEvent ev ) {
//				os.shutdown();
//			}
//		});
		JMenuBar bar = os.getJMenuBar();
		f.setJMenuBar( bar );
		wins = new JMenu("Window");
		bar.add( wins );
		
		f.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev ) {
			}
		});
//		dlg.setVisible(true);
		return os;
	}
	
	void frameToFront( JInternalFrame fi ) {
		fi.moveToFront();
		try {
			fi.setSelected(true);
		} catch( PropertyVetoException ex ) {
		}
	}

	void openAdminFrame( Administrable svc, ServiceID id, String name ) {
		JComponent d = new AdminUIJFrameFactory().getJComponent( svc );
		JInternalFrame f = new JInternalFrame( name );
		f.setResizable(true);
		f.setMaximizable(true);
		f.setIconifiable(true);
		f.setClosable(true);
		desk.add( f );
		f.setContentPane( d );
		f.pack();
		f.setLocation( off, off );
		off += 30;
		f.setVisible(true);
		addInternalFrame(f, id, name);
	}
	
	Hashtable<ServiceID,JInternalFrame> frames = 
		new Hashtable<ServiceID,JInternalFrame>();
	void addInternalFrame( JInternalFrame f, final ServiceID id, String name ) {
		if( frames.get(id) == null ) {
			frames.put( id, f );
			final ServiceTopActionItem itm = new ServiceTopActionItem( name, id, f );
			wins.add( itm );
			f.addInternalFrameListener( new InternalFrameAdapter() {
				public void internalFrameClosing(InternalFrameEvent ev) {
					wins.remove(itm);
					frames.remove(id);
				}
			});
		}
	}
	
	class ServiceTopActionItem extends JMenuItem {
		JInternalFrame frame;
		ServiceID id;
		public ServiceTopActionItem( String name, ServiceID id, JInternalFrame f ) {
			super(name);
			frame = f;
			addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					frameToFront(frame);
				}
			});
		}
	}

	class ServiceInstance {
		String name;
		Component comp;
		Object ref;
		public String toString() {
			return name;
		}
		public ServiceInstance( String name, Component comp, Object ref ) {
			this.name = name;
			this.comp = comp;
			this.ref = ref;
		}
	}
	class UIDWrapper {
		UIDescriptor uid;
		String desc;
		public UIDWrapper( UIDescriptor uid ) {
			this.uid = uid;
			String el[] = uid.role.split("\\.");
			desc = uid.role;
			if( el.length > 0 )
				desc = el[el.length-1];
		}
		public String toString() {
			return desc;
		}
	}

	private Component openRole( UIDWrapper uiw, Object servItem ) {
		UIDescriptor uid = uiw.uid;
		Object fobj = null;
		Component comp = null;
		try {
			fobj = uid.getUIFactory( servItem.getClass().getClassLoader() );
		} catch( Exception ex ) {
			ex.printStackTrace();
			try {
				fobj = uid.getUIFactory( uiw.getClass().getClassLoader() );
			} catch( Exception exx ) {
				exx.printStackTrace();
				JOptionPane.showMessageDialog( f, exx );
				return null;
			}
		}

		if( fobj instanceof JComponentFactory ) {
			JInternalFrame f = new JInternalFrame( uid.role );
			comp = f;
			f.setResizable(true);
			f.setMaximizable(true);
			f.setIconifiable(true);
			f.setClosable(true);
			desk.add( f );
			JComponent d = ((JComponentFactory)fobj).getJComponent( servItem );
			f.setContentPane( d );
			f.pack();
			f.setLocation( off, off );
			off += 30;
 			f.setVisible(true);
		} else if( fobj instanceof JDialogFactory ) {
			JDialog d = ((JDialogFactory)fobj).getJDialog( servItem, f );
			comp = d;
			d.setLocationRelativeTo( f );
			d.setVisible(true);
		} else if( fobj instanceof JFrameFactory ) {
			JFrame d = ((JFrameFactory)fobj).getJFrame( servItem );
			comp = d;
			d.setVisible(true);
		} else if( fobj instanceof JWindowFactory ) {
			JWindow d = ((JWindowFactory)fobj).getJWindow( servItem );
			comp = d;
			d.setVisible(true);
//		} else if( fobj instanceof WindowFactory ) {
//			Window d = ((WindowFactory)fobj).getWindow( servItem, f );
//			d.setLocation(100,100);
//			d.setVisible(true);
		} else {
			throw new IllegalArgumentException("Roll "+fobj.getClass().getName()+" not supported");
		}
		return comp;
	}
}
