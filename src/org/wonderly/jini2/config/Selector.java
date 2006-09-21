package org.wonderly.jini2.config;

import java.awt.Component;

/**
 *  This interface encapsulates UI objects that
 *  can be selected and edited, and then turned
 *  into a ConstraintValue.
 */
public interface Selector {
	public ConstraintValue getConstraintDeclaration();
	public Component getEditor();
	public String getDescriptor();
}