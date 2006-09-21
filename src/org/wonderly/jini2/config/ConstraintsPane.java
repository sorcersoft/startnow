package org.wonderly.jini2.config;

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.html.*;
import org.wonderly.awt.*;
import org.wonderly.swing.*;
import java.security.*;
import net.jini.core.constraint.*;
import java.lang.reflect.*;
import org.wonderly.jini2.config.constraints.*;

public class ConstraintsPane extends JPanel {
	private JList clist, contents;
	private JButton cadd, cremove, add, edit, remove; //, close;
	private Vector<ConstraintSet> cmod;
	private Vector<ConstraintValue> mod;
	private ParameterEditorFactory pef;
	private PrincipalFactory pf;
	private ConstraintFactory cf;

	public static void main( String args[] ) throws Exception {
		String laf = UIManager.getSystemLookAndFeelClassName();
		System.out.println("system laf: "+UIManager.getSystemLookAndFeelClassName() );
		try {
		  	UIManager.setLookAndFeel(laf);
		} catch (UnsupportedLookAndFeelException exc) {
		    System.err.println("Warning: UnsupportedLookAndFeel: " + laf);
		} catch (Exception exc) {
			exc.printStackTrace();
		    System.err.println("Error loading " + laf + ": " + exc);
		}
		final HashMap<String,ConstraintSet> cons =
			new HashMap<String,ConstraintSet>(13);
		ConstraintSet cl = new ConstraintSet( "ClientIn" );
		cons.put( cl.getName(), cl );
		cl = new ConstraintSet( "ClientOut" );
		cons.put( cl.getName(), cl );
		new ConstraintsPane( new ConstraintFactory() {
				public Collection<ConstraintSet> getConstraintSets() {
					return cons.values();
				}
				public void addConstraintSet( ConstraintSet s ) {
					cons.put( s.getName(), s );
				}
			}, new ParameterEditorFactory() {
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
			}).doTest();
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

	protected void reportException( final Throwable ex ) {
		ex.printStackTrace();
		runInSwing( new Runnable() {
			public void run() {
				JOptionPane.showMessageDialog( getTopLevelAncestor(), ex );
			}
		});
	}
	
	protected void runInSwing( final Runnable r ) {
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

	ConstraintSet curset;
	/**
	 *  @param consfact the constraints factory.
	 *  @param pef the factory for getting editors for constraint parameters.
	 *  @param pf the factory for the set of defined Principals.
	 */
	public ConstraintsPane( ConstraintFactory consfact, ParameterEditorFactory pef, PrincipalFactory pf ) {
		this.pef = pef;
		this.pf = pf;
		this.cf = consfact;
		Packer pk = new Packer( this );
		JSplitPane sp = new JSplitPane();
		
		JPanel consgrps = new JPanel();
		consgrps.setBorder(
			BorderFactory.createTitledBorder(
				"Constraint Types" ) );
		Packer cpk = new Packer( consgrps );
		sp.setLeftComponent( consgrps );
		cmod = new Vector<ConstraintSet>();
		cpk.pack( new JScrollPane( clist = 
			new GreyedList() ) ).fillboth().gridh(3);
		cpk.pack( cadd = new JButton("New")
			).gridx(1).gridy(0).fillx().weightx(0);
		cpk.pack( cremove = new JButton("Remove")
			).gridx(1).gridy(1).fillx().weightx(0);
		cpk.pack( new JPanel() ).gridx(1).gridy(2).filly();

		cadd.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				final String str = JOptionPane.showInputDialog(
					getTopLevelAncestor(),
					"New Constraint Type" );
				if( str != null ) {
					new ComponentUpdateThread( cadd ) {
						public Object construct() {
							try {
								ConstraintSet cs = new ConstraintSet(str);
								cf.addConstraintSet( cs );
								cmod.addElement(cs);
							} catch( Exception ex ) {
								reportException(ex);
							}
							return null;
						}
						public void finished() {
							try {
								clist.setListData( cmod );
							} finally {
								super.finished();
							}
						}
					}.start();
				}
			}
		});

