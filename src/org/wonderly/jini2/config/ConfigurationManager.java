package org.wonderly.jini2.config;

import org.wonderly.jini2.*;
import net.jini.config.*;
import java.rmi.*;
import java.io.*;
import java.util.*;
import net.jini.core.event.*;
import java.util.logging.*;
import net.jini.admin.*;
import net.jini.core.entry.*;

/**
 *  This class provides a simple Configuration manager jini service
 *  that implements the ManagedConfiguration remote interface.
 *  This service can be used to store conglomerate Configurations
 *  associated with common platforms.
 */
public class ConfigurationManager 
		extends PersistentJiniService
		implements ManagedConfiguration {
//	protected Hashtable configs;
	protected String name = "Configuration Manager";
	protected String serName = "cfgman.ser";
	protected Hashtable<ConfigurableId,
			Vector<RemoteEventListener>> listeners =
		new Hashtable<ConfigurableId,Vector<RemoteEventListener>>();
	/** Sequence number of change of each config */
	protected Hashtable<ConfigurableId,Integer> seqs = 
		new Hashtable<ConfigurableId,Integer>();
	protected ConfigurationStoreProvider constore = 
		new SerializedConfigurationStore();

	public static void main( String args[] ) throws Exception {
		ConfigurationManager mgr = new ConfigurationManager(args);
	}

	public ConfigurationManager(String args[]) throws IOException, ConfigurationException {
		super(args);
		ConfigurationStoreProvider cp =
			(ConfigurationStoreProvider)conf.getEntry(
				getClass().getName(), "storeProvider",
				ConfigurationStoreProvider.class );
		if( cp != null ) {
			constore = cp;
		}
		startService( name, serName );
		resetCount();
	}

	
	public void addConfigurationListener( ConfigurableId id, RemoteEventListener lis ) throws IOException {
		Vector<RemoteEventListener> v = listeners.get(id);
		if( v == null ) {
			v = new Vector<RemoteEventListener>();
			listeners.put( id, v );
		}
		v.addElement( lis );
	}

	public boolean removeConfigurationListener( ConfigurableId id, RemoteEventListener lis ) throws IOException {
		Vector v = (Vector)listeners.get(id);
		if( v == null ) {
			return false;
		}
		int i = v.indexOf( lis );
		v.removeElement( lis );
		return i >= 0;
	}

	public List getConfigurationKeys() throws IOException {
		List l = constore.getConfigurationSetKeys();
		log.fine( "Returning keys for all configs: "+l );
		return l;
	}

	public synchronized void removeConfiguration( ConfigurableId id ) throws IOException {
		constore.deleteConfigurationSet(id);
		Integer cnt = (Integer)seqs.get(id);
		if( cnt == null )
			cnt = new Integer(-1);
		else
			cnt = new Integer(cnt.intValue()+1);
		notifyListeners( id, cnt.intValue(), ConfigurationChangedEvent.CONF_REMOVED );
		updateCount();
	}
	
	/**
	 *  Update an entry in our service definition so that
	 *  UIs can adjust the available configs.
	 */
	private void updateCount() {
		try {
			((JoinAdmin)getAdmin()).modifyLookupAttributes(
				new Entry[] { new ConfigurationCount() },
				new Entry[] { new ConfigurationCount( constore.getCount() ) } );
		} catch( Exception ex ) {
			log.log(Level.SEVERE, ex.toString(), ex );
		}
	}
	
	/**
	 *  Establish an Entry in our service definition so that
	 *  UIs can adjust the available configs.
	 */
	private void resetCount() {
		try {
			((JoinAdmin)getAdmin()).modifyLookupAttributes(
				new Entry[] { new ConfigurationCount() },
				new Entry[] { null } );
			((JoinAdmin)getAdmin()).addLookupAttributes(
				new Entry[] { new ConfigurationCount( constore.getCount() ) } );
		} catch( Exception ex ) {
			log.log(Level.SEVERE, ex.toString(), ex );
		}
	}

	private class SerializedConfigurationStore implements ConfigurationStoreProvider {
		Hashtable<ConfigurableId,ConfigurationSet> configs = 
			new Hashtable<ConfigurableId,ConfigurationSet>();
		/** 
		 *  Persistence implementation.  Override this in a 
		 *  subclass to store configs elsewhere
		 */
		public void storeConfigurationSet( ConfigurationSet conf ) throws IOException {
			configs.put( conf.id, conf );
			log.fine("Updated Config for: "+conf.id+" to be "+conf );
			updateState();
		}
	
		/** 
		 *  Persistence implementation.  Override this in a 
		 *  subclass to store configs elsewhere
		 */
		public ConfigurationSet retrieveConfigurationSet( ConfigurableId id ) throws IOException {
			log.fine( "returning Config for: "+id+" as "+configs.get(id) );
			return (ConfigurationSet)configs.get( id );
		}
	
		/** 
		 *  Persistence implementation.  Override this in a 
		 *  subclass to store configs elsewhere
		 */
		public ConfigurationSet deleteConfigurationSet( ConfigurableId id ) throws IOException {
			ConfigurationSet set = (ConfigurationSet)configs.remove( id );
			log.fine("removed Config for: "+id+", set: "+set+", remain: "+configs );
			updateState();
			return set;
		}
		
		public List<ConfigurableId> getConfigurationSetKeys() {
			return new ArrayList<ConfigurableId>( configs.keySet() );
		}

		public int getCount() throws IOException {
			return configs.size();
		}
	}

	public synchronized void storeConfiguration( ConfigurationSet conf ) throws IOException {
		constore.storeConfigurationSet( conf );

		Integer cnt = (Integer)seqs.get(conf.id);
		if( cnt == null )
			cnt = new Integer(1);
		else
			cnt = new Integer(cnt.intValue()+1);
		notifyListeners( conf.id, cnt.intValue(),
			ConfigurationChangedEvent.CONF_UPDATED );
		updateCount();
	}

	public ConfigurationSet getConfiguration( ConfigurableId id ) throws IOException {
		return constore.retrieveConfigurationSet(id);
	}
	
	/**
	 *  @param id the id of the configuration the event is for
	 *  @param seq the sequence number of the associated change
	 *  @param type one of the ConfigurationChangedEvent.CONF_* values
	 */
	protected boolean notifyListeners( ConfigurableId id, int seq, int type ) {
		Vector v = (Vector)listeners.get(id);
		if( v == null )
			return true;

		ConfigurationChangedEvent ev = null;
		try {
			ev = new ConfigurationChangedEvent( id, type, seq );
		} catch( IOException ex ) {
			log.log(Level.SEVERE, ex.toString(), ex );
			return false;
		}

		for( int i = 0; i < v.size(); ++i ) {
			RemoteEventListener rel = (RemoteEventListener)v.elementAt(i);
			try {
				rel.notify( ev );
			} catch( Throwable ex ) {
				log.log(Level.SEVERE, ex.toString(), ex );
			}
		}

		return true;
	}

	/**
	 *  Override this to serialize any additional data in the
	 *  serFile passed to the constructor
	 */
	public void writeAppState( ObjectOutputStream os ) throws IOException {
//		if( constore == this ) {
			os.writeInt( 2 );
//			os.writeObject( configs );
			os.writeObject( seqs );
			log.fine("Wrote state to persistence");
//		}
	}
	
	/**
	 *  Override this to deserialize any additional data to the
	 *  persistant state of the service.
	 */
	public void readAppState( ObjectInputStream is ) 
			throws IOException,ClassNotFoundException {
//		if( constore == this ) {
			int vers = is.readInt();
			if( vers < 2 )
				is.readObject();
			seqs = (Hashtable<ConfigurableId,Integer>)is.readObject();
			Hashtable<ConfigurableId,Integer> nseqs = 
				new Hashtable<ConfigurableId,Integer>();
			Iterator<ConfigurableId> i = constore.getConfigurationSetKeys().iterator();
			while( i.hasNext() ) {
				ConfigurableId key = i.next();
				if( seqs.get(key) == null ) {
					nseqs.put( key, new Integer(1) );
				} else {
					nseqs.put( key, seqs.get(key) );
				}
			}
			seqs = nseqs;
			log.fine("loaded ids as: "+seqs);
//		} else {
//			log.severe("I/O implenmentation override not storing");
//		}
	}
}