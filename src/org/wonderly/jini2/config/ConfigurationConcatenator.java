package org.wonderly.jini2.config;

import java.io.*;
import net.jini.config.*;
import java.util.*;
import java.util.logging.*;

/**
 *  This class provides a Configuration implemenation that will
 *  support the use of <code>#include "file"</code> constructs
 *  in a configuration file.  These constructs can be nested to
 *  provide a complete tree of configuration control.  The issue
 *  with this is that we just create a concatenated input stream
 *  that is nameless.  Thus, configuration syntax errors that the
 *  ConfigurationFile instance complains about, will not have 
 *  meaningful indications of where the problem is.  Changes comming
 *  in ConfigurationFile that include the problematic text would
 *  remedy this.  Also, ConfigurationFile could include this
 *  same facility and also include support for a '#file' construct
 *  as CPP emits and the C-compiler recognizes to help make
 *  error messages include the correct file name and line number.
 *
 *  @see net.jini.config.ConfigurationFile
 *  @author Gregg Wonderly - gregg.wonderly@pobox.com
 */
public class ConfigurationConcatenator implements Configuration {
	/** Internal ConfigurationFile that is reading concatenated input */
	protected Configuration conf;
	/** Logger: <b>org.wonderly.jini2.config.ConfigurationConcatenator</b> */
	protected static Logger log = Logger.getLogger( 
		ConfigurationConcatenator.class.getName() );
	private static int mline = 1;

	/** Initialize the file contents */
	private static void init( 
			FileOutputStream fo, String str ) throws IOException {
		fo.write(("Input line "+(mline++)+":"+str+"\r\n").getBytes());
		fo.close();
	}

	/** Provided to run test */
	public static void main( String args[] ) throws Exception {
		readTest(args);
		configTest(args);
		System.exit(1);
	}

	private static String buildEntry( int no ) throws Exception {
		FileOutputStream fo = new FileOutputStream("entry"+no);
		try {
			if( no == 4 )
				fo.write( "#include \"entry5\"\n".getBytes() );
			fo.write( (
				"test.entry.e"+no+" {\n"+
				"	val"+no+" = \"Testing "+no+"\";\n"+
				"}\n").getBytes());
		} finally {
			fo.close();
		}
		
		if( no == 5 )
			return "";
		return "#include \"entry"+no+"\"";
	}

	private static void configTest(String args[]) throws Exception {
				
		StringReader rd = new StringReader( ""+
		buildEntry(1)+"\n"+
		buildEntry(2)+"\n"+
		"test.entry.e3 { val3 = \"Testing 3\"; }\n"+
		buildEntry(4)+"\n"+
		buildEntry(5)+"\n"+
		buildEntry(6)+"\n"+
			"");
		Configuration conf = new ConfigurationConcatenator( rd, "input", args,null);
		for( int i = 1; i <= 6; ++i ) {
			String v = (String)conf.getEntry("test.entry.e"+i, "val"+i, String.class );
			System.out.println("val"+i+": "+v);
			if( i != 3 )
				new File("entry"+i).delete();
		}		
	}

	private static void readTest(String args[]) throws Exception {
		for( int i = 1; i < 8; ++i ) {
			init(new FileOutputStream("input"+i),"input "+i);
		}
		// Create a huge long line for testing that
		String bf = "";
		// 90 chars
		for( int j = 0; j < 8; ++j ) {
			bf += "*=-*=-*=";
		}
		System.out.println("bflen: "+bf.length());
		// 90 * 2^6 == 15378
		for( int i = 0; i < 7;++i ) {
			bf += bf;
			System.out.println("bflen: "+bf.length());
		}
		ConcatenatingReader cr = new ConcatenatingReader( "input", 
			new StringReader("#include \"input1\"\r\n"+
				"#include \"input2\"\r\n"+
				"This is a line\r\n"+
				bf+"\r\n"+
				"This is more lines\r\n"+
				"#include \"input3\"\r\n"+
				"#include \"input4\"\r\n"+
				"#include \"input5\"\r\n"+
				"#include \"input6\"\r\n"+
				"#include \"input7\"\r\n"+
				"") );
		BufferedReader rd = new BufferedReader( cr );
		String str;
		log.fine("Reading");
		while( ( str = rd.readLine() ) != null ) {
			System.out.println(str);
		}
		rd.close();
		for( int i = 1; i < 8; ++i ) {
			new File("input"+i).delete();
		}
	}

	/**
	 *  Construct a concatenated configuration file from a file name
	 *  and arguments.
	 *  
	 *  @param args args[0] is used for the ConfigurationFile
	 *		name.  args[1...] are passed to the ConfigurationFile
	 *		constructor.
	 *  @param ld The classloader to load configuration 
	 */
	public ConfigurationConcatenator( String args[], ClassLoader ld ) 
			throws ConfigurationException {
		if( args.length == 0 ) {
			throw new IllegalArgumentException( 
				"No input file provided for Configuration");
		}
		String[]cargs = new String[ args.length - 1 ];
		System.arraycopy( args, 1, cargs, 0, args.length-1 );
		try {
			FileReader rd = new FileReader( args[0] );
			try {
				BufferedReader br = new BufferedReader( rd );
				conf = new ConfigurationFile( 
					new ConcatenatingReader( args[0], rd ),
					cargs, ld );
			} finally {
				rd.close();
			}
		} catch( IOException ex ) {
			ConfigurationException cex = new ConfigurationException(
				ex.toString() );
			cex.initCause( ex );
			throw cex;
		}
	}

