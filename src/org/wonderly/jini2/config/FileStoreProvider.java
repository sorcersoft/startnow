package org.wonderly.jini2.config;

import java.io.*;
import java.util.*;
import net.jini.id.*;

public class FileStoreProvider implements ConfigurationStoreProvider {
	protected File dir;
	public FileStoreProvider( File directory ) {
		dir = directory;
		dir.mkdirs();
	}

	protected String configName( ConfigurationSet conf ) {
		return conf.getName()+"="+conf.id.toString()+".txt";
	}

	protected String configName( ConfigurableId conf ) {
		return conf.getName()+"="+conf.uuid.toString()+".txt";
	}
	
	public void storeConfigurationSet( ConfigurationSet conf ) throws IOException {
		File f = new File( dir, configName( conf ) );
		FileOutputStream fs = new FileOutputStream( f );
		try {
			conf.writePersist( fs );
		} finally {
			fs.close();
		}
	}

	public ConfigurationSet retrieveConfigurationSet( ConfigurableId id ) throws IOException {
		File f = new File( dir, configName( id ) );
		ConfigurationSet cs = null;
		FileInputStream fs = new FileInputStream( f );
		try {
			cs = new ConfigurationSet( id.name, id, fs, f.length() );
		} finally {
			fs.close();
		}
		return cs;
	}

	public ConfigurationSet deleteConfigurationSet( ConfigurableId id ) throws IOException {
		File f = new File( dir, configName( id ) );
		ConfigurationSet cs = retrieveConfigurationSet( id );
		boolean del = f.delete();

		return cs;
	}
	
	private String nameFor( String file ) {
		return file.substring(0, file.indexOf('='));
	}
	
	private String uuidFor( String file ) {
		String str = file.substring( file.indexOf('=') + 1 ).toLowerCase();
		str = str.substring(0, str.indexOf( ".txt") );
		return str;
	}

	public List<ConfigurableId> getConfigurationSetKeys() throws IOException {
		String[]names = dir.list();
		ArrayList<ConfigurableId> al = new ArrayList<ConfigurableId>();
		for( int i = 0; i < names.length; ++i ) {
			Uuid uid = UuidFactory.create( uuidFor( names[i] ) );
			ConfigurableId id = new ConfigurableId( nameFor( names[i] ),
				uid );
			al.add( id );
		}
		return al;
	}
	
	public int getCount() throws IOException {
		String arr[] = dir.list();
		if( arr != null )
			return arr.length;
		return 0;
	}
}