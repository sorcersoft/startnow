package org.wonderly.awt;

import java.util.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.*;
import javax.swing.table.*;
import javax.swing.event.*;
import org.wonderly.awt.properties.*;

/**
 *  This panel provides a simple property editor for string based properties
 *
 */
public class PropertiesPanel extends JPanel {
	private Properties pDefaults, pCurrent;
	private FeatureDescriptor pKnown[];
	private Hashtable<String,Integer> hKeys;
	private PropertiesPanel me = this;
	private AbstractTableModel mod;
	private JTable tbl;
	private PropertyUIFactory fact;
	private PropertyUI propUI;
	private static PropertyUIFactory defaultFact;
	private boolean propnames;

	/**
	 *  Provide a sorting method to sort FeatureDescriptors
	 */
	private FeatureDescriptor []sortFeatDescs( FeatureDescriptor []descs ) {
		Arrays.sort( descs, new Comparator<FeatureDescriptor>() {
			public int compare( FeatureDescriptor f1, FeatureDescriptor f2 ) {
				return f1.getName().compareTo( f2.getName() );
			}
		});
		return descs;
	}

	/**
	 *  Save the current property values from the table back into the
	 *  <code>pCurrent</code> object passed in the constructor.
	 *  The UI using this object needs to provide the user either a
	 *  <code>Save</code> button, or some other procedural step that
	 *  will cause <code>save()</code> to be called.  Otherwise, the
	 *  users changes will be lost.
	 */
	public void save() {
		if( tbl.isEditing() ) {
			CellEditor ed = tbl.getCellEditor( tbl.getEditingRow(), tbl.getEditingColumn() );
			if( ed != null ) {
				ed.stopCellEditing();
			} else if( tbl.getCellEditor() != null ) {
				System.out.println("Stopping cell editing");
				tbl.getCellEditor().stopCellEditing();
			}
		}
		for( int i = 0; i < pKnown.length; ++i ) {
			FeatureDescriptor fd = pKnown[i];
			String s = fd.getName();						
			int rowIdx = ((Integer)hKeys.get( s )).intValue();
			
			System.out.println(s+" at row: "+rowIdx );
			Object p = mod.getValueAt( rowIdx, 1 );
			System.out.println("value at "+rowIdx+": "+p );
			if( p != null ) {
				if( p instanceof java.awt.Color ) {
					p = Long.toHexString((int)(((Color)p).getRGB()&0xffffff));
				}
				pCurrent.put( s, p );
			}
		}
	}

	public void abort() {
		tbl.editingStopped( new ChangeEvent( this ) );
	}

	public void setPropertyValues( Properties p ) {
		this.pCurrent = p;
		if( propUI != null ) {
			propUI.setPropertyValues( p );
			return;
		}

		Enumeration e = p.keys();
		while( e.hasMoreElements() ) {
			String s = (String)e.nextElement();
			if( hKeys.containsKey( s ) ) {
				int i = ((Integer)hKeys.get( s )).intValue();
				Object o = p.get( s );
				if( mod.getValueAt( i, 2 ) instanceof java.awt.Color ) {
					if( o == null ) {
					} else if( o instanceof java.awt.Color == false ) {
						long num = 0;
						try {
							num = Long.parseLong( o.toString(), 16 );
						} catch( NumberFormatException ex ) { }
						mod.setValueAt( new Color( (int)num ), i, 1 );
					}
				} else {
					mod.setValueAt( o, i, 1 );
				}
			}
		}
	}

	/**
	 *  Copy the current propery values into <code>p</code>
	 *  As a side effect, the current value <code>Properties</code>
	 *  object passed in the constructor is also updated.
	 */
	public void getPropertyValues( Properties p ) {
		save();
		System.out.println("Getting property values");
		if( propUI != null ) {
			propUI.getPropertyValues( p );
			return;
		}
		System.out.println("stop editing");
		tbl.editingStopped( new ChangeEvent( this ) );
		System.out.println("getting props: "+pKnown.length);
		for( int i = 0; i < pKnown.length; ++i ) {
			FeatureDescriptor fd = pKnown[i];
			String s = fd.getName();
			int j = ((Integer)hKeys.get( s )).intValue();
			Object o = mod.getValueAt( j, 1 );
			if( o != null ) {
				if( o instanceof java.awt.Color )
					o = Long.toHexString((int)(((Color)o).getRGB()&0xffffff));
				p.put( s, o );
			}
		}
	}

	/**
	 *  Constructs a properties object and fills in all the details.  
	 *
	 *	@param known Properties with "known field name"/"field description"
	 *	       pairs.
	 *	@param pCurrent Properties with "set field name"/"set value" pairs.
	 */
	public PropertiesPanel( FeatureDescriptor known[],
						Properties pCurrent, Properties pDefaults, PropertyUIFactory fact ) {
		this( known, pCurrent, pDefaults, true, fact );
	}

