package org.wonderly.jini;

import java.util.*;
import java.security.*;
import java.security.acl.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;
import java.lang.reflect.*;

import org.wonderly.util.jini.*;
import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.core.entry.*;
import net.jini.core.lease.*;
import net.jini.lease.*;
import net.jini.admin.*;
import net.jini.entry.*;

import java.awt.Image;
import java.rmi.*;
import java.rmi.server.*;

/**
 *  This class provides a base class for creating persistant Jini services.
 *  This class provides all the necessary things needed to have a persistent
 *  configuration associated with all of the Jini administrative configuration.
 *  It uses the JiniAdmin class to perform all of the standard Jini admin
 *  tasks.  This class does not implement the Adminstrable interface, that
 *  is left to subclasses so that remote access is delegated only if you
 *  wish it to be.
 *
 *  @deprecated - use PersistentJiniService now
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class PersistantJiniService extends PersistentJiniService {
	/**
	 *  @deprecated - use PersistentJiniService now
	 */
	public PersistantJiniService() throws RemoteException {
	}
}
