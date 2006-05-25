package org.wonderly.awt.properties;

import java.util.Properties;
import org.wonderly.awt.PropertiesPanel;
import javax.swing.JComponent;

/**
 *  This factory is used by the PropertyPanel.  When specified, it is used to create the
 *  property editing container instead of the default table oriented editor.
 */
public interface PropertyUI {
	public JComponent getEditingComponent();
	/** Fills in the passed Properties object with the current values. */
	public void getPropertyValues( Properties toFill );
	/** Sets the current values to those values passed. */
	public void setPropertyValues( Properties values );
}