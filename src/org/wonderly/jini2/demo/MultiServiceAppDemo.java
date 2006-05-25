package org.wonderly.jini2.demo;

import org.wonderly.jini2.*;

public class MultiServiceAppDemo extends PersistentJiniService {
	public static void main( String args[] ) throws Exception {
		new MultiServiceAppDemo( args );
	}
	
	public MultiServiceAppDemo( String args[] ) throws Exception {
		super(args);
		log.fine("starting with "+args.length+" args");
		for( int i = 0; i < args.length; ++i ) {
			log.finer( "  args["+i+"]: "+args[i]);
		}
		new java.io.File("cfgs").mkdirs();
//		for( int i = 0; i < 10; ++i ) {
		int j = 0;
		String[]names = new String[] {
			"Director Management", "OverCyte 5.0", "Agency 5.0", "Event Director" };
			for( int i = 0; i < 4; ++i ) {
				JiniExplorerDemo jx = new JiniExplorerDemo( args, j, i );
				jx.getLogger().fine( "starting JiniExporerDemo("+j+"-"+i+")" );
				jx.startService( 
					names[i],
					"cfgs/cfg-"+j+"-"+i+".ser");
//				Thread.sleep(100);
			}
//		}
	}
}