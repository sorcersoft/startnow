package org.wonderly.util.jini;

import java.util.*;
import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.core.event.*;
import net.jini.core.lease.*;
import net.jini.entry.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.lease.*;
import java.net.*;

/**
 *  This class provides an environment container for use in Jini environments.  It is
 *  intended to allow the programmer, deployer and user to have more freedom to select
 *  from a set of environments.  This will be most useful to those using clients that
 *  run on a single device, and that device can be connected to the network through
 *  multiple means.  In the office, a docked laptop may only need multicast lookup.
 *  On the floor connected via an 802.11 wireless network, a unicast lookup server may
 *  be needed, and when at home, connected via the VPN, a specific hostname may need
 *  to be used for all exported RMI objects.  Thus, three instances of this class
 *  could be created/configured/made available to the user to select from so that they
 *  can find the desired services.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class LookupEnv implements java.io.Serializable {
	protected String hostname;
	protected String name;
	protected Vector<String> groups;
	protected Vector<LookupLocator> locs;
	protected String codebase;
	protected ServiceTemplate template;

	/**
	 *  Returns the name of this lookup environment for use in GUI
	 *  components models.
	 */
	public String toString() {
		return name;
	}
	
	public String printable() {
		String str = toString()+": ";
		str += " groups="+groups+", locators="+locs+", "+
			"codebase="+codebase+", hostname: "+hostname+", "+
			"template="+templateToString( template );
		return str;
	}
	
	/**
	 *  Constructs a new LookupEnv object with the specified name
	 *  @param name the name associated with this lookup environment.
	 */
	public LookupEnv( String name ) {
		this.name = name;
		groups = new Vector<String>();
		locs = new Vector<LookupLocator>();
	}
	
	/**
	 *  Constructs a new LookupEnv object with the specified name and template
	 *  @param name the name associated with this lookup environment.
	 *  @param templ the template that should be used for lookup server queries.
	 */
	public LookupEnv( String name, ServiceTemplate templ ) {
		this(name);
		setServiceTemplate( templ );
	}
	
	/**
	 *  Constructs a new LookupEnv object with the specified name, template and groups
	 *  @param name the name associated with this lookup environment.
	 *  @param templ the template that should be used for lookup server queries.
	 *  @param groups the Jini groups that should be used for lookup server queries and registrations.
	 */
	public LookupEnv( String name, ServiceTemplate templ, String[] groups ) {
		this(name,templ);
		for( int i = 0; i < groups.length; ++i )
			addGroup( groups[i] );
	}
	
	/**
	 *  Constructs a new LookupEnv object with the specified name, template and LookupLocator
	 *  instances.
	 *  @param name the name associated with this lookup environment.
	 *  @param templ the template that should be used for lookup server queries.
	 *  @param locs the LookupLocator instances that should be used for lookup and registration
	 */
	public LookupEnv( String name, ServiceTemplate templ, LookupLocator[] locs ) throws MalformedURLException {
		this(name,templ);
		for( int i = 0; i < locs.length; ++i )
			addLookupLocator( locs[i] );
	}
	
	/**
	 *  Constructs a new LookupEnv object with the specified name, template, groups and
	 *  LookupLocator instances.
	 *  @param name the name associated with this lookup environment.
	 *  @param templ the template that should be used for lookup server queries.
	 *  @param groups the Jini groups that should be used for lookup server queries and registrations.
	 *  @param locs the LookupLocator instances that should be used for lookup and registration
	 */
	public LookupEnv( String name, ServiceTemplate templ, String[] groups, 
			LookupLocator[] locs ) throws MalformedURLException {
		this(name,templ,groups);
		for( int i = 0; i < locs.length; ++i )
			addLookupLocator( locs[i] );
	}
	
	/**
	 *  Constructs a new LookupEnv object with the specified name, template, groups,
	 *  LookupLocator instances, hostname and codebase.
	 *  @param name the name associated with this lookup environment.
	 *  @param templ the template that should be used for lookup server queries.
	 *  @param groups the Jini groups that should be used for lookup server queries and registrations.
	 *  @param locs the LookupLocator instances that should be used for lookup and registration
	 *  @param hostname the hostname/ip address that should be used as java.rmi.server.hostname is
	 *  @param codebase the codebase that should be used as java.rmi.server.codebase is.
	 */
	public LookupEnv( String name, ServiceTemplate templ, String[] groups,
				LookupLocator[] locs, String hostname, String codebase ) throws MalformedURLException {
		this(name,templ,groups,locs);
		setHostname( hostname );
		setCodebase( codebase );
	}

	/**
	 *  Add a Jini Group to be used for accessing Lookup Servers
	 */
	public void addGroup( String group ) {
		groups.addElement(group);
	}
	
	public void addLookupLocator( LookupLocator locator ) throws MalformedURLException {
		locs.addElement( locator );
	}
	
	public void addLookupLocator( String locator ) throws MalformedURLException {
		locs.addElement( new LookupLocator( "jini://"+locator ) );
	}

	public void addLookupLocator( String locator, int port ) throws MalformedURLException {
		locs.addElement( new LookupLocator( "jini://"+locator+":"+port ) );
	}
	
	/**
	 *  Get the list of applicable Jini groups that should be used for interacting
	 *  with Lookup Services.
	 */
	public String[] getGroups() {
		String[]grps = new String[groups.size()];
		groups.copyInto( grps );
		return grps;
	}
	
	/**
	 *  Get the list of LookupLocator objects that should be used to find Lookup
	 *  Servers to use.
	 */
	public LookupLocator[] getLookupLocators() {
		LookupLocator[]l = new LookupLocator[locs.size()];
		locs.copyInto(l);
		return l;
	}
	
	/**
	 *  Sets a codebase that new implementations of RMI, such as JERI might
	 *  use to specify codebase download URLs that are applicable to this
	 *  environment.
	 */
	public void setCodebase( String base ) {
		codebase = base;
	}
	
	/**
	 *  Gets the desired codebase set up for this environment.
	 *  @see #setCodebase(String)
	 */
	public String getCodebase() {
		return codebase;
	}
	
	/**
	 *  Sets a hostname that new implementations of RMI, such as JERI might
	 *  use to specify what the server host address/name is for exported
	 *  objects.
	 */
	public void setHostname( String host ) {
		hostname = host;
	}
	
	/**
	 *  Gets the server hostname desired to be used for RMI objects exported
	 *  during the discovery phase
	 *  @see #setHostname(String)
	 */
	public String getHostname() {
		return hostname;
	}
	
	/**
	 *  Gets the ServiceTemplate object to be used to control the discovery
	 *  of applicable services in this environment.
	 *  @see #setServiceTemplate(ServiceTemplate)
	 */
	public ServiceTemplate getServiceTemplate() {
		return template;
	}
	
	/**
	 *  Sets the ServiceTemplate to use for service discovery operations
	 *  in this environment.
	 */
	public void setServiceTemplate( ServiceTemplate temp ) {
		template = temp;
	}
	
	private String templateToString( ServiceTemplate tmpl ) {
		String str = null;
		if( tmpl == null )
			return "{none}";
		if( tmpl.serviceID != null )
			str = tmpl.serviceID.toString();
		if( tmpl.serviceTypes != null ) {
			if( str != null )
				str += ", Class={";
			else
				str = "Class={";
			for( int i = 0 ;i < tmpl.serviceTypes.length; ++i ) {
				if( i > 0 )
					str += ",";
				str += tmpl.serviceTypes[i].getName();
			}
			str += "}";
		}
		if( tmpl.attributeSetTemplates != null ) {
			if( str != null )
				str += ", Entry={";
			else
				str = "Entry={";
			for( int i = 0; i < tmpl.attributeSetTemplates.length; ++i ) {
				if( i > 0 )
					str += ",";
				str += tmpl.attributeSetTemplates[i].getClass().getName()+"=[";
				str += fieldsForEntry( tmpl.attributeSetTemplates[i] );
				str += "]";
			}
		}
		return str;
	}
	
	private String fieldsForEntry( Entry ent ) { 
		Class c = ent.getClass();
		java.lang.reflect.Field f[] = c.getFields();
		String str = "";
		for( int i = 0; i < f.length; ++i ) {
			if( i > 0 )
				str += ",";
			str += f[i].getName()+"("+f[i].getType().getName()+")=";
			try {
				str += f[i].get(ent);
			} catch( IllegalAccessException ex ) {
				str += "<Value inaccessible>";
			}
		}
		
		return str;
	}
}
