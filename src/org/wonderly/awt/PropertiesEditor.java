package org.wonderly.awt;

import java.util.Properties;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.wonderly.awt.properties.*;
import org.wonderly.awt.Packer;

/**

PropertiesEditor serves as a generic interface for allowing the editing
of a Properties structure.  It acts as a modal dialog, presenting the
properties in an intuitive tabular structure for editing.  If given a
second properties structure filled with descriptions, it also provides
tooltips.

@author C2 Technologies, Inc
@version 4.0
*/
public class PropertiesEditor extends JDialog {
	JButton ok, cancel;
	boolean saved;
	PropertiesPanel ppan;

	
	public int getAutoResizeMode() {
		return ppan.getAutoResizeMode();
	}
	
	public void setAutoResizeMode( int mode ) {
		ppan.setAutoResizeMode( mode ); // tbl.AUTO_RESIZE_LAST_COLUMN );
	}

// I left this in.  Just in case testing is desired.
	public static void main(String[] args) {
		JFrame parent=new JFrame();
		parent.setSize( 800, 600 );
		parent.addWindowListener(
			new WindowAdapter() {
				public void windowClosing( WindowEvent e ) {
					System.exit(0);
				}
			});
		parent.setVisible(true);
		Properties def=new Properties();
		Properties c=new Properties();
		FeatureDescriptor feats[] = new FeatureDescriptor[100];
		int cnt = 0;

		FeatureDescriptor fd = new FeatureDescriptor();
		feats[cnt++] = fd;
		fd.setName( "foo" );
		fd.setShortDescription( "A foo Type" );
		fd.setDisplayName( "Foo Value" );

		fd = new FeatureDescriptor();
		feats[cnt++] = fd;
		fd.setName( "bar" );
		fd.setShortDescription( "A bar Type" );
		fd.setDisplayName( "Bar Value" );

		for( int i = cnt; i < feats.length; ++i ) {	
			fd = new FeatureDescriptor();
			feats[i] = fd;
			fd.setName( "bar_"+i );
			fd.setShortDescription( "A bar"+i+" Type" );
			fd.setDisplayName( "Bar#"+i+" Value" );
			def.put( "bar_"+i, "value"+i );
		}

		c.put( "foo", "white" );  // Force at least one property to be edited.
		def.put( "foo", "green" );
		def.put( "bar", "yellow" );
		edit( parent, feats, c, def );
		// List the properties that had changed values...
		c.list( System.out );
		System.exit(0); 
	}

	class ActionHandler implements ActionListener {
		Properties props;
		public ActionHandler( Properties toSave ) {
			props = toSave;
		}

		public void actionPerformed( ActionEvent ev ) {
			if ( ev.getSource() == ok ) {
				// Fill in pCurrent from the fields.
				saved=true;
				ppan.save();
				ppan.getPropertyValues( props );
				setVisible( false );
			} else if ( ev.getSource() == cancel ) {
				saved=false;
				ppan.abort();
				setVisible( false );
			}
		}
	}

	/**
	 *  Provides a simple interface to create a properties dialog, opening
	 *  it and returning the result of whether the user saved the properties or not.
	 *
	 *	@param jfParent Component over which to lay the edit dialog.
	 *	@param pKnown Properties with "known field name"/"field description"
	 *	       pairs.
	 *	@param pCurrent Properties with "set field name"/"set value" pairs.
	 *
	 *	@see java.util.Properties
	 *  @see javax.swing.JDialog
	 */
	public static boolean edit( Component jfParent, FeatureDescriptor pKnown[],
							Properties pCurrent, Properties pDefaults ) {
		return edit( jfParent, pKnown, pCurrent, pDefaults, null );
	}

	/**
	 *  Provides a simple interface to create a properties dialog, opening
	 *  it and returning the result of whether the user saved the properties or not.
	 *
	 *	@param jfParent Component over which to lay the edit dialog.
	 *	@param pKnown Properties with "known field name"/"field description"
	 *	       pairs.
	 *	@param pCurrent Properties with "set field name"/"set value" pairs.
	 *
	 *	@see java.util.Properties
	 *  @see javax.swing.JDialog
	 */
	public static boolean edit( Component jfParent, FeatureDescriptor pKnown[],
							Properties pCurrent, Properties pDefaults, JComponent helpBtn ) {
		PropertiesEditor ed = new PropertiesEditor(jfParent, pKnown, pCurrent, pDefaults, helpBtn );
		ed.setVisible(true);
		return ed.isSaved();
	}
	
