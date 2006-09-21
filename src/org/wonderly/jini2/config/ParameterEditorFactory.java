package org.wonderly.jini2.config;

public interface ParameterEditorFactory {
	/**
	 *  @param cls the class to get a Selector for
	 */
	public Selector getSelector( Class cls, ConstraintType type );
}