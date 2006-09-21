package org.wonderly.jini2.config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.logging.*;

import org.wonderly.awt.*;
import org.wonderly.swing.*;

import org.wonderly.jini2.config.*;

import net.jini.id.*;
import java.util.*;
import org.wonderly.jini2.config.constraints.*;

public class ConfigurationEditor {
	ConstraintFactory consFact;
	JFrame frm;
	Logger log;
	Hashtable<String,ConstraintSet> cons;
	
	public static void main( String args[] ) {
		JFrame f = new JFrame("Testing");
		f.setLocationRelativeTo( null );
		f.setVisible(true);
		ConfigurationEditor ce = new ConfigurationEditor(
			f, null );
		ce.editConfigurationSet( null );
		f.setVisible(false);
		System.exit(1);
	}

	public ConfigurationEditor( JFrame parent, final Logger logger ) {
		frm = parent;
		if( log != null ) {
			this.log = logger;
		} else {
			this.log = Logger.getLogger( getClass().getName() );
		}
		cons = new Hashtable<String,ConstraintSet>();
		consFact = new ConstraintFactory() {
			public Collection<ConstraintSet> getConstraintSets() {
				log.fine("Getting constraintSets: "+cons );
				return cons.values();
			}
			public void addConstraintSet( ConstraintSet s ) {
				log.fine( "Add ConstraintSet: "+s );
				cons.put( s.getName(), s );
			}
		};
	}

	public ConfigurationSet editConfigurationSet( ConfigurationSet val ) {
		final JDialog dlg = new JDialog( frm, "Edit Configuration File", true );
		final boolean cancelled[] = new boolean[1];
		Packer pk = new Packer( dlg.getContentPane() );
		JTabbedPane tabs = new JTabbedPane(JTabbedPane.BOTTOM);
		final JTextField name = new JTextField(val == null ? null : val.getName() );
		final JTextArea area = new JTextArea( 
			val == null ? null : val.getConfigContents() );
		pk.pack( new JLabel( "Configuration Name:") ).gridx(0).gridy(0).inset(2,2,2,2);
		pk.pack( name ).gridx(1).gridy(0).fillx();
		if( val != null ) {
			name.setEditable(false);
			name.setText( val.getName() );
			area.setText( val.getConfigContents() );
		}
		JPanel tp = new JPanel();
		ConfigurableId id = null;
		if( val != null ) {
			id = val.getConfigurableId();
		}
		Packer tpk = new Packer( tp );
		tp.setBorder( BorderFactory.createTitledBorder( "Configuration File Content" ) );
		tpk.pack( new JScrollPane( area ) ).fillboth().gridx(0).gridy(1).gridw(2);
//		pk.pack( tp ).gridx(0).gridy(0).fillboth();
		tp.setPreferredSize( new Dimension( 400, 400 ) );
		JPanel sp = new JPanel();
		Packer spk = new Packer( sp );
		int sy = -1;
		spk.pack( new PreparerPane("Remote Lookup", consFact ) 
			).gridx(0).gridy(++sy).fillboth().gridw(2);
		spk.pack( new PreparerPane("Remote Service", consFact )
			).gridx(0).gridy(++sy).fillboth().gridw(2);
		spk.pack( new ExporterPane("Local Service", consFact ) 
			).gridx(0).gridy(++sy).fillboth().gridw(2);		
		spk.pack( new JLabel( "Logger") ).gridx(0).gridy(++sy);
		spk.pack( new JTextField() ).gridx(1).gridy(sy).fillx();
		spk.pack( new JLabel( "Login Context") ).gridx(0).gridy(++sy);
		spk.pack( new JTextField() ).gridx(1).gridy(sy).fillx();

//		tabs.add( "StandardConfiguration", sp );
		pk.pack( tp ).gridx(0).gridy(1).gridw(2).fillboth();
//		pk.pack( tabs ).gridx(0).gridy(1).gridw(2).fillboth();
//		tabs.add( "ConfigurationFile", tp );
		
		JPanel bp = new JPanel();
		Packer bpk = new Packer( bp );
		int by = -1;
		final JButton okay = new JButton("Okay");
		okay.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				cancelled[0] = false;
				dlg.setVisible(false);
			}
		});
		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				cancelled[0] = true;
				dlg.setVisible(false);
			}
		});
		bpk.pack( okay ).gridx(0).gridy(++by).fillx().weightx(0).inset(4,4,4,4);
		bpk.pack( cancel ).gridx(0).gridy(++by).fillx().weightx(0).inset(4,4,4,4);
		bpk.pack( new JPanel() ).gridx(0).gridy(++by).filly();
		pk.pack( bp ).gridx(2).gridy(0).gridh(2).fillboth().weightx(0);

		// Initialize for window closed case
		cancelled[0] = true;
		dlg.pack();
		dlg.setLocationRelativeTo( frm );
		dlg.setVisible(true);
		
		if( cancelled[0] == false ) {
			log.fine( "getConfigText was okayed");
			if( id == null )
				id = new ConfigurableId( name.getText(), UuidFactory.generate());
			return new ConfigurationSet( name.getText(), 
				id, area.getText() );
		}
		log.fine( "getConfigText was cancelled");
		return null;
		
	}
}