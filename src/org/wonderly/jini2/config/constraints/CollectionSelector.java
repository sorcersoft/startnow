package org.wonderly.jini2.config.constraints;

import javax.swing.*;
import org.wonderly.awt.*;
import org.wonderly.jini2.config.*;

public class CollectionSelector extends BaseSelector {
	public CollectionSelector( String desc, 
			ConstraintType type ) {
		super( desc, type );
		Packer pk = new Packer(comp);
		pk.pack( new JLabel( 
			"Collection for "+type.getName() 
			) ).gridx(0).gridy(0).fillx();
		pk.pack( new JScrollPane( new JList()
			) ).gridx(0).gridy(1).fillboth().gridh(4);
		int y = 0;
		pk.pack( new JButton("Add") 
			).gridx(1).gridy(++y).fillx().weightx(0);
		pk.pack( new JButton("Remove")
			).gridx(1).gridy(++y).fillx().weightx(0);
		pk.pack( new JButton("Edit") 
			).gridx(1).gridy(++y).fillx().weightx(0);
		pk.pack( new JPanel()
			).gridx(1).gridy(++y).fillx().weightx(0);
	}
	public String stringDescr() {
		return "Collection for "+type.getName();
	}
}