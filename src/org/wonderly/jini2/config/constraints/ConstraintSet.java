package org.wonderly.jini2.config.constraints;

import java.io.Serializable;
import java.util.*;
import org.wonderly.jini2.config.ConstraintValue;

/**
 *  This class is not thread safe
 */
public class ConstraintSet implements Serializable {
	protected String name;
	protected ArrayList<ConstraintValue> constraints;
	
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}

	public boolean equals( Object obj ) {
		if( obj instanceof ConstraintSet == false )
			return false;
		return ((ConstraintSet)obj).name.equals(name);
	}

	public int hashCode() {
		return name.hashCode();
	}

	public ConstraintSet( String name ) {
		this.name = name;
		constraints = new ArrayList<ConstraintValue>(13);
	}
	
	public int getCount() {
		return constraints.size();
	}
	
	public void removeConstraintAt( int idx ) {
		constraints.remove( idx );
	}
	
	public void setConstraintAt( ConstraintValue conType, int idx ) {
		constraints.set( idx, conType );
	}
	
	public Collection<ConstraintValue> getConstraints() {
		return constraints;
	}
	
	public void addConstraint( ConstraintValue conType ) {
		constraints.add( conType );
	}
}