package org.wonderly.log;

import java.util.logging.*;
import java.util.Date;
import java.text.*;

/**
 *  This is a simple Logger implementation that logs to stdout.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class StdoutLogger extends PrintStreamLogger {
	public StdoutLogger( String name, String resource ) {
		super( name, resource, System.out );
	}
	public StdoutLogger( String name, String resource, Level lev ) {
		super( name, resource, System.out, lev );
	}
}