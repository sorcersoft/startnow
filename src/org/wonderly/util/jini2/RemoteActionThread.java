package org.wonderly.util.jini2;

import org.wonderly.swing.*;
import javax.swing.*;
import java.util.logging.*;

/**
 *  This class provides a Remote friendly container of the SwingWorker functionality.
 *  It utilizes the {@link org.wonderly.swing.ComponentUpdateThread} class which
 *  provides automatic disabling of components during the remote actions.
 *
 *  This Object is designed to be long lived.  Each time {@link #start()} is called,
 *  a new {@link org.wonderly.swing.ComponentUpdateThread} is created to interface
 *  with the delegated-to methods in this class.
 *
 *  This class is similar to ComponentUpdateThread except that it allows the
 *  {@link #construct} method to throw an exception that will be reported through
 *  the  {@link #reportException(Throwable)}.
 */
public abstract class RemoteActionThread<T> {

	/** The ComponentUpdateThread instance that is currently running */
	protected ComponentUpdateThread<T> cut;
	/** An {@link java.util.logging.Logger} instance using the class name */
	protected Logger log = Logger.getLogger( getClass().getName() );
	/** JComponents to be disabled during processing */
	protected JComponent args[];
	/** Actions to be disablled during processing */
	protected Action aargs[];

	/**
	 *  Construct a new instance specifying the arguments
	 */
	public RemoteActionThread( JComponent... args ) {
		this.args = args;
	}

	/**
	 *  Construct a new instance specifying the arguments
	 */
	public RemoteActionThread( Action... args ) {
		this.aargs = args;
	}

	/**
	 *  This method should be called each time that this action
	 *  should be performed.
	 */
	public void start() {
		if( args != null ) {
			new ComponentUpdateThread<T>( args ) {
				public void setup() {
					super.setup();
					try {
						setup();
					} catch( Exception ex ) {
						reportException(ex);
					}
				}
				public T failed() {
					return null;
				}
				public T construct() {
					try {
						return construct();
					} catch( Exception ex ) {
						reportException(ex);
					}
					return failed();
				}
				public void finished() {
					try {
						RemoteActionThread.this.finished( getValue() );
					} catch( Exception ex ) {
						reportException(ex);
					} finally {
						super.finished();
						after();
					}
				}
			}.start();
		} else if( aargs != null ) {
			new ComponentUpdateThread<T>( aargs ) {
				public void setup() {
					super.setup();
					try {
						setup();
					} catch( Exception ex ) {
						reportException(ex);
					}
				}
				public T failed() {
					return null;
				}
				public T construct() {
					try {
						return construct();
					} catch( Exception ex ) {
						reportException(ex);
					}
					return failed();
				}
				public void finished() {
					try {
						RemoteActionThread.this.finished( getValue() );
					} catch( Exception ex ) {
						reportException(ex);
					} finally {
						super.finished();
						after();
					}
				}
			}.start();
		}
	}

	/**
	 *  Logs a trace of the passed exception to the active {@link java.util.Logger}
	 *  instance.
	 *  @see #log
	 */
	protected void reportException(Throwable t ) {
		log.log( Level.SEVERE, t.toString(), t);
	}

	public void setup() throws Exception {};
	public void finished( T val) throws Exception {};
	public abstract T construct() throws Exception;
	public void after() {};
}
