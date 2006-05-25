package org.wonderly.jini2.proxy;

import java.net.*;
import net.jini.security.*;
import org.wonderly.log.StreamFormatter;
import java.util.logging.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import org.xml.sax.helpers.*;
import java.lang.reflect.*;
import java.text.*;
import net.jini.security.policy.DynamicPolicy;
import java.security.*;
import net.jini.core.entry.*;
import net.jini.config.*;
import java.security.cert.*;
import java.rmi.*;
import net.jini.core.lookup.ServiceItem;
import java.security.cert.Certificate;

/**
 *  This class is a ProxyPreparer that provides the ability to
 *  match codesource to policy.  The matching is done using an
 *  XML specification.  A conglomeration of such XML is shown
 *  here.  A DTD will be forthcoming.
	<pre>

&lt;permissions&gt;
	&lt;template type="all"&gt;
		&lt;permission class="java.security.AllPermission"/&gt;
	&lt;/template&gt;
	&lt;template type="unixtmpfiles"&gt;
		// file system dependent permissions for unix file system
		&lt;permission class="java.io.FilePermission"&gt;
			&lt;arg&gt;"."&lt;/arg&gt;&lt;action&gt;"read,write,delete"&lt;/action&gt;
		&lt;/permission&gt;
		&lt;permission class="java.io.FilePermission"&gt;
			&lt;arg&gt;"./-"&lt;/arg&gt;&lt;action&gt;"read,write,delete"&lt;/action&gt;
		&lt;/permission&gt;
		&lt;permission class="java.io.FilePermission"&gt;
			&lt;arg&gt;"/tmp"&lt;/arg&gt;&lt;action&gt;"read,write,delete"&lt;/action&gt;
		&lt;/permission&gt;
		&lt;permission class="java.io.FilePermission"&gt;
			&lt;arg&gt;"/tmp/-"&lt;/arg&gt;&lt;action&gt;"read,write,delete"&lt;/action&gt;
		&lt;/permission&gt;
		&lt;permission class="java.io.FilePermission"&gt;
			&lt;arg&gt;"/var/tmp"&lt;/arg&gt;&lt;action&gt;"read,write,delete"&lt;/action&gt;
		&lt;/permission&gt;
		&lt;permission class="java.io.FilePermission"&gt;
			&lt;arg&gt;"/var/tmp/-"&lt;/arg&gt;&lt;action&gt;"read,write,delete"&lt;/action&gt;
		&lt;/permission&gt;
	&lt;/template&gt;
	&lt;template type="discovery"&gt;
		&lt;permission class="java.net.SocketPermission"&gt;
			&lt;arg&gt;"224.0.1.84"&lt;/arg&gt;&lt;action&gt;"connect,accept"&lt;/action&gt;
		&lt;/permission&gt;
		&lt;permission class="java.net.SocketPermission"&gt;
			&lt;arg&gt;"224.0.1.85"&lt;/arg&gt;&lt;action&gt;"connect,accept"&lt;/action&gt;
		&lt;/permission&gt;
		&lt;permission class="net.jini.discovery.DiscoveryPermission"&gt;
			&lt;arg&gt;"*"&lt;/arg&gt;
		&lt;/permission&gt;
	&lt;/template&gt;
	
	&lt;!-- Keystore to get signing keys from for comparison --&gt;
	&lt;keystore location="store" type="JKS"/&gt;
	
	&lt;!-- Create principal to reference below --&gt;
	&lt;principal name="user" class="principal.class.name" arg="cons-arg"/&gt;
	
	&lt;!-- A code source to match --&gt;
	&lt;codesource 
		proto="httpmd" 
		host="localhost" port="8090"
		path="/reggie-dl.jar"
		signedby="signer-cert-alias-name"
		&gt;
		&lt;!-- Passed to the grant as a required principal --&gt;
		&lt;mustbe principal="user"/&gt;

		&lt;!-- Only match when this entry with this value is present --&gt;
		&lt;when entry="net.jini.lookup.entry.Name"&gt;
			&lt;attrib name="name" value="The Service Name"/&gt;
		&lt;/when&gt;

		&lt;!-- Only match when the named entry's value is the indicated
			 configuration entry's value --&gt;
		&lt;when entry="net.jini.lookup.entry.ServiceInfo"&gt;
			&lt;attrib name="name" ref="package.name.configEntry"/&gt;
		&lt;/when&gt;

		&lt;!-- Refer to configuration somewhere else --&gt;
		&lt;config name="my.package.name" url="file:/myconfig.cfg"&gt;
			&lt;option&gt;http://startnow.jini.org/nonav/startdesk&lt;/option&gt;
		&lt;/config&gt;

		&lt;!-- or explicitly --&gt;
		&lt;config name="my.package.name"&gt;
			&lt;data&gt;
			import net.jini.core.lease.*;

			my.package.name {
				preparer = null;
				logger = null;
				logLevel = null;
	
			}
			&lt;/data&gt;
		&lt;option&gt;http://startnow.jini.org/nonav/startdesk&lt;/option&gt;
		&lt;/config&gt;

		&lt;permission class="some.specific.Permission"/&gt;
		&lt;!-- or some specific group --&gt;
		&lt;grant type="discovery"/&gt;
	&lt;/codesource&gt;
&lt;/permissions&gt;
	
	</pre>
	*/