	/**
	 *  Create a concatenated configuration using a Reader stream.
	 *  @param rd the stream of the main configuration
	 *  @param name a name to associated with the Reader for logging
	 *  @param options options for ConfigurationFile constructor
	 *  @param ld classloader to pass to the ConfigurationFile constructor  
	 */
	public ConfigurationConcatenator( Reader rd, String name, 
			String options[], ClassLoader ld )
			throws ConfigurationException {
		// Construct the configuration using a ConcatenatingReader.
		conf = new ConfigurationFile( 
			new ConcatenatingReader( name, rd ), options, ld );
	}
	
	/**
	 *  A file context used by the ConcatenatingReader
	 */
	private static class FileContext {
		public int line;
		public String name;
		public BufferedReader rd;
		public FileContext( String name, BufferedReader rd, int line ) {
			this.name = name;
			this.line = line;
			this.rd = rd;
		}
	}

	/**
	 *  The Reader extension that reads line by line looking for
	 *  <code>#include</code> constructs.
	 */
	private static class ConcatenatingReader extends Reader {
		BufferedReader input;
		Stack<FileContext> ins = new Stack<FileContext>();
		String name;
		int lineno;
		public ConcatenatingReader( String name, Reader stream ) {
			input = new BufferedReader( stream );
			this.name = name;
			lineno = 0;
		}

		void pushInput() {
			log.finer("push input: "+name+":"+lineno);
			ins.push( new FileContext( name, input, lineno ) );
		}
		
		void popInput() {
			FileContext ct = (FileContext)ins.pop();
			input = ct.rd;
			lineno = ct.line;
			name = ct.name;
			log.finer("pop input: "+name+":"+lineno);
		}

		public int read( char[]arr, int off, int cnt ) throws IOException {
			log.fine(arr.length+" ("+off+", "+cnt+" )");
			if( input.markSupported() ) {
				// Big an arbitrarily large max buffer length
				// to try and deal with unexpected huge lines
				// gracefully
				input.mark(cnt*10);
			}
			String str = input.readLine();
			if( str != null && input.markSupported() ) {
				log.fine("len: "+str.getBytes().length+" > "+cnt+" ? " );
				if( str.getBytes().length > cnt-off-2 ) {
					input.reset();
					log.warning("line too long: returning "+
						(cnt-off-2)+" of "+str.length()+" chars");
					return input.read(arr,off,cnt);
				}
			}
				
			if( str == null ) {
				log.fine("EOF "+name+":"+lineno);
				if( ins.size() > 0 ) {
					popInput();
					return read( arr, off, cnt );
				}
				return -1;//throw new EOFException();
			}
			++lineno;
			if( str.length() > 10 && str.charAt(0) == '#' ) {
				pushInput();
				lineno = 0;
				int i = 1;
				for( ; i < str.length(); ++i ) {
					if( Character.isSpaceChar( str.charAt(i) ) == false )
						break;
				}
				if( i >= str.length() ) {
					popInput();
					throw new IOException(name+":"+lineno+
						": #... statement incomplete: "+ str );
				}
				if( str.substring(i,"include".length()+1
						).equalsIgnoreCase("include") == false ) {
					popInput();
					throw new IOException(name+":"+lineno+
						": include not found after '#': \""+
						str.substring(i,"include".length()+1)+"\"" );
				}
				i += "include".length();
				
				for( ; i < str.length(); ++i ) {
					if( Character.isSpaceChar( str.charAt(i) ) == false )
						break;
				}
				if( str.charAt(i) == '"' ) 
					++i;
				str = str.substring( i );
				if( str.charAt(str.length()-1) == '"' )
					str = str.substring(0,str.length()-1);
				input = new BufferedReader( new FileReader( str ) );
				lineno = 0;
				name = str;
				return read( arr, off, cnt );
			}
			str += "\r\n";
			char[]b = str.toCharArray();
			System.arraycopy(b,0,arr,off,b.length);
			log.fine("Returning "+b.length+" ("+str+")");
			log.log(Level.FINER, "called from", new Throwable());
			return b.length;
		}

		public void close() throws IOException {
			input.close();
		}
	}

	public Object getEntry( String name, String ent, 
			Class type, Object def,
			Object data ) throws ConfigurationException {
		log.fine( "getEntry( "+name+", "+ent+", "+
			type.getName()+", "+def+", "+data+");");
		return conf.getEntry( name, ent, type, def, data );
	}

	public Object getEntry( String name, String ent, 
			Class type, Object def ) 
				throws ConfigurationException {
		log.fine( "getEntry( "+name+", "+ent+", "+
			type.getName()+", "+def+");");
		return conf.getEntry( name, ent, type, def );
	}

	public Object getEntry( String name, String ent,
			Class type ) throws ConfigurationException {
		log.fine( "getEntry( "+name+", "+ent+", "+type.getName()+");");
		return conf.getEntry( name, ent, type );
	}
}
