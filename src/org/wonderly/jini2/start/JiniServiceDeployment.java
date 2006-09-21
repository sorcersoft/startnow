/*
 * JiniServiceDeployment.java0,
 *
 * Created on May 16, 2006, 1:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.wonderly.jini2.start;

import com.sun.jini.start.*;
import net.jini.id.*;
import java.io.*;
import java.rmi.Remote;
import java.util.*;
import net.jini.config.*;

/**
 *
 * @author gregg
 */
public interface JiniServiceDeployment extends Remote {
	public Uuid createNonActivatableServiceDeployment( String className, String config[],
			List<String> codebase, List<String>classpath, String policy,
 			String[]args, LifeCycle life ) throws IOException, ConfigurationException;
}
