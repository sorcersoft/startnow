package org.wonderly.jini2.config;

public abstract class ConstraintType {
	protected String clsName;
	protected String info = "";
	public String toString() {
		return clsName;
	}
	public String getName() {
		return clsName;
	}
	public String getInfo() {
		return info;
	}
	public boolean hasInfo() {
		return info.length() > 0;
	}
	public abstract Selector getSelector();
	public abstract void setValue( ConstraintValue val );
}