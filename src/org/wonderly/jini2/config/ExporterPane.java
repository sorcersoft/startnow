package org.wonderly.jini2.config;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import org.wonderly.swing.*;
import org.wonderly.awt.*;
import org.wonderly.jini2.config.constraints.*;

public class ExporterPane extends PreparerPane {
	public static void main( String args[] ) {
		final HashMap<String,ConstraintSet> cons =
			new HashMap<String,ConstraintSet>(13);
		new ExporterPane("Service", new ConstraintFactory() {
				public Collection<ConstraintSet> getConstraintSets() {
					return cons.values();
				}
				public void addConstraintSet( ConstraintSet s ) {
					cons.put( s.getName(), s );
				}
			}).doTest();
	}
	public ExporterPane() {
		type = "Exporter";
	}
	protected ClassItem[] getClasses() {
		return new ClassItem[] {
			new ClassItem("ActivationExporter",
				"net.jini.activation.ActivationExporter"),
			new ClassItem("BasicJeriExporter",
				"net.jini.jeri.BasicJeriExporter"),
			new ClassItem("IiopExporter",
				"net.jini.iiop.IiopExporter"),
			new ClassItem("InstantiatorAccessExporter",
				"com.sun.jini.phoenix.InstantiatorAccessExporter"),
			new ClassItem("JrmpExporter",
				"net.jini.jrmp.JrmpExporter"),
			new ClassItem("MonitorAccessExporter",
				"com.sun.jini.phoenix.MonitorAccessExporter"),
			new ClassItem("ProxyTrustExporter",
				"net.jini.security.proxytrust.ProxyTrustExporter"),
			new ClassItem("SunJrmpExporter",
				"com.sun.jini.phoenix.SunJrmpExporter"),
			new ClassItem("SystemAccessExporter",
				"com.sun.jini.phoenix.SystemAccessExporter")
		};
	}
	public ExporterPane( String tl, ConstraintFactory cf ) {
		this();
		preparePane(tl, cf);
	}
}