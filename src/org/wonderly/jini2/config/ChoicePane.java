package org.wonderly.jini2.config;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import org.wonderly.awt.*;

/**
 *  This class provides a basic list of values that can
 *  be selected from.  The list is editable using the
 *  provided buttons.  Users of this class should register
 *  action listeners for the provided button actions
 *  and the provide the correct actions.
 */
class ChoicePane<T> extends ListPane<T> {
	private JButton add, rmv, edit, make;
	ResourceBundle rb = ResourceBundle.getBundle("jiniconfig");

	public ListCellRenderer getCellRenderer() {
		return list.getCellRenderer();
	}
	public void setAddEnabled( boolean how ) {
//		new Throwable("add enabled: "+how).printStackTrace();
		add.setEnabled(how);
		add.repaint();
	}
	public void setRemoveEnabled( boolean how ) {
//		new Throwable("remove enabled: "+how).printStackTrace();
		rmv.setEnabled(how);
		rmv.repaint();
	}
	public void setEditEnabled( boolean how ) {
//		new Throwable("edit enabled: "+how).printStackTrace();
		edit.setEnabled(how);
		edit.repaint();
	}
	public void setMakeEnabled( boolean how ) {
//		new Throwable("make enabled: "+how).printStackTrace();
		make.setEnabled(how);
		make.repaint();
	}
	public void setCellRenderer( ListCellRenderer ren ) {
		list.setCellRenderer( ren );
	}
	public void setListData( Vector v ) {
		list.setListData(v);
	}
	public void clearSelection() {
		list.clearSelection();
	}
	public void addAddListener( ActionListener lis ) {
		add.addActionListener( lis );
	}
	public void addMakeListener( ActionListener lis ) {
		make.addActionListener( lis );
	}
	public void addRemoveListener( ActionListener lis ) {
		rmv.addActionListener( lis );
	}
	public void addEditListener( ActionListener lis ) {
		edit.addActionListener( lis );
	}
	public void setAddTitle( String str ) {
		add.setText( str );
	}
	public void setMakeTitle( String str ) {
		make.setText( str );
	}
	public void setUseMake( boolean how ) {
		if( !how ) {
			buts.remove( make );
		} else {
			bpk.pack( make = new JButton(rb.getString("makeButton") ) 
				).gridx(0).gridy(makey).fillx().weightx(0).inset(3,3,3,3);
		}
	}
	public void setUseEdit( boolean how ) {
		if( !how ) {
			buts.remove( edit );
		} else {
			bpk.pack( edit = new JButton(rb.getString("editButton") ) 
				).gridx(0).gridy(edity).fillx().weightx(0).inset(3,3,3,3);
		}
	}

	JPanel buts;
	Packer bpk;
	int addy, rmvy, edity, makey;
	public ChoicePane( String title ) {
		super(title);
		buts = new JPanel();
		bpk = new Packer( buts );
		int y = -1;
		bpk.pack( add = new JButton(rb.getString("addButton"))
			).gridx(0).gridy(addy=++y).fillx().weightx(0).inset(3,3,3,3);
		bpk.pack( rmv = new JButton(rb.getString("removeButton"))
			).gridx(0).gridy(rmvy=++y).fillx().weightx(0).inset(3,3,3,3);
		bpk.pack( edit = new JButton(rb.getString("editButton") ) 
			).gridx(0).gridy(edity=++y).fillx().weightx(0).inset(3,3,3,3);
		bpk.pack( make = new JButton(rb.getString("makeButton") ) 
			).gridx(0).gridy(makey=++y).fillx().weightx(0).inset(3,3,3,3);
		bpk.pack( new JPanel() ).gridx(0).gridy(++y).filly();
		pk.pack( buts ).gridx(1).gridy(0).fillboth().weightx(0);
//		list.addListSelectionListener( new ListSelectionListener() {
//			public void valueChanged( ListSelectionEvent ev ) {
//				add.setEnabled(true);
//				edit.setEnabled(list.getSelectedValue()!=null);
//				rmv.setEnabled(list.getSelectedValue()!=null);
//			}
//		});
		setAddEnabled(false);
		setMakeEnabled(false);
		setEditEnabled(false);
		setRemoveEnabled(false);
	}
}