public class DynamicPolicyProxyPreparer implements ServiceProxyPreparer   {
	// The logger
	private Logger log;
	// The descriptors found
	private Vector<CodeSource> descrs;
	// The current XML documents location
	private String curDocument;
	// The preparer found in the config or passed in
	private ProxyPreparer prep;
	private Hashtable<String,Vector<Permission>> grantTemplates;
	private KeyStore keystore;
	private Hashtable<String,Principal> prins;

	public Object prepareProxy( Object proxy ) throws RemoteException {
		return prepareProxy( proxy, null, null );
	}

	public Object prepareProxy( Object proxy, ServiceItem it, Configuration conf ) throws RemoteException {
		// If there is a basic preparer, do that up front, before
		// doing any work to find permissions and other configuration
		// for the proxy.  fail fast on error...
		Object origProxy = proxy;
		if(prep != null ) {
			log.fine( "Using preliminary ProxyPreparer: "+prep );
			proxy = prep.prepareProxy( proxy );
			log.fine( "Got returned proxy: "+proxy );
		}
			
		// Get the class loader and make sure it's a URLClassLoader
		ClassLoader cl = origProxy.getClass().getClassLoader();
		if( cl instanceof URLClassLoader == false ) {
			throw new SecurityException("Unsupported ClassLoader: "+
				cl.getClass().getName()+
				" for "+proxy.getClass().getName() );
		}
	
		// Get the URLs to use to find the codesource
		URL[]urls = ((URLClassLoader)cl).getURLs();

		// Find any recognized codesources
		codesource:
		for( int i = 0; i < descrs.size(); ++i ) {
			CodeSource cs = (CodeSource)descrs.elementAt(i);
			log.fine("Check Codesource: "+cs );
			String signer = cs.getSigner();
			// Check for correct signature.
			if( signer != null ) {
				Object[]signers = origProxy.getClass().getSigners();
				boolean found = false;
				try {
					Certificate k[] = keystore.getCertificateChain( signer );
					for( int j=0; j < signers.length; ++j ) {
						for( int l = 0; l < k.length; ++l ) {
							if( signers[j].equals(k[l]) )
								found = true;
						}
					}
				} catch( KeyStoreException ex ) {
					reportException(ex);
					RemoteException rex = new RemoteException(ex.toString());
					rex.initCause(ex);
					throw rex;
				}

				// Not signed by the requested signer, ignore this entry.
				if( found == false ) {
					// go to next codesource
					continue;
				}
			}

			// Check for any Entry's to match field in.
			if( it != null && conf != null ) {
				Entry[]ents = it.attributeSets;
				Vector wh = cs.getWhens();
				if( wh != null && ents != null ) {
					// For each EntryMatch from the policy...
					for( int j = 0; j < wh.size(); ++j ) {
						EntryMatch em = (EntryMatch)wh.elementAt(j);
						boolean matches = false;
						// For each entry the service has...
						// Try to find one that matches.
						for( int k = 0; k < ents.length; ++k ) {
							if( em.match( ents[k], conf ) )
								matches = true;
						}
						if( matches == false ) {
							log.fine( "No Entry matching: "+em );
							continue codesource;
						}
						log.fine("Matched Entry for: "+em);
					}
				}
			} else if( it != null || conf != null ) {
				throw new NullPointerException(
					"ServiceItem and Configuration must either "+
					"both be NULL or not-NULL");
			}
			// Check for codebase URL match
			for( int j = 0; j < urls.length; ++j ) {
				String proto = urls[j].getProtocol();
				String host = urls[j].getHost();
				int port = urls[j].getPort();
				String file = urls[j].getPath();
				log.finer("Check url: "+urls[j] );
				if( cs.matches( proto, host, port, file ) == true ) {
					log.fine("granting with: "+cs );
					try {
						cs.prepare( proxy );
					} catch( ConfigurationException ex ) {
						RemoteException rex = new RemoteException(ex.toString());
						rex.initCause(ex);
						reportException(ex);
						throw rex;
					}
					break codesource;
				}
			}
		}
		return proxy;
	}

