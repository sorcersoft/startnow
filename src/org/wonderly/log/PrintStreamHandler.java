package org.wonderly.log;

import java.util.logging.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.PrintStream;

/**
 *  This is a simple Logger implementation that forces itself
 *  to initially use a {@link StreamFormatter} instance.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class PrintStreamHandler extends StreamHandler {
	/**
	 *  Test output formatting and handling
	 */
	public static void main( String args[] ) {
		Logger log = Logger.getLogger(
			PrintStreamHandler.class.getName() );
		Handler hand = new PrintStreamHandler("handler",System.err,Level.ALL);
		((StreamFormatter)hand.getFormatter()).setBrief(true);
		log.addHandler( hand );
		log.setUseParentHandlers(false);
		log.setLevel( Level.ALL );
		hand.setLevel( Level.ALL );
		log.log(Level.INFO,"This is the {0}, {1} and {2} test", new Object[] { "121", "612", "9213"});
		log.warning("This a warning");
		log.finest("This is at finest");
		log.logp( Level.CONFIG, "PrintStreamHandler", "main", "This is the logp message");
		Throwable ex = new IllegalArgumentException(
			"This is the exception");
		log.log( Level.SEVERE, ex.toString(), ex );
	}

	/**
	 *  Create a Handler logging the indicated messages
	 */
	public PrintStreamHandler( String name, Level lev ) {
		this( name, null, lev );
	}

	/**
	 *  Create a Handler logging the indicated messages
	 */
	public PrintStreamHandler( String name, PrintStream strm, Level lev ) {
		this( name, strm, null, lev );
	}

	/**
	 *  Create a Handler logging Level.ALL messages
	 */
	public PrintStreamHandler( String name ) {
		this( name, (PrintStream)null );
	}

	/**
	 *  Create a Handler logging Level.ALL messages
	 */
	public PrintStreamHandler( String name, PrintStream strm ) {
		this( name, strm, (String)null );
	}

	/**
	 *  Create a Handler logging Level.ALL messages
	 */
	public PrintStreamHandler( String name, PrintStream strm, String resources ) {
		this( name, strm, resources, Level.ALL );
	}

	/**
	 *  Create a Handler logging the indicated messages
	 */
	public PrintStreamHandler( String name, PrintStream strm,
			String resources, Level lev ) {
		super( strm, new StreamFormatter() );
		setLevel( lev );
	}
}