package org.wonderly.threads;

/**
 *  This abstract class provides a foundation for Threads that need to run
 *  'forever'.  Subclasses should Implement <code>process()</code> to
 *  perform the task that should always be active.  If code inside of
 *  <code>process()</code> throws an exception (e.g. IOException
 *  or SocketException) either handle it, or close all sockets and/or
 *  I/O streams and let the main loop reinvoke process() and the
 *  code there can just start over.
 *  <pre>

new Thread( new ForeverThread() {
	public void process() throws IOException {
		Socket s = new Socket( "remotehost", reportport );
		try {
			BufferedReader rd = s.getInputStream();
			String line;
			while( (line = rd.readLine()) != null) {
				processLine( line );
			}
		} finally {
			s.close();
		}
	}
	public void reportException( Throwable ex ) {
		ex.printStackTrace();
	}
}, "MyProcessingThread").start(); 

...

public void processLine( String line ) {
	... do something with lines read from socket ...
}

 *  </pre>
 * 
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public abstract class ForeverThread implements Runnable {
        long errorSleep;
        protected Object lock = new Object();
        protected boolean stopping;

        /**
         *  Called to suggest that the thread stop
         */
        public void stop() {
                synchronized( lock ) {
                        stopping = true;
                        lock.notifyAll();
                }
        }
 
        /**
         *  Runs a forever loop catching <code>Throwable</code> to keep the thread
         *  running at all costs.
         */
        public void run() {
                while( !stopping ) {
                        try {
                                process();
                        } catch( Throwable ex ) {
                                reportException(ex);
                        }
                        try {
                                if( errorSleep > 0 ) {
                                        synchronized( lock ) {
                                                lock.wait(errorSleep);
                                        }
                                }
                        } catch( Throwable ex ) {
                                reportException(ex);
                        }
                }
        }
 
        /**
         *  Sets the time to delay between calls to process if process returns
         *  for any reason.  This will allow socket problems or other non-permanent
         *  error conditions to be continously probed, looking for the time to
         *  start processing again.
         */
        public void setErrorSleep( long millis ) {
                errorSleep = millis;
                synchronized( lock ) {
                        lock.notify();
                }
        }

        /**
         *  Implement this method, watching <code>stopping</code>
         *  for any loops or long term operations.  The <code>lock</code>
         *  should be used for sleeps via <code>lock.wait()</code>
         *  so that the thread will be woke when it is time to stop
         */
        public abstract void process() throws Exception;
        
        /**
         *  An exceptions that occur will be reported via this method.
         */
        public abstract void reportException( Throwable ex );
}
