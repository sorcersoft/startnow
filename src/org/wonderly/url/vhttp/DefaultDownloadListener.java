/*
 * DefaultDownloadListener.java
 *
 * Created on July 30, 2008, 12:04 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.wonderly.url.vhttp;

import java.awt.Container;
import java.awt.HeadlessException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
// import org.wonderly.awt.Packer;

/**
 *
 * @author gregg
 */
public class DefaultDownloadListener implements DownloadListener {
	private volatile JDialog dlg;
	private volatile JFrame parent;
	private volatile JProgressBar bar;
	private volatile JLabel lblProg;
	private volatile JLabel lblInfo;
	private final URL url;
	private final long len;
	private volatile static int xoff, off;
	private final int x, y;
	private final Hashtable<URL,DefaultDownloadListener> map =
		new Hashtable<URL,DefaultDownloadListener>();
	private final Logger log = Logger.getLogger( getClass().getName() );

	public DefaultDownloadListener(final JFrame parent) {
		this.parent = parent;
		this.url = null;
		this.len = 0;
		this.x = 30;
		this.y = 30;
	}

	private DefaultDownloadListener( URL url, long len ) {
		this.url = url;
		this.len = len;
		synchronized( DefaultDownloadListener.class ) {
			x = off + xoff; 
			y = off;
			off += 30;
			if( off > 300 ) {
				off = 0;
				xoff += 10;
				if( xoff > 100 )
					xoff = 0;
			}
		}
		init(url,len);
	}

	public synchronized void downloadStarted(final URL url, final long fileLength) {
		if( map.get(url) != null ) {
			DefaultDownloadListener dl = map.remove(url);
			try {
				dl.dlg.dispose();
			} catch( Exception ex ) {
				Logger log = Logger.getLogger( Handler.class.getName() );
				log.log( Level.SEVERE, ex.toString(), ex );
			}
		}
		map.put( url, new DefaultDownloadListener( url, fileLength ) );
	}

	private void init( final URL url, final long fileLength ) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					bar = new JProgressBar(0,0, 1000);
					lblProg = new JLabel("0 of 0 - 0%", JLabel.CENTER);
					lblInfo = new JLabel("Downloading from "+url.getHost(), JLabel.CENTER);
					dlg = new JDialog(parent, "Downloading "+url.getFile(), false);
					dlg.setAlwaysOnTop( true );
					Container ct = dlg.getContentPane();
					BoxLayout bl = new BoxLayout( ct, BoxLayout.Y_AXIS );
//					Packer pk = new Packer(dlg.getContentPane());

					ct.add( lblProg );
//					pk.pack( lblProg ).fillx().gridx(0).gridy(0).inset(5,5,5,5);
					
					lblProg.setBorder( BorderFactory.createEtchedBorder() );

					ct.add( bar );
//					pk.pack( bar ).fillx().gridx(0).gridy(1).inset(5,5,5,5);
					ct.add( lblInfo );
//					pk.pack( lblInfo ).gridx(0).gridy(2).fillx().inset(5,5,5,5);

					lblInfo.setBorder( BorderFactory.createEtchedBorder() );

					bar.setMinimum(1);
					bar.setMaximum((int)fileLength);
					bar.setValue(1);
					bar.setStringPainted( true );

					dlg.setDefaultCloseOperation( JDialog.DO_NOTHING_ON_CLOSE );
					dlg.pack();
					dlg.setLocationRelativeTo(parent);
					dlg.setLocation( dlg.getLocation().x+x, dlg.getLocation().y+y );
					dlg.setVisible(true);
				}
			});
		} catch (HeadlessException ex) {
			log.log( Level.SEVERE, ex.toString(), ex );
		} catch (SecurityException ex) {
			log.log( Level.SEVERE, ex.toString(), ex );
		} catch (InterruptedException ex) {
			log.log( Level.SEVERE, ex.toString(), ex );
		} catch (InvocationTargetException ex) {
			log.log( Level.SEVERE, ex.toString(), ex );
		}
	}

	public void downloadProgress(URL url,final long progress) {
		final DefaultDownloadListener defl = map.get(url);
		if( defl == null )
			return;
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					defl.bar.setValue((int)progress);
					long max = defl.bar.getMaximum();
					int pct = (int)((progress  * 100) / max );
					defl.lblProg.setText(progress+" of "+max+" - "+pct+"%");
				}
			});
		} catch (InterruptedException ex) {
			log.log( Level.SEVERE, ex.toString(), ex );
		} catch (InvocationTargetException ex) {
			log.log( Level.SEVERE, ex.toString(), ex );
		}
	}

	public void downloadFinished(URL url ) {
		final DefaultDownloadListener defl = map.remove(url);
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					if( defl != null && defl.dlg != null ) {
						defl.dlg.dispose();
					}
				}
			});
		} catch (InterruptedException ex) {
			log.log( Level.SEVERE, ex.toString(), ex );
		} catch (InvocationTargetException ex) {
			log.log( Level.SEVERE, ex.toString(), ex );
		}
	}


	public void checking(URL url) {}
	public void checkComplete(URL url, boolean willDownload) {}

	void setParent(JFrame parent) {
		this.parent = parent;
	}
}
