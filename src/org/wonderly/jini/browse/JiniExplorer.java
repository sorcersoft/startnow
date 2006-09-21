package org.wonderly.jini.browse;

import org.wonderly.jini.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.beans.*;
import org.wonderly.util.jini.*;
import org.wonderly.swing.*;
import org.wonderly.awt.*;
import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.entry.*;
import net.jini.core.event.*;
import net.jini.core.lease.*;
import net.jini.core.transaction.server.*;
import net.jini.entry.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.lease.*;
import net.jini.event.*;
import net.jini.space.*;
import net.jini.admin.*;
import net.jini.lookup.ui.factory.*;
import net.jini.lookup.ui.*;
import java.util.*;
import  org.wonderly.jini.serviceui.*;
import java.awt.*;
import java.io.*;
import java.rmi.*;

/**
 *  .This is an interface that provides a way for a Jini browser
 *  to get a set of LookupEnv objects from a remote service that
 *  is tasked at just providing the lookup environment to the
 *  browser.
 *
 *  @see JiniExplorerImpl
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */

public interface JiniExplorer extends Remote {
	public LookupEnv[] getLookups() throws IOException;
}