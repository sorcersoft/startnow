package org.wonderly.log;

/*
 *  Copyright 2005-2006 Gregg Wonderly
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
	   
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.SimpleFormatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.beans.*;
import java.util.logging.LogManager;

/**
 *  This class provides an {@link java.util.logging.Formatter} implementation which 
 *  formats log entries into single lines of text.  There are several properties
 *  which can be set to alter the output for a more or less brief output format.
 *  <table>
 *  <tr><th>Property<th>Description
 *  <tr><td>org.wonderly.log.StreamFormatter.withMethods
 *      <td>Include source method names on log records.
 *  <tr><td>org.wonderly.log.StreamFormatter.withClasses
 *      <td>Include source class names on log records.
 *  <tr><td>org.wonderly.log.StreamFormatter.brief
 *      <td>Don't include Logger instance name.
 *  <tr><td>org.wonderly.log.StreamFormatter.truncateLoggerName
 *      <td>If brief is not true, use only first letter of each
 *          package component and full last element.  
 *			<code>net.jini.loader.pref.PreferredClassLoader</code>
 *			becomes
 *			<code>n.j.l.p.PreferredClassLoader</code>
 *  <tr><td>org.wonderly.log.StreamFormatter.timeFormat
 *      <td>format of timestamp in logged records
 *  <tr><td>org.wonderly.log.StreamFormatter.dateFormat
 *      <td>format of datestamp in logged records.
 *  </table>
 *  All of these properties default to <b>false</b>.  Setting them to the
 *  value of <b>true</b>, <b>yes</b> or <b>1</b> will activate the indicated
 *  processing of log records.
 */
public class StreamFormatter extends SimpleFormatter {
	/** Whether brief format is active.
	 *  Value assigned here is always overridden by property processing.
	 */
	protected boolean brief;
	/** Whether non-brief format should truncate package names to a single char.
	 *  Value assigned here is always overridden by property processing.
	 */
	protected boolean trunc;
	/** Include source method names.  Value assigned here is
	 *  always overridden by property processing.
	 */
	protected static boolean withMethods = false;
	/** Include source class names.  Value assigned here is
	 *  always overridden by property processing.
	 */
	protected static boolean withClasses = false;
	/**
	 *  Create an initial Date value to just setTime() on.
	 */
	private Date dt = new Date();
	/** Format of date in output */
	protected SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	/** Format of time in output */
	protected SimpleDateFormat bfmt = new SimpleDateFormat("EEE HH:mm:ss #");
	/** Line separator */
	protected static String eol = System.getProperty("line.separator");

	/**
	 * Set if truncated logger names should be used.  Only if {@link #setBrief(boolean)} has been set to
	 * false will the logger name appear in the output stream.
	 * @param isTrunc true to turn on truncated logger names.
	 * @see #setBrief(boolean)
	 */
	public synchronized void setTruncateLoggerName( boolean isTrunc ) {
		trunc = isTrunc;
	}

	/**
	 * Turn on brief logging by removing the name of the logger instance from the output.
	 * @param isBrief true to enable brief output without logger names.
	 * @see #setTruncateLoggerName(boolean)
	 */
	public synchronized void setBrief( boolean isBrief ) {
		brief = isBrief;
	}

	/**
	 * turn on the printing of source method signatures in the logged output.
	 * @param how true to enable method signatures.
	 */
	public synchronized void setWithMethods( boolean how ) {
		withMethods = how;
	}
	
	/**
	 * Turn on/off the inclusing of classname in the formatted output.
	 * @param how true to enable classnames.
	 */
	public synchronized void setWithClasses( boolean how ) {
		withClasses = how;
	}
	
	/**
	 * Instantiate an instance of this formatter.  This will be invoked by the LogManager or other
	 * code which is setting up logging configuration.
	 */
	public StreamFormatter() {

		LogManager.getLogManager().addPropertyChangeListener( new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent ev ) {
				checkProps();
			}
		});
		checkProps();
	}
	
	/**
	 * Check properties settings and configure selected options.  Called from the
	 * constructor, and the property change listener used to monitor the LogManager
	 * configured properties.
	 */
	private void checkProps() {
		LogManager lm =  LogManager.getLogManager();
		String v = lm.getProperty("org.wonderly.log.StreamFormatter.withMethods");
		setWithMethods( v != null && ( v.equals("true") || 
			v.equals("yes") || v.equals("1") ) );

		v = lm.getProperty("org.wonderly.log.StreamFormatter.withClasses");
		setWithClasses( v != null && ( v.equals("true") ||
			v.equals("yes") || v.equals("1") ) );

		v = lm.getProperty("org.wonderly.log.StreamFormatter.brief");
		setBrief( v != null && ( v.equals("true") ||
			v.equals("yes") || v.equals("1") ) );

		v = lm.getProperty("org.wonderly.log.StreamFormatter.truncateLoggerName");
		setTruncateLoggerName( v != null && ( v.equals("true") ||
			v.equals("yes") || v.equals("1") ) );

		v = lm.getProperty("org.wonderly.log.StreamFormatter.timeFormat");
		if( v != null )
			fmt = new SimpleDateFormat( v );

		v = lm.getProperty("org.wonderly.log.StreamFormatter.dateFormat");
		if( v != null )
			bfmt = new SimpleDateFormat( v );
	}

	/**
	 * Create an instance of StreamFormatter with the initial value for {@link #setBrief(boolean)}.
	 * @param brief true to enable brief output with no logger name.
	 */
	public StreamFormatter( boolean brief ) {
		setBrief( brief );
	}

	/**
	 * Create an instance of StreamFormatter with the indicated values for
	 * {@link #setBrief(boolean)} and {@link #setTruncateLoggerName(boolean)}.
	 * @param brief true to enable brief output.
	 * @param truncate if brief is false, then true to enable truncated logger names.
	 * @see #setBrief(boolean)
	 */
	public StreamFormatter( boolean brief, boolean truncate ) {
		setBrief( brief );
		setTruncateLoggerName( truncate );
	}

	/**
	 * Called by the logging infrastructure to
	 * @param rec The log record to format.
	 * @return The formatted log record value.
	 */
	public String format( LogRecord rec ) {
		dt.setTime( rec.getMillis() );
		StringBuffer b = new StringBuffer();
		b.append( (brief ? bfmt : fmt).format( dt ) );
		if( !brief ) {
			b.append(" [" );
			if( trunc ) {
				String[] s = rec.getLoggerName().split("\\.");
				for( int i = 0; i < s.length-1; ++i ) {
					b.append(s[i].charAt(0));
					b.append(".");
				}
				b.append(s[s.length-1]);
			} else {
				b.append(rec.getLoggerName());
				b.append("#");
				b.append(rec.getSequenceNumber());
			}
			b.append("] ");
		} else {
			b.append(" ");
		}
		b.append( rec.getLevel() );
		b.append(" # ");

		if( withClasses) {
			b.append(": from=");
			b.append(rec.getSourceClassName());
			b.append(".");
		}
		if( withMethods) {
			b.append(rec.getSourceMethodName());
			b.append("(");
			Object a[] = rec.getParameters();
			if( a != null ) {
				b.append(" ");
				for( int i = 0; i < a.length; ++i ) {
					if( i > 0 )
						b.append(", ");
					b.append( a[i]+"" );
				}
			}
			b.append(" ) ");
		}
		b.append( formatMessage( rec ) );
		if( rec.getThrown() != null ) {
			StringWriter wr = new StringWriter();
			rec.getThrown().printStackTrace(new PrintWriter(wr));
			b.append( eol );
			b.append( wr.toString() );
		}
		b.append( eol );
		return b.toString();
	}
}
