package org.wonderly.jini.serviceui;

import java.beans.*;
import java.util.*;

/**
 *  This class encapsulates the description of a property.  Unfortunately,
 *  the FeatureDescriptor class has nothing but transient fields in it, and
 *  when you want to send them across the pond to the client, all the values
 *  disappear.  So, this class subclasses FeatureDescriptor and restores 
 *  Serializability of all the appropriate fields needed for this application.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class PropertyDescriptorImpl extends FeatureDescriptor implements java.io.Serializable {
	private String name;
	private String display;
	private String descr;
	private boolean hidden;
	private boolean expert;
	private Hashtable<String,Object> items = new Hashtable<String,Object>(7);	

	public PropertyDescriptorImpl( PropertyDescriptor pd ) throws IntrospectionException {
		setName(pd.getName());
		setShortDescription(pd.getShortDescription());
		setHidden(pd.isHidden());
		setExpert(pd.isExpert());
		if( pd.getDisplayName() != null )
			setDisplayName(pd.getDisplayName());
		else
			setDisplayName( pd.getName() );
	}
	
	/**
	 *  Returns the name of the feature
	 */
    public String getName() {
    	return name;
    }
    
    /**
     *  Sets the name of the feature
     */
    public void setName(String nm) {
    	name = nm;
    }
    
    /**
     *  Returns the user visible name of the property.
     */
    public java.lang.String getDisplayName() {
    	return display;
    }
    
    /**
     *  Sets the user visible name of the property.
     */
    public void setDisplayName(String dsp) {
    	display = dsp;
    }

    /**
     *  OverCyte does not use this part of the FeatureDescriptor API
     */
    public boolean isExpert() {
    	return expert;
    }

    /**
     *  OverCyte does not use this part of the FeatureDescriptor API
     */
    public void setExpert(boolean exp) {
    	expert = exp;
    }

    /**
     *  OverCyte does not use this part of the FeatureDescriptor API
     */
    public boolean isHidden() {
    	return hidden;
    }

    /**
     *  OverCyte does not use this part of the FeatureDescriptor API
     */
    public void setHidden(boolean hide) {
    	hidden = hide;
    }

     /**
     *  Gets the help text that will be visible to the user to
     *  describe how to configure this feature.
     */
   public String getShortDescription() {
    	return descr;
    }

     /**
     *  Sets the help text that will be visible to the user to
     *  describe how to configure this feature.
     */
    public void setShortDescription(String desc) {
    	descr = desc;
    }

    /**
     *  OverCyte does not use these values.  They can be specified for other uses,
     *  put are not used in the OverCyte system currently.
     */
    public void setValue(String item, Object val) {
    	items.put( item, val );
    }

    /**
     *  @see #setValue(String,Object)
     */
    public Object getValue(String name) {
    	return items.get(name);
    }

    /**
     *  OverCyte does not use these values.  They can be specified for other uses,
     *  put are not used in the OverCyte system currently.
     */
    public Enumeration<String> attributeNames() {
    	return items.keys();
    }
}