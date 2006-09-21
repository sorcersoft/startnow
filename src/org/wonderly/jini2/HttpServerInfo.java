package org.wonderly.jini2;

import net.jini.core.entry.Entry;

/**
 *  This class is an Entry implementation that encapsulates all of
 *  the attributes of an http servers access.
 *
 *  @author Gregg Wonderly - gregg@wonderly.org
 */
public class HttpServerInfo implements Entry {
	/** The hostname of the server */
	public String host;
	/** The port of the server */
	public Integer port;
	/** The protocol for the server, http, https, httpmd etc */
	public String proto;
	/** The serial version is 1 */
	public static final long serialVersionUID = 1;
	
	public String toString() {
		return proto+"://"+host+":"+port+"/";
	}
	
	public String formatUrl( String path, String digestFormat, String digest ) {
		String str = proto+"://"+host+":"+port+"/"+
				path;
		if( proto.equals("httpmd") ) {
			str += ";"+digestFormat+"="+digest;
		}
		return str;
	}

	/**
	 *  No-args constuctor for Jini's use
	 */
	public HttpServerInfo() {
	}

	/**
	 *  @param proto the protocol for access to the server "http" for example
	 *  @param host the name of the host
	 *  @param port the port that the service is provided on
	 */
	public HttpServerInfo( String proto, String host, int port ) {
		this.proto = proto;
		this.host = host;
		this.port = new Integer(port);
	}
	/**
	 *  @param proto the protocol for access to the server "http" for example
	 *  @param host the name of the host
	 *  @param port the port that the service is provided on
	 */
	public HttpServerInfo( String proto, String host, Integer port ) {
		this.proto = proto;
		this.host = host;
		this.port = port;
	}
	/**
	 *  @param host the name of the host
	 *  @param port the port that the service is provided on
	 */
	public HttpServerInfo( String host, int port ) {
		this.proto = "http";
		this.host = host;
		this.port = new Integer(port);
	}
	/**
	 *  @param host the name of the host
	 *  @param port the port that the service is provided on
	 */
	public HttpServerInfo( String host, Integer port ) {
		this.proto = "http";
		this.host = host;
		this.port = port;
	}
}