package org.wonderly.awt.properties;

import java.util.Properties;
import org.wonderly.awt.PropertiesPanel;
import java.beans.FeatureDescriptor;

/**
 *  This factory is used by the PropertyPanel.  When specified, it is used to create the
 *  property editing container instead of the default table oriented editor.
 */
public interface PropertyUIFactory {
	public PropertyUI getPropertyUI( FeatureDescriptor known[],
		Properties pCurrent, Properties pDefaults, PropertiesPanel pan );
}