	public static void main( String args[] ) throws Exception {
		DynamicPolicyProxyPreparer d =
			new DynamicPolicyProxyPreparer( null,
				new URL("file:/desktop.policy" ) );
		System.out.println( d.descrs.size()+": "+d.descrs );
		d.prepareProxy( d );
	}

	public DynamicPolicyProxyPreparer( ProxyPreparer prep, URL permdesc ) 
			throws ParserConfigurationException,SAXException,
				SAXParseException,IOException {
		log = Logger.getLogger( getClass().getName() );
//		Handler h;
		this.prep = prep;
//		while( log.getHandlers().length > 0 )
//			log.removeHandler( log.getHandlers()[0] );
//		log.addHandler( h = new ConsoleHandler() );
//		h.setFormatter( new StreamFormatter(true) );
//		h.setLevel( Level.FINEST );
//		log.setLevel( Level.FINEST );
		curDocument = permdesc.toString();
		descrs = new Vector<CodeSource>();
		log.config("loading from "+permdesc );
		InputStream is = permdesc.openStream();
		try {
	
			SAXParserFactory fact = SAXParserFactory.newInstance();
			fact.setValidating(true);
			SAXParser p = fact.newSAXParser();
			if( Policy.getPolicy() instanceof DynamicPolicy == false ) {
				throw new IllegalArgumentException( "Active Policy must implement "+DynamicPolicy.class.getName() );
			}
			p.parse( new InputSource( is ), new XMLHandler( (DynamicPolicy)Policy.getPolicy() ) );
		} catch( SAXParseException ex ) {
			throw new LocalSAXException( ex );
		} finally {
			is.close();
		}
	}
	

	final class XMLHandler extends DefaultHandler {
		private String perm;
		private String method;
		private Vector<String> actions;
		private String data;
		private Vector<String> permArgs;
		private Vector<String> methArgs;
		private CodeSource curSrc;
		private DynamicPolicy policy;
		private boolean haveConf;
		private String confUrl;
		private String confName;
		private Vector<String> confopts;
		private Vector<Permission> tempPerms;
		private String templName;
		private Vector<Permission> methPerms;
		private EntryMatch curWhen;

		public XMLHandler( DynamicPolicy policy ) {
			this.policy = policy;
		}

		public void startDocument() {
		}

		public void endDocument() {
		}

