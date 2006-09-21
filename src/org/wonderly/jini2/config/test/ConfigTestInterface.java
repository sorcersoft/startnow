package org.wonderly.jini2.config.test;

import java.rmi.*;

public interface ConfigTestInterface extends Remote {
	public void showConfig() throws RemoteException;
}