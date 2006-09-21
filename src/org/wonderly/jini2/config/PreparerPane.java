package org.wonderly.jini2.config;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;
import java.security.*;

import org.wonderly.awt.*;
import org.wonderly.swing.*;
import java.lang.reflect.*;
import org.wonderly.jini2.config.constraints.*;
import java.io.*;

public class PreparerPane extends JPanel {

	public static void main( String args[] ) throws Exception {
		final HashMap<String,ConstraintSet> cons = 
			new HashMap<String,ConstraintSet>(13);
		new PreparerPane("Lookup Server", new ConstraintFactory() {
				public Collection<ConstraintSet> getConstraintSets() {
					return cons.values();
				}
				public void addConstraintSet( ConstraintSet s ) {
					cons.put( s.getName(), s );
				}
			} ).doTest();
	}
	protected void doTest() {
		JFrame f = new JFrame( "Test" );
		Packer pk = new Packer( f.getContentPane() );
		pk.pack( this ).fillboth();
		f.pack();
		f.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev ) {
				System.exit(1);
			}
		});
		f.setLocationRelativeTo( null );
		f.setVisible(true);
	}

	protected PreparerPane() {
		type = "Preparer";
	}

	GrayableTextField login;
	JCheckBox dologin;
	ModalComponents mc;
	JComboBox cls, cons;
	JButton edit;
	String type = "";
	ConstraintFactory consfact;

	public PreparerPane( String tl, ConstraintFactory cf ) {
		this();
		preparePane( tl, cf );
	}

	protected ClassItem[] getClasses() {
		return new ClassItem[] {
			new ClassItem("BasicProxyPreparer", 
				"net.jini.security.BasicProxyPreparer")
		};
	}
	
	protected class ClassItem {
		String name;
		String cls;

		public String toString() {
			return name;
		}

		public ClassItem( String str, String clsnm ) {
			name = str;
			this.cls = clsnm;
		}
	}

	protected void preparePane( String tl, ConstraintFactory cf ) {
		consfact = cf;
		setBorder( BorderFactory.createTitledBorder( tl+" "+type ) );
		Packer pk = new Packer( this );
		JPanel lp = prepareLeftPane(tl);
		JPanel rp = prepareRightPane(tl);
		pk.pack( lp ).gridx(0).gridy(0).fillboth().weightx(0);
		pk.pack( rp ).gridx(1).gridy(0).fillboth();
	}
	
	protected JCheckBox cb;
	protected JPanel prepareLeftPane(String tl) {
		JPanel lp = new JPanel();
		Packer lpk = new Packer(lp);
		lpk.pack( cb = new JCheckBox( "Use "+tl+" "+type+"?") 
			).gridx(0).gridy(0).top().west().inset(4,4,4,4);
		lpk.pack( new JPanel() ).gridx(0).gridy(1).filly();
		return lp;
	}
	
	protected JPanel prepareRightPane(String tl ) {
		JLabel l;
		JPanel rp = new JPanel();
		Packer rpk = new Packer(rp);
		int y = -1;
		mc = new ModalComponents( cb );
		rp.setBorder( BorderFactory.createEtchedBorder() );
		rpk.pack( l = new JLabel( "Class:") ).gridx(0).gridy(++y).east().inset(2,2,2,2);
		mc.add(l);
		rpk.pack( cls = new JComboBox() ).gridx(1).gridy(y).fillx().gridw(2);
		ClassItem[]clss = getClasses();
		for( int i = 0; i < clss.length; ++i ) {
			cls.addItem( clss[i] );
		}
		mc.add(cls);
		rpk.pack( l = new JLabel( "Constraints:" ) ).gridx(0).gridy(++y).east().inset(2,2,2,2);
		mc.add(l);
		rpk.pack( cons = new JComboBox() ).gridx(1).gridy(y).fillx();
		mc.add(cons);
		rpk.pack( edit = new JButton( "Edit") ).gridx(2).gridy(y);
		mc.add( edit);
		edit.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				editConstraints();
			}
		});
		rpk.pack( dologin = new JCheckBox( "Login Context" ) ).gridx(0).gridy(++y).east();//.inset(2,2,2,2);
		mc.add(dologin);
		rpk.pack( login = new GrayableTextField() {
			public void setEnabled( boolean how ) {
				super.setEnabled( how && dologin.isSelected() );
			}
		} ).gridx(1).gridy(y).fillx().gridw(2);
		mc.add(login);
		dologin.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				login.setEnabled( dologin.isSelected() );
			}
		});
		mc.configure();
		login.setEnabled(dologin.isSelected() );
		rpk.pack( new JPanel() ).gridx(0).gridy(++y).filly();
		return rp;
	}
	
	void editConstraints() {
		final JDialog dlg = new JDialog( (JFrame)getTopLevelAncestor(),
			"Edit Constraints", true );
		Packer pk = new Packer( dlg.getContentPane() );
		HashMap conns = new HashMap(13);
		pk.pack( new ConstraintsPane(
			consfact,
			new ParameterEditorFactory() {
				public Selector getSelector( Class cls, ConstraintType type ) {
					if( cls == Principal.class ) {
						return new PrincipalSelector("Principal", type);
					} else if( cls == Class.class ) {
						return new ClassSelector("Class", type);
					} else if( cls == (new Class[]{}).getClass() ) {
						return new ClassArraySelector("Class Array", type);
					} else if( cls == (new Principal[]{}).getClass() ) {
						return new PrincipalArraySelector("Principal", type);
					} else if ( cls == Collection.class ) {
						return new CollectionSelector( 
							"Collection for "+type.getName(), type);
					}
					return null;
				}
			}, new PrincipalFactory() {
				public Set getUsablePrincipals() throws IOException {
					return new HashSet();
				}
			}) {
		} ).gridx(0).gridy(0).fillboth().gridw(2);
		pk.pack( new JSeparator() ).gridx(0).gridy(1).fillx().gridw(2).inset(4,4,4,4);
		final JButton okay = new JButton("Okay");
		final JButton cancel = new JButton("Cancel");
		final boolean cancelled[] = new boolean[1];
		okay.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				cancelled[0] = false;
				dlg.setVisible(true);
			}
		});
		cancel.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				cancelled[0] = true;
				dlg.setVisible(true);
			}
		});
		pk.pack( okay ).gridx(0).gridy(2).west();
		pk.pack( cancel ).gridx(1).gridy(2).east();
		dlg.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev ) {
				cancelled[0] = true;
			}
		});
		dlg.pack();
		dlg.setLocationRelativeTo( getTopLevelAncestor() );
		dlg.setVisible(true);
	}

	class ModalComponents {
		Hashtable<Component,Component> comps = 
			new Hashtable<Component,Component>();
		JCheckBox modal;

		public ModalComponents( JCheckBox box ) {
			modal = box;
			box.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					configure();
				}
			});
		}

		public void configure() {
			boolean sel = modal.isSelected();
			Enumeration e = comps.keys();
			while( e.hasMoreElements() ) {
				Component comp = (Component)e.nextElement();
				comp.setEnabled( sel );
			}
		}

		public void add( Component comp ) {
			comps.put( comp, comp );
		}
	}

	class GrayableTextField extends JTextField {
		public GrayableTextField( String str ) {
			super( str );
		}
		public GrayableTextField() {
		}
		public void setEnabled( boolean how ) {
			super.setEnabled(how);
			setOpaque(how);
		}
	}
}