		public void startElement( String namespace, String qName,
				String elem, Attributes attrs ) {
			try {
				doStartElement( namespace, qName, elem, attrs );
			} catch( RuntimeException ex ) {
				reportException(ex);
				throw ex;
			} catch( Exception ex ) {
				reportException(ex);
				RuntimeException rex = new RuntimeException( ex.toString() );
				rex.initCause( ex );
				throw rex;
			}
		}

		public void doStartElement( String namespace, String qName, 
					String elem, Attributes attrs ) 
						throws KeyStoreException,
							MalformedURLException,
							IOException,
							NoSuchAlgorithmException,
							CertificateException,
							ClassNotFoundException,
							InstantiationException,
							IllegalAccessException,
							NoSuchMethodException,
							InvocationTargetException {
			if( elem.equals("permissions") ) {
				// Start of document
			} else if( elem.equals("option") ) {
				data = "";
			} else if( elem.equals("config") ) {
				String url = attrs.getValue("url");
				confName = reqAttr( elem, attrs, "name" );
				if( url != null ) {
					haveConf = true;
					confUrl = url;
				}
				confopts = new Vector<String>();
			} else if( elem.equals("principal") ) {
				String name = reqAttr( elem, attrs, "name" );
				String cls = reqAttr( elem, attrs, "class" );
				String arg = attrs.getValue( "arg" );
				Class cz = Class.forName( cls );
				Principal prin = null;
				if( arg == null ) {
					prin = (Principal)cz.newInstance();
				} else {
					Constructor c = cz.getConstructor( 
						new Class[] { String.class } );
					prin = (Principal)c.newInstance(
						new Object[] { arg } );
				}
				prins.put( name, prin );
			} else if( elem.equals("keystore") ) {
				String loc = reqAttr( elem, attrs, "location" );
				String pass = attrs.getValue("password");
				String type = attrs.getValue("type");
				if( type == null )
					type = "JKS";
				keystore = KeyStore.getInstance( type );
				InputStream is = new URL( loc ).openStream();
				try {
					keystore.load( is, pass.toCharArray() );
				} finally {
					is.close();
				}
			} else if( elem.equals("data") ) {
				
				// Start of configuration file
				if( haveConf ) {
					throw new IllegalArgumentException(
						"configuration URL already specified for this entry" );
				}
				confUrl = null;
				data = "";
			} else if( elem.equals("template") ) {
				templName = reqAttr( elem, attrs, "type" );
				tempPerms = new Vector<Permission>();
				grantTemplates.put( templName, tempPerms );
			} else if( elem.equals("codesource") ) {
				// Make sure we don't put perms here now
				tempPerms = null;
				
				String proto = reqAttr( elem, attrs, "proto" );
				String host = null;
				String port = null;
				String signer = attrs.getValue("signedby");
				if(proto.equals("file") == false ) {
					host = reqAttr( elem, attrs, "host" );
					port = reqAttr( elem, attrs, "port" );
				}
				String jar = reqAttr( elem, attrs, "path" );
				curSrc = new CodeSource( policy, proto, host == null ? "" : host, 
					port == null ? -1 : Integer.parseInt(port), jar, signer );
			} else if( elem.equals("when") ) {
				String entry = reqAttr( elem, attrs, "entry" );
				curWhen = new EntryMatch( entry );				
			} else if( elem.equals("attrib") ) {
				if( curWhen == null ) {
					throw new IllegalArgumentException( 
						"<attrib> must be inside a <when>" );
				}
				String name = reqAttr( elem, attrs, "name" );
				String val = attrs.getValue( "value" );
				if( val != null ) {
					log.finer( "Adding String attribute value match "+
						"\""+name+"\", value=\""+val+"\"" );
					curWhen.addEntry( name, val );
				} else {
					val = attrs.getValue("ref");
					if( val == null ) {
						throw new NullPointerException(
							"Either name and value or name and ref "+
							"must be specified" );
					}
					log.finer( "Adding ref attr match for \""+
						name+"\" ref=\""+val+"\"" );
					curWhen.addEntryRef( name, val );
				}
				
			} else if( elem.equals("mustbe") ) {
				String prin = reqAttr( elem, attrs, "principal" );
				Principal p = (Principal)prins.get(prin);
				if( p == null )
					throw new NullPointerException( "Principal named \""+prin+"\" not found" );
				curSrc.addPrincipal( p );
			} else if( elem.equals("grant") ) {
				String grantType = reqAttr( elem, attrs, "type" );
				curSrc.addGrantType( grantType );
			} else if( elem.equals("permission") ) {
				perm = reqAttr( elem, attrs, "class" );
				permArgs = new Vector<String>();
				actions = new Vector<String>();
			} else if( elem.equals("arg") ) {
				data = "";
			} else if( elem.equals("action") ) {
				data = "";
			} else if( elem.equals("method") ) {
				method = reqAttr( elem, attrs, "name" );
				methArgs = new Vector<String>();
				methPerms = new Vector<Permission>();
			}
		}

