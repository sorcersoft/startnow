package org.wonderly.jini.serviceui;

import org.wonderly.awt.*;
import org.wonderly.swing.*;
import java.lang.reflect.*;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.rmi.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.*;

import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.entry.*;
import net.jini.admin.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.lookup.ui.factory.*;
import com.sun.jini.admin.*;
import org.wonderly.swing.*;

import java.net.UnknownHostException;

/**
 *  Provides a UI Component for JoinAdmin implementations to manage
 *  the Entry values configured for the service.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class AttributesAdminUI extends UIJPanel {
	JList aList, avList;
	VectorListModel<String> aMod, avMod;
	JButton aAdd, aRmv, aEdit;
	Entry curEntry;
	Hashtable<String,Entry> attrMap = new Hashtable<String,Entry>();
	ServiceID id;

	public AttributesAdminUI( final JoinAdmin serviceInst ) {		
		setBorder(BorderFactory.createTitledBorder("Configured Service Attributes") );
		JPanel attrs = new JPanel();
		Packer atpk = new Packer( this );
		Packer apk = new Packer( attrs );
		apk.pack( new JScrollPane( aList = new JList( 
			aMod = new VectorListModel<String>() ) 
			) ).fillboth().weightx(1).gridh(4).gridx(0).gridy(0);
		JPanel vp = new JPanel();
		Packer vpk = new Packer( vp );
		vp.setBorder( BorderFactory.createTitledBorder( "Selected Entry Field Values" ) );
		vpk.pack( new JScrollPane( avList = new JList(
			avMod = new VectorListModel<String>() ) ) ).gridx(0).gridy(0).fillboth();
		atpk.pack( attrs ).fillboth().gridx(0).gridy(0);
		atpk.pack( vp ).fillboth().gridx(1).gridy(0);
		avList.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent ev ) {	
				if( ev.getValueIsAdjusting() )
					return;
				aEdit.setEnabled(avList.getSelectedValue() == null );
			}
		});
		aList.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent ev ) {	
				if( ev.getValueIsAdjusting() )
					return;
				avList.clearSelection();
				if(aList.getSelectedValue() == null )
					curEntry = null;
				else {
					avMod.removeAllElements();
					curEntry = (Entry)attrMap.get(aList.getSelectedValue());
					Field flds[] = curEntry.getClass().getFields();
					String val;
					for( int i=0;i < flds.length; ++i ) {
						try {
							Object c = flds[i].get(curEntry);
							if( c == null )
								c = "";
							val = c.toString();
						} catch( Exception ex ) {
							ex.printStackTrace();
							val = "";
						}
						avMod.addElement( flds[i].getName()+" = "+val );
					}
					avList.repaint();
				}
				aEdit.setEnabled( curEntry instanceof
					ServiceControlled == false);
				aRmv.setEnabled( curEntry instanceof
					ServiceControlled == false);
				enableEntries( curEntry != null && curEntry instanceof ServiceControlled == false );
			}
		});
		// Disallow selection in the list, without disabling it.
		avList.setSelectionModel(new DefaultListSelectionModel() {
				public int getMinSelectionIndex() {
					return -1;
				}
				public int getMaxSelectionIndex() {
					return -1;
				}
				public int getLeadSelectionIndex() {
					return -1;
				}
				public int getAnchorSelectionIndex() {
					return -1;
				}
			});
		JPanel bpan = new JPanel();
		Packer bpk = new Packer( bpan );
		apk.pack( bpan ).gridx(1).gridy(0).fillboth().weightx(0);
		bpk.pack( aAdd = new JButton( "Add" ) ).gridx(2).fillx().weightx(0).gridy(0).inset(2,2,2,2);
		aAdd.setToolTipText( "Add another Entry to this services set" );
		bpk.pack( aRmv = new JButton( "Remove" ) ).gridx(2).fillx().weightx(0).gridy(1).inset(2,2,2,2);
		bpk.pack( new JPanel() ).gridx(2).filly().weightx(0).gridy(2);
		bpk.pack( new JPanel() ).gridx(2).filly().weightx(0).gridy(4);
		bpk.pack( aEdit = new JButton( "Edit" ) ).gridx(2).fillx().weightx(0).gridy(3).inset(2,2,2,2);
		aAdd.setMargin( new Insets(0,0,0,0));
		aRmv.setMargin( new Insets(0,0,0,0));
		aEdit.setMargin( new Insets(0,0,0,0));
		aEdit.setEnabled(false);
		aRmv.setEnabled(false);
		attrs.revalidate();

		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if( ev.getSource() == aAdd) {
						final Entry[] ents = getAddedEntries( serviceInst );
						if( ents != null ) {
							runRemoteAction( aAdd, new RemoteAction() {
								public Object construct() throws Exception {
									serviceInst.addLookupAttributes( ents );
									return null;
								}
							});

							for( int i = 0; i< ents.length; ++i ) {
								aMod.addElement( ents[i].getClass().getName() );
								attrMap.put( ents[i].getClass().getName(), ents[i] );
							}
						}
				} else if( ev.getSource() == aRmv) {
					if( curEntry == null )
						return;
					final Entry[] ea = new Entry[ 1 ];
					final Entry[] ta = new Entry[ 1 ];
					ta[0] = curEntry;
					runRemoteAction( aRmv, new RemoteAction() {
						public Object construct() throws Exception {
							serviceInst.modifyLookupAttributes( ta, ea );
							return null;
						}
						public void finished() {
							curEntry = null;
							aList.clearSelection();
							loadAttrs( serviceInst );
						} 
					});
				} else if( ev.getSource() == aEdit) {
					if( curEntry == null )
						return;
					// Create an empty entry.
					final Entry[]ma = new Entry[ 1 ];
					try {
						ma[0] = (Entry)curEntry.getClass().newInstance();
						Field[]flds = curEntry.getClass().getFields();
						// Copy the values of the fields.
						for( int i = 0; i < flds.length; ++i ) {
							flds[i].set( ma[0], flds[i].get(curEntry) );
						}
					} catch( Exception ex ) {
						reportException(ex);
						return;
					}

					runRemoteAction( aEdit, new RemoteAction() {
						public Object construct() {
							return new Boolean(editEntryProperties(curEntry));
						}
						public Object failed() {
							return new Boolean(false);
						}
						public void finished(Object val) {
							if( ((Boolean)val).booleanValue() ) {
								runRemoteAction( aList, new RemoteAction() {
									public Object construct() throws Exception{
										Entry[]ea = new Entry[ 1 ];
										ea[0] = curEntry;
										System.out.println("Sending modify request to: "+serviceInst );
										serviceInst.modifyLookupAttributes( ma, ea );
										return serviceInst;
									}
									public void finished(Object val) {
										JoinAdmin adm = (JoinAdmin)val;
										if( adm != null ) {
											loadAttrs(adm);
										}
									}
								});
							}
						}
					});
				}
			}
		};
		aAdd.addActionListener(lis);
		aRmv.addActionListener(lis);
		aEdit.addActionListener(lis);
		enableEntries(false);
		configForService( serviceInst );
	}
	
	protected void enableEntries( boolean how ) {
		aEdit.setEnabled(how);
		aRmv.setEnabled(how);
	}

	private boolean editEntryProperties( Entry e ) {
		Field[]flds = e.getClass().getFields();
		Properties p = new Properties();
		Properties def = new Properties();

		// Create a Properties object with the name and value pairs
		// for each field in the object.  Also create a PropertyDescriptor
		// for the property editor to use.
		FeatureDescriptor fd[] = new FeatureDescriptor[flds.length];
		for( int i = 0; i < fd.length; ++i ) {
			try {
				fd[i] = new PropertyDescriptor( flds[i].getName(), String.class, null, null );
				Object val = flds[i].get(e);
				if( val != null )
					p.put( flds[i].getName(), val.toString() );
			} catch( IllegalAccessException ex ) {
						reportException(ex);
			} catch( IntrospectionException ex ) {
						reportException(ex);
			}
		}
		
		// Edit the fields.
		if( flds.length > 0 && PropertiesEditor.edit( getTopLevelAncestor(), fd, p, def ) ) {
			// If editing okay'd, copy the values back into the Entry object.
			for( int i = 0; i < fd.length; ++i ) {
				try {
					String prop = flds[i].getName();
					String val;
					// If no new value or existing value, don't set one.
					if( (val = p.getProperty(prop)) == null )
						continue;
					System.out.println("updating: "+prop+" to "+val );
					if( flds[i].getType() == String.class ) {
						flds[i].set( e, val );
					} else if( flds[i].getType() == Integer.class ) {
						flds[i].set( e, new Integer(val ) );
					} else if( flds[i].getType() == Long.class ) {
						flds[i].set( e, new Long(val ) );
					} else if( flds[i].getType() == Short.class  ) {
						flds[i].set( e, new Short(val ) );
					} else if( flds[i].getType() == Float.class  ) {
						flds[i].set( e, new Float(val ) );
					} else if( flds[i].getType() == Double.class ) {
						flds[i].set( e, new Double( val ) );
					} else if( flds[i].getType() == Character.class  ) {
						flds[i].set( e, new Character( val.charAt(0) ) );
					} else if( flds[i].getType() == Byte.class ) {
						flds[i].set( e, new Byte( (byte)Integer.parseInt( val ) ) );
					} else if( flds[i].getType() == Boolean.class  ) {
						flds[i].set( e, new Boolean( val.equals("true") || val.equals("yes") || val.equals("1") ) );
					} else {
						JOptionPane.showMessageDialog( getTopLevelAncestor(), 
							prop+" has type "+flds[i].getType().getName()+
								"\nThis type can not be edited here\n"+
								"This field will be ignored", 
								"Ignoring Field", 
								JOptionPane.WARNING_MESSAGE );
					}
				} catch( IllegalAccessException ex ) {
					ex.printStackTrace();
				}
			}
		} else {
			return false;
		}
		
		return true;
	}
	
	private Entry[] getAddedEntries( JoinAdmin adm ) {
		JComboBox box = new JComboBox();
		box.addItem("org.wonderly.jini.entry.DesktopIcon");
		box.addItem("org.wonderly.jini.entry.DesktopGroup");
		box.addItem("net.jini.lookup.entry.Address");
		box.addItem("net.jini.lookup.entry.Comment");
		box.addItem("net.jini.lookup.entry.Location");
		box.addItem("net.jini.lookup.entry.Name");
		box.addItem("net.jini.lookup.entry.ServiceInfo");
		box.addItem("com.sun.jini.lookup.entry.BasicServiceType");
		int idx = JOptionPane.showConfirmDialog(
			getTopLevelAncestor(), box,
			"Select an Entry Class",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE );
		if( idx == JOptionPane.CANCEL_OPTION )
			return null;
		String name = (String)box.getSelectedItem();			
		if( name == null )
			return null;
		try {
			Class c = Thread.currentThread().getContextClassLoader().loadClass( name );
			Entry ent = (Entry)c.newInstance();
			if( editEntryProperties( ent ) )
				return new Entry[] { ent };
		} catch( Exception ex ) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog( getTopLevelAncestor(), ex );
		}
		return null;
	}

	public void configForService( JoinAdmin adm ) {
		enableAll( true );
		loadAttrs( adm );					
	}
	
	void enableAll( boolean how ) {
		aList.setEnabled( how );
		aAdd.setEnabled( how );
		aRmv.setEnabled( false );
		aEdit.setEnabled( false );
	}
	
	private void loadAttrs( final JoinAdmin adm ) {
		runRemoteAction( new JComponent[]{ aList,aEdit,aAdd,aRmv },
			new RemoteAction() {
			public void setup() {
				aMod.removeAllElements();
			}
			public Object construct() throws Exception {
				attrMap = new Hashtable<String,Entry>();
				return adm.getLookupAttributes();
			}
			
			public void finished(Object val) {
				Entry[]aArr = (Entry[])val;
				if( aArr == null ) 
					return;
				for( int i = 0; i < aArr.length; ++i ) {
					if( aMod.contains( aArr[i].getClass().getName() ) == false ) {
						aMod.addElement( aArr[i].getClass().getName() );
						attrMap.put( aArr[i].getClass().getName(), aArr[i] );
					}
				}
				aList.repaint();
			}
			public void after() {
				aEdit.setEnabled( curEntry != null );
				aRmv.setEnabled( curEntry != null );
			}
		});
	}
}