		cremove.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				new ComponentUpdateThread( cremove ) {
					public Object construct() {
						return null;
					}
					public void finished() {
						try {
							int idxs[] = clist.getSelectedIndices();
							for( int i = idxs.length-1 ; i >= 0; --i ) {
								cmod.removeElementAt(idxs[i]);
							}
							clist.setListData( cmod );
						} finally {
							super.finished();
							clist.clearSelection();
							cremove.setEnabled(false);
						}
					}
				}.start();
			}
		});
		JPanel conts = new JPanel();
		conts.setBorder(
			BorderFactory.createTitledBorder( 
			"Constraints" ) );
		Packer ctpk = new Packer( conts );
		mod = new Vector<ConstraintValue>();
		ctpk.pack( new JScrollPane(
			contents = new GreyedList() ) ).gridh(3).fillboth();
		contents.setEnabled(false);
		ctpk.pack( add = new JButton("Add") 
			).gridx(1).gridy(0).fillx().weightx(0);
		ctpk.pack( edit = new JButton("Edit") 
			).gridx(1).gridy(1).fillx().weightx(0);
		ctpk.pack( remove = new JButton("Remove") 
			).gridx(1).gridy(2).fillx().weightx(0);
		ctpk.pack( new JPanel() ).gridx(1).gridy(2).filly();
		add.setEnabled(false);		
		contents.setEnabled(false);
		add.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				try {
					ConsEditor ce = new ConsEditor();
					if( ce.editConstraint() ) {
						mod.addElement( ce.getConstraint() );
						curset.addConstraint( ce.getConstraint() );
						contents.setListData( mod );
					}
				} catch( RuntimeException ex ) {
					reportException(ex);
				}
			}
		});
		edit.setEnabled(false);
		remove.setEnabled(false);
		contents.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent ev ) {
				if( ev.getValueIsAdjusting() )
					return;
					
				edit.setEnabled( contents.getSelectedIndex() >= 0 );
				remove.setEnabled( contents.getSelectedIndex() >= 0 );
			}
		});
		edit.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				int idx = contents.getSelectedIndex();
				ConstraintValue ct = 
					(ConstraintValue)contents.getSelectedValue();
				ConsEditor ce = new ConsEditor(ct);
				if( ce.editConstraint() ) {
					curset.setConstraintAt( ce.getConstraint(), idx );
					mod.setElementAt( ce.getConstraint(), idx );
					contents.setListData( mod );
				}
			}
		});

		cremove.setEnabled(false);
		remove.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				int idxs[] = contents.getSelectedIndices();
				for( int i = idxs.length-1; i >= 0; --i ) {
					mod.removeElementAt(idxs[i]);
					curset.removeConstraintAt( idxs[i] );
				}
			}
		});
		clist.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent ev ) {
				if( ev.getValueIsAdjusting() )
					return;
				new ComponentUpdateThread( new JComponent[] {
					clist, contents, add, remove, cadd, edit, cremove } ) {
					public Object construct() {
						try {
							ConstraintSet cs = (ConstraintSet)clist.getSelectedValue();
							curset = cs;
							if( cs != null ) {
								Iterator<ConstraintValue> i = cs.getConstraints().iterator();
								mod.removeAllElements();
								while( i.hasNext() ) {
									mod.addElement( i.next() );
								}
							}
						} catch( Exception ex ) {
							reportException(ex);
						}
						return null;
					}
					public void finished() {
						try {
							contents.setListData( mod );
							contents.setEnabled(true);	
							add.setEnabled( clist.getSelectedIndex() >= 0 );
							cremove.setEnabled( clist.getSelectedIndex() >= 0 );
							edit.setEnabled( contents.getSelectedIndex() >= 0 );
							remove.setEnabled( contents.getSelectedIndex() >= 0 );
						} finally {
							super.finished();
						}
					}
				}.start();
			}
		});
		sp.setRightComponent( conts );
		pk.pack( sp ).gridx(0).gridy(0).fillboth();
		pk.pack( new JSeparator() ).gridx(0).gridy(1).fillx();
