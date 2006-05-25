package org.wonderly.jini2.demo;

import java.rmi.*;

public interface DemoInterface extends Remote {
	public int getGroup() throws RemoteException;
	public int getInstance() throws RemoteException;
}