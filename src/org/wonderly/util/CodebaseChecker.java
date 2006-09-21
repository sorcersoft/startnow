package org.wonderly.util;

import java.net.*;
import java.io.*;

/**
 *  This class provides a simple way validate that an indicated class
 *  can be resolved using the passed codebase.  This is a simple way to
 *  evaluate the completeness of a Jar file for getting a Class object
 *  to be constructed.  This class should be used to get the Class object,
 *  and then further probing can occur on that Class object by using
 *  the associated ClassLoader object as the contextClassLoader.
 *
 *  @author Gregg Wonderly <gregg.wonderly@pobox.com>
 */
public class CodebaseChecker {
	protected String cls;
	protected URL codebase[];

	public static void main( String args[] ) 
			throws ClassNotFoundException,MalformedURLException {
		Class c = new CodebaseChecker( args ).check();
		System.out.println("Loaded Class: "+c );
	}

	/**
	 *  Constructs an instance using command line arguments
	 *
	 *  @param args args[0] is the classname, args[1-n] are the
	 *   codebase URLs.
	 */
	public CodebaseChecker( String args[] )
			throws ClassNotFoundException,MalformedURLException {
		// Need at least 2 args
		if( args.length < 2 ) {
			System.err.println("usage: "+getClass().getName()+
				" <classname> <codebase-urls>");
			System.exit(1);
		}
		// Build the URL array
		URL u[] = new URL[ args.length - 1 ];
		for( int i = 0; i < args.length-1; ++i ) {
			u[i] = new URL(args[1+i]);
			System.out.println("adding url: "+u[i]);
		}
		cls = args[0];
		codebase = u;
	}

	/**
	 *  Constructs and instance with the indicated class name and associated
	 *  codebase strings.
	 *  @param clsName the name of the class to resolve.
	 *  @param codebase the list of codebase URL strings to use for loading
	 */
	public CodebaseChecker( String clsName, String codebase[] ) throws MalformedURLException {
		cls = clsName;
		this.codebase = new URL[ codebase.length ];
		for( int i = 0; i < codebase.length; ++i ) {
			this.codebase[i] = new URL( codebase[i] );
		}
	}

	/**
	 *  Constructs and instance from classname and URL array.
	 *  @param clsName the name of the class to resolve
	 *  @param codebase the codebase URLs to use for loading
	 */
	public CodebaseChecker( String clsName, URL codebase[] ) {
		cls = clsName;
		this.codebase = codebase;
	}

	/**
	 *  Check the codebase URLs ability to resolve the
	 *  class by constructing a URLClassLoader and then
	 *  loading the class using that classLoader.
	 */
	public Class check() throws ClassNotFoundException {
		URLClassLoader ld = new URLClassLoader( codebase );
		return ld.loadClass( cls );
	}
}