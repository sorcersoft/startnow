package org.wonderly.jini.serviceui;

import net.jini.admin.*;
import java.rmi.*;

/**
 *  Services using PersistentJiniService, and wanting to be
 *  administrable, can implement this interface so that they
 *  get a Remote tagged interface that will cause rmic to export
 *  the correct methods into the proxy.
 *
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public interface RemoteAdministrable extends Administrable,Remote {
}