//		pk.pack( close = new JButton("Close") ).gridx(0).gridy(2);
		new ComponentUpdateThread<Iterator<ConstraintSet>>( clist ) {
			public Iterator<ConstraintSet> construct() {
				try {
					Collection<ConstraintSet> c = cf.getConstraintSets();
					return c.iterator();
				} catch( Exception ex ) {
					reportException(ex);
				}
				return null;
			}
			public void finished() {
				try {
					Iterator<ConstraintSet> i = getValue();
		
					while( i != null && i.hasNext() ) {
						cmod.addElement( i.next() );
					}
					clist.setListData(cmod);
				} finally {
					super.finished();
				}
			}
		}.start();
	}

	class GreyedTextArea extends JEditorPane {
		public void setEnabled( boolean how ) {
			super.setEnabled(how);
			setOpaque(how);
		}
	}
	class GreyedList extends JList {
		public GreyedList() {
			super();
		}
		public GreyedList( ListModel mod ) {
			super(mod);
		}
		public void setEnabled( boolean how ) {
			super.setEnabled(how);
			setOpaque(how);
		}
	}

	class BooleanSelector extends BaseSelector implements Selector {
		boolean val;
		JRadioButton yes, no;
		public void setValue( boolean val ) {
			this.val = val;
			if( val )
				yes.setSelected(true);
			else
				no.setSelected(true);
		}
		public boolean getValue() {
			return val;
		}
		public String stringDescr() {
			return desc+"."+(val ? "YES" : "NO");
		}
		public BooleanSelector( String desc, ConstraintType type ) {
			super( desc, type );
			Packer pk = new Packer( comp );
			pk.pack( yes = new JRadioButton( desc+".YES" )
				).gridx(0).gridy(0).fillx();
			pk.pack( no = new JRadioButton( desc+".NO" )
				).gridx(0).gridy(1).fillx();
			ButtonGroup grp = new ButtonGroup();
			grp.add(yes);
			grp.add(no);
			yes.setSelected(val=true);
			ActionListener lis = new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					val = yes.isSelected();
				}
			};
			yes.addActionListener( lis );
			no.addActionListener(lis);
		}
	}
	
	class MillisValuedSelector extends LongValuedSelector {
		public MillisValuedSelector( String desc, ConstraintType type ) {
			super(desc, type);
		}

		public String stringDescr() {
			String str = desc+" = ";
			if( val < 60000 ) {
				str += val+" millis";
			} else if( val < 60000*1000 ) {
				str += (val/1000.0)+" secs";
			} else {
				str += (val/60000.0)+" mins";
			}
			return str;
		}
	}

	class LongValuedSelector extends BaseSelector {
		long val;
		JRadioButton yes, no;
		JSpinner fld;
		SpinnerModel mod;

		public long getValue() {
			return val;
		}
		public void setValue( long val ) {
			this.val = val;
			mod.setValue( new Long(val) );
		}
		public String stringDescr() {
			return val+"";
		}
		public LongValuedSelector( String desc,
				ConstraintType type ) {
			super( desc, type );
			Packer pk = new Packer( comp );
			pk.pack( new JLabel( desc+" value:") 
				).gridx(0).gridy(0).fillx().inset(3,3,3,3);
			pk.pack( fld = new JSpinner( 
				mod = new SpinnerNumberModel()
				) ).gridx(0).gridy(1).fillx();
			fld.addChangeListener( new ChangeListener() {
				public void stateChanged( ChangeEvent ev ) {
					val = ((Number)mod.getValue()).longValue();
				}
			});
		}
	}

	class ParameterEditor {
		Selector sel;
		String clsName;
		public ParameterEditor( String className, Selector sel ) {
			this.clsName = className;
			this.sel = sel;
		}
	}

	class ParameterizedSelector extends BaseSelector {
		JComboBox types;
		Class[] c;
		Selector tsel;
		CardLayout tcards;
		JPanel vp;
		Vector<Selector> selList = new Vector<Selector>();
		public ParameterizedSelector( String desc, ConstraintType type ) {
			super( desc, type );

			if( type instanceof ParameterizedConstraint == false ) {
				throw new IllegalArgumentException( "type must be instanceof "+
					ParameterizedConstraint.class.getName() );
			}

			ParameterizedConstraint pc = (ParameterizedConstraint)type;
			Packer pk = new Packer(comp);
			types = new JComboBox();
			pk.pack( types ).gridx(0).gridy(0).fillx();
			c = pc.getClasses();

			for( int i = 0; i < c.length; ++i ) {
				if( c[i].isArray() == false ) {
					types.addItem( c[i].getName() );
				} else {
					types.addItem( c[i].getComponentType().getName()+" [ ]");
				}
			}

			types.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					int idx = types.getSelectedIndex();
					tcards.show( vp, ""+idx );
				}
			});

			vp = new JPanel();
			vp.setBorder( BorderFactory.createTitledBorder("Value") );
			pk.pack( vp ).gridx(0).gridy(1).fillboth();
			Packer vpk = new Packer();
			tcards = new CardLayout();
			vp.setLayout( tcards );

			for( int i = 0; i < c.length; ++i ) {
				Selector ts = pef.getSelector( c[i], type );
				if( ts != null ) {
					selList.addElement(ts);
					vp.add( ""+i, ts.getEditor() );
				} else {
					selList.addElement( new NullSelector( type ) );
					vp.add( ""+i, new JPanel() );
				}
			}
		}
		
		class NullSelector extends BaseSelector {
			public NullSelector( ConstraintType type ) {
				super( "Null", type );
			}
			public String stringDescr() {
				return "<null>";
			}
		}

		public String stringDescr() {
			return "unknown";
		}
	}
	
	class ParameterizedConstraint extends ConstraintType {
		Class[]classes;
		ParameterizedSelector sel;
		String desc;
		public ParameterizedConstraint( String desc,
				String className, Class[]types ) {
			clsName = className;
			this.desc = desc;
			classes = types;
			sel = new ParameterizedSelector(
				desc, this );
		}
		public Class[] getClasses() {
			return classes;
		}
		public void setValue( ConstraintValue val ) {
		}
		public Selector getSelector() {
			return sel;
		}
	}
	
	class InvocationSelector extends BaseSelector {
		JComboBox types;
		Class[][] c;
		Selector tsel;
		CardLayout tcards;
		JPanel vp;
		Vector<Selector> selList = new Vector<Selector>();
		public InvocationSelector( String desc, ConstraintType type ) {
			super( desc, type );

			if( type instanceof InvocationConstraint == false ) {
				throw new IllegalArgumentException( "type must be instanceof "+
					InvocationConstraint.class.getName() );
			}

			InvocationConstraint pc = (InvocationConstraint)type;
			Packer pk = new Packer(comp);
			types = new JComboBox();
			pk.pack( types ).gridx(0).gridy(0).fillx();
			c = pc.getClasses();

			for( int i = 0; i < c.length; ++i ) {
				Class[]cc = c[i];
				if( cc[0].isArray() == false ) {
					types.addItem( cc[0].getName() );
				} else {
					types.addItem( cc[0].getComponentType().getName()+" [ ]");
				}
			}

			types.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					int idx = types.getSelectedIndex();
					tcards.show( vp, ""+idx );
				}
			});

			vp = new JPanel();
			vp.setBorder( BorderFactory.createTitledBorder("Value") );
			pk.pack( vp ).gridx(0).gridy(1).fillboth();
			Packer vpk = new Packer();
			tcards = new CardLayout();
			vp.setLayout( tcards );

			for( int i = 0; i < c.length; ++i ) {
				Class cc[] = c[i];
				Selector ts = pef.getSelector( cc[0], type );
				if( ts != null ) {
					selList.addElement(ts);
					vp.add( ""+i, ts.getEditor() );
				} else {
					selList.addElement( new NullSelector( type ) );
					vp.add( ""+i, new JPanel() );
				}
			}
		}
		
		class NullSelector extends BaseSelector {
			public NullSelector( ConstraintType type ) {
				super( "Null", type );
			}
			public String stringDescr() {
				return "<null>";
			}
		}

		public String stringDescr() {
			return "unknown";
		}
	}

	class InvocationConstraint extends ConstraintType {
		Class[][]classes;
		String desc;
		InvocationSelector sel;
		public InvocationConstraint( String desc,
				String className, Class[][]types ) {
			clsName = className;
			this.desc = desc;
			classes = types;
		}
		public Class[][] getClasses() {
			return classes;
		}
		public void setValue( ConstraintValue val ) {
		}
		public Selector getSelector() {
			return null;
		}
	}
	
	class BooleanConstraint extends ConstraintType {
		BooleanSelector sel;
		public BooleanConstraint( String desc, String className, String info ) {
			this( desc, className );
			this.info = info;
		}
		public BooleanConstraint( String desc, String className ) {
			clsName = className;
			sel = new BooleanSelector( desc, this );
		}
		public void setValue( ConstraintValue val ) {
			sel.setValue( ((BooleanSelector)val.getSelector()).getValue() );
		}
		public Selector getSelector() {
			return sel;
		}
	}
	
	class MillisValuedConstraint extends LongValuedConstraint {
		public MillisValuedConstraint( String desc, String className ) {
			super(desc,className);
			sel = new MillisValuedSelector(desc, this);
		}
	}

	class LongValuedConstraint extends ConstraintType {
		LongValuedSelector sel;
		public LongValuedConstraint( String desc, String className ) {
			clsName = className;
			sel = new LongValuedSelector(desc, this);
		}
		public Selector getSelector() {
			return sel;
		}
		public void setValue( ConstraintValue val ) {
			sel.setValue( ((LongValuedSelector)val.getSelector()).getValue() );
		}
	}

	protected ConstraintType[] getConstraints() {
		return new ConstraintType[] {
			new BooleanConstraint("ClientAuthentication",
				"ClientAuthentication",
"<html>Represents a constraint on authentication\r"+
"of the client to the server.\r"+
"<p>Network authentication by a client (to a server)\r"+
"is scoped and controlled by the client's Subject\r"+
"The client's subject is the current subject \r"+
"associated with the thread making the remote call.\r"+
"</html>" 
			),
			new ParameterizedConstraint( "ClientMaxPrincipal", 
				"ClientMaxPrincipal", 
				new Class[] {
					Collection.class,
					Principal.class,
					(new Principal[]{}).getClass()
				}
			),
			new ParameterizedConstraint( "ClientMaxPrincipalType",
				"ClientMaxPrincipalType",
				new Class[] {
					Collection.class,
					Class.class,
					(new Class[]{}).getClass()
				}
			),
			new ParameterizedConstraint( "ClientMinPrincipal",
				"ClientMinPrincipal", 
				new Class[] {
					Collection.class,
					Principal.class,
					(new Principal[]{}).getClass()
				}
			),
			new ParameterizedConstraint( "ClientMinPrincipalType",
				"ClientMinPrincipalType",
				new Class[] {
					Collection.class,
					Class.class,
					(new Class[]{}).getClass()
				}
			),
			new BooleanConstraint("Confidentiality",
				"Confidentiality"),
			new MillisValuedConstraint("ConnectionAbsoluteTime", 
				"ConnectionAbsoluteTime"),
			new MillisValuedConstraint("ConnectionRelativeTime",
				"ConnectionRelativeTime"),
			new BooleanConstraint("Delegation", 
				"Delegation"),
			new MillisValuedConstraint("DelegationAbsoluteTime",
				"DelegationAbsoluteTime"),
			new MillisValuedConstraint("DelegationRelativeTime",
				"DelegationRelativeTime"),
			new BooleanConstraint("Integrity",
				"Integrity"),
			new BooleanConstraint("ServerAuthentication",
				"ServerAuthentication"),
			new ParameterizedConstraint( "ServerMinPrincipal",
				"ServerMinPrincipal", 
				new Class[] {
					Collection.class,
					Principal.class,
					(new Principal[]{}).getClass()
				}
			),
			new InvocationConstraint( "InvocationConstraint",
				"InvocationConstraint",
				new Class[][] {
					new Class[] { 
						Collection.class, 
						Collection.class 
					},
					new Class[] { 
						new InvocationConstraints[]{}.getClass(),
						new InvocationConstraints[]{}.getClass() 
					},
					new Class[] { 
						net.jini.core.constraint.InvocationConstraints.class, 
						net.jini.core.constraint.InvocationConstraints.class 
					},
						
				}
			)
		};
	}

	class ConsEditor extends JDialog {
		CardLayout cards;
		boolean cancelled;
		ConstraintType sel;

		public boolean editConstraint() {
			pack();
			setLocationRelativeTo( ConstraintsPane.this );
			setVisible(true);
			return !cancelled;
		}

		public ConstraintValue getConstraint() {
			if( cancelled )
				return null;
			Selector s = sel.getSelector();
			return s.getConstraintDeclaration();
		}

		public ConsEditor( ConstraintValue val ) {
			this();
			ConstraintType[]cons = getConstraints();
			ConstraintType c = val.getConstraintType();
			if( c == null ) {
				throw new NullPointerException(
					"type not provided for: "+
					val.getClass().getName() );
			}
			int idx = 0;
			for( int i = 0; i < cons.length; ++i ) {
//				System.out.println(
//					"cons[i].getClass(): "+
//					cons[i].getClass().getName()+
//					" == "+c.getClass().getName() );
				if( cons[i].getClass() == c.getClass() ) {
					if( cons[i].getName().equals(c.getName()) ) {
						idx = i;
						break;
					}
				}
			}
			cs.setSelectedIndex( idx );
			cs.setEnabled(false);
			sel.setValue( val );
		}
			
		JComboBox cs;
		public ConsEditor() {
			super( (JDialog)getTopLevelAncestor(), "Select Constraint", true );
			ConstraintType[]cons = null;
			cs = new JComboBox( cons = getConstraints() );
			final GreyedTextArea info = new GreyedTextArea();
			info.setEditable( false );
			info.setContentType( "text/html" );
			Packer pk = new Packer( getContentPane() );
			pk.pack( cs ).gridx(0).gridy(0).fillx().inset(4,4,4,4);
			final JPanel ip = new JPanel();
			
			Packer ipk = new Packer( ip );
			ipk.pack( new JScrollPane( info ) ).fillboth();
			ip.setBorder( 
				BorderFactory.createTitledBorder( 
				"Constraint Description" ) );
			pk.pack( ip ).gridx(0).gridy(1).fillboth();
//			pk.pack( new JPanel() ).gridx(0).gridy(1).gridh(2).filly();
			
			final JPanel sp = new JPanel();

			cs.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					sel = (ConstraintType)cs.getSelectedItem();
					info.setText( sel.getInfo() );
					info.setEnabled( sel.hasInfo() );
					ip.setEnabled( sel.hasInfo() ); 
					sp.setBorder( BorderFactory.createTitledBorder( sel.getName()+" Setup" ) );
					if( sel.getSelector() != null ) {
						cards.show( sp,
							sel.getSelector().getDescriptor() );
					} else {
						cards.show( sp, "$blank$" );
					}
				}
			});

			pk.pack( sp ).gridx(1).gridy(0).gridh(2).fillboth();
			sp.setLayout( cards = new CardLayout() );
			for( int i = 0; i < cons.length; ++i ) {
				Selector s = cons[i].getSelector();
				if( s != null )
					sp.add( s.getDescriptor(), s.getEditor() );
			}
			JPanel bp = new JPanel();
			bp.setBorder( BorderFactory.createEtchedBorder() );
			sp.add( "$blank$", new JPanel() );

			sel = (ConstraintType)cs.getSelectedItem();
			pk.pack( new JSeparator() ).gridx(0).gridy(2).gridw(2).fillx();

			final JButton okay = new JButton("Okay");
			okay.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					cancelled = false;
					setVisible(false);
				}
			});

			final JButton cancel = new JButton("Cancel");
			cancel.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					cancelled = true;
					setVisible(false);
				}
			});

			addWindowListener( new WindowAdapter() {
				public void windowClosing( WindowEvent ev ) {
					cancelled = true;
				}
			});
			cs.setSelectedIndex(0);

			pk.pack( okay ).gridx(0).gridy(3).west();
			pk.pack( cancel ).gridx(1).gridy(3).east();
		}
	}
}