package org.wonderly.jini2.config;

import javax.swing.*;
import java.awt.*;

/**
 *  A basic Selector implementation that includes all
 *  the basic functionality that allows a Selector to
 *  function in the system.
 */
public abstract class BaseSelector implements Selector {
	/** A user readable description of the ConstraintType */
	protected String desc;
	/** The ConstraintType that we are editing */
	protected ConstraintType type;
	/** The base component to put everything inside of */
	protected JPanel comp;

	public BaseSelector( String desc, ConstraintType type ) {
		this.desc = desc;
		this.type = type;
		comp = new JPanel();
		comp.setBorder( BorderFactory.createTitledBorder(desc) );
	}
	public String getDescriptor() {
		return desc;
	}
	public abstract String stringDescr();

	public ConstraintValue getConstraintDeclaration() {
		return new ConstraintValue( stringDescr(), this, type );
	}
	public Component getEditor() {
		return comp;
	}
}