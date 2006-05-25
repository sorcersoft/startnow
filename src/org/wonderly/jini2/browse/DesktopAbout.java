package org.wonderly.jini2.browse;

import java.awt.event.*;
import javax.help.*;
import java.net.*;
import javax.swing.*;
import org.wonderly.awt.*;
import java.util.*;
import java.awt.*;
import javax.imageio.*;
import java.awt.image.*;
import org.wonderly.swing.*;
import java.util.logging.*;

public class DesktopAbout extends LabeledAction {
	private JFrame par;
	private int year = new GregorianCalendar().get(Calendar.YEAR);
	private Logger log = Logger.getLogger( getClass().getName() );

	public DesktopAbout( String name, JFrame parent ) {
		super( name );
		par = parent;
	}
	
	public static void main( String args[] ) {
		final JFrame f = new JFrame();
		DesktopAbout da = new DesktopAbout( "EOI Desktop", f );
		f.setLocationRelativeTo( null );
		Packer pk = new Packer( f.getContentPane() );
		JButton b = new JButton( da );
		pk.pack( b ).fillboth();
		f.pack();
		f.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev ) {
				f.dispose();
			}
		});
		f.setVisible(true);
	}

	public void actionPerformed( ActionEvent ev ) {
		final JDialog dlg = new JDialog( par, "EOI Desktop v1.0", true );
		Packer pk = new Packer( dlg.getContentPane() );
		int y = -1;
		JLabel l;
		pk.pack( l = new JLabel((String)null) ).gridx(0).gridy(++y);
		try {
			URL u = new URL("file:/"+System.getProperty("user.dir")+"/bin/icon.bmp");
			BufferedImage im = ImageIO.read( u );
			l.setIcon( new ImageIcon( im ) );
		} catch( Exception ex ) {
			log.log( Level.SEVERE, ex.toString(), ex );
		}
		pk.pack( l = new JLabel("EOI Desktop v1.0") 
			).gridx(1).gridy(y).fillx().inset(10,10,10,10);
		l.setFont( new Font( "serif", Font.BOLD, 36 ) );
		pk.pack( new JSeparator() 
			).gridx(0).gridy(++y).gridw(2).fillx().inset(4,4,4,4);
		pk.pack( new JLabel("Copyright 2004-"+year )
			).gridx(0).gridy(++y).west().inset(0,3,0,3);
		pk.pack( new JLabel("Cyte Technologies Inc" )
			).gridx(0).gridy(++y).west().inset(0,3,0,3);
		pk.pack( l = new JLabel("http://www.cytetech.com" )
			).gridx(1).gridy(y-1).east().inset(0,3,0,10).gridh(3);
		l.setFont( new Font( "courier", Font.BOLD, 16 ) );
		l.setForeground( Color.blue );
		pk.pack( new JLabel("All Rights Reserved" )
			).gridx(0).gridy(++y).west().inset(0,3,0,3);
		pk.pack( new JSeparator() ).gridx(0).gridy(++y).gridw(2).fillx().inset(4,4,4,4);
		final JButton okay = new JButton("Okay");
		pk.pack( okay ).gridx(0).gridy(++y).gridw(2).inset(4,4,4,4);
		okay.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				dlg.setVisible(false);
			}
		});
		dlg.pack();
		dlg.setLocationRelativeTo( par );
		dlg.setVisible(true);
		dlg.dispose();
	}
}