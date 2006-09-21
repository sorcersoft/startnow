package org.wonderly.jini2.config.test;

import java.io.*;
import net.jini.config.*;

public class ConcatenateTest {
	public static void main( String args[] ) throws Exception {
		Configuration c = ConfigurationProvider.getInstance( args );
		for( int i = 1; i <= 4; ++i ) {
			System.out.println("val"+i+" = "+
				c.getEntry("test.config.e"+i, "val"+i, String.class ) );
		}
	}
}