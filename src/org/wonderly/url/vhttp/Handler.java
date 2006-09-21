package org.wonderly.url.vhttp;

import java.net.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import java.util.logging.*;
import java.awt.event.*;
import java.awt.*;
import java.security.*;
import org.wonderly.awt.*;

/**
 *  This is the protocol handler for the vhttp protocol.
 *  This protocol caches http:// requests in a local directory, and displays
 *  download progress if <code>setParent()</code> is called with a non-null frame.
 *  The caching directory is established using <code>setCacheDir( dirName )</code> and
 *  is homed to <code>System.getProperty("user.dir")</code> if it is relative.  If the
 *  caching directory is not set, no caching is done.
 *  <p>
 *  So, to use this in some java application, you need to do
 *  <pre>
		if( System.getProperty("java.protocol.handler.pkgs") == null ) {
			System.getProperties().put("java.protocol.handler.pkgs",
				"org.wonderly.url");
		}
		JFrame top;
		...
		// Set the parent frame for JDialog homing.
		org.wonderly.url.vhttp.Handler.setParent(top);
		// Set caching directory as 'cache' under the running directory.
		org.wonderly.url.vhttp.Handler.setCacheDir("cache");
 *  </pre>
 *  <p>
 *
 *  The following properties are consulted in a static initializer
 *  <dl>
 *		<dt>org.wonderly.vhttp.protocol<dd>defaults to http
 *		<dt>org.wonderly.vhttp.cache<dd>defaults to no cache directory, which is null
 *		<dt>org.wonderly.vhttp.debug<dd>set to non-empty string enables debug logging
 *		    using the <code>org.wonderly.url.vhttp.Handler</code> logger
 *  </dl>
 *
 *  @see #setParent(JFrame)
 *  @see #setCacheDir(String)
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class Handler extends URLStreamHandler {
	static JFrame parent;
	static String cache;
	static String proto = "http";
	static Logger log;
	
	static {
		String pr = System.getProperty("org.wonderly.vhttp.protocol");
		if( pr != null )
			proto = pr;
		String cd = System.getProperty("org.wonderly.vhttp.cache");
		if( cd != null )
			cache = cd;
		log = Logger.getLogger(Handler.class.getName());
		if( log.isLoggable(Level.FINE)) log.fine("static initialization");
		if( log.isLoggable(Level.FINE)) log.fine("protocol from system property: "+proto );
		if( log.isLoggable(Level.FINE)) log.fine("cache from system property: "+cache );
		if( log.isLoggable(Level.FINE)) log.fine("static init completed");
	}

	public static void main( String args[] ) throws Exception {
		if( System.getProperty("java.protocol.handler.pkgs") == null ) {
			System.getProperties().put("java.protocol.handler.pkgs",
				"org.wonderly.url");
		}
		final JFrame top = new JFrame( "Testing download" );
		top.setSize( 700, 500 );
		top.setLocation( 100, 100 );
		top.setVisible(true);
		org.wonderly.url.vhttp.Handler.setParent(top);
		org.wonderly.url.vhttp.Handler.setCacheDir("test-cache");
		top.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev ) {
//				System.gc();
//				new File( "cache/reggie-dl.jar" ).delete();
				top.dispose();
			}
		});
		URL u = new URL( "vhttp://chipmunk:8090/reggie-dl.jar" );
		Object obj = u.getContent();
		if( log.isLoggable(Level.FINE)) log.fine("got content: "+obj );
		u = null;
		obj = null;
	}
	
	/**
	 *  Set the currently used protocol for final retrieval
	 */
	public static void setProtocol( String proto ) {
		Handler.proto = proto;
	}

	public static String getProtocol() {
		return proto;
	}

	/**
	 *  Public constructor that allows java.net.URL to construct us
	 *
	 *  @see java.net.URL
	 */
	public Handler() {
		if( log.isLoggable(Level.FINE)) log.fine("Constructing: "+this );
	}

	public static void setParent( JFrame par ) {
		parent = par;
		if(log.isLoggable(Level.FINE)) log.fine( "Parent set to: "+parent );
	}

	public static void setCacheDir( String dir ) {
		if( log.isLoggable(Level.FINE)) log.fine("setting cache ("+cache+") to: "+dir );
		cache = dir;
		new File(cache).mkdirs();
	}

	/**
	 *  Open a new connection to the ftp URL by creating an instance
	 *  of the inner class, <code>URLConnection</code> that will do the
	 *  creation of the process to run ftp in.
	 */
	public java.net.URLConnection openConnection( final URL url ) throws IOException {
		log.log(Level.FINEST,"openConnection: "+url, new Throwable(url.toString()) );
		final IOException exc[] = new IOException[1];
		URLConnection ret = (URLConnection) AccessController.doPrivileged(
			new PrivilegedAction<URLConnection>() {
			public URLConnection run() {
				try {
					return openIt( url );
				} catch( IOException ex ) {
					exc[0] = ex;
					log.log(Level.SEVERE, ex.toString(), ex );
					return null;
				}
			}
		});
		if( ret == null && exc[0] != null )
			throw exc[0];
		return ret;
	} 
	
	public static void holdOffDownloadOf( URL u, Long time ) {
		if( log.isLoggable(Level.FINE) ) {
			log.fine("holdoffdownload: "+u+", till: "+
				new Date(time.longValue()+(60*1000) )+": lastget="+lastget );
		}
		lastget.put( u, new Long( time+(60*1000)) );
	}
	
	// This hashtable holds references until GC and then we'll check again
	private static WeakHashMap<URL,Long> lastget = new WeakHashMap<URL,Long>();
	private java.net.URLConnection openIt( URL url ) throws IOException {
		if( log.isLoggable(Level.FINE)) log.fine("=== openit(\""+url+"\") cache: "+cache );
		String p = url.getPath();
		if( cache == null ) {
			if( log.isLoggable(Level.FINE)) log.fine("no caching enabled, going direct: "+
				proto+"://"+url.getHost()+":"+url.getPort()+p);
			return new URL( proto+"://"+url.getHost()+":"+url.getPort()+p ).openConnection();
		}
		log.log(Level.FINEST,  "url path: "+p, new Throwable(p) );
		File dc = new File( cache );
		dc = new File( dc, url.getHost() );
		dc = new File( dc, url.getPort()+"" );
		if( dc.exists() == false ) {
			if( log.isLoggable(Level.FINE)) log.fine("making initial dir: "+dc );
			dc.mkdirs();
		} else {
			if( log.isLoggable(Level.FINE)) log.fine("existing cache dir: "+dc );
		}
		String f = url.getFile();
		File cd = new File( dc, p );
		if( log.isLoggable(Level.FINER)) log.finer("cd is: "+cd );
		File pd = new File( cd.getParent());
		if( log.isLoggable(Level.FINER)) log.finer("pd is: "+pd );
		String durl = System.getProperty("user.home")+"/.jarcache/";
		String npref = "";
		if( durl.charAt(0) != '/' && durl.charAt(0) != '\\' )
			npref = "/";
		if( log.isLoggable(Level.FINER)) log.finer("directory url is: "+"file:"+npref+durl);
		if( log.isLoggable(Level.FINER)) log.finer("dc is: "+"file:"+npref+dc);
		durl = "file:"+npref+durl;
		String fp = dc.toString().replace('\\','/')/*.replace(' ','+')*/+
			p.replace('\\','/')/*.replace(' ','+')*/;
		if( log.isLoggable(Level.FINER)) log.finer("fp: "+fp );
		URL uf = new URL( new URL( "file:"+npref+ durl/*.replace(' ','+')*/ ), "file:"+npref+fp ); //+"/"+f );
		if( log.isLoggable(Level.FINER)) log.finer("uf path: "+uf.getPath() );
		File file = new File( uf.getPath() ); //new File( cd,  f );
		File ff = new File( file.getParent() );
		if( ff.exists() == false ) {
			if( log.isLoggable(Level.FINE)) log.fine("making cache directory: "+ff);
			ff.mkdirs(); 
		}

		URL u = new URL( proto+"://"+url.getHost()+":"+url.getPort()+p ); //+"/"+f );
			Long lastTime = (Long)lastget.get(url);
		if( log.isLoggable(Level.FINE)) {
			log.fine("Checking cachefile: "+file+" with url="+url+", time: "+
				(lastTime == null ? "<none>" : (""+
				new Date( lastTime.longValue() )))+", lastget: "+lastget );
		}
		String tnm = Thread.currentThread().getName();
		java.net.URLConnection uc = null;
		if( lastTime == null || (System.currentTimeMillis() - lastTime.longValue()) > 60*1000 ) {
			if( log.isLoggable(Level.FINE)) {
				log.fine("Connecting to "+u+" to check date/time/size");
			}

			if( log.isLoggable(Level.FINE)) log.fine("Connecting to "+u+" to check date/time/size");
			uc = u.openConnection();
			
			try {
				Thread.currentThread().setName( "Check: "+u.toString() );
			
				// Deny caching from previous fetch
				uc.setUseCaches(false); 
		
				if( uc instanceof HttpURLConnection )
					((HttpURLConnection)uc).setRequestMethod("HEAD");
		
				// Force connection to web server
				try {
					uc.getInputStream();
				} catch( Exception ex ) {
					ex.printStackTrace();
				}
				if( log.isLoggable(Level.FINE)) log.fine("Connected to "+u);
		
				if( file.exists() ) {
					long flast = file.lastModified();
					long ulast = uc.getLastModified();
					int len = uc.getContentLength();
					if( log.isLoggable(Level.FINER))  {
						log.finer("local file: ("+file+")");
						log.finer( "local Date: "+new Date( flast )+", len="+file.length() );
						log.finer("remote file ("+u+")" );
						log.finer("remote Date: "+new Date(ulast)+", len="+len );
					}
					if( ulast > flast ) {
						file.delete();
						if( log.isLoggable(Level.FINE)) log.fine("file out of date");
					} else if( flast >= ulast && file.length() != uc.getContentLength() ) {
						file.delete();
						if( log.isLoggable(Level.FINE)) log.fine("file wrong size");
					} else {
						if( log.isLoggable(Level.FINE)) log.fine("Cache uptodate");
					}
				}
				lastget.put( url, new Long( System.currentTimeMillis() ) );
			} finally {
				// Force stream closed, don't need HEAD output after here.
				try {
					uc.getInputStream().close();
				} catch( Exception ex ) {
					ex.printStackTrace();
				}
				Thread.currentThread().setName( tnm );
			}
		} else {
			if( log.isLoggable(Level.FINE)) {
				log.fine("Using cached time check to "+u+" to check date/time/size");
			}
		}

		if( log.isLoggable(Level.FINER)) log.finer("after date check, file: "+file+" exists? "+file.exists() );
		// Is the file already in the cache?
		if( file.exists() == false ) {
			if( log.isLoggable(Level.FINE)) log.fine("No local file, downloading...");
		Thread.currentThread().setName( "Download: "+u.toString() );
			uc = u.openConnection();
			pd.mkdirs();
			JDialog d = null;
			JLabel plab = null;
			JProgressBar bar = null;
			JLabel info = null;
			if( log.isLoggable(Level.FINER)) log.finer("creating dialog for progress display: "+parent);
			if( parent != null ) {
				d = new JDialog( parent, "Downloading into "+file.getName(), false );				
				Packer pk = new Packer( d.getContentPane() );

				plab = new JLabel( "0 of 0 - 0%", JLabel.CENTER );
				pk.pack( plab ).fillx().gridx(0).gridy(0).inset(5,5,5,5);
				plab.setBorder( BorderFactory.createEtchedBorder() );
				
				bar = new JProgressBar(0,0, 1000);
				pk.pack( bar ).fillx().gridx(0).gridy(1).inset(5,5,5,5);

				info = new JLabel( "Downloading "+file.getName()+" from "+u.getHost(), JLabel.CENTER );
				pk.pack( info ).gridx(0).gridy(2).fillx().inset(5,5,5,5);
				info.setBorder( BorderFactory.createEtchedBorder() );

				bar.setValue(1);

				d.setDefaultCloseOperation( d.DO_NOTHING_ON_CLOSE );
				d.pack();
				d.setLocationRelativeTo( parent );
				d.setVisible(true);
			}

			try {
				log.info( "downloading to \""+file+"\", via: \""+u+"\"" );
				// Make connection first
				InputStream is = uc.getInputStream();

				// Find out how much to download
				long length = uc.getContentLength();
				if( bar != null ) {
					// Set up progress bar with amount to download if active
					bar.setValue(0);
					bar.setMinimum(0);
					bar.setMaximum( (int)length );
				}

				// Create output stream to cached file
				OutputStream os = new FileOutputStream(file);
				byte[]arr = new byte[1024];
				int n;
				try {
					int cnt = 0;
					while( ( n = is.read(arr, 0, arr.length) ) >= 0 ) {
						os.write( arr, 0, n );
						cnt += n;
						if( bar != null ) {
							bar.setValue( cnt );
							bar.repaint();
							plab.setText( cnt+" of "+length+" - "+((cnt*100)/length)+"%" );
							plab.revalidate();
							plab.repaint();
						}
					}
					// do close here in case file can not be written
					os.close();
					os = null;
				} catch( Exception ex ) {
					ex.printStackTrace();
					if( os != null ) {
						try {
							os.close();
						} catch( Exception exx ) {
							exx.printStackTrace();
						}
					}
					file.delete();
					
					// Just return uncached stream if caching fails
					return u.openConnection();
				} finally {
					is.close();
				}
			} finally {
				if( d != null ) {
					d.setVisible(false);
					d.dispose();
				}
				Thread.currentThread().setName( tnm );
			}
		}

		try {
		Thread.currentThread().setName( "Using: "+u.toString() );
			if( log.isLoggable(Level.FINER)) log.finer("File part of url: "+fp );
			u = new URL( new URL( "file:"+npref+durl/*.replace(' ','+')*/ ), "file:"+npref+fp ); //+"/"+f );
			URLConnection ruc = null;
			if( log.isLoggable(Level.FINER)) log.finer("open url to cached data: "+u );
			ruc = u.openConnection();
			if( log.isLoggable(Level.FINE)) log.fine("returning url to cached data: "+u );
			
			return ruc;
		} catch( IOException ex ) {
			log.log(Level.WARNING, ex.toString(), ex );
		} finally {
		Thread.currentThread().setName( tnm );
		}

		return u.openConnection();
	}
}