	/**
	 *  Constructs a properties object and fills in all the details.  
	 *
	 *	@param known Properties with "known field name"/"field description"
	 *	       pairs.
	 *	@param pCurrent Properties with "set field name"/"set value" pairs.
	 */
	public PropertiesPanel( FeatureDescriptor known[],
						Properties pCurrent, Properties pDefaults ) {
		this( known, pCurrent, pDefaults, true, null );
	}

	private Object data[][];
	private String pdata[];
	private Object cols[] = { "Name", "Value", "Default", "Description" };
	private boolean showDescrs;
	private JTextArea desc;

	private void showDescForRow( int row ) {
		desc.setText( mod.getValueAt( row, 3 ).toString() );
	}

	protected void buildDefault(final boolean showDescrs) {
		this.showDescrs = showDescrs;

		mod = new AbstractTableModel() {
            public String getColumnName(int col) {
                return propnames && col == 0 ? "Property" : cols[col].toString(); 
            }
			/*
			 * JTable uses this method to determine the default renderer/
			 * editor for each cell.  If we didn't implement this method,
			 * then the last column would contain text ("true"/"false"),
			 * rather than a check box.
			 */
			public Class getColumnClass(int c) {			 			 			 
				Object o = getValueAt( 0, c );
				if( o == null )
					return String.class;
			    return o.getClass();
			}
		
            public int getRowCount() {
            	if( me == null || me.pKnown == null )
            		return 0;
            	return me.pKnown.length;
            }
 
            public int getColumnCount() { return 3; }
            
            public Object getValueAt(int row, int col) {
            	if( data == null )
            		return null;
				return (propnames && col == 0 ) ? pdata[row] : data[row][col]; 
            }
            public boolean isCellEditable(int row, int col) { return col == 1; }
            public void setValueAt(Object value, int row, int col) {
            	if( data == null )
            		return;
                data[row][col] = value;
                fireTableCellUpdated(row, col);
            }
            public void fireTableStructureChanged() {
            	super.fireTableStructureChanged();
            }
        };
        
		tbl = new JTable( mod ) {
			public boolean editCellAt( int row, int column, EventObject ev ) {
				showDescForRow( row );
				return super.editCellAt( row, column, ev );
			}
			public boolean editCellAt( int row, int column ) {
				showDescForRow( row );
				return super.editCellAt( row, column );
			}
		};
		tbl.setAutoResizeMode( tbl.AUTO_RESIZE_LAST_COLUMN );

		tbl.addFocusListener( new FocusAdapter() {
			public void focusLost( FocusEvent ev ) {
				if( tbl.isEditing() )
					tbl.getCellEditor().stopCellEditing();
			}
		});
		tbl.setCellSelectionEnabled(false);
		tbl.setColumnSelectionAllowed(false);
		tbl.setRowSelectionAllowed(false);
		tbl.getColumnModel().setColumnSelectionAllowed(false);
		final TableCellEditor prevEditor = tbl.getCellEditor();
		// Not used yet, working on supporting java.beans editors...GGW
		final TableCellEditor newEditor = new TableCellEditor() {
			PropertyEditor curMgr;
			Vector<CellEditorListener> listeners = 
					new Vector<CellEditorListener>();
			 public Component getTableCellEditorComponent(JTable table, Object value,
			 		boolean isSelected, int row, int column ) {
			 	curMgr = PropertyEditorManager.findEditor( value.getClass() );
			 	if( curMgr == null ) {
			 		return prevEditor.getTableCellEditorComponent(table, value,
			 			isSelected, row, column );
			 	}
			 	return curMgr.getCustomEditor();
			 }
			 public Object getCellEditorValue() {
			 	return curMgr.getValue();
			 }
			 public boolean isCellEditable( EventObject ev ) {
			 	return true;
			 }
			 public boolean shouldSelectCell( EventObject ev ) {
			 	return true;
			 }
			 public boolean stopCellEditing() {
			 	return false;
			 }
			 public void cancelCellEditing() {
			 }
			 public void addCellEditorListener( CellEditorListener lis ) {
			 	listeners.addElement( lis );
			 }
			 public void removeCellEditorListener( CellEditorListener lis ) {
			 	listeners.removeElement( lis );
			 }
		};
		
		final JScrollPane jspGrid=new JScrollPane(tbl);
        		
		Packer jpk = new Packer( this );
		final JPanel lpPane = new JPanel();
		Packer lpk = new Packer( lpPane );
		
		lpk.pack( jspGrid ).fillboth().gridw(2).gridy(0).gridx(0);
		final JCheckBox names = new JCheckBox( "Show Properties" );
		names.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				propnames = names.isSelected();
				mod.fireTableStructureChanged();
				jspGrid.repaint();
			}
		});
		lpk.pack( names ).gridx(0).gridy(1).west();
		final JButton export = new JButton( "Export Properties..." );
		export.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				try {
					exportProps();
				} catch( Exception ex ) {
					JOptionPane.showMessageDialog( PropertiesPanel.this,
						ex, "Exception", JOptionPane.ERROR_MESSAGE );
				}
			}
		});
		lpk.pack( export ).gridx(1).gridy(1).east();
		lpPane.setBorder( new TitledBorder( "Defined Properties") ) ;
		
		JSplitPane sp = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
		desc = new JTextArea();
		desc.setBackground( new Color( 240,240,255 ) );
		desc.setEditable(false);
		desc.setLineWrap(true);
		JPanel descp = new JPanel();
		Packer dpk = new Packer( descp );
		dpk.pack( new JScrollPane( desc ) ).fillboth();
		descp.setBorder( BorderFactory.createTitledBorder( "Selected Property Description" ) );

		sp.setTopComponent( lpPane );
		lpPane.setMinimumSize( new Dimension( 200, 200 ) );
		sp.setBottomComponent( descp );
		sp.setDividerLocation( .5 );
		sp.revalidate();
		jpk.pack( sp ).gridx(0).gridy(0).fillboth();
	}
	
	private String lastexpdir;
	private void exportProps() throws IOException {
		JFileChooser fc = new JFileChooser();
		if( lastexpdir == null )
			lastexpdir = System.getProperty( "user.dir" );
		fc.setCurrentDirectory( new File( lastexpdir ) );
		int ret = fc.showSaveDialog( this );
		if( ret == fc.APPROVE_OPTION ) {
			lastexpdir = fc.getSelectedFile().getParent();
			System.out.println("Exporting to: "+fc.getSelectedFile() );
			FileOutputStream fo = new FileOutputStream( fc.getSelectedFile() );
			try {
				pCurrent.store( fo, "Saved Properties" );
			} finally {
				fo.close();
			}
		}
	}
	
	public PropertiesPanel( boolean showDescrs ) {
		buildDefault( showDescrs );
	}

	/**
	 *  Constructs a properties object and fills in all the details.  After
	 *  constructing the dialog, it should be displayed to the user.  Finally when
	 *  setVisible(true) returns, isSaved() can be used to determine whether the user
	 *  pressed okay, or whether they cancelled the dialog.  see the edit() method for
	 *  a convienent interface to property editing.
	 *
	 *	@param known Properties with "known field name"/"field description"
	 *	       pairs.
	 *	@param pCurrent Properties with "set field name"/"set value" pairs.
	 */
	public PropertiesPanel( FeatureDescriptor known[],
						Properties pCurrent, Properties pDefaults, boolean showDescrs, PropertyUIFactory fact ) {
		if( fact == null && defaultFact != null )
			fact = defaultFact;
	
		// Save the currently selected factory.
		this.fact = fact;

		if( fact == null ) {
			buildDefault(showDescrs);
			setPropertyData( known, pCurrent, pDefaults );
		} else {
			propUI = fact.getPropertyUI( known, pCurrent, pDefaults, this );
			JComponent c = propUI.getEditingComponent();
			Packer pk = new Packer( this );
			pk.pack( c ).fillboth();
		}
	}
	
	/** Provides for an application wide override of the default property UI factory. */
	public static void setDefaultPropertyUIFactory( PropertyUIFactory fact ) {
		defaultFact = fact;
	}
	
	public void setPropertyData( FeatureDescriptor known[],
						Properties pCurrent, Properties pDefaults ) {
		if( fact != null ) {
			propUI.setPropertyValues( pCurrent );
			return;
		}

		this.pKnown = sortFeatDescs(known);
		if( pCurrent == null )
			pCurrent = new Properties();
		this.pCurrent = pCurrent;
		this.pDefaults = pDefaults;
		hKeys=new Hashtable<String,Integer>(23);
		String s;
		Enumeration ve = pCurrent.keys();
		while( ve.hasMoreElements() ) {
			String nm = (String)ve.nextElement();
			boolean found = false;
			for( int i = 0; i < pKnown.length; ++i ) {
				FeatureDescriptor fd = pKnown[i];
				s = fd.getName();
				if( nm.equalsIgnoreCase(s) )
					found = true;
			}
		}
		Object descData[][] = null;
		descData = new Object[pKnown.length][4];
		data = descData;
		pdata = new String[pKnown.length];
		int i;
		for( i = 0; i < pKnown.length; ++i ) {
			FeatureDescriptor fd = pKnown[i];
			s = fd.getName();
			data[i][0] = fd.getDisplayName();
			pdata[i] = fd.getName();
			data[i][1] = pCurrent.get(s);
			data[i][2] = pDefaults.get(s);
			if(data[i][2] instanceof java.awt.Color ) {
				if( data[i][1] == null ) {
				} else if( data[i][1] instanceof java.awt.Color == false ) {
					long num = 0;
					try {
						num = Long.parseLong( data[i][1].toString(), 16 );
					} catch( NumberFormatException ex ) {
					}
					data[i][1] = new Color( (int)num );
				}
			}
			data[i][3] = fd.getShortDescription();
			hKeys.put( s, new Integer(i) );
		}
		// Make parameters get requeried
		tbl.setModel( mod );
		tbl.revalidate();
		tbl.repaint();
	}
	
	public int getAutoResizeMode() {
		return tbl.getAutoResizeMode();
	}
	
	public void setAutoResizeMode( int mode ) {
		tbl.setAutoResizeMode( mode ); // tbl.AUTO_RESIZE_LAST_COLUMN );
	}

}