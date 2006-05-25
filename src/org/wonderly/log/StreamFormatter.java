package org.wonderly.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.SimpleFormatter;
import java.util.logging.LogRecord;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *  A stream based formatter that creates single line log entries
 *  The property <b><code>org.wonderly.log.streamformatter.brief</code></b> can be set
 *  to eliminate the logger name from the output.
 */
public class StreamFormatter extends SimpleFormatter {
	/** Whether brief format is active */
	protected boolean brief;
	private Date dt = new Date();
	/** Format of date in output */
	protected SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

	public void setBrief( boolean isBrief ) {
		brief = isBrief;
	}

	public boolean isBrief() {
		return brief;
	}
	
	public StreamFormatter() {
		brief = System.getProperty(
			"org.wonderly.log.streamformatter.brief") != null;
	}
	
	public StreamFormatter( boolean brief ) {
		setBrief( brief );
	}

	public String format( LogRecord rec ) {
		dt.setTime( rec.getMillis() );
		StringBuffer b = new StringBuffer();
		b.append( fmt.format( dt ) );
		if( !brief ) {
			b.append(" [" );
			b.append(rec.getLoggerName());
			b.append("#");
			b.append(rec.getSequenceNumber());
			b.append("] ");
			b.append( rec.getLevel() );
		}
		b.append(" # ");	
		b.append( formatMessage( rec ) );

		// Format any exception message
		if( rec.getSourceMethodName() != null ) {
			b.append(": from=");
			b.append(rec.getSourceClassName());
			b.append(".");
			b.append(rec.getSourceMethodName());
			b.append("(): ");
		}
		if( rec.getThrown() != null ) {
			StringWriter wr = new StringWriter();
			rec.getThrown().printStackTrace(new PrintWriter(wr));
			b.append(System.getProperty("line.separator"));
			b.append( wr.toString() );
		}
		b.append(System.getProperty("line.separator"));
		return b.toString();
	}
}