		public void characters(char[] ch, int start, int length) {
			if( data == null )
				data = new String(ch,start,length);
			else
				data += new String(ch,start,length);
		}

		public void skippedEntity( String name ) {
		}

		public void startPrefixMapping( String prefix, String uri ) {
		}

		public void endPrefixMapping( String prefix ) {
		}
		
		public String[] getConfOptions() {
			if( confopts.size() == 0 )
				return null;
				
			String[] arr = new String[ confopts.size() ];
			confopts.copyInto(arr);
			return arr;
		}

		public void endElement( String namespace, String qName, String elem ) {
			try {
				doEndElement( namespace, qName, elem );
			} catch( RuntimeException ex ) {
				reportException(ex);
				throw ex;
			} catch( Exception ex ) {
				reportException(ex);
				RuntimeException rex = new RuntimeException( ex.toString() );
				rex.initCause(ex);
				throw rex;
			}
		}

		public void doEndElement( String namespace, String qName,
				String elem ) throws MalformedURLException,
					ConfigurationException,
					IOException {
			if( elem.equals("arg") ) {
				if( perm != null ) {
					log.fine("adding permission constructor arg: "+data );
					permArgs.addElement( data );
				} else if( method != null ) {
					log.fine("adding method arg: "+data );
					methArgs.addElement( data );
				}
			} else if( elem.equals("data") ) {
				// Read from the string data.
				Reader r = new StringReader( data );

				// Construct the configuration
				ConfigurationFile f = new ConfigurationFile(
					r, getConfOptions() );
				curSrc.setConfiguration( confName, f );
			} else if( elem.equals("when") ) {
				curSrc.addWhen( curWhen );
			} else if( elem.equals("config") ) {
				// If a URL was provided load from there
				if( confUrl != null ) {
					Reader r = new InputStreamReader(
						new URL( confUrl ).openStream() );
					// Construct the configuration
					ConfigurationFile f = new ConfigurationFile(
						r, getConfOptions() );
					curSrc.setConfiguration( confName, f );
				}
			} else if( elem.equals("option") ) {
				// add configuration option string
				confopts.addElement( data.trim() );
				data = "";				
			} else if( elem.equals("permission") ) {
				haveConf = false;
				try {
					Class c = Class.forName( perm );
					Class argTypes[] = new Class[ permArgs.size() +actions.size() ];
					Object []args = new Object[permArgs.size()+actions.size()];
					log.fine( args.length+" args to "+perm+" constructor");
					for( int i = 0; i < permArgs.size(); ++i ) {
						argTypes[i] = String.class;
						args[i] = (String)permArgs.elementAt(i);
						log.finest("adding permission arg: "+args[i] );
					}
					for( int i = 0; i < actions.size(); ++i ) {
						argTypes[i+permArgs.size()] = String.class;
						args[i+permArgs.size()] = (String)actions.elementAt(i);
						log.finest("adding permission action: "+args[i+permArgs.size()] );
					}
					Constructor cs = c.getConstructor( argTypes );
					log.fine( "constructing: "+perm+" with: "+cs+" args cnt="+args.length );
					Permission p = (Permission)cs.newInstance( args );
					if( methPerms != null ) {
						log.finer( "adding method("+method+") permission: "+p);
						methPerms.addElement(p);
					} else if( tempPerms != null ) {
						log.finer( "adding template("+templName+") permission: "+p);
						tempPerms.addElement(p);
					} else {
						log.finer( "adding codesource permission: "+p);
						curSrc.addPermission(p);
					}
				} catch( Exception ex ) {
					IllegalArgumentException iex = new IllegalArgumentException( ex.toString() );
					iex.initCause( ex );
					throw iex;
				}
			} else if( elem.equals("action") ) {
				actions.addElement( data );
				log.finest("found action: "+data );
			} else if( elem.equals("grant") ) {
			} else if( elem.equals("method") ) {
				MethodGrant mg = defineMethod( method, methArgs );
				mg.setPermissions( methPerms );
				curSrc.addGrant( mg );
				log.finest("finished method: "+method );
				
				// Reset vars, done with method def.
				methPerms = null;
				method = null;
				methArgs = null;
			} else if( elem.equals("codesource") ) {
				descrs.addElement( curSrc );
				log.fine("finished codesource: "+curSrc );
				curSrc = null;
			}
		}
	