	/**
	 *  Called after the dialog is closed to determine if the user
	 *  selected okay or cancel.
	 *
	 *  @return true if okay was selected, false if cancel was selected or the window
	 *               was closed.
	 */
	public boolean isSaved() {
		return saved;
	}

	/**
	 *  Constructs a properties object and fills in all the details.  After
	 *  constructing the dialog, it should be displayed to the user.  Finally when
	 *  setVisible(true) returns, isSaved() can be used to determine whether the user
	 *  pressed okay, or whether they cancelled the dialog.  see the edit() method for
	 *  a convienent interface to property editing.
	 *
	 *	@param jfParent Component over which to lay the edit dialog.
	 *	@param pKnown Properties with "known field name"/"field description"
	 *	       pairs.
	 *	@param pCurrent Properties with "set field name"/"set value" pairs.
	 *
	 *  @see #edit
	 */
	public PropertiesEditor( Component jfParent, FeatureDescriptor pKnown[],
							Properties pCurrent, Properties pDefaults ) {
		this( jfParent, pKnown, pCurrent, pDefaults, null );
	}

	/**
	 *  Constructs a properties object and fills in all the details.  After
	 *  constructing the dialog, it should be displayed to the user.  Finally when
	 *  setVisible(true) returns, isSaved() can be used to determine whether the user
	 *  pressed okay, or whether they cancelled the dialog.  see the edit() method for
	 *  a convienent interface to property editing.
	 *
	 *	@param jfParent Component over which to lay the edit dialog.
	 *	@param pKnown Properties with "known field name"/"field description"
	 *	       pairs.
	 *	@param pCurrent Properties with "set field name"/"set value" pairs.
	 *	@param helpBtn a button or other component to provide for help with property
	 *		           entry.  When this is non-null, the description column of the
	 *                 dialog will not be provided.
	 *  @see #edit
	 */
	public PropertiesEditor( Component jfParent, FeatureDescriptor pKnown[],
							Properties pCurrent, Properties pDefaults, JComponent helpBtn ) {
		this( jfParent, pKnown, pCurrent, pDefaults, helpBtn, null );
	}

	/**
	 *  Constructs a properties object and fills in all the details.  After
	 *  constructing the dialog, it should be displayed to the user.  Finally when
	 *  setVisible(true) returns, isSaved() can be used to determine whether the user
	 *  pressed okay, or whether they cancelled the dialog.  see the edit() method for
	 *  a convienent interface to property editing.
	 *
	 *	@param jfParent Component over which to lay the edit dialog.
	 *	@param pKnown Properties with "known field name"/"field description"
	 *	       pairs.
	 *	@param pCurrent Properties with "set field name"/"set value" pairs.
	 *	@param helpBtn a button or other component to provide for help with property
	 *		           entry.  When this is non-null, the description column of the
	 *                 dialog will not be provided.
	 *  @see #edit
	 */
	public PropertiesEditor( Component jfParent, FeatureDescriptor pKnown[],
				Properties pCurrent, Properties pDefaults, JComponent helpBtn, PropertyUIFactory fact ) {
		super( JOptionPane.getFrameForComponent( jfParent ), "Edit Properties", true );
		if( pCurrent == null )
			throw new NullPointerException( "pCurrent must be non-null" );
		ppan = new PropertiesPanel( pKnown, pCurrent, pDefaults, helpBtn == null, fact );
		JPanel jpPane = new JPanel();
		Packer jpk = new Packer( jpPane );
		ActionHandler ah = new ActionHandler(pCurrent);
		
		ok = new JButton( "OK" );
		cancel = new JButton( "Cancel" );
		ok.addActionListener( ah );
		cancel.addActionListener( ah );

		JPanel jpButtons=new JPanel();
		jpButtons.add( ok );
		if( helpBtn != null ) {
			jpButtons.add( helpBtn );
		}
		jpButtons.add( cancel );
		jpk.pack( ppan ).gridx(0).gridy(0).fillboth();
		jpk.pack( jpButtons ).gridx(0).gridy(1).fillx();
		jpPane.setPreferredSize( new Dimension( 250, 200 ) );
		setContentPane( jpPane );
		addWindowListener( new WindowAdapter() {
				public void windowClosing(WindowEvent ev) {
					saved = false;
				}
			}
			);
		pack();
		if( jfParent != null )
			setLocationRelativeTo( jfParent );
		setSize( 500, 350 );
		setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
	}
}
