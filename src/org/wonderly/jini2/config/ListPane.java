package org.wonderly.jini2.config;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import org.wonderly.awt.*;
import java.util.logging.*;

/**
 *  This is a typed JList panel that uses a TitledBorder.
 */
class ListPane<T> extends JPanel {
	protected JList list;
	protected Vector<T> mod = new Vector<T>();
	protected Packer pk;
	Logger log = Logger.getLogger( getClass().getName() );
	
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
	public void removeAllElements() {
		mod.removeAllElements();
		list.setListData(mod);
	}
//	public void add( final Object itm ) {
//		log.finer( this+": adding entry "+itm );
//		runInSwing( new Runnable() {
//			public void run() {
//				mod.addElement(itm);
//				list.setListData( mod );
//				list.repaint();
//			}
//		});
//	}
	public void add( final T itm ) {
		log.finer( this+": adding set "+itm );
		runInSwing( new Runnable() {
			public void run() {
				mod.addElement(itm);
				list.setListData( mod );
				list.repaint();
			}
		});
	}
	public boolean contains( T itm ) {
		return mod.contains(itm);
	}

	public T getSelectedValue() {
		return mod.elementAt(list.getSelectedIndex());
	}

	public void clear() {
		runInSwing( new Runnable() {
			public void run() {
				mod.removeAllElements();
				list.setListData(mod);
			}
		});
	}
	public void addListSelectionListener( ListSelectionListener lis ) {
		list.addListSelectionListener(lis);
	}
	public Vector<T> getValues() {
		return mod;
	}
	public void setEnabled( boolean how ) {
//		new Throwable("Listpane enabed: "+how).printStackTrace();
		list.setEnabled(how);
	}
	public ListPane( String title ) {
		setBorder( BorderFactory.createTitledBorder( title ) );
		pk = new Packer(this);
		pk.pack( new JScrollPane( list = new JList() {
			public void setEnabled( boolean how ) {
				super.setEnabled(how);
				setOpaque(how);
			}
			}) ).gridx(0).gridy(0).fillboth();
	}
}