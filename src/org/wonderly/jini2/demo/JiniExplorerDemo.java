package org.wonderly.jini2.demo;

import org.wonderly.jini.browse.JiniExplorer;
import org.wonderly.util.jini.LookupEnv;
import org.wonderly.jini2.PersistentJiniService;
import java.io.IOException;
import net.jini.config.ConfigurationException;
import org.wonderly.jini2.NameableObjectImpl;

public class JiniExplorerDemo extends PersistentJiniService implements DemoInterface {
	public JiniExplorerDemo( String args[], int grp,
			int inst ) throws IOException,ConfigurationException {
		super(args);
		group = grp;
		instance = inst;
		log.fine("starting "+grp+"-"+instance );
	}

	int group, instance;
	public String getName() {
		return super.getName()+"_"+group;
	}

	public int getGroup() {
		return group;
	}

	public int getInstance() {
		return instance;
	}
}