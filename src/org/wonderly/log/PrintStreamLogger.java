package org.wonderly.log;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.PrintStream;

/**
 *  This is a simple Logger implementation that logs to the indicated
 *  PrintStream.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class PrintStreamLogger extends Logger {
	Date dt = new Date();
	SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	PrintStream strm;

	public PrintStreamLogger( String name, String resources,
					PrintStream strm ) {
		this( name, resources, strm, Level.ALL );
	}

	public PrintStreamLogger( String name, String resources, 
				PrintStream strm, Level lev ) {
		super( name, resources );
		this.strm = strm;
		setLevel( lev );
	}
	
	public void publish( LogRecord rec ) {
		if( getLevel().intValue() > rec.getLevel().intValue() )
			return;
		dt.setTime( rec.getMillis() );
		System.out.print( fmt.format( dt ) );
		System.out.print(" ["+rec.getLoggerName()+"#"+rec.getSequenceNumber()+"] ");
		System.out.print( "# " +rec.getLevel()+ " # "+rec.getMessage() );
		Object parms[] = rec.getParameters();
		for( int i = 0; parms != null && i < parms.length; ++i ) {
			System.out.print( " "+parms[i] );
		}
		if( rec.getThrown() != null ) {
			System.out.println(": method="+rec.getSourceClassName()+"."+rec.getSourceMethodName());
			rec.getThrown().printStackTrace();
		} else {
			System.out.println();
		}
	}
}