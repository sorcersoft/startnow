package org.wonderly.jini2.config;

import net.jini.config.*;
import java.util.*;

/**
 *  This class manages a ConfigurationSet, version 
 *  and ServiceEntry tuple
 */
class ConfigurationEntry {
	protected ServiceEntry svc;
	protected String name;
	protected ConfigurationSet conf;
	protected int version;
	protected ResourceBundle rb = ResourceBundle.getBundle("jiniconfig");

	public ServiceEntry getServiceEntry() {
		return svc;
	}
	
	public void setConfigurationSet( ConfigurationSet cs ) {
		conf = cs;
	}

	public ConfigurationEntry( ServiceEntry se, ConfigurationSet config ) {
		svc = se;
		conf = config;
		this.name = se.name;
	}
	
	public String getName() {
		return name;
	}
	
	public Configuration getConfig() {
		return conf;
	}

	public String toString() {
		return svc.name + (conf == null ? " "+rb.getString("noConfigs") : "");
	}
}