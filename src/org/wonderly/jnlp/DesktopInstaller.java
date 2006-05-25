package org.wonderly.jnlp;

import javax.jnlp.*;
import java.net.*;
import java.io.*;

/**
 *  This class is an installer for installing the appropriate
 *  files to make the JiniDesktop be launchable as a JNLP application.
 *  This installer utilizes a set of files that are provided in
 *  the directory of the JNLP file on the server.  These files are
 *  <table>
 *	<tr><th>File<th>Installed in
 *  <tr><td>jini-ext.jar<td>JRE/lib/ext
 *  <tr><td>jini-core.jar<td>JRE/lib/ext
 *  <tr><td>sun-util.jar<td>JRE/lib/ext
 *  <tr><td>jsk-policy.jar<td>JRE/lib/ext
 *  <tr><td>jsk-resources.jar<td>JRE/lib/ext
 *  <tr><td>desktop.cfg<td>${user.home}
 *  <tr><td>desktop.policy<td>${user.home}
 *  </table>
 */
public class DesktopInstaller {
	private ExtensionInstallerService eis;
	private BasicService bs;
	
	/** Find installer and basic service and setup for install */
	public void installerSetup() throws Exception {
		eis = (ExtensionInstallerService)ServiceManager.lookup(
			"javax.jnlp.ExtensionInstallerService");
		bs = (BasicService)ServiceManager.lookup(
			"javax.jnlp.BasicService");
		eis.setHeading("Installing Jini platform");
		eis.setStatus("Installing Jini platform");
	}

	/** Do the installation of the files */
	public void installOptionalPackage() throws Exception {
		String extDir = "\\lib\\ext\\";

		// Get a URL pointing to the installable JRE6
		URL jreURL = null;
		try {
			jreURL = new URL("http://java.sun.com/products/autodl/j2se");
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Get installation location of latest JRE
		String jrePath = eis.getInstalledJRE(jreURL,"1.4.2_03");

		// Get path to JRE/lib/ext
		File javaFile = new File( jrePath );//where java is located
		String jreRoot = javaFile.getParentFile().getParent();
		File libDir = new File( new File(jreRoot), extDir );
		if (!libDir.exists())
			libDir.mkdirs();
		
		// Get dir to install config stuff into
		File userDir = new File( new File( System.getProperty("user.home") ),
			".startDesk" );
		if( !userDir.exists() )
			userDir.mkdirs();

		// The obvious thing to do here would be to get this list from
		// a file in the install directory.  However we can't get
		// all the locations without being in this context, so we'd
		// have to at least have a limited set of possible destination
		// directories and have a pseudo name reference.
		Object files[] = new Object[] {
			new Object[] { "jini-core.jar", libDir },
			new Object[] { "jini-ext.jar", libDir },
			new Object[] { "sun-util.jar", libDir },
//			new Object[] { "jsk-platform.jar", libDir },
			new Object[] { "jsk-resources.jar", libDir },
			new Object[] { "jsk-policy.jar", libDir },
			new Object[] { "desktop.cfg", userDir },
			new Object[] { "desktop.policy", userDir },
		};

		for( int i = 0; i < files.length; ++i ) {
			Object[] info = (Object[])files[i];
			String file = (String)info[0];
			File dir = (File)info[1];
			File instFile = new File( dir, file );
			eis.setStatus( "Installing: "+file+" in "+instFile );

			FileOutputStream fos = new FileOutputStream(instFile);
			try {
				URL fileURL = new URL(bs.getCodeBase(), file );
				URLConnection fileConn = fileURL.openConnection();
				int len = fileConn.getContentLength();
				int div = len / 100;
				InputStream is = fileConn.getInputStream();
				byte data[] = new byte[div];
				try {
					int q,tot=0;
					while ((q = is.read(data,0,data.length)) != -1) {
						fos.write(data,0,q);
						tot+=q;
						eis.updateProgress( (tot * 100 ) / len );
					}
				} finally {
					is.close();
				}
			} finally {
				fos.close();
			}
		}
		eis.installSucceeded(false);
	}

	/** Construct the installer */
	public DesktopInstaller() throws Exception {
		try {
			installerSetup();
		} catch( Exception ex ) {
			ex.printStackTrace();
			eis.installFailed();
			throw ex;
		}
		installOptionalPackage();
	}

	/** Main entry point to run as a JNLP app with all privs */
	public static void main(String[] args) throws Exception {
		DesktopInstaller installer = new DesktopInstaller();
		try {
			installer.installerSetup();
		} catch( Exception ex ) {
			ex.printStackTrace();
			installer.eis.installFailed();
			throw ex;
		}
		installer.installOptionalPackage();
	}
}