		private String reqAttr( String tag, Attributes attrs,
				String name ) throws MissingAttributeException {
			return defAttr( tag, attrs, name, null );
		}
	
		private String defAttr( String tag, Attributes attrs,
					String name, String def ) throws MissingAttributeException {
			String val = attrs.getValue(name);
			if( val == null ) {
				if( def == null )
					throw new MissingAttributeException( name+
						" must be provide in <"+tag+">" );
				val = def;
			}
			return expandVal(val);
		}
	
		/** expand the value of a string to include variable values */
		private String expandVal( String val ) {
			return val;
		}
	}

	static class MethodGrant {
		String name;
		Vector sig;
		Vector perms;
		
		public void setPermissions( Vector v ) {
			perms = v;
		}
		public Vector getPermissions() {
			return perms;
		}
		public String toString() {
			return name+"("+sig+")";
		}
	
		public MethodGrant( String methName, Vector methSig ) {
			name = methName;
			sig = methSig;
		}
	}

	private MethodGrant defineMethod( String name, Vector args ) {
		return new MethodGrant( name, args );
	}

	protected static class MissingAttributeException extends IllegalArgumentException {
		public MissingAttributeException( String str ) {
			super(str);
		}
	}

	protected void reportException( Throwable ex ) {
		log.log( Level.SEVERE, ex.toString(), ex );
	}

	private class LocalSAXException extends SAXException {
		SAXParseException ex;
		public LocalSAXException( SAXParseException ex ) {
			super( ex );
			this.ex = ex;
		}
		public String toString() {
			return ex.toString()+": "+curDocument+", LN: "+
				ex.getLineNumber()+", COL: "+ex.getColumnNumber();
		}
	}

	/** A Field with a string as the value */
	private static class EntryValue {
		protected String attrName;
		protected String attrVal;
		
		public String toString() {
			return "EntryValue("+attrName+","+attrVal+")";
		}

		public EntryValue( String attr, String val ) {
			attrName = attr;
			attrVal = val;
		}
		public String getAttr() {
			return attrName;
		}
		public String getValue() {
			return attrVal;
		}
		public boolean isRefType() {
			return false;
		}
	}		

	/** A Field with a reference to a configuration entry as the value */
	private static class EntryRef extends EntryValue {
		public String toString() {
			return "EntryRef("+attrName+","+attrVal+")";
		}
		public EntryRef( String attr, String val ) {
			super( attr, val );
		}
		
		public boolean isRefType() {
			return true;
		}
	}		

