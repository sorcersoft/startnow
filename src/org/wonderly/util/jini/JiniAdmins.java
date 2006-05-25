package org.wonderly.util.jini;

import net.jini.admin.*;
import com.sun.jini.admin.*;

import java.rmi.*;
import java.rmi.server.*;
import com.sun.jini.admin.DestroyAdmin;
import net.jini.admin.JoinAdmin;
import com.sun.jini.admin.StorageLocationAdmin;

/**
 *  A convienent interface to collect all the standard administration interfaces
 *  and have a Remote Implementation of those.
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public interface JiniAdmins extends JoinAdmin,DestroyAdmin,StorageLocationAdmin,Remote {
}
