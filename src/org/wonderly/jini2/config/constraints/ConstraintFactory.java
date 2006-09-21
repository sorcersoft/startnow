package org.wonderly.jini2.config.constraints;

import java.util.Collection;
import java.io.IOException;

public interface ConstraintFactory {
	public Collection<ConstraintSet> getConstraintSets() throws IOException;
	public void addConstraintSet( ConstraintSet conset ) throws IOException;
}