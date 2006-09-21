package org.wonderly.jini2.config;

import java.io.Serializable;

public class ConstraintValue implements Serializable {
	protected String val;
	transient Selector sel;
	transient ConstraintType type;

	public Selector getSelector() {
		return sel;
	}

	public ConstraintType getConstraintType() {
		return type;
	}

	public ConstraintValue( String val, Selector sel, ConstraintType type ) {
		this.val = val;
		this.sel = sel;
		this.type = type;
	}

	public String toString() {
		return val;
	}

	public Selector selector() {
		return sel;
	}
}