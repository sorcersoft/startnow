package org.wonderly.jini2.serviceui;

import java.lang.reflect.*;
import java.beans.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import org.wonderly.jini.*;
import net.jini.entry.*;
import net.jini.lookup.entry.*;
import net.jini.core.entry.*;
import net.jini.lookup.*;
import net.jini.core.discovery.*;
import java.io.*;
import net.jini.admin.*;
import org.wonderly.util.jini.*;
import org.wonderly.jini2.PersistentJiniService;
import org.wonderly.jini.serviceui.PropertiesAccess;
import org.wonderly.jini.serviceui.RemoteAdministrable;
import org.wonderly.jini2.serviceui.PropertyAdminDelegate;
import org.wonderly.jini.serviceui.PropertyDescriptorImpl;
import org.wonderly.jini.serviceui.AdminDescriptor;

/**
 *  This is a demonstration service. It utilizes all of the convenience
 *  classes defined in this package to show how they all interact and work together.
 *  This class shows what needs to be done in other classes to have them automatically
 *  provide a ServiceUI implementation for the AdminUI so that no UI code needs to be
 *  provided by the developer. It also implements the PropertiesAccess interface and
 *  defines several properties using the Java Beans conventions of <code>set&lt;PropertyName&gt;</b>
 *  and <code>is&lt;PropertyName&gt;</code>, <code>get&lt;PropertyName&gt;</code>.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class PropertiesUIDemo extends PersistentJiniService implements PropertiesAccess, RemoteAdministrable {
	MyDelegate mydel;
	String ip, file;
	int port;

	public static void main( String args[] ) throws Exception {
		new PropertiesUIDemo(args);
	}

	public String toString() {
		return getName();
	}

	public PropertiesUIDemo(String args[]) throws Exception {
		super(args);
		mydel = new MyDelegate();
		System.out.println("Starting service" );
		LookupLocator lus[] = getLocators();
		startService( "PropertiesUIDemo",
			getName()+".ser" );
		((JiniAdmin)getAdmin()).setDestroyAdminEnabled(true);
	}

	public String getFileName() {
		return file;
	}

	public void setFileName( String str ) throws IOException {
		file = str;
		writeObjState(ctx.state);
	}

	public void setIPAddress( String str ) throws IOException {
		ip = str;
		writeObjState(ctx.state);
	}

	public String getIPAddress() {
		return ip;
	}

	public void setPort( int p ) throws IOException {
		port = p;
		writeObjState(ctx.state);
	}

	public int getPort() {
		return port;
	}

	public void writeAppState( ObjectOutputStream os ) throws IOException {
		os.writeInt( 1 );		// stream version number
		os.writeObject( ip );
		os.writeObject( file );
	}

	public void readAppState( ObjectInputStream is ) throws IOException,ClassNotFoundException {
		int ver = is.readInt();
		ip = (String)is.readObject();
		file = (String)is.readObject();
	}

	public PropertyDescriptorImpl[]descriptors() throws IntrospectionException,RemoteException {
		return mydel.descriptors(this);
	}

	public Properties currentValues() throws RemoteException,InvocationTargetException,IllegalAccessException,IntrospectionException {
		return mydel.currentValues(this);
	}

	public Properties defaultValues() throws RemoteException,IntrospectionException {
		return mydel.defaultValues(this);
	}

	public void setProperties( Properties vals )  
		throws RemoteException,
			InvocationTargetException,
			IllegalAccessException,
			PropertyVetoException,
			IntrospectionException {
		mydel.setProperties( this, vals );
	}

	class MyDelegate extends PropertyAdminDelegate {
		boolean debug = true;

		public void setDebug( boolean how ) {
			debug = how;
		}
		public PropertyDescriptorImpl[]descriptors( Object svc ) throws IntrospectionException,RemoteException {
			PropertyDescriptorImpl[]d = super.descriptors(svc);
			for( int i = 0; i < d.length; ++i ) {
				if( d[i].getName().equals("IPAddress") ) {
					d[i].setShortDescription("IP Address to bind to");
					d[i].setDisplayName( "Bind To Address");
				} else if( d[i].getName().equals("port") ) {
					d[i].setShortDescription("Port to bind to.  0 means next unused port.");
					d[i].setDisplayName( "Bind To Port");
				} else if( d[i].getName().equals("fileName") ) {
					d[i].setShortDescription("Configuration File to Load Config From");
					d[i].setDisplayName( "Configuration File");
				}
			}
			return d;
		}

		public void reportException( Throwable ex ) {
			ex.printStackTrace();
		}
		public void println( String msg ) {
			System.out.println( msg );
		}
		public void debugln( String msg ) {
			if(debug)
				System.out.println(msg);
		}
	}
}