	/** Match one or more Fields in an Entry */
	private class EntryMatch {
		public String cls;
		public Vector<EntryValue> attrs;
		
		public EntryMatch( String cls ) {
			this.cls = cls;
			attrs = new Vector<EntryValue>(5);
		}
		
		public String toString() {
			return "EntryMatch("+cls+") "+attrs;
		}

		public void addEntry( String attrName, String value ) {
			attrs.addElement( new EntryValue( attrName, value ) );
		}
		
		public void addEntryRef( String attrName, String ref ) {
			attrs.addElement( new EntryRef( attrName, ref ) );
		}
		
		public boolean match( Entry obj, Configuration conf ) {
			ClassLoader ld = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader( 
					obj.getClass().getClassLoader() );
				Class c = Class.forName( cls );
				if( c.isAssignableFrom( obj.getClass() ) == false )
					return false;
				for( int i = 0; i < attrs.size(); ++i ) {
					EntryValue ev = (EntryValue)attrs.elementAt(i);
					String name = ev.getAttr();
					Field f = null;
					try {
						f = c.getField( name );
					} catch( NoSuchFieldException ex ) {
						log.fine( "Can't find field \""+name+"\" in "+cls );
						log.log( Level.INFO, ex.toString(), ex );
						return false;
					}
					if( f == null )
						return false;
					Object val = null;
					try {
						val = f.get( obj );
					} catch( IllegalAccessException ex ) {
						log.fine( "Can't access field \""+name+"\" in "+cls );
						log.log( Level.INFO, ex.toString(), ex );
						return false;
					}
					if( ev.isRefType() ) {
						String[]comps = ev.getValue().split(".");
						String pkg = comps[0];
						for( int k = 1; k < comps.length - 1; ++k ) {
							pkg += "."+comps[k];
						}
						
						Object cval = null;
						try {
							cval = conf.getEntry( pkg,
								comps[comps.length-1], obj.getClass() );
						} catch( ArrayIndexOutOfBoundsException ex ) {
							log.fine( "Can't access configuration entry \""+
								ev.getValue()+"\" spliting on '.' for "+
								"package and config entry name" );
							log.log( Level.INFO, ex.toString(), ex );
							return false;
						} catch( ConfigurationException ex ) {
							log.fine( "Can't access configuration entry \""+
								comps[comps.length-1]+"\" in "+pkg );
							log.log( Level.INFO, ex.toString(), ex );
							return false;
						}
						if ( !cval.equals(val) )
							return false;
					} else {
						if( !ev.getValue().equals( val ) )
							return false;
					}
				}
				return true;
			} catch( ClassNotFoundException ex ) {
				log.fine( "Can't find "+cls+" to check "+
					obj.getClass().getName() );
				log.log( Level.INFO, ex.toString(), ex );
			} finally {
				Thread.currentThread().setContextClassLoader( ld );
			}
			return false;
		}
	}


	private  class CodeSource {
		private Vector<Permission> perms;
		private Vector<MethodGrant> grants;
		private Vector<String> grantType;
		private String host;
		private int port;
		private String jar;
		private String proto;
		private DynamicPolicy provider;
		private Configuration config;
		private String confName;
		private Vector<Principal> codePrins;
		private String signer;
		private Vector<EntryMatch> matches;

		public String toString() {
			return jar+" ("+host+":"+port+")\n  perms: "+perms+"\n  grants: "+grants;
		}

		/** Set the configuration data */
		public void setConfiguration( String name, Configuration config ) {
			this.confName = name;
			this.config = config;
		}

		public boolean matches( String proto, String host, int port, String path ) {
			log.finest("checking "+proto+","+host+","+port+",\""+path+"\" vs "+
				this.proto+","+this.host+","+this.port+",\""+this.jar+"\"" );
			if(  ((host == null && this.host == null) ||
					(this.host!=null && this.host.equals(host)) ) && 
				(this.port == port ) &&
				(this.proto.equalsIgnoreCase(proto) ) && proto.equals("httpmd") ) {
			
				int idx = path.indexOf( ";md5=" );
				return this.jar.equals( path.substring( 0, idx ) );
			}
				
			return 
				(this.proto.equalsIgnoreCase(proto) ) &&
				((host == null && this.host == null) ||
					(this.host!=null && this.host.equals(host)) ) && 
				(this.port == port ) &&
				(this.jar.equals(path) );
		}

		/** Grant permissions to the passed proxy */
		public void grant( Object proxy ) {
			Principal[]prins = getPrincipals();
			// Grant explict permissions
			if( perms != null && perms.size() > 0 ) {
				Permission[]parr = new Permission[ perms.size() ];
				perms.copyInto(parr);
				provider.grant( proxy.getClass(), prins, parr );
			}

			// Grant template permissions
			if( grantType != null && grantType.size() > 0 ) {
				for( int i = 0; i < grantType.size(); ++i ) {
					String name = (String)grantType.elementAt(i);
					Vector v = (Vector)grantTemplates.get(name);
					if( v == null )
						throw new NullPointerException(name+": permission template not found");
					Permission[]parr = new Permission[ v.size() ];
					v.copyInto(parr);
					provider.grant( proxy.getClass(), prins, parr );
				}
			}
		}

		/**
		 *  Prepare the proxy for use by utilizing all preparers and
		 *  granting appropriate permissions.
		 */
		public Object prepare( Object proxy )
				throws ConfigurationException, RemoteException {
			Logger l = log;
			
			// If we have a configuration, see if there is anything
			// in there that we want to use to do the preparation.
			if( config != null ) {
				ProxyPreparer p = (ProxyPreparer)config.getEntry(
					confName, "preparer", ProxyPreparer.class, null );
				l = (Logger)config.getEntry( confName,
					"logger", Logger.class, log );
				Level lv = (Level)config.getEntry( confName,
					"logLevel", Level.class, log.getLevel() );
				Level old = l.getLevel();
				l.setLevel( lv );
				try {
					l.fine( "per object preparer: "+p );
					if( p != null ) {
						l.fine("preparing proxy");
						proxy = p.prepareProxy( proxy );
					}
				} finally {
					l.setLevel( old );
				}
			}
			
			// grant the permissions to the proxy.
			grant( proxy );

			// we defer even toString() on the proxy until
			// we have prepared and granted everything specified
			l.fine("returning proxy: "+proxy );
			return proxy;
		}

		public CodeSource( DynamicPolicy pol, String proto, 
				String host, int port, String jar, String signer ) {
			this.proto = proto;
			this.host = host;
			this.port = port;
			this.jar = jar;
			this.signer = signer;
			provider = pol;
		}
		
		public String getSigner() {
			return signer;
		}

		public void addPrincipal( Principal prin ) {
			if( codePrins == null )
				codePrins = new Vector<Principal>();
			codePrins.addElement(prin);
		}
		
		public Principal[] getPrincipals() {
			if( codePrins == null )
				return null;
			Principal p[] = new Principal[codePrins.size()];
			codePrins.copyInto(p);
			return p;
		}
		
		public Vector getWhens() {
			return matches;
		}

		public void addWhen( EntryMatch ent ) {
			if( matches == null )
				matches = new Vector<EntryMatch>(10);
			matches.addElement( ent );
		}

		public void addPermission( Permission perm ) {
			if( perms == null )
				perms = new Vector<Permission>( 10 );
			perms.addElement( perm );
		}

		public void addGrantType( String grant ) {
			if( grantType == null )
				grantType = new Vector<String>( 10 );
			grantType.addElement( grant );
		}

		public void addGrant( MethodGrant grant ) {
			if( grants == null )
				grants = new Vector<MethodGrant>( 10 );
			grants.addElement( grant );
		}

		public Vector getPermissions() {
			return perms;
		}

		public Vector getGrants() {
			return grants;
		}
	}
}