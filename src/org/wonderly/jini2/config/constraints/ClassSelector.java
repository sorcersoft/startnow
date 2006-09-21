package org.wonderly.jini2.config.constraints;

import javax.swing.*;
import org.wonderly.awt.*;
import org.wonderly.jini2.config.*;

public class ClassSelector extends BaseSelector {
	public ClassSelector( String desc, ConstraintType type ) {
		super( desc, type );
		Packer pk = new Packer(comp);
		pk.pack( new JLabel( "Class:" ) ).gridx(0).gridy(0).fillx();
		pk.pack( new JComboBox() ).gridx(0).gridy(1).fillx();
		pk.pack( new JPanel() ).gridx(0).gridy(2).filly();
	}
	public String stringDescr() {
		return "Class type ...";
	}
}
	