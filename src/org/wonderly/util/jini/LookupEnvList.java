package org.wonderly.util.jini;

import net.jini.admin.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import org.xml.sax.helpers.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.net.*;

/**
 *  This class provides a mechanism for loading a complete LookupEnv array of data
 *  from an XML file.  The format of the XML file is define as follows.
 *  <pre>
 &lt;lookupEnvs&gt;
	&lt;lookupEnv
			codebase="http://eqwe.com:8080/srv-dl.jar"
			name="Remote Services"&gt;
		&lt;group name="xyzzy"/&gt;
		&lt;locator name="host" port="port"/&gt;
		&lt;bindas host="host"/&gt;
		&lt;template serviceid="840329489304312-392103210232"&gt;
			&lt;class name="com.xxxxxx"/&gt;
			&lt;entry class="com.foo.bar.entry.MyMagicEntry"&gt;
				&lt;int val="99"/&gt;
				&lt;string val="yywewq"/&gt;
				&lt;float val="2321.22"/&gt;
				&lt;double val="2932139213.0232"/&gt;
			&lt;/entry&gt;
		&lt;/template&gt;
	&lt;/lookupEnv&gt;
&lt;/lookupEnvs&gt;
 *  </pre>
 */
public class LookupEnvList extends ArrayList<LookupEnv> {
	private InputStream is;
	private boolean close;

	/**
	 *  The name of the file to read from
	 */
	public LookupEnvList( String file ) throws IOException {
		is = new FileInputStream(file);
		close = true;
	}

	/**
	 *  The File to read from
	 */
	public LookupEnvList( File file ) throws IOException{
		is = new FileInputStream(file);
		close = true;
	}

	/**
	 *  @param url The URL to read from.  This URL is not closed by this class
	 */
	public LookupEnvList( URL url ) throws IOException {
		is = url.openConnection().getInputStream();
		close = true;
	}

	/**
	 *  @param in The InputStream to read from.  This Stream is NOT closed by this class
	 */
	public LookupEnvList( InputStream in ) {
		is = in;
		close = false;
	}

	public void reportException( Throwable ex ) {
		ex.printStackTrace();
	}

	class LookupHandler extends DefaultHandler {
		String data;
		LookupEnv l;
		Entry e;
		ServiceID sid;
		Vector<Class> cv;
		Class ec;
		Vector<Entry> ev;
		Vector<Object> evt;

		public void startDocument() {
			cv = new Vector<Class>();
			ev = new Vector<Entry>();
		}
		public void endDocument() {
		}
		public void characters(char[] ch, int start, int length) {
			data = new String(ch,start,length);
		}
		public void endElement( String namespace, String qName, String elem ) {
			try {
				doEndElement( namespace, qName, elem );
			} catch( Exception ex ) {
				reportException(ex);
			}
		}
		public void doEndElement( String namespace, String qName, String elem ) throws
				InvocationTargetException,
				NoSuchMethodException,
				IllegalAccessException,
				InstantiationException {

			if( elem.equalsIgnoreCase("entry") ) {
				Class cls[] = new Class[evt.size()];
				for( int i = 0; i < evt.size(); ++i ) {
					cls[i] = evt.elementAt(i).getClass();
				}
				Constructor cns = ec.getConstructor(cls);
				Object args[] = new Object[evt.size()];
				evt.copyInto(args);
				Entry e = (Entry)cns.newInstance(args);
				ev.addElement(e);
			} else if( elem.equalsIgnoreCase("template") ) {
				Entry[]ents = null;
				if( ev.size() > 0 ) {
					ents = new Entry[ev.size()];
					ev.copyInto(ents);
				}
				Class clss[] = null;
				if( cv.size() > 0 ) {
					clss = new Class[cv.size()];
					cv.copyInto(clss);
				}
				ServiceTemplate st = new ServiceTemplate(
					sid,
					clss,
					ents
				);
				l.setServiceTemplate(st);
			}
		}
	
		public void startElement( String namespace, String qName,
				String elem, Attributes attrs ) {
			try {
				doStartElement( namespace, qName, elem, attrs );
			} catch( Exception ex ) {
				reportException(ex);
			}
		}
	
		public void doStartElement( String namespace, String qName,
				String elem, Attributes attrs ) throws
					MalformedURLException,
					ClassNotFoundException
					{
			if( elem.equalsIgnoreCase("lookupenvs") ) {
			} else if( elem.equalsIgnoreCase("lookupenv") ) {
				l = new LookupEnv(attrs.getValue("name"));
				l.setCodebase( attrs.getValue("codebase") );
				add(l);
			} else if( elem.equalsIgnoreCase("group") ) {
				l.addGroup(attrs.getValue("name"));
			} else if( elem.equalsIgnoreCase("locator") ) {
				if(attrs.getValue("port")==null ) {
					l.addLookupLocator( attrs.getValue("name") );
				} else {
					l.addLookupLocator(
						attrs.getValue("name")+":"+
						attrs.getValue("port") );
				}
			} else if( elem.equalsIgnoreCase("bindas") ) {
				l.setHostname( attrs.getValue("host") );
			} else if( elem.equalsIgnoreCase("template") ) {
				String id[] = attrs.getValue("serviceid").split("[:,-]");
				long l1 = Long.parseLong(id[0]);
				long l2 = Long.parseLong(id[1]);
				sid = new ServiceID( l1, l2 );
			} else if( elem.equalsIgnoreCase("class") ) {
				cv.addElement( Class.forName( attrs.getValue("name") ) );
			} else if( elem.equalsIgnoreCase("entry") ) {
				ec = Class.forName( attrs.getValue("class") );
				evt = new Vector<Object>();
			} else if( elem.equalsIgnoreCase("int") ) {
				evt.addElement( new Integer( attrs.getValue("val") ) );
			} else if( elem.equalsIgnoreCase("string") ) {
				evt.addElement( attrs.getValue("val") );
			} else if( elem.equalsIgnoreCase("float") ) {
				evt.addElement( new Float( attrs.getValue("val") ) );
			} else if( elem.equalsIgnoreCase("long") ) {
				evt.addElement( new Long( attrs.getValue("val") ) );
			} else if( elem.equalsIgnoreCase("double") ) {
				evt.addElement( new Double( attrs.getValue("val") ) );
			}
		}
	}

	private static class LocalSAXException extends SAXException {
		SAXParseException ex;
		public LocalSAXException( SAXParseException ex ) {
			super( ex );
			this.ex = ex;
		}
		public String toString() {
			return ex.toString()+", LN: "+ex.getLineNumber()+", COL: "+ex.getColumnNumber();
		}
	}

	public void loadEnvs( ) throws IOException,SAXException,ParserConfigurationException {
		try {
			SAXParserFactory fact = SAXParserFactory.newInstance();
			fact.setValidating(true);
			SAXParser p = fact.newSAXParser();
			try {
				p.parse( new InputSource( is ), new LookupHandler() );
			} catch(  SAXParseException ex ) {
				throw new LocalSAXException( ex );
			}
		} finally {
			if( close ) {
				is.close();
			}
		}
	}
}
