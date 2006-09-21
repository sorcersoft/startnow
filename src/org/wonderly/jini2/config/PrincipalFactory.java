package org.wonderly.jini2.config;

import java.security.*;
import java.io.*;
import java.util.*;

public interface PrincipalFactory {
	/**
	 *  @throws IOException if there is a problem getting the list of Principals
	 */
	public Set getUsablePrincipals() throws IOException;
}