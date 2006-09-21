package org.wonderly.jini2.browse;

import java.security.*;
import org.wonderly.jini.*;
import org.wonderly.log.*;
import javax.swing.*;
import javax.help.*;
import java.util.Map;
import javax.swing.event.*;
import java.awt.event.*;
import java.beans.*;
import net.jini.id.*;
import org.wonderly.io.*;
import org.wonderly.swing.tabs.*;
import org.wonderly.jini.browse.JiniExplorer;
import org.wonderly.util.jini.*;
import org.wonderly.util.jini2.*;
import org.wonderly.util.jini2.RemoteListener;
import org.wonderly.swing.*;
import org.wonderly.awt.*;
import net.jini.discovery.*;
import net.jini.core.discovery.*;
import net.jini.core.lookup.*;
import net.jini.core.constraint.*;
import net.jini.core.entry.*;
import net.jini.loader.pref.*;
import net.jini.core.event.*;
import net.jini.core.lease.*;
import net.jini.core.transaction.server.*;
import net.jini.entry.*;
import net.jini.lookup.entry.*;
import net.jini.lookup.*;
import net.jini.lease.*;
import net.jini.event.*;
import net.jini.space.*;
import net.jini.loader.*;
import net.jini.admin.*;
import net.jini.lookup.ui.factory.*;
import net.jini.lookup.ui.*;
import org.wonderly.jini2.NameableObject;
import java.util.*;
import java.net.*;
import java.util.List;
import java.util.Set;
import org.wonderly.jini.serviceui.*;
import org.wonderly.jini.entry.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import javax.swing.tree.*;
import java.util.logging.*;
import net.jini.export.*;
import net.jini.config.*;
import net.jini.security.*;
import net.jini.security.policy.DynamicPolicyProvider;
import org.wonderly.jini2.ConfigurableJiniApplication;
import net.jini.core.lookup.ServiceLookup;
import org.wonderly.swing.VectorListModel;
import org.wonderly.util.jini2.entry.*;
import java.util.concurrent.*;
import java.util.prefs.*;
// import com.cytetech.jini.ApplicationUIDescriptor;

/**
 *  This is an application meant to be given to users in an environment
 *  where serviceUI is the means of getting to applications.  The users
 *  can startup this application, and it will find serviceUI accesible
 *  services, and then let the user organize a desktop and open and close
 *  serviceUI implementations.
 *
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */

public class JiniDeskTop extends ConfigurableJiniApplication 
		implements ServiceLookupHandler {
	private JDesktopPane pane;
	private LookupDiscoveryManager ldm;
	private JTree allList, moreList;
	private MyTreeModel allMod, moreMod;
	private DesktopItem curItem;
	private DragSource dgs;
	private NamedVector<NamedVector<DesktopItem>> allRoot;
	private NamedVector<NamedVector<DesktopItem>> moreRoot;
	private JComboBox mine;
	private JSplitPane morePane, allPane;
	private JCheckBoxMenuItem internals, tabs;
	private int allSplit= 200, moreSplit = 200;
	private boolean sized;
	private JFrame frame;
	private CloseableTabbedPane apptabs; 
	private boolean reqDlPerm = false;
	private JCheckBoxMenuItem tabsmi;
	private JTabbedPane atabs;
	private JCheckBoxMenuItem allDesc;
	private Hashtable<ServiceRegistration,Point> desklocs = 
		new Hashtable<ServiceRegistration,Point>();
	private JMenu grpMen;
	private Hashtable<String,JCheckBoxMenuItem> grpItems = 
		new Hashtable<String,JCheckBoxMenuItem>();
	private Hashtable<String,String> mygroups = 
		new Hashtable<String,String>();
	private Hashtable<DesktopItem,DesktopItem> items =
		new Hashtable<DesktopItem,DesktopItem>();
	private static DynamicPolicyProvider pol;
	private Hashtable<ServiceID,String> svcCodebases = 
		new Hashtable<ServiceID,String>();
	private Hashtable<ServiceID,Codebase> codebaseLoader =
		new Hashtable<ServiceID,Codebase>();
//	private JCheckBoxMenuItem autoPerms;
//	private JCheckBoxMenuItem reqdl;
	private ActionManager am = new ActionManager();
	private JCheckBoxMenuItem wrole, wicon;
	private JMenu helpMenu;
	private Preferences prefs = Preferences.userNodeForPackage( getClass() ).node("desktop");

	public JFrame getFrame() {
		return frame;
	}

	public LookupLocator[] getLocators( final NameableObject no,
			final boolean cache ) throws IOException,ConfigurationException {
		return getLocators();
	}

	public LookupLocator[] getLocators() throws IOException,ConfigurationException {
		log.fine("getLocators has "+hosts.size()+" locators to return");
		List<LookupLocator> lus = new ArrayList<LookupLocator>();

		for( String host : hosts ) {
			try {
				// Ignore empty entries.
				if( host != null && host.length() > 0 ) {
					log.info("Adding locator for: "+host );
					lus.add( new LookupLocator("jini://"+host ) );
				}
			} catch( Exception ex ) {
				log.log( Level.FINER, ex.toString(), ex );
			}
		}
		LookupLocator larr[] = new LookupLocator[lus.size()];
		return lus.toArray(larr);
	}

	// This is unfinished as is all related code referencing it
	private static class Codebase implements Serializable {
		private URL urls[];
		private ServiceID id;
		private String codebase;
		private transient ClassLoader ld;

		public Principal[] getPrincipals() {
			return null;
		}

		public Permission[] getPermissions() {
			return new Permission[] {
				new AllPermission()
			};
		}

		public Codebase( ServiceID id, URL u[],
					String codebase ) {
			this.urls = u;
			this.id = id;
			this.codebase = codebase;
		}
		
		public void setClassLoader( ClassLoader ld ) {
			this.ld = ld;
		}
	}

	// This is unfinished as is all related code referencing it
	private void activateCodebase( ServiceID id, 
				String codebase, boolean dlperm ) throws IOException {
		log.warning( "Activing codebase ("+id+"): "+codebase+", downloadperm="+dlperm );
		String arr[] = codebase.split(" ");
		URL u[] = new URL[arr.length];
		for( int i = 0; i < arr.length; ++i ) {
			u[i] = new URL(arr[i]);
		}
		codebaseLoader.put( id, new Codebase( id, u, codebase ) );
	}

	// This is unfinished as is all related code referencing it
	private void deactivateCodebase( ServiceID id ) {
		log.warning( "deactiving codebase ("+id+")");
		codebaseLoader.remove( id );
	}

	/** The tree model for service lists */
	protected static class MyTreeModel implements TreeModel {
		private NamedVector<NamedVector<DesktopItem>> root;
		private Vector<TreeModelListener> listeners = 
			new Vector<TreeModelListener>();
		public MyTreeModel( NamedVector<NamedVector<DesktopItem>> root ) {
			this.root = root;
		}
		public int size() {
			return root.size();
		}
		public Object getRoot() {
			return root;
		}
		public Object getChild( Object parent, int idx ) {
			return ((NamedVector)parent).elementAt(idx);
		}
		public int getChildCount( Object parent ) {
			return ((NamedVector)parent).size();
		}
		public boolean isLeaf( Object node ) {
			return node instanceof DesktopItem;
		}
		public void valueForPathChanged( TreePath path, Object val ) {
		}
		public int getIndexOfChild( Object par, Object node ) {
			return ((NamedVector)par).indexOf(node);
		}
		public void addTreeModelListener( TreeModelListener lis ) {
			listeners.addElement(lis);
		}
		public void removeTreeModelListener( TreeModelListener lis ) {
			listeners.removeElement(lis);
		}
		public void update() {
			for( int i = 0; i < listeners.size(); ++i ) {
				TreeModelListener l = (TreeModelListener)listeners.elementAt(i);
				l.treeStructureChanged(
					new TreeModelEvent( this, new Object[]{root} ) );
			}
		}
	}

	/** Each node in the tree model is one of these */
	protected static class NamedVector<T> extends Vector<T> {
		String name;
		public boolean equals( Object obj ) {
			if( obj instanceof NamedVector == false )
				return false;
			return ((NamedVector)obj).name.equals(name);
		}
		public int hashCode() {
			return name.hashCode();
		}

		public String contentsString() {
			return super.toString();
		}

		public String toString() {
			return name;
		}
		public NamedVector( String name ) {
			super();
			this.name = name;
		}
		public NamedVector( String name, int sz ) {
			super(sz);
			this.name = name;
		}
	}

	private static List<ProtectionDomain>domains;
	/** Entry point to the application.  A jini configuration is expected
	 *  to be passed as the arguments.
	 */
	public static void main( String args[] ) throws Exception {
		if( System.getProperty("java.protocol.handler.pkgs") == null ) {
			System.getProperties().put( "java.protocol.handler.pkgs",
				"net.jini.url|org.wonderly.url");
		}
		final Logger log = Logger.getLogger( JiniDeskTop.class.getName() );
/*
		ClassLoading.neverPrefer( Name.class.getName() );
		ClassLoading.neverPrefer( ServiceInfo.class.getName() );
		ClassLoading.neverPrefer( DesktopIcon.class.getName() );
		ClassLoading.neverPrefer( DesktopCodebase.class.getName() );
		ClassLoading.neverPrefer( DesktopEntry.class.getName() );
		ClassLoading.neverPrefer( DesktopGroup.class.getName() );
		ClassLoading.neverPrefer( UIDescriptor.class.getName() );
// 		ClassLoading.neverPrefer( ApplicationUIDescriptor.class.getName() );
		ClassLoading.neverPrefer( AdminDescriptor.class.getName() );
		ClassLoading.neverPrefer( JavaHelpEntry.class.getName() );
		ClassLoading.neverPrefer( HtmlHelpEntry.class.getName() );
		ClassLoading.neverPrefer( HelpEntry.class.getName() );
		ClassLoading.neverPrefer( MarshalledObject.class.getName() );
//		ClassLoading.neverPrefer( java.lang.reflect.Proxy.class.getName() );
//		ClassLoading.neverPrefer( net.jini.jeri.BasicInvocationHandler.class.getName() );
//		ClassLoading.neverPrefer( net.jini.jeri.BasicObjectEndpoint.class.getName() );
//		ClassLoading.neverPrefer( net.jini.jeri.BasicInvocationHandler.class.getName() );
//		ClassLoading.neverPrefer( net.jini.jeri.ssl.SslEndpoint.class.getName() );
//		ClassLoading.neverPrefer( net.jini.jeri.tcp.TcpEndpoint.class.getName() );
//		ClassLoading.neverPrefer( net.jini.id.UuidFactory.class.getName() );
//		ClassLoading.neverPrefer( net.jini.id.Uuid.class.getName() );
			ClassLoading.neverPrefer( com.artima.lookup.util.ConsistentSet.class.getName() );
			ClassLoading.neverPrefer( new Object[]{}.getClass().getName() );
			ClassLoading.neverPrefer( new byte[]{}.getClass().getName() );
*/
		// Use System look and feel by default.
		String laf = UIManager.getSystemLookAndFeelClassName();
		if( System.getProperty("org.wonderly.desktop.laf") != null ) {
			laf = System.getProperty("org.wonderly.desktop.laf");
		}
		try {
		  	UIManager.setLookAndFeel(laf);
		    // If you want the Cross Platform L&F instead, comment out the
			// above line and uncomment the following:

		    // UIManager.setLookAndFeel( 
		  	//	UIManager.getCrossPlatformLookAndFeelClassName());

		} catch (UnsupportedLookAndFeelException exc) {
		    System.err.println("Warning: UnsupportedLookAndFeel: " + laf);
		} catch (Exception exc) {
		    System.err.println("Error loading " + laf + ": " + exc);
		}

		domains = new ArrayList<ProtectionDomain>();
		// Establish a dynamic policy provider.
		Policy.setPolicy( pol = new DynamicPolicyProvider( Policy.getPolicy() ) {
			public Permission[]getGrants( Class cl, Principal[]prins ) {
				if( seclog.isLoggable( Level.FINE ) ) {
					seclog.fine( "getGrants("+cl+","+prins );
				}
				return super.getGrants(cl,prins);
			}
			public PermissionCollection getPermissions(CodeSource source) {
				PermissionCollection pc =  super.getPermissions(source);
				if( seclog.isLoggable( Level.FINE ) ) {
					seclog.fine("getPermissions source("+source+") "+pc);
				}
				return pc;
			}
			public PermissionCollection getPermissions(ProtectionDomain domain) {
				if( !here && seclog.isLoggable(Level.FINE) ) {
					here = true;
					seclog.fine("getPermissions domain("+domain.getCodeSource().getLocation()+")");	
					here = false;
				}
				return super.getPermissions(domain);
			}

			boolean here, here2;
			WeakHashMap<ProtectionDomain,String> urls = new WeakHashMap<ProtectionDomain,String>();
			public boolean implies(ProtectionDomain domain, Permission permission) {
//				return true;
//			}
//			public boolean ximplies(ProtectionDomain domain, Permission permission) {
				if( !here && seclog.isLoggable( Level.FINE ) ) {
					here = true;
					seclog.fine("implies ("+domain.getCodeSource().getLocation()+","+permission+")");
					here = false;
				}

				// Create a copy of the domains to avoid concurrent modifications
				List<ProtectionDomain> dms = new ArrayList<ProtectionDomain>();
				synchronized( domains ) {
					for( ProtectionDomain d : domains ) {
						dms.add(d);
					}
				}
				URL domurl = domain.getCodeSource().getLocation();
				if( domurl == null || domurl.getProtocol().equals("file") )
					return super.implies( domain, permission );
				if( log.isLoggable( Level.FINER) ) log.finer("domain url: "+domurl );
				String domurls = urlToString(domurl);
				for( ProtectionDomain d : dms ) {
					if( seclog.isLoggable( Level.FINER ) ) {
						seclog.finer("checking under: "+d.getCodeSource().getLocation().toString());
					}
					final URL du = d.getCodeSource().getLocation();
					String str = null;
					
//					boolean found = true;
//					synchronized( urls ) {
//						if( here ) 
//							return true;
//						here = true;
//						try {
							str = urls.get(d);
							if( str == null ) {
								str = urlToString(du);
								urls.put(d,str);
							}
//						} finally {
//							here = false;
//						}
//					}
	
					if( domurls.equals( str ) ) {
						if( d.implies(permission) ) {
							return true;
						}
					}

					if( !here2 && seclog.isLoggable( Level.FINER ) ) {
						here2 = true;
						seclog.finer("Checking under "+domain.getCodeSource().getLocation().toString()+" perm="+permission );
						here2 = false;
					}
				}
				return super.implies( domain, permission);
			}
		});
		loadPolicies( pol, domains );
	
		// Launch the desktop
		new JiniDeskTop(args.length == 0 ? 
			new String[]{ "file:/"+System.getProperty("user.home")+
				"/.startdesk/desktop.cfg"} : args);
	}

	static String urlToString( URL u ) {
		String us = u.getProtocol()+"://"+u.getHost()+":"+u.getPort()+u.getPath();
//		String ms = u.toString();
//		if( ms.equals(us) == false )
//			Logger.getLogger( JiniDeskTop.class.getName()).warning( u+" >> "+us );
		return us;
	}

	final static private Logger seclog = Logger.getLogger( "org.wonderly.jini.policy" );
	final static private ArrayList<String>hosts = new ArrayList<String>();
	private static void loadPolicies( DynamicPolicyProvider pol, List<ProtectionDomain> domains )
			throws IOException, ConfigurationException {

		Logger log = Logger.getLogger( JiniDeskTop.class.getName() );
		
		// Get the name of the hosts file
		String fn  = System.getProperty("org.wonderly.desktop.hosts");


		log.fine( "desktop hosts: "+fn );

		if( fn == null )
			return;
		FileReader fr = new FileReader( fn );
		try {
			BufferedReader rd = new BufferedReader( fr );
			String host;

			while( ( host = rd.readLine() ) != null ) {
				// Ignore empty lines
				if( host.trim().length() == 0 )
					continue;
				log.info("authorizing codesource host: "+host );
				enableForHost( host, domains );
			}

			if( log.isLoggable( Level.FINEST ) ) {
				log.finest( "domains are now: "+domains );
			}
			rd.close();
		} finally {
			fr.close();
		}
	}

	static HashMap<String,List<ProtectionDomain>>hostProts = new HashMap<String,List<ProtectionDomain>>();
	
	/**
	 *  @throws IOException when the "urls.txt" file can not be read successfully.
	 */
	protected static void enableForHost( String host, List<ProtectionDomain> domains ) throws IOException {

		Logger log = Logger.getLogger( JiniDeskTop.class.getName() );

		// Get the host address too, and ignore hosts which don't resolve.
		// This will speed up security domain traversals on multi site
		// configured setups.
		String haddr = null;
		if( System.getProperty("org.wonderly.desktop.security.includeHostaddress") != null ) {
			try {
				haddr = InetAddress.getByName(host).getHostAddress();
				log.info("authorizing host address too: "+haddr );
			} catch( java.net.UnknownHostException ex ) {
				log.log( Level.WARNING, ex.toString(), ex );
				return;
			}
		}

		// Get the name of the URLs file
		String pfn = System.getProperty("org.wonderly.desktop.policy.config");
		log.fine( "desktop policy: "+pfn );

		if( hosts.contains(host) == false ) {
			hosts.add( host );
		}
		
		// Create an AllPermission collection to use
		PermissionCollection pc = new Permissions();
		pc.add( new AllPermission() );

		List<ProtectionDomain>prots = null;
		synchronized( hostProts ) {
			prots = hostProts.get( host );
			if( prots != null ) {
				log.log( Level.FINE, "Already have protections for "+host, 
					new Throwable("Protections already exist for: "+host) );
				return;
			}
			prots = new ArrayList<ProtectionDomain>();
			hostProts.put( host, prots );
		}

		// Get all of the URLs in the urls.txt file
		String[]urls = readLines(pfn);
		for( String u : urls ) {
			if( seclog.isLoggable( Level.FINER ) ) {
				seclog.finer("adding an AllPermission domain for: "+"vhttp://"+host+":"+u );
			}

			ProtectionDomain pd;
			if( haddr != null ) {
				domains.add( pd = new ProtectionDomain( 
					new CodeSource( new URL( "vhttp://"+haddr+":"+u ), 
						(java.security.cert.Certificate[])null ), pc ) );
				prots.add( pd );
			}

			domains.add( pd = new ProtectionDomain( 
				new CodeSource( new URL( "vhttp://"+host+":"+u ), 
					(java.security.cert.Certificate[])null ), pc ) );
			prots.add( pd );

			if( seclog.isLoggable( Level.FINER ) ) {
				seclog.finer("adding an AllPermission domain for: "+"http://"+host+":"+u );
			}

			if( haddr != null ) {
				domains.add( pd = new ProtectionDomain( 
					new CodeSource( new URL( "http://"+haddr+":"+u ), 
						(java.security.cert.Certificate[])null ), pc ) );
				prots.add( pd );
			}

			domains.add( pd = new ProtectionDomain( 
				new CodeSource( new URL( "http://"+host+":"+u ), 
					(java.security.cert.Certificate[])null ), pc ) );
			prots.add( pd );
		}
	}

	private static String[] readLines( String name ) throws IOException {
		ArrayList<String>lst = new ArrayList<String>();
		FileReader fr = new FileReader( name );
		try {
			BufferedReader rd = new BufferedReader( fr );
			String ln;
			while( ( ln = rd.readLine() ) != null ) {
				lst.add(ln);
			}
			rd.close();
		} finally {
			fr.close();
		}
		String[]arr = new String[lst.size()];
		lst.toArray(arr);
		return arr;
	}

	/** Construct a new instance with the passed configuration */
	public JiniDeskTop( Configuration conf ) throws Exception {
		super(conf);
		buildFrame();
	}

	/** Place a DesktopItem at the indicated point */
	protected void placeItem( final DesktopItem it, final Point p ) {
		// If not on the desktop yet, add it
		if( items.get(it) == null ) {
			runInSwing( new Runnable() {
				public void run() {
					it.activate();
					pane.add( it, pane.DEFAULT_LAYER );
					it.setVisible(true);
					it.setSize(it.getPreferredSize());
					it.setVisible(true);
					it.requestFocus();
					log.fine( "added: "+it+" to "+pane+" at: "+p );
					pane.revalidate();
					items.put(it,it);
				}
			});
		}
		// Save the location and move it there
		desklocs.put( it.sr, p );
		runInSwing( new Runnable() {
			public void run() {
				it.setLocation( p.x, p.y );
			}
		});
	}
	
	/** Rebuild the groups display based on the passed set of group names */
	protected synchronized void buildGroups( Set<String> keys ) {
		grpMen.removeAll();
		synchronized( grpMen ) {
			grpMen.add( am.getAction( "Configure...") );
			grpMen.addSeparator();
		}
		Hashtable<String,JCheckBoxMenuItem> ogi = grpItems;
		grpItems = new Hashtable<String,JCheckBoxMenuItem>();
		for( final String g: keys ) {
//			final String g = keys.nextElement();
			final JCheckBoxMenuItem cb = new JCheckBoxMenuItem( g );
			cb.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					if( cb.isSelected() ) {
						moreRoot.addElement(groups.get(g));
						Collections.sort( moreRoot, 
							new VectorSorter<NamedVector<DesktopItem>>() );
						addMine( groups.get(g) );
						mygroups.put(g,g);
					} else {
						moreRoot.removeElement(groups.get(g));
						removeMine( groups.get(g) );						
						mygroups.remove(g);
					}
					saveGroups();
					moreMod.update();
				}
			});
			// Copy over selection of group, or select if one of mygroups
			JCheckBoxMenuItem ocb = (JCheckBoxMenuItem)ogi.get(g);
			cb.setSelected( (ocb != null && ocb.isSelected()) || mygroups.get(g) != null );
			// Remember the checkbox for the group
			grpItems.put( g, cb );
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					// Put the checkbox on the menu
					synchronized( grpMen ) {
						for( int i = 2; i < grpMen.getItemCount(); ++i ) {
							JMenuItem itm = grpMen.getItem(i);
							if( itm.getText().compareTo( g ) > 0 ) {
								grpMen.insert( cb, i );
								return;
							}
						}
						grpMen.add( cb );
					}
				}
			});
			// Find the destktop items in this group
			NamedVector<DesktopItem> v = groups.get(g);
			log.fine("buildgroups: mygroups("+g+") = "+mygroups.get(g));
			// If the group is not selected any longer, remove the items for the group
			if( mygroups.get(g) != null ) {
//				addMine(v);
			} else {
				removeMine(v);
			}
		}
	}

	/** Get a file name for an application file stored under ${user.home}/.jinidesk.
	 *  Any required directories are created.
	 */
	protected File fileFor( String name ) {
		File d = new File( System.getProperty("user.home"), ".jinidesk" );
		d.mkdirs();
		return new File( d, name );
	}

	/** Load the saved data from the known storage location returned by,
	 *  fileFor("group.ser") 
	 */
	protected NamedVector<NamedVector<DesktopItem>> loadGroups() {
		try {
			FileInputStream fs = new FileInputStream( fileFor( "groups.ser") );
			final ObjectInputStream is = new ObjectInputStream( fs );
			try {
				if( moreRoot == null )
					moreRoot = new NamedVector<NamedVector<DesktopItem>>( "My Apps");
				int vers = is.readInt(); // v1
				log.fine("Loading groups: "+vers );
				int cnt = is.readInt();
				mygroups.clear();
				log.fine(cnt+": groups to load");
				for( int i = 0; i < cnt; ++i ) {
					String name = (String)is.readObject();
					log.finer("group at "+i+"="+name);
					mygroups.put(name,name);
					if( moreRoot.contains(new NamedVector<DesktopItem>(name)) == false ) {
						final NamedVector<DesktopItem> v = new NamedVector<DesktopItem>(name);
						groups.put( name, v );
						mygroups.put(name,name);
						runInSwing( new Runnable() {
							public void run() {
								moreRoot.addElement(v);
								allRoot.addElement(v);
							}
						});
					}
				}
				Collections.sort( moreRoot, new VectorSorter<NamedVector<DesktopItem>>() );
				buildGroups( groups.keySet() );
				Enumeration e = groups.keys();
				while( e.hasMoreElements() ) {
					String g = (String)e.nextElement();
					JCheckBoxMenuItem mi = (JCheckBoxMenuItem)grpItems.get(g);
					mi.setSelected(true);
				}
				if( vers > 1 ) {
					Point p = (Point)is.readObject();
					Dimension d = (Dimension)is.readObject();
					frame.setSize( d );
					frame.setLocation(p);
					sized = true;
					final int sp1 = is.readInt();
					final int sp2 = is.readInt();
					runInSwing( new Runnable() {
						public void run() {
							if( morePane != null ) {
								morePane.setDividerLocation( moreSplit = sp1 );
								morePane.revalidate();
								morePane.repaint();
							}
							if( allPane != null ) {
								allPane.setDividerLocation( allSplit = sp2 );
								allPane.revalidate();
								allPane.repaint();
							}
						}
					});
				}
				if( vers > 2 ) {
					desklocs = new HashtableReader<ServiceRegistration,
						Point>(is).read();
				}
				if( vers > 3 ) {
					final boolean ival = is.readBoolean();
					final boolean rval = is.readBoolean();
					runInSwing( new Runnable() {
						public void run() {
							wicon.setSelected( ival );
							wrole.setSelected( rval );
						}
					});
				}
				return moreRoot;
//				return (NamedVector)is.readObject();
			} finally {
				is.close();
			}
		} catch( FileNotFoundException ex ) {
		} catch( Exception ex ) {
			reportException(ex);
		}
		return moreRoot;
	}

	/** Save everything to "groups.ser" */
	protected void saveGroups() {
		log.fine("Saving groups" );
		try {
			FileOutputStream fs = new FileOutputStream( fileFor( "groups.ser") );
			ObjectOutputStream os = new ObjectOutputStream(fs);
			try {
				os.writeInt( 4 );
				os.writeInt(moreRoot.size());
				for( int i = 0; i < moreRoot.size(); ++i ) {
					String name = moreRoot.elementAt(i).name;
					os.writeObject(name);
				}

//				os.writeObject( moreRoot );
				if( frame != null ) {
					os.writeObject( frame.getLocation() );
					os.writeObject( frame.getSize() );
				} else {
					os.writeObject( new Point( 100,100 ) );
					os.writeObject( new Dimension( 600, 400 ) );
				}
				os.writeInt( moreSplit );
				os.writeInt( allSplit );
				os.writeObject( desklocs );
				os.writeBoolean( wicon.isSelected() );
				os.writeBoolean( wrole.isSelected() );
			} finally {
				os.close();
			}
		} catch( IOException ex ) {
			reportException(ex);
		}
	}
	
	/** Reset the list of services to restart discovery */
	protected void flushServices() {
		log.finer("flushServices: minev is: "+minev );
		resetAll();
//		moreList.reload();
//		allList.reload();
	}
	
	private synchronized void removeMine( final NamedVector<DesktopItem> v ) {
		runInSwing( new Runnable() {
			public void run() {
				log.finest("removeMine: removing: "+v+", from: "+minev );
				for( int i = 0; i < v.size(); ++i ) {
					minev.removeElement( v.elementAt(i) );
				}
				log.finest("removeMine now have: "+minev );
				mine.setModel( new DefaultComboBoxModel( minev ) );
			}
		});
	}

	private synchronized void addMine( final NamedVector<DesktopItem> v ) {
		runInSwing( new Runnable() {
			public void run() {
				log.finest("addMine: "+v+", to: "+minev );
				for( int i = 0; i < v.size(); ++i ) {
					addMineItem( v.elementAt(i) );
//					mine.addItem( v.elementAt(i) );
				}
//				mine.setModel( new DefaultComboBoxModel( minev ) );
			}
		});
	}

	private volatile boolean userSelMine;
	private volatile MouseMotionAdapter mml;
	private Vector<DesktopItem> appitems;
	private JMenu applmenu;

	/** Create a new instance using the passed arguments to create a
	 *  Configuration instance
	 */
	public JiniDeskTop(String args[]) throws Exception {
		super(args);
		buildFrame();
	}
	
	/** Build the base frame for the application */
	private void buildFrame() throws Exception {
		int threadCnt = ((Integer)conf.getEntry( getClass().getName(), "entryThreads",
			Integer.class, new Integer(4) )).intValue();
		int queueDepth = ((Integer)conf.getEntry( getClass().getName(), "queueDepth",
			Integer.class, new Integer(20) )).intValue();
		entryQueue = new ArrayBlockingQueue<Runnable>( queueDepth );
		entryExec = new ThreadPoolExecutor( 1, threadCnt, queueDepth,
			TimeUnit.SECONDS, entryQueue, 
			new RejectedExecutionHandler() {
				public void rejectedExecution(Runnable r, ThreadPoolExecutor exec ) {
					try {
						entryQueue.put(r);
					} catch( InterruptedException ex ) {
						log.log( Level.SEVERE, ex.toString(), ex );
					}	
				}
			}
		);
		// build all known actions into options with the ApplicationManager
		buildActions();
		
		// Dump out the current class loader for the case that we are launched
		// from the network and want to see what is happening
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL us[] = {};
		if( classLoader instanceof URLClassLoader ) {
			us = ((URLClassLoader)classLoader).getURLs();
		}
		if( log.isLoggable( Level.FINE ) ) {
			log.fine("Current classLoader: "+classLoader );
			for( int i = 0; us != null && i < us.length; ++i ) {
				log.finer( "  loader URL["+i+"]: "+us[i] );
			}
		}

		// Create a PreferredClassLoader instance to watch what happens as
		// clases are loaded.
		PreferredClassLoader ld = new PreferredClassLoader(
				us,	classLoader, null, true ) {
			public boolean isPreferredResource(String name, 
					boolean isClass) throws IOException {
				Logger.getLogger("net.jini.loader.pref.PreferredClassLoader").
					fine("Check preferred resource[isClass="+isClass+"]: \""+name+"\"");
				boolean pref = super.isPreferredResource( name, isClass );
				Logger.getLogger("net.jini.loader.pref.PreferredClassLoader").
					fine("Checked preferred resource[isClass="+isClass+"]: \""+name+"\": "+pref);
				return pref;
			}

			protected Class loadClass(String name, boolean resolve) 
						throws ClassNotFoundException {
				Logger.getLogger("net.jini.loader.pref.PreferredClassLoader").
					fine("loadClass( \""+name+"\", "+
					(resolve ? "resolve" : "noresolve" )+")" );	
				Class cl = null;
				try {
					cl = super.loadClass( name, resolve );
				} catch( ClassNotFoundException ex ) {
					Logger.getLogger("net.jini.loader.pref.PreferredClassLoader").
						fine("load fail Class ( \""+name+"\", "+
						(resolve ? "resolve" : "noresolve" )+"): "+ex );
					throw ex;
				}
				Logger.getLogger("net.jini.loader.pref.PreferredClassLoader").
					fine("loaded Class ( \""+name+"\", "+
					(resolve ? "resolve" : "noresolve" )+"): "+cl );
				return cl;
			}
			public String toString() {
				return super.toString();
			}
		};

		// Install preferred class loader as context class loader
		log.fine( "installing new context class loader: "+ld );
		Thread.currentThread().setContextClassLoader( ld );

		// Create the top level frame object
		apptabs = new CloseableTabbedPane( JTabbedPane.BOTTOM );
		frame = deskContext = new JDeskFrame( 
			System.getProperty("user.name")+"'s EOI Desktop", apptabs );

		// Support vhttp interactions
		org.wonderly.url.vhttp.Handler.setParent( frame );
		log.config("vhttp frame set to: "+frame );
		String cd = System.getProperty("org.wonderly.desktop.cache");
		if( cd == null )
			cd = System.getProperty("user.home").replace('\\','/')+ "/.jarcache/desktop";
		File cdf = new File( cd );
		org.wonderly.url.vhttp.Handler.setCacheDir( cd );
		cdf.mkdirs();
		
		// Create inner desktop
		pane = new JDesktopPane();
		pane.setForeground( Color.black );
		pane.setOpaque(false);
		pane.setBackground( new Color( 200, 200, 225 ) );
		pane.setBorder( BorderFactory.createEtchedBorder() );

		final JPanel p = new JPanel();
		final Packer ppk = new Packer(p);
		JPanel tools = new JPanel();
		Packer tlpk = new Packer( tools );
		ppk.pack( tools ).gridx(0).gridy(0).fillx();
		mine = new JComboBox();
		mine.setMaximumRowCount(20);
		tlpk.pack( new JLabel("My Apps:") ).gridx(0).gridy(0).inset(0,6,0,0);
		tlpk.pack(mine).gridx(1).gridy(0).fillx().inset(3,3,3,3);

		mine.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				log.finest("mine action: "+ev);
				if( userSelMine ) {
					new ComponentUpdateThread( mine ) {
						public Object construct() {
							try {
								if( mine.getSelectedItem() != null ) {
								   	((DesktopItem)mine.getSelectedItem()).invoke();
								}
							} catch( Exception ex ) {
								reportException(ex);
							}
							return null;
						}
					}.start();
				}
			}
		});

		mine.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				log.finest("mine item: "+ev);
			}
		});

		mine.addPopupMenuListener( new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent ev) {
				log.finest("mine popup: "+ev);
			}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent ev) {
				log.finest("mine will invis: "+ev);
				userSelMine = false;
			}
			public void popupMenuWillBecomeVisible(PopupMenuEvent ev) {
				log.finest("mine will vis: "+ev);
				userSelMine = true;
			}
		});

		atabs = new JTabbedPane( JTabbedPane.TOP );
		JPanel ppane = new JPanel();
		final Packer pk = new Packer( ppane );
		apptabs.setUnclosableTab(0);
		apptabs.addTabCloseListener( new TabCloseListener() {
			public void tabClosed( TabCloseEvent ev ) {
				int idx = ev.getClosedTab();
				deskContext.shutdownTabUI( idx );
			}
		});
		apptabs.add( "Desktop", pane );
		appitems = new Vector<DesktopItem>();
		pk.pack( apptabs ).fillboth();
		atabs.add( "My Apps", ppane );
		pane.addMouseListener( new MouseAdapter() {
			public void mousePressed( MouseEvent ev ) {
				if( lastItem != null ) {
					lastItem.exit();
					lastItem = null;
				}
			}
		});

		apptabs.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent ev ) {
				int idx = apptabs.getSelectedIndex();

				if( idx <= 0 ) {
					log.info("No appl menu for desktop will be visible");
					applmenu.removeAll();
					applmenu.setEnabled(false);
					return;
				}

				DesktopItem di = (DesktopItem)appitems.elementAt(idx);
				if( di == null ) {
					log.info("oops, no desktop item at tab index: "+idx );
					return;
				}
				applmenu.removeAll();
				
				log.info("di.menus.size()="+di.menus.size() );
				if( di.menus.size() > 0 ) {
					for( int i = 0; i < di.menus.size(); i++ ) {
						applmenu.add((JMenu)di.menus.elementAt(i));
					}
				} else {
					// Need to account for application shutdown
					// and removal from UI.  The reordering
					// of the tabs means readjusting the 
					// data items.
					//
					// If we add a JMenuItem to allow the user to
					// close the application, we'll have to clean
					// up the datastructures which we've created
					// that are based on the tab index.
				}
				applmenu.setEnabled(applmenu.getItemCount() > 0 );
			}			
		});

		morePane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		allPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		grpMen = new JMenu("My Groups");
		synchronized( grpMen ) {
			grpMen.add( am.getAction("Configure...") );
			grpMen.addSeparator();
		}
		allRoot = new NamedVector<NamedVector<DesktopItem>>("Apps");
 		wicon = new JCheckBoxMenuItem( "Icons on Left" );
 		wrole = new JCheckBoxMenuItem( "Roles on Icons" );
 		wicon.setSelected( prefs.getBoolean( wicon.getText(), true ) );
 		wrole.setSelected( prefs.getBoolean( wrole.getText(), false ) );

		moreRoot = loadGroups();
		if( moreRoot == null )
			moreRoot = new NamedVector<NamedVector<DesktopItem>>("Apps");
			
		moreList = new JTree(moreMod = new MyTreeModel(moreRoot));
		final TreeCellRenderer orend = moreList.getCellRenderer();
		final TreeCellRenderer rend = new TreeCellRenderer() {
			public Component getTreeCellRendererComponent( JTree tree, Object value,
				boolean sel, boolean expand, boolean leaf,
					int row, boolean focus ) {
				JLabel l = (JLabel)orend.getTreeCellRendererComponent( tree, value,
					sel , expand, leaf, row, focus );
				if( value instanceof DesktopItem ) {
					DesktopItem di = (DesktopItem)value;
					l.setForeground( !di.isActive() ? l.getForeground() : Color.gray  );
					if( di.isActive() )
						l.setBorder( BorderFactory.createRaisedBevelBorder() );
					else
						l.setBorder( null );
					if( di.icon != null ) {
						l.setIcon( new ScaledImageIcon( di.icon, moreList ) );
					} else {
						l.setIcon( new ScaledImageIcon(
							new ImageIcon("images/bigservice.jpg"), moreList ) );
					}
				} else {
					l.setBorder( null );
					if( expand ) {
						l.setIcon( new ScaledImageIcon(
							((DefaultTreeCellRenderer)orend).getOpenIcon(), moreList ) );
					} else {
						l.setIcon( new ScaledImageIcon(
							((DefaultTreeCellRenderer)orend).getClosedIcon(), moreList ) );
					}
				}
//				l.setOpaque(true);
				return l;
			}
		};
		moreList.setCellRenderer( rend );
		moreList.setRowHeight(18);

		allList = new JTree(allMod = new MyTreeModel(allRoot));
		allList.setCellRenderer( rend );
		allList.setRowHeight(18);
		allList.addMouseListener( new MouseAdapter() {
			public void mouseClicked( MouseEvent ev ) {
				if( ev.getClickCount() == 2 && ev.getButton() == 1 ) {
					new ComponentUpdateThread( allList ) {
						public Object construct() {
							try {
								invokePath( allList.getSelectionPath() );
								log.fine("Client invoked");
							} catch( Exception ex ) {
								reportException (ex);
							}
							return null;
						}
					}.start();			
				}
			}
	
			public void mousePressed( MouseEvent ev ) {
				trigger(ev);
			}
			public void mouseReleased( MouseEvent ev ) {
				trigger(ev);
			}
			public void trigger( MouseEvent ev ) {
				if( ev.isPopupTrigger() ) {
						DesktopItem it = deskItemFor( allList.getSelectionPath() );
						if( it == null ) {
							int r = allList.getClosestRowForLocation( ev.getX(), ev.getY() );	
							if( r == -1 ) 
								return;
							TreePath p = allList.getPathForRow(r);
							if( p != null ) {
								it = deskItemFor( p );
								if( it == null )
									return;
							}
						}
						if( it != null )
							it.popup(ev, allList);
					}
				}
			}
		);

		moreList.addMouseListener( new MouseAdapter() {
			public void mouseClicked( MouseEvent ev ) {
				if( ev.getClickCount() == 2 && ev.getButton() == 1 ) {
					new ComponentUpdateThread( moreList ) {
						public Object construct() {
							try {
								invokePath( moreList.getSelectionPath() );
								log.fine("Client invoked");
							} catch( Exception ex ) {
								reportException (ex);
							}
							return null;
						}
					}.start();			
				}
			}
			public void mousePressed( MouseEvent ev ) {
				trigger(ev);
			}
			public void mouseReleased( MouseEvent ev ) {
				trigger(ev);
			}
			public void trigger( MouseEvent ev ) {
				if( ev.isPopupTrigger() ) {
					DesktopItem it = deskItemFor( moreList.getSelectionPath() );
					if( it == null ) {
						int r = moreList.getClosestRowForLocation( ev.getX(), ev.getY() );	
						if( r == -1 ) 
							return;
						TreePath p = moreList.getPathForRow(r);
						if( p != null ) {
							it = deskItemFor( p );
							if( it == null )
								return;
						}
					}
					if( it != null )
						it.popup(ev, moreList);
				}
			}
		});

		morePane.setLeftComponent( new JScrollPane( moreList ) );
		allPane.setLeftComponent( new JScrollPane( allList ) );
		new DropTarget( pane, new DropTargetAdapter() {
			public void drop( final DropTargetDropEvent dtde) {
				int act = dtde.getDropAction();
				Transferable t = dtde.getTransferable();
				try {
					Object o = t.getTransferData(
						new DataFlavor(
							DataFlavor.javaJVMLocalObjectMimeType) );
					log.finer("dropped: "+o);
					final DesktopItem it = (DesktopItem)o;

					runInSwing( new Runnable() {
						public void run() {
							placeItem(it,dtde.getLocation());
						}
					});
				} catch( Exception ex ) {
					reportException(ex);
				}
			}
		});
		atabs.add( "More Apps", morePane );
		atabs.add( "All Apps", allPane );
		atabs.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent ev ) {
				log.finer("moreSplit: "+moreSplit+
					", morePane: "+morePane.getDividerLocation()+
					", allSplit: "+allSplit+
					", allPane: "+allPane.getDividerLocation() );
				if( atabs.getSelectedIndex() == 0 ) {
					if( apptabs.getParent() == allPane ) {
						allSplit = allPane.getDividerLocation();
					} else if( apptabs.getParent() == morePane ) {
						moreSplit = morePane.getDividerLocation();
					}
					apptabs.getParent().remove(apptabs);
					pk.pack( apptabs ).fillboth();
				} else if( atabs.getSelectedIndex() == 1 ) {
					if( apptabs.getParent() == allPane ) {
						allSplit = allPane.getDividerLocation();
					}
					apptabs.getParent().remove(apptabs);
					morePane.setRightComponent( apptabs );
					morePane.setDividerLocation(moreSplit);
					morePane.revalidate();
				} else if( atabs.getSelectedIndex() == 2 ) {
					if( apptabs.getParent() == morePane ) {
						moreSplit = morePane.getDividerLocation();
					}
					apptabs.getParent().remove(apptabs);
					allPane.setRightComponent( apptabs );
					allPane.setDividerLocation(allSplit);
					allPane.revalidate();
				}
			}
		});
		TreeSelectionListener lis = new TreeSelectionListener() {
			public void valueChanged( TreeSelectionEvent ev ) {
				Object o = ev.getPath().getLastPathComponent();
				if( o instanceof DesktopItem )
					curItem = (DesktopItem)o;
			}
		};
		dgs = DragSource.getDefaultDragSource();
		DragGestureRecognizer adgr = dgs.createDefaultDragGestureRecognizer(
			allList, -1, new DragGestureListener() {
				public void dragGestureRecognized(DragGestureEvent dge) {
					if( curItem == null )
						return;
					dgs.startDrag( dge, null, 
						new Transferable() {
							public DataFlavor[] getTransferDataFlavors() {
								try {
									return new DataFlavor[] {
										new DataFlavor( 
											DataFlavor.javaJVMLocalObjectMimeType )
									};
								} catch( Exception ex ) {
									reportException(ex);
								}
								return new DataFlavor[0];
							}
							public boolean isDataFlavorSupported(DataFlavor flav) {
								return true;
							}
							public Object getTransferData(DataFlavor flav) {
								if( ! wicon.isSelected() ) {
									curItem.setHorizontalTextPosition(curItem.CENTER);
									curItem.setVerticalTextPosition(curItem.BOTTOM);
								} else {
									curItem.setHorizontalTextPosition(curItem.RIGHT);
									curItem.setVerticalTextPosition(curItem.CENTER);
								}
								curItem.setText( curItem.name+(wrole.isSelected() ? " ("+curItem.role+")" : "") );
								return curItem;
							}
						},null );
				}
			});
		DragGestureRecognizer mdgr = dgs.createDefaultDragGestureRecognizer(
			moreList, -1, new DragGestureListener() {
				public void dragGestureRecognized(DragGestureEvent dge) {
					if( curItem == null )
						return;
					dgs.startDrag( dge, null, 
						new Transferable() {
							public DataFlavor[] getTransferDataFlavors() {
								return new DataFlavor[] {
								};
							}
							public boolean isDataFlavorSupported(DataFlavor flav) {
								return true;
							}
							public Object getTransferData(DataFlavor flav) {
								return curItem;
							}
						},null );
				}
			} );
		allList.addTreeSelectionListener(lis);
		moreList.addTreeSelectionListener(lis);
		ppk.pack( atabs ).gridx(0).gridy(1).fillboth();
		frame.setContentPane(p);
		JMenuBar bar = new JMenuBar();
		frame.setJMenuBar( bar );
		JMenu m = new JMenu("File");
		bar.add(m);
		
		JMenuItem mi = new JMenuItem("Exit");
		m.add(getAction("Rescan Services"));
		m.addSeparator();
		m.add(mi);
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				checkExit();
			}
		});

		m = new JMenu( "Options" );
		bar.add(m);
//		m.add( autoPerms = new JCheckBoxMenuItem("Add Perms Automatically") );
//		autoPerms.addActionListener( new ActionListener() {
//			public void actionPerformed(ActionEvent ev) {
//			}
//		});
//		autoPerms.setSelected( ((Boolean)conf.getEntry( getClass().getName(),
//			"autoPermissions", Boolean.class, new Boolean(false) )).booleanValue() );
//		autoPerms.setEnabled( ((Boolean)conf.getEntry( getClass().getName(),
//			"allowAutoPermissions", Boolean.class, new Boolean(true) )).booleanValue() );
//		m.add( reqdl = new JCheckBoxMenuItem("Require DownloadPermission") );
//		reqdl.addActionListener( new ActionListener() {
//			public void actionPerformed(ActionEvent ev) {
//				reqDlPerm = reqdl.isSelected();
//			}
//		});
		if( System.getProperty( "org.wonderly.desktop.authmenu") != null )
			m.add( unauthmen );
		unauthmen.add( am.getAction("Add New Host") );
		unauthmen.addSeparator();
		JMenu om = new JMenu("Default - Open UIs As");
 		m.add(om);
 		m.add( wrole );
 		wrole.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				prefs.putBoolean( wrole.getText(), wrole.isSelected() );
				for( DesktopItem di: items.keySet() ) {
					di.setText( di.name+(wrole.isSelected() ? " ("+di.role+")" : "") );
					di.revalidate();
					di.setSize( di.getPreferredSize() );
					di.repaint();
				}
 			}
 		});
 		m.add( wicon );
 		wicon.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				prefs.putBoolean( wicon.getText(), wicon.isSelected() );
				for( DesktopItem di: items.keySet() ) {
					if( ! wicon.isSelected() ) {
						di.setHorizontalTextPosition(di.CENTER);
						di.setVerticalTextPosition(di.BOTTOM);
					} else {
						di.setHorizontalTextPosition(di.RIGHT);
						di.setVerticalTextPosition(di.CENTER);
					}
					di.revalidate();
					di.setSize( di.getPreferredSize() );
					di.repaint();
				}
 			}
 		});
 		m.add( allDesc = new JCheckBoxMenuItem("All Services") );
 		allDesc.setSelected( prefs.getBoolean( allDesc.getText(), false ) );
 		allDesc.addActionListener( new ActionListener() {
 			public void actionPerformed( ActionEvent ev ) {
 				prefs.putBoolean( allDesc.getText(), allDesc.isSelected() );
 			}
 		});
 
		ButtonGroup openbg = new ButtonGroup();
		internals = new JCheckBoxMenuItem("Internal Frame");
		internals.setSelected(true);
		om.add(internals);
		tabsmi = new JCheckBoxMenuItem("New Tab");
		tabsmi.setSelected(true);
		om.add(tabsmi);
		openbg.add(internals);
		openbg.add(tabsmi);
		
		bar.add(grpMen);
		
		// Put the Application menu on the end.
//		m = new JMenu("  ");
//		m.setEnabled(false);
//		bar.add(m);
		applmenu = new JMenu("Appl");
		bar.add(applmenu);
		// Initiall not active.
		applmenu.setEnabled(false);
		
		String helpClass = System.getProperty("org.wonderly.jini.desktop.help");
		String aboutClass = System.getProperty("org.wonderly.jini.desktop.about");
		m = new JMenu("Help");
		bar.add(m);
		helpMenu = new JMenu("Application");
		m.add( helpMenu );
		m.addSeparator();
		if( helpClass != null || aboutClass != null ) {
			if( helpClass != null ) {
				Class c = Class.forName( helpClass );
				try {
					Constructor cs = c.getConstructor( new Class[]{ String.class, String.class } );
					Action a = (Action)cs.newInstance( "EOI Desktop", "startEOI_Desktop" );
					m.add( a );
					if( aboutClass != null ) {
						m.addSeparator();
					}
				} catch( Throwable ex ) {
					log.log( Level.SEVERE, ex.toString(), ex );
				}
			}
			if( aboutClass != null ) {
				Class c = Class.forName( aboutClass );
				Constructor cs = c.getConstructor( new Class[]{ String.class, JFrame.class } );
				Action a = (Action)cs.newInstance( "About EOI Desktop", frame );
				m.add( a );
			}
		}
		
		frame.setDefaultCloseOperation(frame.DO_NOTHING_ON_CLOSE);
		if( !sized ) {
			frame.pack();
			frame.setSize(300,300);
			frame.setLocation(10,100);
		}
		frame.setVisible(true);
		final boolean exited[] = new boolean[1];
		frame.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent ev ) {
				synchronized( exited ) {
					if( exited[0] == false ) {
						exited[0] = true;
						checkExit();
					}
				}
			}
		});
		startLookup();
		frame.addComponentListener( new ComponentAdapter() {
			public void componentResized( ComponentEvent ev ) {
				saveGroups();				
			}
			public void componentMoved( ComponentEvent ev ) {
				saveGroups();
			}
		});
		expandTree( moreList, moreMod );
		expandTree( allList, allMod );
		if( mygroups.size() != 0 ) {
			atabs.setSelectedIndex(1);
		} else if( mygroups.size() == 0 && allMod.size() != 0 ) {
			atabs.setSelectedIndex(2);
		}
	}

	private void expandTree( JTree tree, MyTreeModel mod ) {
		Object root = mod.getRoot();
		int cnt = mod.getChildCount( root );
		tree.expandPath( new TreePath( new Object[] { root } ) );
		for( int i = 0; i < cnt; ++i ) {
			Object o = mod.getChild( root, i );
			tree.expandPath( new TreePath( new Object[] { root, o } ) );
		}
		tree.repaint();
	}

	private static class ScaledImageIcon implements Icon {
		Icon icon;
		int SIZE;
		Logger log = Logger.getLogger( getClass().getName() );
		JTree moreList;

		public ScaledImageIcon( Icon ic, JTree mlist ) {
			if( ic == null ) {
				if( log.isLoggable(Level.FINER) ) {
					log.log( Level.FINER, "No icon for tree: "+mlist,
						new Throwable("null icon provided for tree: "+mlist ) );
				}
			}
			icon = ic;
			moreList = mlist;
			SIZE = moreList.getRowHeight();
		}
		public int getIconHeight() {
			return SIZE;
		}
		public int getIconWidth() {
			return SIZE;
		}
		public void paintIcon( Component c, Graphics g, int x, int y ) {
			int iw = getIconWidth();
			int ih = getIconHeight();
			int xo = 0;
			int yo = 0;
			
			if( icon == null )
				return;

			log.finest( "dims "+
				icon.getIconWidth()+","+icon.getIconHeight()+" - "+
				getIconWidth()+","+getIconHeight() );
			if( icon.getIconWidth() < icon.getIconHeight() ) {
				iw = ( icon.getIconWidth() * getIconHeight() ) /
					icon.getIconHeight();
				xo = getIconWidth() - iw;
				xo /= 2;
			} else {
				ih = ( icon.getIconHeight() * getIconWidth() ) /
					icon.getIconWidth();
				yo = getIconHeight() - ih;
				yo /= 2;
			}

			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			if( h > 0 && w > 0 ) {
			Image im = c.createImage( icon.getIconWidth(), icon.getIconHeight() );
			if( im != null ) {
				icon.paintIcon( c, im.getGraphics(), 0, 0 );
			}
			log.finest("draw into ("+x+"+"+xo+","+y+"+"+yo+") "+
				"("+iw+","+ih+")");
			g.drawImage( im, x+xo, y+yo, iw, ih, c );
			} else {
				g.setColor( Color.black );
				for( int i = 1; i < iw/2; i += 2 ) {
					g.drawRect( x+xo+i, y+yo+i, iw-i-i, ih-i-i );
				}
			}
		}
	}

	protected int askInSwing( final String quest ) {
		final int res[] = new int[1];
		runInSwing( new Runnable() {
			public void run() {
				res[0] = JOptionPane.showConfirmDialog( frame, quest, "Confirm Needed",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
			}
		});
		return res[0];
	}

	protected void invokePath( TreePath p ) {
		DesktopItem it = deskItemFor( p );
		if( it == null ) {
			log.warning("no item for: "+p);
			return;
		}
		try {
			log.fine("invoking "+it);
			it.invoke();
			log.fine(it+" opened");
		} catch( Exception ex ) {
			reportException(ex);
		}
	}
	
	protected DesktopItem deskItemFor( TreePath p ) {
		if( p == null )
			return null;
		Object tn = p.getLastPathComponent();
		if( tn instanceof DesktopItem == false )
			return null;
		return (DesktopItem)tn;
	}

	/**
	 *  Check if we can exit and do everything needed if so
	 */
	protected void checkExit() {
		Enumeration e = groups.keys();
		int cnt = 0;
		while( e.hasMoreElements() ) {
			NamedVector v = (NamedVector)groups.get(e.nextElement());
			for( int i = 0; i < v.size(); ++i ) {
				DesktopItem di = (DesktopItem)v.elementAt(i);
				if( di.isActive() )
					cnt++;
			}
		}

		log.info("Saving service group layouts etc.");
		saveGroups();

		if( cnt > 0 ) {
			int cd = askInSwing( "Ready To Exit?\n\n"+
				"There are "+cnt+" applications\n"+
				"still active!" );
			if( cd != JOptionPane.OK_OPTION )
				return;
			e = groups.keys();
			while( e.hasMoreElements() ) {
				NamedVector v = (NamedVector)groups.get(e.nextElement());
				log.fine("Exiting all in group: "+v.name );
				for( int i = 0; i < v.size(); ++i ) {
					DesktopItem di = (DesktopItem)v.elementAt(i);
					if( di.isActive() ) {
						log.fine("Closing: "+di );
						di.close();
					}
				}
			}
			log.fine("Closing desktop frame");
//			frame.setVisible(false);
//			frame.dispose();
		}
	
		log.info("droping services");
		/** Copy services because they will be removed from
		 *  svcregs as we drop them and we'll get a concurrent
		 *  modification, or iterator changed if we don't
		 *  operate on the copy
		 */
		Vector<ServiceRegistrar> vr = new Vector<ServiceRegistrar>();
		for( ServiceRegistrar sr: svcregs.keySet() ) {
			vr.addElement(sr);
			log.fine("dropping registrar: "+sr );
		}
		
		// Now process the registrars.
		for( ServiceRegistrar sr: vr ) {
			try {
				dropServicesFor(sr);
			} catch( Exception ex ) {
				log.log( Level.WARNING, ex.toString(), ex );
			}
		}
		
		log.info("Shutting down ldm and lrm");
//		JPanel lp = new JPanel();
//		Packer lpk = new Packer( lp );
//		JLabel ll;
//		lpk.pack( ll = new JLabel( "Shutting down Service Location")
//			).fillboth().inset(10,10,10,10);
//		ll.setFont( new Font( "serif", Font.BOLD, 18 ) );
//		ll.setForeground( Color.blue );
		
		new ComponentUpdateThread( /*frame, new ComponentUpdateThread.CancelHandler() {
			public void cancelled( ComponentUpdateThread th ) {
				log.info("Wait cancelled, closing frame");
					th.finished();
				} }, "Please Wait...", lp */ ) {
			public Object construct() {
				try {
					// Clear all leases
					log.info("shutting down lrm");
					lrm.clear();
				} catch( Exception ex ) {
					log.log(Level.WARNING, ex.toString(),ex);
				}
				lrm = null;
				try {
					// Terminate discovery
					log.info( "shutting down ldm");
					ldm.terminate();				
				} catch( Exception ex ) {
					log.log(Level.WARNING, ex.toString(),ex);
				}
				ldm = null;
				return null;
			}

			public void finished() {
				try {
					// If logging turned up, dump threads for debugging of threads
					// created inside of serviceUI's that need to be terminated.
					if( log.isLoggable( Level.FINE ) ) {
						Map<Thread,StackTraceElement[]>m = Thread.getAllStackTraces();
						for( Thread t: m.keySet() ) {
							log.fine( "Thread ("+
								(t.isDaemon() ? "daemon" : "user")+"): "+t);
							for( StackTraceElement ste: m.get(t) ) {
								log.finer( "\t"+ste );
							}
						}
					}
				} finally {
					log.info("Closing dialog...");
					super.finished();

					log.info("Closing frame and disposing for exit");
					// Close the frame
					if( frame != null ) {
						frame.setVisible(false);
						frame.dispose();
						frame = null;
					}
					deskContext = null;
					if( System.getProperty("org.wonderly.desktop.forceExit") != null )
						System.exit(1);
				}
			}
		}.start();
	}

	private int offx = 3;
	private int offy = 3;
	private JDeskFrame deskContext;

	private static class JDeskFrame extends JFrame implements JDesktopContext {
		private HashMap<Uuid,Container> comps = new HashMap<Uuid,Container>();
		private HashMap<Container,Uuid> insts = new HashMap<Container,Uuid>();
		private HashMap<Uuid,Vector<Runnable>>handler = new HashMap<Uuid,Vector<Runnable>>();
		private HashMap<Uuid,DesktopItem> tabs = new HashMap<Uuid,DesktopItem>();
		private HashMap<Uuid,DesktopItem> actives = new HashMap<Uuid,DesktopItem>();
		private Logger log = Logger.getLogger( getClass().getName() );
		private JTabbedPane apptabs;

		public void closing( Uuid inst ) {
			log.log(Level.FINE,"Appl closing: "+inst, new Throwable(inst+" closing") );
			closeInstance( inst );
		}

		public JDeskFrame( String str, JTabbedPane aptabs ) {
			super(str);
			this.apptabs = aptabs;
		}

		public void closeComponent( JComponent comp ) {
			log.fine("Closing Component: "+comp );
			closeInstance( insts.get(comp) );
		}

		public void closeInstance( Uuid id ) {
			log.fine("Closing instance: "+id );
			if( id == null )
				return;
			invokeHandler( id );
			Container c = comps.remove( id );
			if( c instanceof JInternalFrame ) {
				log.fine("disposing internal frame: ("+id+"): "+c);
				((JInternalFrame)c).dispose();
			} else if( c instanceof JFrame ) {
				log.fine("disposing frame: ("+id+"): "+c);
				((JFrame)c).dispose();
			} else if( c instanceof JDialog ) {
				log.fine("disposing dialog: ("+id+"): "+c);
				((JDialog)c).dispose();
			}
			log.fine( "remove comps for ("+id+"): "+c );
			if( c != null ) {
				insts.remove(c);
			}
			unregisterItem( id );
		}	

		public void registerInternalFrameUI( final Uuid inst, DesktopItem item, final JInternalFrame comp ) {
			log.fine( "registerInternalFrameUI: "+inst+", "+comp );
			comps.put( inst, comp );
			insts.put( comp, inst );
			registerItem( inst, item );
			comp.addInternalFrameListener( new InternalFrameAdapter() {
				public void internalFrameClosing( InternalFrameEvent ev ) {
					closeInstance( inst );
				}
			});
			tabs.remove( inst );
		}

		public DesktopItem closeTabUI( int idx ) {
			log.fine("close tabui: "+idx );
			if( idx == 0 )
				return null;
			Component c = apptabs.getComponent( idx );
			apptabs.remove( idx );
			Uuid inst = insts.get( c );
			log.finer("inst for "+c+": "+inst );
			DesktopItem it = tabs.remove( inst );
			for( int i = 0; i < apptabs.getTabCount(); ++i ) {
				c = apptabs.getComponent(i);
				inst = insts.get(c);
				if( inst != null ) {
					DesktopItem im = tabs.get(inst);
					if( im != null ) {
						im.setTabIndex( i );
					}
				}
			}
			closeInstance( inst );
			return it;
		}

		public void shutdownTabUI( int idx ) {
			log.fine("remove tabui: "+idx );
			DesktopItem it = closeTabUI( idx );
			Uuid inst = insts.get( it.comp );
			closeInstance( inst );
		}

		public void registerItem( final Uuid inst, final DesktopItem item ) {
			actives.put( inst, item );
		}

		public void unregisterItem( final Uuid inst ) {
			DesktopItem item = actives.remove( inst );
			if( item != null ) {
				item.close();
				item.active = false;
			}
		}

		public void registerTabUI( final Uuid inst, DesktopItem item, final DesktopItem comp ) {
			if( comp.comp == null )
				throw new NullPointerException("DesktopItem.comp can not be null");
			if( inst == null )
				throw new NullPointerException("Uuid instance can not be null");
			log.fine( "registerTabUI: " + inst + ", " + comp.comp );
			registerItem( inst, item );
			comps.put( inst, (JPanel)comp.comp );
			insts.put( (JComponent)comp.comp, inst );
			tabs.put( inst, comp );
		}

		public void invokeHandler( final Uuid inst ) {
			final Vector<Runnable> vr = handler.get(inst);
			// No handlers, just return
			if( vr == null )
				return;
			for( Runnable r : vr ) {
				log.fine("launching Runnable for "+inst+", r: "+r );
				new Thread(	r, "item: "+actives.get(inst)+" shutdown handler" ).start();
			}
			handler.remove(inst);
		}

		public void invokeDesktopItemHandler( final DesktopItem inst ) {
			for( Uuid id : actives.keySet() ) {
				DesktopItem item = actives.get(id);
				log.fine("check item: "+item+" <> "+inst );
				if( item == inst ) {
					log.fine( "invokingHandler("+inst+"): "+id );
					invokeHandler( id );
					return;
				}
			}
		}

		public JDeskFrame() {
			super();
		}

		// Reuse this method from JComponent to get it for free
        // if the context is already a JComponent
        public Container getTopLevelAncestor() {
        	return this;
        }

        // Report errors to the user with an explicit interface
        // provided by the implementation.  Might need another signatures
        // with just Throwable?
        public void reportException( Uuid inst, String msg, Throwable ex, boolean prompt ) {
        	requestFocus( inst );
        	reportExceptionFor( this, inst, msg, ex, prompt );
        }

        // Allow a component to request that it get the focus.  For a desktop
        // using JInternalFrames, this would cause the frame to be raised
        // to the top or otherwise made visible to the user as needing
        // attention.
        public void requestFocus( Uuid inst ) {
        	log.fine("requestFocus: "+inst );
        	if( comps.get(inst) instanceof JInternalFrame ) {
        		((JInternalFrame)comps.get(inst)).toFront();
        	} else if( comps.get(inst) instanceof JFrame ) {
        		((JFrame)comps.get(inst)).toFront();
        	} else if( comps.get(inst) instanceof JDialog ) {
        		((JDialog)comps.get(inst)).toFront();
        	}
        	comps.get(inst).setVisible(true);
        	if( tabs.get( inst ) != null ) {
        		Container c = tabs.get(inst);
        		for( int i = 0; i < apptabs.getTabCount(); ++i ) {
        			if( c == apptabs.getComponentAt(i) ) {
        				flashTabAt(i, c);
        			}
        		}
        	}
        	comps.get(inst).requestFocus();
        }
 
        private void flashTabAt( final int i, final Component c ) {
        	log.fine("flashTabAt("+i+","+c+")");

        	final Object stopTabs = new Object();
        	final boolean[] stopped = new boolean[1];
        	new Thread("tab flasher "+i) {
        		public void run() {
        			boolean state = false;
        			Color fg = c.getForeground();
        			Color bg = c.getBackground();
        			while( stopped[0] == false ) {
        				if( state ) {
        					apptabs.setForegroundAt( i, fg );
        					apptabs.setBackgroundAt( i, bg);
        				} else {
        					apptabs.setForegroundAt( i, bg);
        					apptabs.setBackgroundAt( i, fg );
        				}
        				state = !state;
        				synchronized( stopTabs ) {
        					try {
        						stopTabs.wait( 500 );
        					} catch( Exception ex ) {
        					}
        				}
        			}
  					apptabs.setForegroundAt( i, fg);
  					apptabs.setBackgroundAt( i, bg );
        		}
        	}.start();
        	apptabs.addChangeListener( new ChangeListener() {
        		public void stateChanged(ChangeEvent e) {
        			if( apptabs.getSelectedIndex() == i ) {
        				synchronized( stopTabs ) {
        					stopped[0] = true;
        					stopTabs.notifyAll();
        				}
        			}
        		}
        	});
        }

        // Ask to be notified when the container shutsdown the UI.
        // This Runnable is first removed from the context, and then executed.
        // Its execution is protected by exeception handling and any exceptions
        // that do occur will be reported to the user.
        public synchronized void registerShutdownHandler( Uuid inst, Runnable r ) {
        	log.fine("registerShutdownHandler: "+inst+", "+r );
//        	if( handler.get(inst) != null )
//        		throw new IllegalArgumentException( "only one handler may be registered for: "+inst );
        	Vector<Runnable> v = handler.get(inst);
        	if( v == null ) {
        		v = new Vector<Runnable>();
        		handler.put( inst, v );
        	}
        	v.add( r );
        }
	}

	private void showHelpsFor( DesktopItem it ) {
		List<String>h = it != null ? it.getHelpSetNames() : new ArrayList<String>();
		List<HelpEntry>menu = new ArrayList<HelpEntry>(helpsets.size());
		for( String nm : h ) {
			List<HelpEntry> lhe = helpsets.get( nm );
			if( lhe.size() > 0 ) {
				HelpEntry the = lhe.get(0);
				if( menu.contains(the) == false )
					menu.add(the);
			}
		}
		helpMenu.removeAll();
		JMenu m = helpMenu;
		Collections.sort( menu, new Comparator<HelpEntry>() {
			public int compare( HelpEntry h1, HelpEntry h2 ) {
				return h1.getDescr().compareTo( h2.getDescr() );
			}
		});
		for( HelpEntry he: menu ) {
			try {
				m.add( new HelpMenuItem( he ) );
			} catch( Exception ex ) {
				log.log( Level.WARNING, ex.toString(), ex );
			}
		}
		m.addSeparator();
		for( List<HelpEntry> lhe : helpsets.values() ) {
			if( lhe.size() > 0 ) {
				HelpEntry the =  lhe.get(0) ;
				if( menu.contains(the) == false ) {
					try {
						m.add( new HelpMenuItem( the ) );
					} catch( Exception ex ) {
						log.log( Level.WARNING, ex.toString(), ex );
					}
				}
			}
		}
	}
	
//	class DeferredHelpItem extends JMenuItem {
//		ClassLoader cl;
//		URL url;
//		public DeferredHelpItem( ClassLoader cl, URL u ) {
//			this.cl = cl;
//			this.url = u;
//		}
//	}
//	
	private class HelpMenuItem extends JMenuItem implements ActionListener {
		HelpEntry he;
		volatile boolean isJavaHelp;

		public HelpMenuItem( HelpEntry he ) throws HelpSetException {
			super( he.getDescr() );
			this.he = he;
			if( he instanceof JavaHelpEntry ) {
				isJavaHelp = true;
				addActionListener( this );
			} else {
				addActionListener( this );
			}
		}

		public void actionPerformed( final ActionEvent ev ) {
			if( isJavaHelp ) {
				new ComponentUpdateThread( this ) {
					public Object construct() {
						try {
							ClassLoader cl = getClass().getClassLoader();
							HelpSet hs = new HelpSet( cl, he.getHelpURL() );
							// Create a HelpBroker object:
							HelpBroker hb = hs.createHelpBroker();
							hb.enableHelpOnButton( HelpMenuItem.this, ((JavaHelpEntry)he).getStartID(), hs );
							removeActionListener(HelpMenuItem.this);
						} catch( Exception ex ) {
							reportException(ex);
						}
						return null;
					}
					public void finished() {
						try {
							fireActionPerformed( ev );
						} finally {
							super.finished();
						}
					}
				}.start();
			} else {
				new ComponentUpdateThread( this ) {
					public Object construct() {
						try {
							openHelpEntry( he );
						} catch( Exception ex ) {
							reportException(ex);
						}
						return null;
					}
				}.start();
			}
		}
	}
	
	private static void openHelpEntry( HelpEntry he ) throws IOException {
		Logger log = Logger.getLogger( JiniDeskTop.class.getName() );
		URL u = he.getHelpURL();
		if( he instanceof HtmlHelpEntry ) {
			if( System.getProperty("os.name").contains( "indows" ) ) {
				String us = u.toString();
				String nus = "";
				for( char c: us.toCharArray() ) {
					if( c == '&' ) {
						nus += "^&";
					} else if( c == '^') {
						nus += "^^";
					} else if( c == ' ') {
						nus += "%20";
					} else {
						nus += c;
					}
				}
				String ext = System.getProperty("start.exec.plain");
				if( ext == null ) {
					Runtime.getRuntime().exec( new String[]
						{"cmd", "/c", "start "+nus } );
				} else {
					Runtime.getRuntime().exec( new String[]
						{ "start "+nus } );
				}
			}
		} else if( he instanceof JavaHelpEntry ) {
			if( System.getProperty("os.name").contains( "indows" ) ) {
				String us = u.toString();
				String nus = "";
				for( char c: us.toCharArray() ) {
					if( c == '&' ) {
						nus += "^&";
					} else if( c == '^') {
						nus += "^^";
					} else if( c == ' ') {
						nus += "%20";
					} else {
						nus += c;
					}
				}
				Runtime.getRuntime().exec( "start "+nus );
			}
		}
	}

	private class DesktopItem extends JLabel implements DesktopControlHandler {
		private String name;
		private ServiceItem svcItem;
		private ServiceEntry svcEntry;
		private ServiceID sid;
		private UIDescriptor uid;
		private String role = "MainUI";
		private DragGestureRecognizer adgr;
		private int tabidx;
		private boolean intab;
		private boolean active;
		private Component comp;
		private boolean attached;
		private ClassLoader ld;
		private Vector<JMenu> menus = new Vector<JMenu>();
		private boolean isTransient;
		private Icon icon;
		private ServiceRegistration sr;
		private List<String>helps;
		private Logger log = Logger.getLogger( getClass().getName() );

		public List<String> getHelpSetNames() {
			return helps;
		}

		public ServiceID serviceID() {
			if( svcEntry != null )
				return svcEntry.getServiceID();
			return svcItem.serviceID;
		}

		public String getName() {
			return name;
		}
	
		public void setTabIndex( int tab ) {
			tabidx = tab;
		}

		/** Invoke the UI associated with this item. */
		public void invoke() {
			log.fine("invoking with no prefs for frame or tab");
			try {
				invoke( internals.isSelected(), tabsmi.isSelected() );
			} catch( Exception ex ) {
				reportException(ex);
			}
		}

		public void invoke( final boolean internFrame, final boolean inTab ) {
			if( log.isLoggable( Level.FINE ) ) {
				log.log( Level.FINE, "Invoking "+this, new Throwable("invoking:"+this) );
			}
//			final ClassLoader ocl[] = new ClassLoader[1];
//			final ClassLoader cl = ld; //svc.getClass().getClassLoader();
			final ClassLoader ocl[] = new ClassLoader[1];
			final ClassLoader cl = ld; //svcInst.getClass().getClassLoader();
			log.fine("Invoking: "+this);
			setBorder( BorderFactory.createRaisedBevelBorder() );
			ComponentUpdateThread th = new ComponentUpdateThread( this ) {
				public void setup() {
					super.setup();
				}
				public Object construct() {
					try {
						// Here we force the context class loader of the event dispatch thread
						// to be that of the services class loader.  This should allow the UI
						// classes to be loaded for the first phase of class loading.  If there
						// is other class loading done by another thread, it will have to
						// manage its context class loader by itself.
//						try {
//							ocl[0] = Thread.currentThread().getContextClassLoader();
//							EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
//							Thread.currentThread().setContextClassLoader(cl);
//							Thread.currentThread().setContextClassLoader(svc.service.getClass().getClassLoader());
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
						log.fine("doInvoke'ing(): "+name);
						doInvoke( internFrame, inTab );
						log.fine("doInvoke returns for: "+name);
					} catch( java.security.AccessControlException ex ) {
						reportMessage( "You don't seem to have a Security Policy\n"+
							"which will allow you to do that.", ex );
					} catch( Exception ex ) {
						reportException(ex);
					}
					return null;
				}
				public void finished() {
					try {
						moreList.repaint();
						allList.repaint();
					} finally {
						super.finished();
					}
				}
			};
			th.start();
//			th.get();
		}
		
		private void doInvoke( final boolean internFrame, final boolean inTab ) throws IOException {
			log.fine("Invoking UIFactory("+active+") par="+(comp != null ? comp.getParent() : null ));
			Object fobj = null;
			pane.setLayer( DesktopItem.this, 
				JLayeredPane.DEFAULT_LAYER.intValue(), 0 );

			if( active && comp.getParent() != null ) {
				log.fine("Already Active, bring to front");
				// Just bring it to the top
				runInSwing( new Runnable() {
					public void run() {
						if( comp instanceof JInternalFrame ) {
							log.fine("activating JInternalFrame");
							// Switching apps, check if current frame is
							// maximized, and maximize this one of so.
							JInternalFrame f = pane.getSelectedFrame();
							JInternalFrame cf = (JInternalFrame)comp;
							if( f != null && f.isMaximum() ) {
								try {
									log.fine("Maxmimizing");
									cf.setMaximum(true);
								} catch( Exception ex ) {
								}
							}
							log.fine("Sending "+cf+" to the front");
							cf.toFront();
							try {
								log.fine("Set selected true");
								cf.setSelected(true);
							} catch( Exception ex ) {
								reportException(ex);
							}
						} else if( comp instanceof JFrame ) {
							((JFrame)comp).toFront();
						} else if( comp instanceof JDialog ) {
							((JDialog)comp).toFront();
						} else if( intab ) {
							if( apptabs.getSelectedIndex() != tabidx )
								apptabs.setSelectedIndex( tabidx );
						}
						log.fine("set visible and repaint");
						comp.setVisible(true);
						comp.repaint();
					}
				});
				return;
			}
			log.fine("Invoke UIFactory for: "+this );
			try {
//				log.fine("using svcEntry loader: "+svcEntry.getClass().getClassLoader() );
//				Thread.currentThread().setContextClassLoader( 
//					svcEntry.getClass().getClassLoader() );
				if( svcItem == null && svcEntry != null )
					svcItem = svcEntry.getItem();
				log.fine("using service class loader: "+svcItem.service.getClass().getClassLoader());
				fobj = uid.getUIFactory( svcItem.service.getClass().getClassLoader() );
				ClassLoader fldr = fobj.getClass().getClassLoader();
				log.fine( "Factory object class Loader: "+fldr );
				// Turn on Permissions for codebase if allowed
				Codebase cb = codebaseLoader.get( svcItem.serviceID );
				if( cb != null ) {
					cb.setClassLoader( fldr );
					pol.grant( fobj.getClass(), cb.getPrincipals(), cb.getPermissions() );
					pol.grant( uid.getClass(), cb.getPrincipals(), cb.getPermissions() );
					pol.grant( svcItem.service.getClass(), cb.getPrincipals(), cb.getPermissions() );
				}				
				Thread.currentThread().setContextClassLoader( fldr );
			} catch( InvalidClassException ex ) {
				log.throwing( getClass().getName(), 
					"doInvoke", ex );
				String str = ex.getMessage();
				int idx = str.indexOf(";");
				if( idx >= 0 ) {
					str = str.substring( 0, idx );
					idx = str.indexOf( ":" );
					if( idx >= 0 )
						str = str.substring( idx+1 ).trim();
					str = str+":\n\nCheck codebase and classpath "+
						"for incompatible definitions\n";
				}
				InvalidClassException iex = new InvalidClassException(str);
				iex.initCause( ex );
				throw iex;
			} catch( NoClassDefFoundError ex ) {
				log.throwing( getClass().getName(), 
					"doInvoke", ex );
				String str = ex.getMessage();
				int idx = str.indexOf(";");
				if( idx >= 0 ) {
					str = str.substring( 0, idx );
					idx = str.indexOf( ":" );
					if( idx >= 0 )
						str = str.substring( idx+1 ).trim();
					str = str+":\n\nCheck codebase and classpath "+
						"for incompatible definitions\n";
				}
				InvalidClassException iex = new InvalidClassException(str);
				iex.initCause( ex );
				throw iex;
			} catch( Exception ex ) {
				log.throwing( getClass().getName(), 
					"doInvoke", ex );
				log.fine( "trying to use the "+uid.getClass()+
					" class loader to invoke factory" );
				try {
					fobj = uid.getUIFactory( uid.getClass().getClassLoader() );
//					Thread.currentThread().setContextClassLoader( 
//						uid.getClass().getClassLoader() );
					Codebase cb = codebaseLoader.get( svcItem.serviceID );
					if( cb != null ) {
						cb.setClassLoader( fobj.getClass().getClassLoader() );
						pol.grant( fobj.getClass(), cb.getPrincipals(), 
							cb.getPermissions() );
						pol.grant( svcItem.service.getClass(),
							cb.getPrincipals(), cb.getPermissions() );
					}				
				} catch( Exception exx ) {
				 	log.throwing( getClass().getName(), "doInvoke", exx );
					JOptionPane.showMessageDialog( frame, exx );
					return;
				}
			}

			log.fine( "UIFactory returned is "+fobj+", loader: "+
				fobj.getClass().getClassLoader() );
			ClassLoader fld = fobj.getClass().getClassLoader();
			if( fld instanceof URLClassLoader ) {
				log.fine( fld+": is URLClassLoader" );
			} else if( fld.getParent() == null ) {
				log.fine( fld+": parent is: "+fld.getParent() );
			} else {
				log.config( "unrecoginzed factory codebase config for "+
					fobj+" from "+fld );
//				log.config( "granting all permission to "+fobj+" from "+fld );
//				pol.grant( fobj.getClass(),
//					null,
//					new Permission[] { new java.security.AllPermission() } );
			}

			// Get a component to embed
			if( fobj instanceof JDesktopComponentFactory ) {
				openComponent( fobj, internFrame, inTab );
			} else if( fobj instanceof JComponentFactory ) {
				openComponent( fobj, internFrame, inTab );
			} else if( fobj instanceof JFrameFactory ) {
				final JFrame f = ((JFrameFactory)fobj).getJFrame(svcItem);
				comp = f;
//				f.setSize( 400, 300 );
				f.setVisible(true);
				if( fobj instanceof JMenuBarFactory ) {
					f.setJMenuBar( ((JMenuBarFactory)fobj).getJMenuBar() );
				}
				f.addWindowListener( new WindowAdapter() {
					public void windowClosing( WindowEvent ev ) {
						log.fine("Closing: "+DesktopItem.this );
						active = false;
						close();
						f.dispose();
					}
				});
				attached = false;
			} else if( fobj instanceof JDialogFactory ) {
				final JDialog f = ((JDialogFactory)fobj).getJDialog( 
					svcItem, JiniDeskTop.this.frame );
				comp = f;
//				f.setSize( 400, 300 );
				f.setVisible(true);
				if( fobj instanceof JMenuBarFactory ) {
					f.setJMenuBar( ((JMenuBarFactory)fobj).getJMenuBar() );
				}
				f.addWindowListener( new WindowAdapter() {
					public void windowClosing( WindowEvent ev ) {
						log.fine("Closing: "+DesktopItem.this );
						active = false;
						close();
						f.dispose();
					}
				});
				attached = false;
			} else {
				reportException( new RuntimeException(
					fobj.getClass().getName()+" is not compatible with "+
						"the desktop requirements of JFrame, "+
						"JComponent or JInternalFrame" ) );
				active = false;
				return;
			}
			log.fine("adding help menu items for"+this);
			showHelpsFor( this );
			active = true;
			log.fine("item active="+active);
		}
		
		private void openComponent( Object fobj, boolean internFrame, boolean inTab ) {
			JComponent cp;
			Uuid inst = UuidFactory.generate();
			if( svcItem == null && svcEntry != null ) {
				try {
					svcItem = svcEntry.getItem();
				} catch( ServiceAccessException ex ) {
					reportException(ex);
				}
			}
			if( svcItem == null ) {
				reportException( new ConfigurationException("Desktop item access not available") );
				return;
			}
			
			if( fobj instanceof JDesktopComponentFactory ) {
				// Get the component through the factory passing context if supported
				try {
					cp = ((JDesktopComponentFactory)fobj).getJDesktopComponent(deskContext, svcItem);
					inst = ((JDesktopComponentFactory)fobj).getInstanceId();
				} catch( RuntimeException ex ) {
					reportException(ex);
					reportException( new RuntimeException( 
						"Error getting UI component from service factory: "+ex+
						"\nCheck Entry objects and any persistent Entry for\n"+
						"out of date classes") );
					throw ex;
				}
			} else {
				try {
					cp = ((JComponentFactory)fobj).getJComponent(svcItem);
				} catch( RuntimeException ex ) {
					reportException(ex);
					reportException( new RuntimeException( 
						"Error getting UI component from service factory: "+ex+
						"\nCheck Entry objects and any persistent Entry for\n"+
						"out of date classes") );
					throw ex;
				}
			}

			// Add handler to remove border when component closed.
			deskContext.registerShutdownHandler( inst, new Runnable() {
				public void run() {
					setBorder(null);
				}
			});
			deskContext.registerItem( inst, this );
			
			JMenuBar bar = null;
			menus.removeAllElements();
			// Move the applications menubar to a menu
			if( fobj instanceof JMenuBarFactory ) {
				bar = ((JMenuBarFactory)fobj).getJMenuBar();
				if( bar == null ) {
					log.warning("JMenuBarFactory for: "+this+" returns no menu");
				} else {
					int cnt = bar.getMenuCount();
					for( int i =0; i < cnt; ++i ) {
						JMenu m = bar.getMenu(i);
						log.fine("Storing menu reference: "+m );
						menus.addElement(m);
					}
				}
			}
			
			// If internal frame is requested, open it in one.
			if( internFrame ) {
				JInternalFrame f;
				if( cp instanceof JInternalFrame == false ) {
					f = new JInternalFrame( name );
					f.setContentPane( cp );
					f.pack();
					if( inst != null ) {
						deskContext.registerInternalFrameUI( inst, this, f );
					}
				} else {
					f = (JInternalFrame)cp;
				}
				attached = true;
				comp = f;
				int minw = pane.getSize().width;
				int minh = pane.getSize().height;
// 					f.setSize( w/3, h/3);
				offx += 30;
				offy += 30;
				if( offx > minw-(minw/10) || offy > minh-(minh/10) ) {
					offx = 30 + (offy-offx);
					offy = 3;
				}
//				JMenuBar mbar = null;
//				if( fobj instanceof JMenuBarFactory ) {
//					mbar = ((JMenuBarFactory)fobj).getJMenuBar();
//				}
				configureInternalFrame( f, bar, f.getContentPane() );
			} else if( inTab ) {
				JInternalFrame f;
				if( cp instanceof JInternalFrame == false ) {
					intab = true;
					int tab = apptabs.getTabCount();
					apptabs.add( name, cp );
					if( appitems.size() < tab+1 )
						appitems.setSize( tab+1);
					appitems.setElementAt(DesktopItem.this,tab);
					tabidx = tab;
					if( apptabs.getSelectedIndex() != tab )
						apptabs.setSelectedIndex(tab);
					comp = cp;
					deskContext.registerTabUI( inst, this, this );
					attached = true;
					return;
				} else {
					f = (JInternalFrame)cp;
				}
				attached = true;
				comp = f;
				int minw = pane.getSize().width;
				int minh = pane.getSize().height;
// 					f.setSize( w/3, h/3);
				offx += 30;
				offy += 30;
				if( offx > minw-(minw/10) || offy > minh-(minh/10) ) {
					offx = 30 + (offy-offx);
					offy = 3;
				}
				JMenuBar mbar = null;
				if( fobj instanceof JMenuBarFactory ) {
					f.setJMenuBar( mbar = ((JMenuBarFactory)fobj).getJMenuBar() );
				}
				configureInternalFrame( f, mbar, f.getContentPane() );
			} else {
				final JFrame f = new JFrame( name );
				comp = f;
				//((JComponentFactory)fobj).getJComponent(svcItem)
				f.setContentPane( cp );
				if( fobj instanceof JMenuBarFactory ) {
					f.setJMenuBar( ((JMenuBarFactory)fobj).getJMenuBar() );
				}
				f.pack();
				f.setLocation( 100, 100 );
				f.setVisible(true);
				f.addWindowListener( new WindowAdapter() {
					public void windowClosing( WindowEvent ev ) {
						log.fine("Closing: "+DesktopItem.this );
						active = false;
						f.dispose();
					}
				});
				attached = false;
			}

		}

		public boolean isActive() {
			return active;
		}

		public String toString() {
			return name + " ("+role+")";
		}

		public void activate() {
		}
		public void xactivate() {
			adgr = dgs.createDefaultDragGestureRecognizer(
				this, -1, new DragGestureListener() {
					public void dragGestureRecognized(DragGestureEvent dge) {
						if( curItem == null )
							return;
						dgs.startDrag( dge, null, 
							new Transferable() {
								public DataFlavor[] getTransferDataFlavors() {
									try {
										return new DataFlavor[] {
											new DataFlavor( 
												DataFlavor.javaJVMLocalObjectMimeType )
										};
									} catch( Exception ex ) {
										reportException(ex);
									}
									return new DataFlavor[0];
								}
								public boolean isDataFlavorSupported(DataFlavor flav) {
									return true;
								}
								public Object getTransferData(DataFlavor flav) {
									return DesktopItem.this;
								}
							},null );
					}
				});
		}
		
		private void checkId( ServiceID id ) {
			if( this.serviceID().equals(id) == false ) {
				throw new SecurityException("ID incorrect");
			}
		}
		

		/** User is done with this desktop item */
		public void exiting( ServiceID id ) {
			checkId( id );
			close();
		}

		public void close() {
			setBorder(null);
			if( comp instanceof JInternalFrame ) {
				pane.remove( comp );
			} else if( comp != null ) {
				comp.setVisible(false);
				if( comp instanceof Window ) {
					((Window)comp).dispose();
				}
			}
			log.fine("Closing down UI for: "+this );
			deskContext.invokeDesktopItemHandler(DesktopItem.this);
			comp = null;
			// If using ServiceLookup, drop service reference
			if( svcItem != null && svcEntry != null ) {
				svcItem = null;
			}
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					moreList.repaint();
					allList.repaint();
				}
			});
			// pane.remove(this);
		}

		/** Move application from JInternalFrame to JFrame */
		public void detach( ServiceID id ) {
			checkId( id );
			if( attached == false ) {
				throw new IllegalArgumentException(
					"Application Frame not attached to desktop");
			}
			if( comp instanceof JInternalFrame == true ) {
				if( comp.getParent() != null )
					comp.getParent().remove(comp);
				JInternalFrame f = (JInternalFrame)comp;
				JMenuBar bar = f.getJMenuBar();
				f.setJMenuBar(null);
				Container content = f.getContentPane();
				f.setContentPane( new JPanel() );
				f.setVisible(false);
				f.dispose();
				final JFrame nf = new JFrame( f.getTitle() );
				nf.setJMenuBar( bar );
				nf.setContentPane( content );
				nf.setSize( f.getSize() );
				nf.setLocationRelativeTo( f );
				nf.setVisible(true);
				nf.addWindowListener( new WindowAdapter() {
					public void windowClosing(WindowEvent ev) {
						active = false;
						attached = true;
						nf.dispose();
						close();
					}
				});
				comp = nf;
			} else if( intab ) {
				DesktopItem item = deskContext.closeTabUI( tabidx );
//				apptabs.remove( tabidx );
//				if( comp.getParent() != null )
//					comp.getParent().remove(comp);
//				JInternalFrame f = (JInternalFrame)comp;
//				JMenuBar bar = f.getJMenuBar();
//				f.setJMenuBar(null);
//				Container content = f.getContentPane();
//				f.setContentPane( new JPanel() );
				final JFrame nf = new JFrame( name );
//				nf.setJMenuBar( bar );
				nf.setContentPane( (JPanel)comp );
				nf.pack();
//				nf.setSize( f.getSize() );
				nf.setLocationRelativeTo( frame );
				nf.setVisible(true);
				intab = false;
				nf.addWindowListener( new WindowAdapter() {
					public void windowClosing(WindowEvent ev) {
						active = false;
						comp = null;
						attached = true;
						nf.dispose();
					}
				});
				comp = nf;				
			} else {
				throw new IllegalArgumentException(
					"Application Frame not JInternalFrame");
			}
			pane.repaint();
			attached = false;
		}

		/** Move application from JFrame to JInternalFrame */
		public void reAttach( ServiceID id ) {
			checkId( id );
			if( attached == true ) {
				throw new IllegalArgumentException(
					"Application Frame already attached to desktop");
			}
			if( comp instanceof JFrame == true ) {
				JFrame f = (JFrame)comp;
				JMenuBar bar = f.getJMenuBar();
				Container content = f.getContentPane();
				JInternalFrame nf = new JInternalFrame( f.getTitle() );
				f.setContentPane(new JPanel());
				f.setJMenuBar( null );
				f.setVisible(false);
				f.dispose();
				configureInternalFrame( nf, bar, content );
				nf.setSize( f.getSize() );
				comp = nf;
			} else {
				throw new IllegalArgumentException(
					"Application Frame not JFrame: "+comp.getClass().getName() );
			}
			pane.repaint();
			attached = true;
		}
		
		private void configureInternalFrame( final JInternalFrame nf,
				JMenuBar bar, Container content ) {
			nf.setJMenuBar( bar );
			nf.setContentPane( content );
			pane.add( nf, pane.MODAL_LAYER );
			nf.setMaximizable(true);
			nf.setClosable(true);
			nf.setIconifiable(true);
			nf.setResizable(true);
			nf.setLocation( offx, offy );
			nf.addInternalFrameListener( new InternalFrameAdapter() {
				public void internalFrameClosing( InternalFrameEvent ev ) {
					log.fine("Closing: "+DesktopItem.this );
					nf.dispose();
				}
			});
			try {
				nf.setSelected(true);
			} catch( Exception ex ) {
				reportException(ex);
			}
			nf.setVisible(true);
			nf.toFront();
			nf.repaint();
		}
		
		public void setItem( ServiceItem item ) {
			this.svcItem = item;
		}

		public DesktopItem( String name, ServiceItem svcItem,
				UIDescriptor uid, String role, 
				boolean trans, String group, List<String>helps ) {
			super( name );
			if( log.isLoggable(Level.FINER) ) {
				log.log( Level.FINER, "creating item for: "+name,
					new Throwable("create item for: "+name ) );
			}
			isTransient = trans;
			sr = new ServiceRegistration( svcItem.serviceID, uid.role, group );
//			ld = Thread.currentThread().getContextClassLoader();
			log.fine( "Create DesktopItem("+name+","+uid.role+"): loader="+ld );
			this.svcItem = svcItem;
			this.helps = helps;
			if( helps.size() > 0 ) {
				showHelpsFor( this );
			}

			sid = svcItem.serviceID;
			setIcon( new Icon() {
				int sz = 24;
				public int getIconWidth() {
					return sz+2;
				}
				public int getIconHeight() {
					return sz+2;
				}
				public void paintIcon( Component c, Graphics g, int x, int y ) {
					if( isOpaque() )
						g.setColor( getBackground() );
					else
						g.setColor( getParent().getBackground() );
					g.fillRect( x, y, sz, sz );
					g.setColor( getParent().getForeground() );
					for( int i = 0; i < sz/2; i += 4 ) {
						g.drawOval( x+(i/2), y+(i/2), sz-i, sz-i );
					}
				}
			});
			this.name = name;
			this.svcItem = svcItem;
			this.uid = uid;
			//String els[] = uid.role.split("\\.");
			//role = els[els.length-1];
			this.role = role;
			setText( name+(wrole.isSelected() ? " ("+role+")" : "") );
			setForeground( pane.getForeground() );
			if( ! wicon.isSelected() ) {
				setHorizontalTextPosition(CENTER);
				setVerticalTextPosition(BOTTOM);
			} else {
				setHorizontalTextPosition(RIGHT);
				setVerticalTextPosition(CENTER);
			}
			setFont( new Font( "serif", Font.BOLD, 12 ) );
			addMouseListener( new MouseAdapter() {
				public void mouseClicked( MouseEvent ev ) {
					if( ev.getClickCount() == 2 ) {
						log.finer("Invoking: "+DesktopItem.this);
						try {
							DesktopItem.this.invoke();
						} catch( Exception ex ) {
							reportException(ex);
						}
					}
				}

				public void mousePressed( MouseEvent ev ) {
					if( ev.isPopupTrigger() ) {
						if( lastItem != DesktopItem.this ) {
							lastItem.exit();
						}
						lastItem = DesktopItem.this;
						lastItem.enter();
						popup(ev, DesktopItem.this);
						return;
					}
//					if( lastItem == DesktopItem.this && ev.getButton() == 0 )
//						return;
					if( lastItem != null ) {
						log.finer("Exiting lastItem: "+lastItem );
						lastItem.exit();
						lastItem = null;
					}
					if( ev.getButton() != 1 )
						return;
//					lastItem = DesktopItem.this;
//					lastItem.enter();
//					Point p = lastItem.getLocation();
//					log.finer( "drag starts at: "+ev.getX()+", "+ev.getY() );
//					final int offx = ev.getX();
//					final int offy = ev.getY();
					createMotionListener(ev);
//					pane.addMouseMotionListener(mml);
					addMouseMotionListener(mml);
				}
				private void createMotionListener(MouseEvent ev) {
					log.finer("ServiceItem: "+DesktopItem.this+": CreateMotionListener: "+ev );
					if( ev.getButton() != 1 )
						return;
					lastItem = DesktopItem.this;
					lastItem.enter();
					log.finer("entered into: "+lastItem );
					Point p = lastItem.getLocation();
					log.finer( "drag starts at: "+ev.getX()+", "+ev.getY() );
					final int offx = ev.getX();
					final int offy = ev.getY();
					
					// Check if we already have the lister.
					if( mml != null ) {
						log.finer("Already have listener: "+mml );
						return;
					}
					mml = new MouseMotionAdapter() {
						public void mouseMoved( MouseEvent ev ) {
							mouseDragged(ev);
						}
						public void mouseDragged( MouseEvent ev ) {
//							adgr.resetRecognizer();
							log.finer("item mouse drag/move: "+ev );
//							if( ev.getModifiers() != ev.Button1 )
//								return;
							log.finer("item: offx: "+offx+", offy: "+offy+
								", evx: "+ev.getX()+", evy: "+ev.getY());
							Point pt = getLocation();
							Point p = new Point( ev.getX()+pt.x-offx,
								ev.getY()+pt.y-offy);
							int xoff = p.x % 5;
							int yoff = p.y % 5;
							p = new Point( p.x-xoff+5, p.y-yoff+5);
							desklocs.put( sr, p );
							pane.setLayer( DesktopItem.this,
								JLayeredPane.DRAG_LAYER.intValue() );
							setLocation(p.x,p.y);
						}
					};
				}
//				public void mouseEntered( MouseEvent ev ) {
//					createMotionListener(ev);
//					addMouseMotionListener(mml);
//				}
				public void mouseExit( MouseEvent ev ) {
					log.finer("item: Mouse exit: "+ev );
					removeMouseMotionListener(mml);
					pane.setLayer( DesktopItem.this, 
						JLayeredPane.DEFAULT_LAYER.intValue(), 0 );
//					pane.removeMouseMotionListener(mml);
				}
				public void mouseReleased( MouseEvent ev ) {
					log.finer("item: Mouse Released: "+ev );
					if( ev.isPopupTrigger() ) {
						if( lastItem != DesktopItem.this ) {
							lastItem.exit();
						}
						lastItem = DesktopItem.this;
						lastItem.enter();
						popup(ev,DesktopItem.this);
						return;
					}
					removeMouseMotionListener(mml);
					pane.setLayer( DesktopItem.this, 
						JLayeredPane.DEFAULT_LAYER.intValue(), 0 );
//					pane.removeMouseMotionListener(mml);
				}
			});
			setBackground( pane.getBackground().darker() );
		}

		public DesktopItem( String name, ServiceEntry se,
				UIDescriptor uid, String role, boolean trans, String group, List<String>helps ) {
			super( name );
			svcEntry = se;
			long svcItem;	// Use this to make references to svcItem be errors
			isTransient = trans;
			sr = new ServiceRegistration( se.getServiceID(), uid.role, group );
//			ld = Thread.currentThread().getContextClassLoader();
			log.fine( "Create DesktopItem("+name+","+uid.role+"): loader="+ld );
			this.svcItem = null;
			this.helps = helps;
			if( helps.size() > 0 ) {
				showHelpsFor( this );
			}

			sid = se.getServiceID();
			setIcon( new Icon() {
				int sz = 24;
				public int getIconWidth() {
					return sz+2;
				}
				public int getIconHeight() {
					return sz+2;
				}
				public void paintIcon( Component c, Graphics g, int x, int y ) {
					if( isOpaque() )
						g.setColor( getBackground() );
					else
						g.setColor( getParent().getBackground() );
					g.fillRect( x, y, sz, sz );
					g.setColor( getParent().getForeground() );
					for( int i = 0; i < sz/2; i += 4 ) {
						g.drawOval( x+(i/2), y+(i/2), sz-i, sz-i );
					}
				}
			});
			this.name = name;
			this.uid = uid;
			this.role = role;
			setText( name+(wrole.isSelected() ? " ("+role+")" : "") );
			setForeground( pane.getForeground() );
			if( ! wicon.isSelected() ) {
				setHorizontalTextPosition(CENTER);
				setVerticalTextPosition(BOTTOM);
			} else {
				setHorizontalTextPosition(RIGHT);
				setVerticalTextPosition(CENTER);
			}
			setFont( new Font( "serif", Font.BOLD, 12 ) );
			addMouseListener( new MouseAdapter() {
				public void mouseClicked( MouseEvent ev ) {
					if( ev.getClickCount() == 2 ) {
						log.finer("Invoking: "+DesktopItem.this);
						try {
							DesktopItem.this.invoke();
						} catch( Exception ex ) {
							reportException(ex);
						}
					}
				}

				public void mousePressed( MouseEvent ev ) {
					if( ev.isPopupTrigger() ) {
						if( lastItem != DesktopItem.this ) {
							lastItem.exit();
						}
						lastItem = DesktopItem.this;
						lastItem.enter();
						popup(ev, DesktopItem.this);
						return;
					}
					if( lastItem != null ) {
						lastItem.exit();
					}
					if( ev.getButton() != 1 )
						return;
					createMotionListener(ev);
					DesktopItem.this.addMouseMotionListener(mml);
				}
				private void createMotionListener(MouseEvent ev) {
					log.finer( "ServiceEntry: "+DesktopItem.this+": CreateMotionListener: "+ev );
					lastItem = DesktopItem.this;
					lastItem.enter();
					Point p = lastItem.getLocation();
					log.finer( "drag starts at: "+ev.getX()+", "+ev.getY() );
					final int offx = ev.getX();
					final int offy = ev.getY();
					if( mml != null )
						return;
					mml = new MouseMotionAdapter() {
						public void mouseMoved( MouseEvent ev ) {
							mouseDragged(ev);
						}
						public void mouseDragged( MouseEvent ev ) {
							log.finer("entry: offx: "+offx+", offy: "+offy+
								", evx: "+ev.getX()+", evy: "+ev.getY());
//							Point pt = getLocation();
//							Point p = new Point( ev.getX()+pt.x-offx,
//								ev.getY()+pt.y-offy);
							Point pt = getLocation();
							Point p = new Point( ev.getX()+pt.x-offx,
								ev.getY()+pt.y-offy);
							int xoff = p.x % 5;
							int yoff = p.y % 5;
							p = new Point( p.x-xoff+5, p.y-yoff+5);
							desklocs.put( sr, p );
							pane.setLayer( DesktopItem.this,
								JLayeredPane.DRAG_LAYER.intValue() );
							setLocation(p.x,p.y);
						}
					};
				}

				public void mouseExit( MouseEvent ev ) {
					log.info("entry: Mouse exit: "+ev );
					DesktopItem.this.removeMouseMotionListener(mml);
					pane.setLayer( DesktopItem.this, 
						JLayeredPane.DEFAULT_LAYER.intValue(), 0 );
				}
				public void mouseReleased( MouseEvent ev ) {
					log.finer("entry: Mouse Released: "+ev );
					if( ev.isPopupTrigger() ) {
						// Make sure the correct last item is identified.
						if( lastItem != DesktopItem.this ) {
							lastItem.exit();
						}
						lastItem = DesktopItem.this;
						lastItem.enter();
						popup(ev, DesktopItem.this);
						if( mml != null )
							DesktopItem.this.removeMouseMotionListener(mml);
						return;
					}
					DesktopItem.this.removeMouseMotionListener(mml);
					pane.setLayer( DesktopItem.this, 
						JLayeredPane.DEFAULT_LAYER.intValue(), 0 );
				}
			});
			setBackground( pane.getBackground().darker() );
		}
		MouseMotionListener mml;

		protected void popup( MouseEvent ev, JComponent inside ) {
			JPopupMenu m = new JPopupMenu();
			JMenuItem mi = new JMenuItem( name );
			mi.setEnabled(false);
			m.add(mi);
			m.addSeparator();
			mi = new JMenuItem( attached ? "Detach" : "Attach" );
			mi.setEnabled( active );
			mi.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					if( attached ) {
						detach( serviceID() );
					} else {
						reAttach( serviceID() );
					}
				}
			});
			m.add(mi);

			mi = new JMenuItem( !active ? "Open" : "Switch To" );
			m.add( mi );
			mi.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					log.fine("Activating...");
					try {
						DesktopItem.this.invoke();
					} catch( Exception ex ) {
						reportException(ex);
					}
				}
			});

			mi = new JMenuItem("Open In Tab");
			m.add( mi );
			mi.setEnabled( !active );
			mi.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					log.fine("Activating in tab...");
					try {
						DesktopItem.this.invoke( false, true );
					} catch( Exception ex ) {
						reportException(ex);
					}
				}
			});

			mi = new JMenuItem( "Delete Icon" );
			m.add( mi );
			mi.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					Container c = getParent();
					c.remove( DesktopItem.this );
					c.repaint();
					items.remove( DesktopItem.this );
					desklocs.remove( sr );
				}
			});
			
			
//			// We don't have a way to implement this yet.  Need to have a more
//			// explicit indication of what type of component this desktop item is
//			// open as so that we can remove it from a tab, close the frame etc.
//			mi = new JMenuItem( "Close" );
//			m.add( mi );
//			mi.addActionListener( new ActionListener() {
//				public void actionPerformed( ActionEvent ev ) {
//				}
//			});

			mi.setEnabled( !active );
			m.show( inside, ev.getX(), ev.getY() );
		}

		protected void enter() {
			setOpaque(true);
			repaint();
		}

		protected void exit() {
			setOpaque(false);
			repaint();
		}

		public DesktopItem( Icon icon, String name, 
				ServiceEntry se, UIDescriptor uid, 
					String role, boolean trans, String group, List<String> helps ) {
			this( name, se, uid, role, trans, group, helps );
			log.fine( name + ": icon=" + icon );
			if( icon != null )
				setIcon(icon);
			this.icon = icon;
			isTransient = trans;
		}

		public DesktopItem( Icon icon, String name, 
				ServiceItem svcItem, UIDescriptor uid, 
					String role, boolean trans, String group, List<String> helps ) {
			this( name, svcItem, uid, role, trans, group, helps );
			log.fine( name + ": icon=" + icon );
			if( icon != null )
				setIcon(icon);
			this.icon = icon;
			isTransient = trans;
		}
	}
	volatile DesktopItem lastItem;
	
	public UIDescriptor getDescriptor() {
		log.info("allDesc == "+allDesc );
		return new UIDescriptor();
//		return allDesc.isSelected() ? new UIDescriptor() : new ApplicationUIDescriptor();
	}

	/** The default template for any serviceUI services. */
	protected ServiceTemplate templ;

	protected void startLookup() throws Exception {
		if( System.getSecurityManager() == null )
			System.setSecurityManager( getSecurityManager() );

//		templ = getTemplate();
//		log.info("Got Template: "+templ );
//		if( templ == null ) {
//			log.info( "using default ApplicationUIDescriptor only template" );
			templ = new ServiceTemplate( null,
				null, //new Class[] { Administrable.class }, 
				new Entry[] { getDescriptor() } );
//		}
		String group[] = getGroups();

		if( ldm != null ) {
			try {
				log.fine("Terminating existing LookupDiscoveryManager");
				ldm.terminate();
			} catch( Exception ex ) {
				log.log(Level.SEVERE,ex.toString(),ex);
			}
			ldm = null;
		}

		log.fine("Creating new LookupDiscoveryManager");
		LookupLocator[]locs = getLocators();
		if( locs != null ) {
			for( LookupLocator ll : locs ) {
				log.fine("Lookup Locator: "+ll );
				addAuth( ll.getHost() );
			}
		}
		ldm = new LookupDiscoveryManager(
			group,
			getLocators(),
			new DiscoveryListener() {
				public void discarded( DiscoveryEvent ev ) {
					log.fine("discarded: "+ev );
					final ServiceRegistrar regs[] = ev.getRegistrars();
					new Thread() {
						public void run() {
							try {
								discardRegs( regs );
							} catch( Throwable ex ) {
								log.log( Level.SEVERE, ex.toString(), ex );
							}
						}
					}.start();					
				}
				public void discovered( DiscoveryEvent ev ) {
					log.fine("discovered: "+ev );
					final ServiceRegistrar regs[] = ev.getRegistrars();
					new Thread() {
						public void run() {
							try {
								processRegs( regs );
							} catch( Throwable ex ) {
								log.log( Level.SEVERE, ex.toString(), ex );
							}
						}
					}.start();
				}
			}, conf
		);
		log.fine("Lookup Started");
	}
	
	protected void discardRegs( ServiceRegistrar []regs ) {
		Logger log = this.log;
		log.fine("discarding: "+
			(regs == null ? 0 : regs.length)+" registrars" );
		for( int i = 0; i < regs.length; ++i ) {
			try {
				dropServicesFor( regs[i] );
			} catch( Exception ex ) {
				log.log( Level.WARNING, ex.toString(), ex );
			}
		}
	}
	
	protected void processRegs( ServiceRegistrar []regs ) {
		Logger log = this.log;
		log.fine("discovered: "+
			(regs == null ? 0 : regs.length)+" registrars" );
		for( int i = 0; i < regs.length; ++i ) {
			if( regs[i] instanceof ServiceLookup && 
					System.getProperty("org.wonderly.servicelookup") == null ) {
				addServicesFor( (ServiceLookup)regs[i] );
			} else {
				addServicesFor( regs[i] );
			}
		}
	}
	
	protected void addServicesFor( ServiceRegistrar reg ) {
		try {
			// Create a proxy preparer for service lookup proxy preparation.
			ProxyPreparer p = (ProxyPreparer)conf.getEntry(
				getClass().getName(), "lusPreparer",
					ProxyPreparer.class, null );
			log.finer(getClass().getName()+".lusPreparer: "+p );
			
			// Nothing in the default configuration, try ConfigurableJiniApplication's method
			if( p == null ) {
				p = getProxyPreparer();
				log.finer("no lusPreparer, using default: "+p );
			}

			// Keep the original registrar object reference around
			final ServiceRegistrar freg = reg;
			
			// If there is one configured use it to prepare the proxy.
			if( p != null && reg instanceof RemoteMethodControl ) {
				reg = (ServiceRegistrar)p.prepareProxy( reg );
				if(log.isLoggable(Level.FINE) ) log.fine("preparing proxy for "+reg.getLocator()+" with: "+p );
			}

			// Create a logger for this registrars hostname
			Logger slog = log;
			if( log.isLoggable(Level.FINE) ) {
				String locname = reg.getLocator().getHost()+"";
				slog = Logger.getLogger( "org.wonderly.desktop."+locname );
			}
			
			// Turn on notification before the lookup
			slog.fine("Using lookup template: "+dumpTemplate(templ));
			final ServiceRegistrar sfreg = reg;
			new Thread() {
				public void run() {
					try {
						JiniDeskTop.this.notify( freg, sfreg );
					} catch( Exception ex ) {
						reportException(ex);
					}
				}
			}.start();
			
			// Now lookup using the template to see what is visible.
			if( slog.isLoggable(Level.FINE) ) slog.fine("Notify requested, lookup now");
			ServiceMatches mt = reg.lookup( templ, Integer.MAX_VALUE );
			
			// Get the matching service items.
			ServiceItem itms[] = mt.items;
			if( slog.isLoggable(Level.FINE) ) slog.fine("lookup found "+itms.length+" matches");
			
			for( ServiceItem sitm : itms ) {
//				ClassLoader ld = Thread.currentThread().getContextClassLoader();
//				log.finer("["+j+"] Current context class loader: "+ld );
				try {
					if( sitm == null || sitm.service == null ) {
						throw new NullPointerException( "Service null, check codebase" );
					}

					if( slog.isLoggable(Level.FINE) ) slog.fine("Processing "+
						sitm.service.getClass().getName()+
						" using context laoder: "+
						sitm.service.getClass().getClassLoader() );
					processItem( sitm, freg );
				} catch( SecurityException ex ) {
					log.log(Level.SEVERE,ex.toString(),ex);
					if( ex.getMessage().indexOf("RemoteMethodControl") >= 0 ) {
						log.warning("Found Jini 1.X registrar: "+reg.getLocator()+", ignoring");
					} else {
						JOptionPane.showMessageDialog( frame, ex,
							"Configuration Error Detected", JOptionPane.ERROR_MESSAGE );
					}
				} catch( Throwable ex ) {
					log.log(Level.SEVERE,ex.toString(),ex);
					JOptionPane.showMessageDialog( frame, ex,
						"Discovery Error Detected", JOptionPane.ERROR_MESSAGE );
//				} finally {
//					Thread.currentThread().setContextClassLoader(ld);
				}
			}
			if( slog.isLoggable(Level.FINE) ) slog.fine("checked all "+itms.length+" matches");
		} catch( AccessControlException ex ) {
			if(log.isLoggable(Level.FINE)) {
				throw ex;
			} else {
				log.log( Level.WARNING, ex.toString(), ex );
			}
		} catch( IOException ex ) {
			log.log(Level.SEVERE,ex.toString(),ex);
			JOptionPane.showMessageDialog( frame, ex,
				"Remote Error Occured", JOptionPane.ERROR_MESSAGE  );
		} catch( ConfigurationException ex ) {
			log.log(Level.SEVERE,ex.toString(),ex);
			JOptionPane.showMessageDialog( frame, ex,
				"Configuration Error Occured", JOptionPane.ERROR_MESSAGE  );
		} catch( RuntimeException ex ) {
			log.log(Level.SEVERE,ex.toString(),ex);
		} finally {
			if( log.isLoggable(Level.FINE) ) log.fine( "discovered handled: " + reg );
		}

		if( log.isLoggable(Level.FINE) ) log.fine("discovered handling exiting");
	}
	
	/**
	 *  Perform service lookup using the ServiceLookup mechanism instead of
	 *  the ServiceRegistrar mechanism.
	 */
	protected void addServicesFor( ServiceLookup reg ) {
		try {
			// Get a logger for the locators host
			Logger slog = log;
			if( log.isLoggable(Level.FINE) ) {
				String locname = reg.getLocator().getHost()+"";
				slog = Logger.getLogger( "org.wonderly.desktop."+locname );
			}
			
			// Get a proxy preparer for this lus
			ProxyPreparer p = (ProxyPreparer)conf.getEntry(
				getClass().getName(), "lusPreparer",
					ProxyPreparer.class, null );
			if( slog.isLoggable(Level.FINER) ) slog.finer("lusPreparer: "+p );
			
			// If not defined in configuration, try the ConfigurableJiniApplication method
			if( p == null ) {
				p = getProxyPreparer();
				if( slog.isLoggable(Level.FINER) ) slog.finer("no lusPreparer, using default: "+p );
			}
			
			// Save a reference to the original registrar
			final ServiceLookup freg = reg;
			if( p != null ) {
				if( slog.isLoggable(Level.FINE) ) slog.fine("preparing proxy for "+reg.getLocator()+" with: "+p );
				reg = (ServiceLookup)p.prepareProxy( reg );
			}
			
			// Request notification before performing the lookup.
			if( slog.isLoggable(Level.FINE) ) slog.fine("Using lookup template: "+dumpTemplate(templ));
			final ServiceLookup sfreg = reg;
			new Thread() {
				public void run() {
					try {
						JiniDeskTop.this.notify( (ServiceRegistrar)freg, (ServiceRegistrar)sfreg );
					} catch( Exception ex ) {
						reportException( ex );
					}
				}
			}.start();
			doLookup( log, reg, freg );
		} catch( IOException ex ) {
			log.log(Level.SEVERE,ex.toString(),ex);
			JOptionPane.showMessageDialog( frame, ex,
				"Remote Error Occured", JOptionPane.ERROR_MESSAGE  );
		} catch( ConfigurationException ex ) {
			log.log(Level.SEVERE,ex.toString(),ex);
			JOptionPane.showMessageDialog( frame, ex,
				"Configuration Error Occured", JOptionPane.ERROR_MESSAGE  );
		} catch( RuntimeException ex ) {
			log.log(Level.SEVERE,ex.toString(),ex);
		} finally {
			if( log.isLoggable(Level.FINE) ) log.fine( "discovered handled: " + reg );
		}
		if( log.isLoggable(Level.FINE) ) log.fine("discovered handling exiting");
	}
	
	private ArrayBlockingQueue<Runnable> entryQueue;
	private ThreadPoolExecutor entryExec;

	private void doLookup( final Logger log, ServiceLookup reg, final ServiceLookup freg ) throws IOException {
		final String host = reg.getLocator().toString();
		try {
			// Perform the lookup.
			if( log.isLoggable(Level.FINE) ) log.fine("Notify requested, lookup now: "+host);
			RemoteIterator<ServiceEntry> mt = reg.lookupEntries( templ, Integer.MAX_VALUE );
			if( log.isLoggable(Level.FINE) ) log.fine(host+": lookup returned: "+mt);
			int j = 0;
			while( mt.hasNext() ) {
				final ServiceEntry se = mt.next();
				if( log.isLoggable(Level.FINE) ) log.fine("enqueuing["+host+"]: "+j+" entry: "+se);
				entryExec.execute( new Runnable() {
					public void run() {
						try {
							if( log.isLoggable(Level.FINE) ) log.fine(host+": processing item: "+se );
							processItem( se, freg );
						} catch( NoSuchMethodError ex ) {
							log.log( Level.SEVERE, ex.toString()+": "+se.getClass().getClassLoader(), ex );
						} catch( SecurityException ex ) {
							log.log( Level.SEVERE, ex.toString(), ex );
							JOptionPane.showMessageDialog( frame, ex,
								"Configuration Error Detected for "+host, JOptionPane.ERROR_MESSAGE );
						} catch( Exception ex ) {
							log.log( Level.SEVERE, ex.toString(), ex );
							log.throwing( getClass().getName(), "discovered", ex );
							JOptionPane.showMessageDialog( frame, ex,
								"Discovery Error Detected for "+host, JOptionPane.ERROR_MESSAGE );
						}
					}
				});
				++j;
			}
			if( log.isLoggable(Level.FINE) ) log.fine(host+": checked all "+j+" matches");
		} catch( IOException ex ) {
			log.log( Level.SEVERE, ex.toString(), ex );
			log.throwing( getClass().getName(), "discovered", ex );
			JOptionPane.showMessageDialog( frame, ex,
				"Remote Error Occured for "+host, JOptionPane.ERROR_MESSAGE  );
//		} catch( ConfigurationException ex ) {
//			log.log( Level.SEVERE, ex.toString(), ex );
//			log.throwing( getClass().getName(), "discovered", ex );
//			JOptionPane.showMessageDialog( frame, ex,
//				"Configuration Error Occured", JOptionPane.ERROR_MESSAGE  );
		} catch( Exception ex ) {
			log.log( Level.SEVERE, ex.toString(), ex );
			log.throwing( getClass().getName(), "discovered", ex );
		} finally {
			if( log.isLoggable(Level.FINE) ) log.fine( "discovered handled: "+host );
		}
	}

	protected void dropServicesFor( ServiceRegistrar reg ) {
		Lease l = leases.remove(reg);
		log.fine("Dropping services for registrar: "+reg );
		if( l != null ) {
			log.fine("cancelling notify lease: "+l);
			try {
 				lrm.cancel(l);
			} catch( Exception ex ) {
				log.log(Level.WARNING, ex.toString(), ex );
			}
		}
		log.fine("removing all desktop items for: "+reg );
		removeItemsFor( reg );
		ldm.discard(reg);
	}

	protected void addAuthorizationFor( String host ) throws IOException {
		enableForHost( host, domains );
	}

	protected void removeAuthorizationFor( String host ) {
		synchronized( hostProts ) {
			List<ProtectionDomain>prots = hostProts.remove( host );
			
			log.info("Protection domains for: "+host+" = "+prots );
			for( ProtectionDomain p: prots ) {
				log.info("removing ProtectionDomain: "+p );
				domains.remove( p );
			}
		}
	}

	JMenu unauthmen = new JMenu("Hosts");
	class AuthorizationNeededItem extends JCheckBoxMenuItem implements ActionListener,Comparable<AuthorizationNeededItem> {
		String host;
		public int compareTo( AuthorizationNeededItem obj ) {
			if( obj instanceof AuthorizationNeededItem == false )
				return -1;
			return ((AuthorizationNeededItem)obj).host.compareTo( host );
		}
		public boolean equals( Object obj ) {
			if( obj instanceof AuthorizationNeededItem == false )
				return false;
			return ((AuthorizationNeededItem)obj).host.equals(host);
		}
		public int hashCode() {
			return host.hashCode();
		}

		public AuthorizationNeededItem( String host ) throws IOException {
			this( host, false );
		}

		public AuthorizationNeededItem( String host, boolean sel ) throws IOException {
			super(host);
			setSelected( sel );
			addActionListener( this );
			this.host = host;
			if( sel ) {
				addAuthorizationFor( host );
			}
		}
		public void actionPerformed( ActionEvent ev ) {
			if( isSelected() ) {
				try {
					addAuthorizationFor( host );
				} catch( Exception ex ) {
					log.log( Level.SEVERE, ex.toString(), ex );
				}
			} else {
				removeAuthorizationFor( host );
			}
		}
	}

	ArrayList<AuthorizationNeededItem>noauths = new ArrayList<AuthorizationNeededItem>();
	private void addAuth( final ServiceRegistrar reg ) {
		new Thread() {
			public void run() {
				try {
					String host = reg.getLocator().getHost();
					addAuth( host );
				} catch( Exception ex ) {
					log.log( Level.WARNING, ex.toString(), ex );
				}
			}
		}.start();
	}

	private void addAuth( String host ) {
		try {
			AuthorizationNeededItem item = null;
			synchronized( noauths ) {
				item = new AuthorizationNeededItem( host, hosts.contains(host) );
				if( noauths.contains( item ) == false ) {
					noauths.add( item );
					final AuthorizationNeededItem fitem = item;
					SwingUtilities.invokeAndWait( new Runnable() {
						public void run() {
							unauthmen.add( fitem );
						}
					});
				}
			}
		} catch( Exception ex ) {
			log.log( Level.WARNING, ex.toString(), ex );
		}
	}

	private volatile long seq;
	private Hashtable<ServiceRegistrar,Lease> leases = new Hashtable<ServiceRegistrar,Lease>();	
	protected void notify( final ServiceRegistrar freg, final ServiceRegistrar reg ) 
			throws IOException, ConfigurationException {
		// We do this call and throw the exception so that the caller will
		// know if we have an immediate reachability problem.
		String regnm = null;
		if( leases.get( freg ) != null ) {
			if( log.isLoggable( Level.FINE ) ) {
				regnm = reg.getLocator().toString();
				log.fine("Already have active notify for: "+regnm );
			}
			return;
		}
		if( log.isLoggable(Level.FINE) )
			log.fine("adding notify for "+reg.getLocator() );

		// Create the transition listener
		RegistrarTransitionListener reglis = new RegistrarTransitionListener() {
			public void removeInstance( ServiceEvent ev, ServiceRegistrar reg ) {
				log.fine("removeInstance["+seq+"->"+ev.getSequenceNumber()+"]: "+ev+" for: "+reg );
				dropServiceEvent( reg, ev );
				seq = ev.getSequenceNumber();
				// TODO: track seq for each registrar, and fire off a forced
				// lookup if they are not in sequence, or close to it.
			}

			public void addInstance( ServiceEvent ev, ServiceRegistrar reg ) {
				log.fine("adding["+seq+"->"+ev.getSequenceNumber()+"]: service: "+ev );
				addServiceEvent( freg, ev );
				seq = ev.getSequenceNumber();
				// TODO: track seq for each registrar, and fire off a forced
				// lookup if they are not in sequence, or close to it.
			}

			public void updateInstance( ServiceEvent ev, ServiceRegistrar reg ) {
				log.fine("updating["+seq+"->"+ev.getSequenceNumber()+"]: service: "+ev );
				updateServiceEvent( reg, ev );
				seq = ev.getSequenceNumber();
				// TODO: track seq for each registrar, and fire off a forced
				// lookup if they are not in sequence, or close to it.
			}
		};

		// Create the RemoteListener that will call back through our transition listener
		RemoteEventListener remlis = new RemoteListener( reglis, reg );

		// Get an exporter for the remote listener and export it.
		Exporter exp = getExporter();
		log.fine("Exporting remote listener instance with: "+exp);
		remlis = (RemoteEventListener)exp.export(remlis);
		
		// Remember the listener to unexport it.
		remotelisteners.put( reg, exp );
		log.fine( "calling notify" );
		addAuth(freg);
		
		// Make the remote call to register the notification request.
		EventRegistration er = null;
		try {
			er = reg.notify( templ,
				reg.TRANSITION_MATCH_NOMATCH |
				reg.TRANSITION_NOMATCH_MATCH |
				reg.TRANSITION_MATCH_MATCH,
				remlis, null, Lease.FOREVER );
		} catch( AccessControlException ex ) {
			if( log.isLoggable(Level.FINE) ) {
				throw ex;
			} else {
				log.log( Level.WARNING, ex.toString(), ex );
				return;
			}
		} catch( RemoteException ex ) {
			log.log( Level.WARNING, ex.toString(), ex );
			return;
		}
			
			
		// If/when the call returns, put up a lease renewal for the
		// the notification.
		Lease l = er.getLease();
		lrm.renewUntil( l, Lease.FOREVER, new LeaseListener() {
			public void notify( LeaseRenewalEvent ev ) {
                        Throwable lex = ev.getException();
                        if( lex != null ) {
                            log.log(Level.WARNING,lex.toString(),lex);
                        }
                        log.log(Level.FINE,ev.getLease()+
                        	" expiration Event: "+new Date(ev.getExpiration()));
                        try {
							if( log.isLoggable( Level.FINE ) ) {
								String regnm = reg.getLocator().toString();
								log.fine("Discarding lease expired registrar: "+regnm);
							}
                            ldm.discard( freg );
                        } catch( Exception ex ) {
                            log.log(Level.WARNING,ex.toString(),ex);
                        }
				log.fine("Lease notify: "+ev );
				try {
					leases.remove(freg).cancel();
				} catch( Exception ex ) {
					log.log( Level.WARNING, ex.toString(), ex );
				}
				removeItemsFor(freg);
			}
		});
		
		// Save the lease for later removal or cancellation.
		log.fine("Recording notification lease for: "+freg+" with "+l );
		leases.put( freg, l );
	}

	private LeaseRenewalManager lrm = new LeaseRenewalManager();
	private Hashtable<ServiceRegistrar,Exporter> remotelisteners =
		new Hashtable<ServiceRegistrar,Exporter>();

	public void serviceLost( ServiceID id, ServiceRegistrar reg ) {
		seen.remove( id );
	}

	public void updateItem( ServiceItem item, ServiceRegistrar reg ) throws ConfigurationException,IOException {
		try {
			log.warning("Service Item update for: "+dumpItem( item )+" at "+reg.getLocator() );
		} catch( Exception ex ) {
			log.log( Level.WARNING, ex.toString(), ex );
		}
	}
	
	protected String nameFor( ServiceItem item ) {
		Entry[]ents = item.attributeSets;
		String name = null;

		for( Entry e : ents ) {
			if( e instanceof Name ) {
				name = ((Name)e).name;
			} else if( e instanceof ServiceInfo && name == null ) {
				name = ((ServiceInfo)e).name;
			}
		}
		if( name == null )
			name = item.serviceID.toString();
		return name;
	}

	protected String dumpItem( ServiceItem item ) {
		return nameFor(item)+"("+item.serviceID+")";
	}

	protected void addServiceEvent( ServiceRegistrar reg, ServiceEvent ev ) {
		try {
			if( reg instanceof ServiceLookup && ev instanceof ServiceEntryEvent ) {
				RemoteIterator<ServiceEntry> ents = ((ServiceEntryEvent)ev).getEntries();
				while( ents.hasNext() ) {
				processItem( ents.next(), (ServiceLookup)reg );
				}
			} else {
				processItem( ev.getServiceItem(), reg );
			}
		} catch( Exception ex ) {
			reportException(ex);
		}
	}

	Hashtable<ServiceID,ServiceItem> seen = 
		new Hashtable<ServiceID,ServiceItem>();
	public void processItem( ServiceItem item,
		final ServiceRegistrar freg ) 
				throws ConfigurationException,IOException {
		ServiceRegistrar ereg;
		ProxyPreparer lp = (ProxyPreparer)conf.getEntry(
			getClass().getName(), "lusPreparer",
				ProxyPreparer.class, null );
		if( log.isLoggable( Level.FINER ) ) {
			log.log(Level.FINER, "Processing Item",
				new Throwable("processing item: "+item ) );
		}
		log.finer("lusPreparer: "+lp );
		if( lp == null ) {
			lp = getProxyPreparer();
			log.finer("no lusPreparer, using default: "+lp );
		}
		ereg = freg;
		if( lp != null ) {
			if( log.isLoggable( Level.FINE) ) 
				log.fine("preparing proxy for "+ereg.getLocator()+" with: "+lp );
			ereg = (ServiceRegistrar)lp.prepareProxy( ereg );
		}
		log.fine("Checking ("+freg.getLocator()+") serviceItem id: "+item.serviceID );
		synchronized( seen ) {
			if( seen.get( item.serviceID ) != null ) {
				if( log.isLoggable(Level.FINE) ) {
					log.fine("already have service: "+
						item.serviceID+" from: "+ereg.getLocator() );
				}
				return;
			}
			seen.put( item.serviceID, item );
		}
		Entry ent[] = item.attributeSets;
		Object svcInst = item.service;
		if( log.isLoggable(Level.FINER) ) {
			log.finer("Service is: "+svcInst+(svcInst == null ? 
				"" : (" -> "+svcInst.getClass().getName()+")" ) ) );
			Class c[] = svcInst.getClass().getInterfaces();
			for( int i = 0; i < c.length; ++i ) {
				log.finer("interface["+i+"]: "+c[i].getName() );
			}
		}
		
		if( svcInst == null )
			throw new NullPointerException("Service is null, check codebase: "+name);
		ProxyPreparer p = getProxyPreparer();
		log.finer("ProxyPreparer is: "+p.getClass().getName() );
		item.service = svcInst = p.prepareProxy(svcInst);
		log.finer("Prepared proxy is: "+svcInst.getClass().getName() );
		String name = "unknown service";
		if( svcInst != null )
			name = svcInst.getClass().getName();
		String vs[] = name.split("\\.");
		name = vs[vs.length-1];
		String icon = null;
		String group = null;
		boolean trans = true;
		String irole;
		// A place to store any provided help.
		final List<HelpEntry>helps = new ArrayList<HelpEntry>(3);
		// The entries created from the UIDescriptors here.
		HashMap<String,DesktopEntry> desks =
			new HashMap<String,DesktopEntry>();
		// The icons for this service
		HashMap<String,String> icons =
			new HashMap<String,String>();
		// Now extract all the data out of the Entry objects
		for( int i = 0; i < ent.length; ++i ) {
			if( ent[i] instanceof Name ) {
				name = ((Name)ent[i]).name;
			} else if( ent[i] instanceof ServiceInfo ) {
				name = ((ServiceInfo)ent[i]).name;
			} else if( ent[i] instanceof HelpEntry ) {
				helps.add( (HelpEntry)ent[i] );
			} else if( ent[i] instanceof DesktopIcon ) {
				DesktopIcon di = (DesktopIcon)ent[i];
				if( di.iconRole == null ) {
					irole = null;
				} else {
					log.fine("Setting Role Icon " + di.iconRole + "=" + di.iconUrl );
					icons.put( di.iconRole, di.iconUrl );
				}
				icon = di.iconUrl;
			} else if( ent[i] instanceof DesktopCodebase ) {
				String path = ((DesktopCodebase)ent[i]).path;
				svcCodebases.put( item.serviceID, path );
				activateCodebase( item.serviceID, path, reqDlPerm );
			} else if( ent[i] instanceof DesktopEntry ) {
				DesktopEntry dent = (DesktopEntry)ent[i];
				desks.put( dent.iconRole, dent );
				log.fine("Desktop Entry found: "+dent );
			} else if( ent[i] instanceof DesktopGroup) {
				group = ((DesktopGroup)ent[i]).group;
				trans = ((DesktopGroup)ent[i]).isTransient();
			}
		}

		// Look for all UIDescriptors and add DesktopItems for each role.
		int cnt = 0;
		log.finer("Using name: "+name+", group: "+group );
		for( int i = 0; i < ent.length; ++i ) {
			if( ent[i] instanceof UIDescriptor ) {
				log.fine("ent["+i+"] ServiceUI("+
					ent[i].getClass().getName()+
					") found for: "+name );
				cnt++;
				UIDescriptor uid = (UIDescriptor)ent[i];
				DesktopEntry dent = desks.get( uid.role );
				if( dent != null ) {
					log.finer( "using desktop: "+dent );
					icon = dent.iconUrl;
					group = dent.group;
					trans = dent.istrans;
				}
				String micon = icons.get( uid.role );
				if( micon != null ) {
					log.finer("overriding role icon: "+uid.role+"="+micon);
					icon = micon;
				}
				final String fname = name;
				final ServiceItem fitem = item;
				final String ficon = icon;
				final String fgroup = group;
				final boolean ftrans = trans;
				final UIDescriptor fuid = uid;
				new ComponentUpdateThread() {
					public Object construct() {
						addRole( fname, freg, fitem, 
							fuid, ficon, fgroup, ftrans, helps );
						return null;
					}
				}.start();
			}
		}

		// No UIDescriptor found, add a default one for services without a
		// serviceUI that will handle Administrable
		if( cnt == 0 ) {
			log.fine("No UI, adding adminstrable descr");
			UIDescriptor uid = new AdminDescriptor();
			String micon = (String)icons.get( uid.role );
			if( micon != null )
				icon = micon;
//			final String fname = name;
//			final ServiceItem fitem = item;
//			final String ficon = icon;
//			final String fgroup = group;
//			final boolean ftrans = trans;
//			final UIDescriptor fuid = uid;
//			new ComponentUpdateThread() {
//				public Object construct() {
//					addRole( fname, freg, fitem, 
//						fuid, ficon, fgroup, ftrans, helps );
//					return null;
//				}
//			}.start();

			// Do this synchronously to avoid race conditions in the
			// code that are creating problems with swing and other
			// data values.
			addRole( name, freg, item, uid, 
				icon, group, trans, helps );
		}

		// The JiniExplorer interface tells us how to find LookupEnv instances that will
		// allow us to find new desktop services.
		if( svcInst instanceof JiniExplorer ) {
			log.fine("Found JiniExplorer instance, performing lookups");
			LookupEnv[]arr = ((JiniExplorer)svcInst).getLookups();
			for(int i = 0; i < arr.length; ++i ) {
				log.finer("LookupEnv["+i+"]: "+arr[i] );
				try {
					org.wonderly.util.jini2.ServiceLookup sl =
						new org.wonderly.util.jini2.ServiceLookup( arr[i], this, log, conf );
					sl.start();
				} catch( ConfigurationException ex ) {
					log.throwing( getClass().getName(), "processItem", ex );
				} catch(IOException ex ) {
					log.throwing( getClass().getName(), "processItem", ex );
				}
			}
		}
		log.fine("Processing completed");
	}

	Hashtable<ServiceID,ServiceEntry> seSeen = 
		new Hashtable<ServiceID,ServiceEntry>();
	public void processItem( ServiceEntry se,
		final ServiceLookup freg ) 
				throws ConfigurationException,IOException {
					
		// Ignore ServiceLookup if set.
		if( System.getProperty("org.wonderly.servicelookup") != null ) {
			log.warning("processing "+se+" as a serviceitem instead");
			processItem( se.getItem(), (ServiceRegistrar)freg );
			return;
		}
		ServiceLookup ereg;
		ProxyPreparer lp = (ProxyPreparer)conf.getEntry(
			getClass().getName(), "lusPreparer",
				ProxyPreparer.class, null );
		if(log.isLoggable(Level.FINER) ) log.finer("lusPreparer: "+lp );
		if( lp == null ) {
			lp = getProxyPreparer();
			if(log.isLoggable(Level.FINER) ) log.finer("no lusPreparer, using default: "+lp );
		}
		ereg = freg;
		if( lp != null ) {
			if(log.isLoggable(Level.FINE) ) log.fine("preparing proxy for "+ereg.getLocator()+" with: "+lp );
			ereg = (ServiceLookup)lp.prepareProxy( ereg );
		}
		log.finer("id="+se.getServiceID()+", seSeen has: "+seSeen );
		if( log.isLoggable( Level.FINE) ) log.fine("Checking ("+freg.getLocator()+") serviceEntry id: "+se.getServiceID() );
		synchronized( seSeen ) {
			if( seSeen.get( se.getServiceID() ) != null ) {
				if(log.isLoggable(Level.FINE) ) log.fine("already have service: "+
					se.getServiceID()+" from: "+ereg.getLocator() );
				return;
			}
			seSeen.put( se.getServiceID(), se );
		}
		
		if(log.isLoggable(Level.FINE) ) log.fine("getting marshalled attribute sets");
		Set<ServiceDataAccess<? extends Entry>>ents = se.getMarshalledAttributeSets();
		if(log.isLoggable(Level.FINE) ) log.fine("service attribute sets: "+ents);

		String name = se.getServiceID().toString();

		String icon = null;
		String group = null;
		boolean trans = true;
		String irole;
		// A place to store any provided help.
		final List<HelpEntry>helps = new ArrayList<HelpEntry>(3);
		HashMap<String,DesktopEntry> desks =
			new HashMap<String,DesktopEntry>();
		HashMap<String,String> icons =
			new HashMap<String,String>();

		for( ServiceDataAccess<? extends Entry> sda: ents ) {
			List<String>enn = sda.getDataClassNames();
			if(log.isLoggable(Level.FINER) ) log.finer("processing attribute types: "+enn );
			if( enn.contains( Name.class.getName() ) ) {
				if(log.isLoggable(Level.FINER) ) log.finer("handling name entry");
				name = ((Name)sda.getData()).name;
			} else if( enn.contains( ServiceInfo.class.getName() ) ) {
				if(log.isLoggable(Level.FINER) ) log.finer("handling ServiceInfo entry");
				name = ((ServiceInfo)sda.getData()).name;
			} else if( enn.contains(  HelpEntry.class.getName() ) ) {
				if(log.isLoggable(Level.FINER) ) log.finer("handling help entry");
				helps.add( (HelpEntry)sda.getData() );
			} else if( enn.contains( DesktopIcon.class.getName() ) ) {
				DesktopIcon di = (DesktopIcon)sda.getData();
				if( di.iconRole == null ) {
					irole = null;
				} else {
					if(log.isLoggable(Level.FINER) ) log.finer("Setting Role Icon " + di.iconRole + "=" + di.iconUrl );
					icons.put( di.iconRole, di.iconUrl );
				}
				icon = di.iconUrl;
			} else if( enn.contains( DesktopCodebase.class.getName() ) ) {
				if(log.isLoggable(Level.FINER) ) log.finer("handling DesktopCodebase entry");
				String path = ((DesktopCodebase)sda.getData()).path;
				svcCodebases.put( se.getServiceID(), path );
				activateCodebase( se.getServiceID(), path, reqDlPerm );
			} else if( enn.contains( DesktopEntry.class.getName() ) ) {
				if(log.isLoggable(Level.FINER) ) log.finer("handling DesktopEntry entry");
				DesktopEntry dent = (DesktopEntry)sda.getData();
				desks.put( dent.iconRole, dent );
				if(log.isLoggable(Level.FINE) ) log.fine("Desktop Entry found: "+dent );
			} else if( enn.contains( DesktopGroup.class.getName() ) ) {
				if(log.isLoggable(Level.FINER) ) log.finer("handling DesktopGroup entry");
				group = ((DesktopGroup)sda.getData()).group;
				trans = ((DesktopGroup)sda.getData()).isTransient();
			}
		}

		// Look for all UIDescriptors and add DesktopItems for each role.
		int cnt = 0;
		log.finer("Using name: "+name+", group: "+group );
		for( ServiceDataAccess<? extends Entry> sda: ents ) {
			List<String>enn = sda.getDataClassNames();
			if( enn.contains( UIDescriptor.class.getName() ) ) {
				log.finer("ent["+cnt+"] ServiceUI("+
					sda.getData().getClass().getName()+
					") found for: "+name );
				cnt++;
				UIDescriptor uid = (UIDescriptor)sda.getData();
				DesktopEntry dent = desks.get( uid.role );
				if( dent != null ) {
					log.fine( "using desktop: "+dent );
					icon = dent.iconUrl;
					group = dent.group;
					trans = dent.istrans;
				}
				String micon = icons.get( uid.role );
				if( micon != null ) {
					log.fine("overriding role icon: "+uid.role+"="+micon);
					icon = micon;
				}
				final String fname = name;
				final ServiceEntry fse = se;
				final String ficon = icon;
				final String fgroup = group;
				final boolean ftrans = trans;
				final UIDescriptor fuid = uid;
				
				log.fine("adding role: "+fname+", freg: "+freg+", fuid: "+fuid );
				addRole( fname, freg, fse, 
							fuid, ficon, fgroup, ftrans, helps );
				if( mygroups.size() == 0 && allMod.size() == 1 ) {
					if( atabs.getSelectedIndex() != 2 )
						atabs.setSelectedIndex(2);
				}
//				addRole( name, freg, se, 
//					uid, icon, group, trans );
			}
		}

		// No UIDescriptor found, add a default one for services without a
		// serviceUI that will handle Administrable
		if( cnt == 0 ) {
			log.fine("No UI, adding adminstrable descr");
			UIDescriptor uid = new AdminDescriptor();
			String micon = (String)icons.get( uid.role );
			if( micon != null )
				icon = micon;
			final String fname = name;
			final ServiceEntry fse = se;
			final String ficon = icon;
			final String fgroup = group;
			final boolean ftrans = trans;
			final UIDescriptor fuid = uid;

			addRole( fname, freg, fse, 
						fuid, ficon, fgroup, ftrans, helps );
		}
		log.fine("Processing completed");
	}

	protected static void reportExceptionFor( final JFrame frame,
			final Uuid inst, final String msg, 
				final Throwable ex, final boolean show ) {
		Logger.getLogger( JiniDeskTop.class.getName() ).
			log(Level.SEVERE, ex.toString()+": "+msg, ex);
		if( !show )
			return;
		runInSwing( new Runnable() {
			public void run() {
				String str = ex.toString();
				Throwable nex = ex.getCause();
				while( nex != null ) {
					str += "\nCaused by: "+nex;
					nex = nex.getCause();
				}
				JOptionPane.showMessageDialog( frame, str, 
					msg, JOptionPane.ERROR_MESSAGE );
			}
		});
	}

	protected void reportMessage( final String msg, final Throwable ex ) {
		log.log(Level.SEVERE, ex.toString(), ex );
		runInSwing( new Runnable() {
			public void run() {
				String str = msg;
//				Throwable nex = ex.getCause();
//				while( nex != null ) {
//					str += "\nCaused by: "+nex;
//					nex = nex.getCause();
//				}
				JOptionPane.showMessageDialog( frame, str, 
					"Unexpected Error",  JOptionPane.ERROR_MESSAGE );
			}
		});
	}

	protected void reportException( final Throwable ex ) {
		log.log(Level.SEVERE, ex.toString(), ex);
		runInSwing( new Runnable() {
			public void run() {
				String str = ex.toString();
				Throwable nex = ex.getCause();
				while( nex != null ) {
					str += "\nCaused by: "+nex;
					nex = nex.getCause();
				}
				JOptionPane.showMessageDialog( frame, str, 
					"Unexpected Error",  JOptionPane.ERROR_MESSAGE );
			}
		});
	}
	
	protected static void runInSwing( final Runnable r ) {
		if( SwingUtilities.isEventDispatchThread() ) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(r);
			} catch( Exception ex ){
			}
		}
	}

	private static class ServiceRegistration implements Serializable {
		ServiceID id;
		String role;
		String group;
		static final long serialVersionUID = 1L;

		public ServiceRegistration( ServiceID id, String role, String group ) {
			this.id = id;
			this.role = role;
			this.group = group;
		}

		public boolean equals( Object obj ) {
			if( obj instanceof ServiceRegistration == false )
				return false;
			ServiceRegistration sr = (ServiceRegistration)obj;
			return sr.id.equals(id) && sr.role.equals(role) && sr.group.equals(group);
		}

		public int hashCode() {
			return (((id.hashCode() ^ role.hashCode())<<8) + role.hashCode()) ^ group.hashCode();
		}
	}

	/**
	 *  Map of known services by serviceID
	 */
	protected Hashtable<ServiceRegistration,
			Hashtable<ServiceRegistrar,ServiceID>> known = 
		new Hashtable<ServiceRegistration,
			Hashtable<ServiceRegistrar,ServiceID>>();

	protected Hashtable<ServiceRegistrar,Vector<ServiceRegistration>> svcregs =
		new Hashtable<ServiceRegistrar,Vector<ServiceRegistration>>();

	protected void resetAll() {
		
		saveGroups();
		runInSwing( new Runnable() {
			public void run() {
				known.clear();
				svcregs.clear();
				groups.clear();
				seen.clear();
				seSeen.clear();

				minev.removeAllElements();

				mine.setModel( new DefaultComboBoxModel( minev ) );
				moreRoot.removeAllElements();
				allRoot.removeAllElements();
				Component[]arr = pane.getComponents();
				for( int i = 0; i < arr.length; ++i ) {
					if( arr[i] instanceof DesktopItem ) {
						pane.remove( arr[i] );
					}
				}
				allMod.update();
				moreMod.update();
				allList.repaint();
				moreList.repaint();
				new Thread("Loading Groups") {
					public void run() {
						loadGroups();
					}
				}.start();
			}
		});
	}

	protected void updateServiceEvent( ServiceRegistrar reg, ServiceEvent ev ) {
		dropServiceEvent( reg, ev );
		addServiceEvent( reg, ev );
	}
	
	protected void dropServiceEvent( ServiceRegistrar reg, ServiceEvent ev ) {
		Vector<ServiceRegistration> v = svcregs.get( reg );
		log.fine("looking for: "+reg+" in "+svcregs );
		if( v == null ) {
			log.warning("No registered services for: "+reg+", can't drop service: "+ev );
			return;
		}
		
		for( ServiceRegistration sr: v ) {
			if( sr.id == ev.getServiceID() ) {
				v.remove( sr );
				Hashtable<ServiceRegistrar,ServiceID> h = 
					known.get( sr );
				h.remove( reg );
				if( h.size() == 0 )
					known.remove( reg );
				Vector<DesktopItem>removes = new Vector<DesktopItem>();
				Vector<DesktopItem>dv = groups.get( sr.group );
				if( dv != null ) {
					for( DesktopItem di: dv ) {
						if( di.sr.equals(sr) ) {
							removes.addElement(di);
							dv.removeElement( di );
							removeMineItem(di);
							final DesktopItem fdi = di;
							serviceLost( di.svcItem.serviceID, reg );
							seen.remove( di.svcItem.serviceID );
							runInSwing( new Runnable() {
								public void run() {
									pane.remove(fdi);
									pane.repaint();
								}
							});
							break;
						}
					}
					for( DesktopItem di: removes ) {
						di.active = false;
						dv.remove( di );
					}
				}
				runInSwing( new Runnable() {
					public void run() {
						allMod.update();
						moreMod.update();
						allList.repaint();
						moreList.repaint();
					}
				});
				log.fine("Removed service definition: "+sr);
				return;
			}
		}
		log.fine("No service definition for: "+ev );
	}

	/**
	 *  Remove all references to the passed ServiceRegistrar.  If that
	 *  removes the last reference to a service, its desktop item is
	 *  removed as well.
	 */
	protected void removeItemsFor( final ServiceRegistrar reg ) {
		// Check for any ServiceRegistrations for this registrar.
		Vector<ServiceRegistration> v = svcregs.remove( reg );
		if( v == null )
			return;

		// Cycle through the entries
		for( ServiceRegistration sr: v ) {
			log.fine("Removing registration for: "+sr );
			// Get the map from registrar to serviceID registered with
			Hashtable<ServiceRegistrar,ServiceID> h = 
				known.get( sr );
			// Get the serviceID for this service
			if( h != null )
				h.remove(reg);
			// If this is the last entry, then remove the desktop item
			if( h == null || h.size() == 0 ) {
				log.fine( "Removing item: " + sr +
					", last registration from: "+reg );
				// Remove the 
				known.remove( sr );
				Vector<DesktopItem>removes = new Vector<DesktopItem>();
				Vector<DesktopItem>dv = groups.get( sr.group );
				if( dv != null ) {
					for( DesktopItem di: dv ) {
						if( di.sr.equals(sr) ) {
							removes.addElement(di);
							dv.removeElement( di );
							removeMineItem(di);
							final DesktopItem fdi = di;
							serviceLost( di.serviceID(), reg );
							seen.remove( di.serviceID() );
							runInSwing( new Runnable() {
								public void run() {
									pane.remove(fdi);
									pane.repaint();
								}
							});
							break;
						}
					}
					for( DesktopItem di: removes ) {
						di.active = false;
						dv.remove( di );
					}
				}
			}
		}
		try {
			Exporter exp = remotelisteners.remove(reg);
			log.fine("Unexporting RemoteEventListener for: "+reg );
			exp.unexport(true);			
		} catch( Exception ex ) {
			log.log(Level.WARNING, ex.toString(), ex );
		}
		runInSwing( new Runnable() {
			public void run() {
				allMod.update();
				moreMod.update();
				allList.repaint();
				moreList.repaint();
			}
		});
	}

	private void registerHelp( HelpEntry he ) {
		synchronized( helpsets ) {
			List<HelpEntry> le = helpsets.get( he.getName() );
			if( le == null ) {
				le = new ArrayList<HelpEntry>(2);
				helpsets.put( he.getName(), le );
			}
			if( le.contains( he ) == false ) {
				le.add(he);
			}
		}
	}

	protected HashMap<String,List<HelpEntry>>helpsets =
		new HashMap<String,List<HelpEntry>>();
	protected Hashtable<String,NamedVector<DesktopItem>> groups =
			new Hashtable<String,NamedVector<DesktopItem>>();

	/**
	 *  Create a new desktop Item for the named service using
	 *  the indicated UIDescriptor for the serviceUI.
	 *  @param name the name for the service typically from a Name Entry.
	 *  @param reg the registrar we learned about it from
	 *  @param svcItem the associated ServiceItem
	 *  @param uid the UIDescriptor for the serviceUI
	 *  @param icon the icon to use
	 *  @param group the group this role should be associated with
	 *  @param trans is this a transient or persistent registration?
	 *
	 *  @see DesktopGroup
	 *  @see DesktopIcon
	 */
	protected void addRole( String name,
			ServiceRegistrar reg, 
			ServiceItem svcItem, UIDescriptor uid, String icon, 
				String group, boolean trans, List<HelpEntry>helps ){

		Hashtable<ServiceRegistrar,ServiceID> h;
		String els[] = new String[]{};
		if( uid.role != null )
			els = uid.role.split("\\.");

		String role = "admin";
		if( els != null && els.length > 0 )
			role = els[els.length-1];

		if( group == null )
			group = "Other";

		/** Check for registrations of this Role.  This ServiceRegistration is only used
		 *  as a key, it doesn't get used elsewhere
		 */
		ServiceRegistration sr = new ServiceRegistration( svcItem.serviceID, uid.role, group );
		if( (h = known.get(sr)) == null ) {
			// Add new registration map
			h = new Hashtable<ServiceRegistrar,ServiceID>();
			known.put( sr, h );
		}
		h.put( reg, svcItem.serviceID );

		// Save the registrations for each LUS.
		Vector<ServiceRegistration>sv = svcregs.get(reg);
		if( sv == null ) {
			sv = new Vector<ServiceRegistration>();
			svcregs.put( reg, sv );
		}

		if( sv.contains( sr ) == false )
			sv.addElement( sr );

		// Build the desktop item
		DesktopItem it;
		List<String>hlpnm = new ArrayList<String>(helps.size());
		for( HelpEntry he : helps ) {
			if( he != null ) {
				hlpnm.add(he.getName());
				registerHelp( he );
			}
		}
		if( icon != null ) {
			Icon ic = null;
			try {
				ic = new ImageIcon( new URL( icon ) );
			} catch( Exception ex ) {
				log.log( Level.WARNING, ex.toString(), ex );
			}
			it = new DesktopItem( ic, name, svcItem, uid, role, trans, group, hlpnm );
		} else {
			it = new DesktopItem( name, svcItem, uid, role, trans, group, hlpnm );
		}

		NamedVector<DesktopItem> v;
		// If the group is not visible yet, build it and attach all the
		// tree branches for the group.
		synchronized( this ) {
			if( (v = groups.get( group )) == null ) {
				v = new NamedVector<DesktopItem>(group);
				groups.put( group, v );
				buildGroups( groups.keySet() );
				allRoot.addElement(v);
				Collections.sort( allRoot, new VectorSorter<NamedVector<DesktopItem>>() );
			}
		}

		// Put the desktop item in the list if we care about this group.
		v.addElement( it );
		if( mygroups.get(group) != null ) {
			addMineItem( it );
			log.finest("adding ("+group+"): "+it );
//			mine.addItem(it);
		}

		// Sort the items in the groups list
		Collections.sort( v, new Comparator<DesktopItem>() {
			public int compare( DesktopItem i1, DesktopItem i2 ) {
				return i1.name.compareToIgnoreCase( i2.name );
			}
		});

		// Check if we have a saved desktop location and place the item on the desk if so
		Point p = (Point)desklocs.get( sr );
		if( p != null ) {
			placeItem( it, p );
		}
		
		doUpdates();
	}
	
	private void doUpdates() {
		synchronized( updLock ) {
			if( updsInprog == false ) {
				updsInprog = true;
				SwingUtilities.invokeLater( new Runnable() {
					public void run() {
						updsInprog = false;
						allMod.update();
						moreMod.update();
						expandTree( moreList, moreMod );
						expandTree( allList, allMod );
						allList.repaint();
						moreList.repaint();
					}
				} );
			}
		}
	}

	private Object updLock = new Object();
	volatile boolean updsInprog;
	/**
	 *  Create a new desktop Item for the named service using
	 *  the indicated UIDescriptor for the serviceUI.
	 *
	 *  @param name the name for the service typically from a Name Entry.
	 *  @param reg the registrar we learned about it from
	 *  @param  se the associated ServiceEntry
	 *  @param uid the UIDescriptor for the serviceUI
	 *  @param icon the icon to use
	 *  @param group the group this role should be associated with
	 *  @param trans is this a transient or persistent registration?
	 *
	 *  @see DesktopGroup
	 *  @see DesktopIcon
	 */
	protected void addRole( String name,
			ServiceLookup reg, 
			ServiceEntry se, UIDescriptor uid, String icon, 
				String group, boolean trans, List<HelpEntry>helps ) {

		try {
			if(log.isLoggable(Level.FINE) ) log.fine("adding "+name+" ("+se.getServiceID()+") from "+
				((ServiceRegistrar)reg).getLocator() );
		} catch( Exception ex ) {
			reportException(ex);
		}
		Hashtable<ServiceRegistrar,ServiceID> h;
		String els[] = new String[]{};
		if( uid.role != null )
			els = uid.role.split("\\.");

		String role = "admin";
		if( els != null && els.length > 0 )
			role = els[els.length-1];

		if( group == null )
			group = "Other";

		/** Check for registrations of this Role.  This ServiceRegistration is only used
		 *  as a key, it doesn't get used elsewhere
		 */
		ServiceRegistration sr = new ServiceRegistration( se.getServiceID(), uid.role, group );
		if( (h = known.get(sr)) == null ) {
			// Add new registration map
			h = new Hashtable<ServiceRegistrar,ServiceID>();
			known.put( sr, h );
		}
		h.put( (ServiceRegistrar)reg, se.getServiceID() );

		// Save the registrations for each LUS.
		Vector<ServiceRegistration>sv = svcregs.get(reg);
		if( sv == null ) {
			sv = new Vector<ServiceRegistration>();
			svcregs.put( (ServiceRegistrar)reg, sv );
		}

		if( sv.contains( sr ) == false )
			sv.addElement( sr );

		// Build the desktop item
		DesktopItem it;
		List<String>hlpnm = new ArrayList<String>(helps.size());
		for( HelpEntry he : helps ) {
			if( he != null ) {
				hlpnm.add(he.getName());
				registerHelp( he );
			}
		}
		if( icon != null ) {
			Icon ic = null;
			try {
				ic = new ImageIcon( new URL( icon ) );
			} catch( Exception ex ) {
				log.log( Level.WARNING, ex.toString(), ex );
			}
			it = new DesktopItem( ic, name, se, uid, role, trans, group, hlpnm );
		} else {
			it = new DesktopItem( name, se, uid, role, trans, group, hlpnm );
		}

		NamedVector<DesktopItem> v;
		// If the group is not visible yet, build it and attach all the
		// tree branches for the group.
		boolean newgroup = false;
		Set<String>keyset = null;

		synchronized( groups ) {
			if( (v = groups.get( group )) == null ) {
				v = new NamedVector<DesktopItem>(group);
				groups.put( group, v );
				newgroup = true;
				// Get a copy of the keys for buildGroups
				keyset = new HashSet<String>( groups.keySet() );
			}
		}

		if( newgroup ) {
			buildGroups( keyset );
			final NamedVector<DesktopItem> fv = v;
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					log.info("Adding group: "+fv );
					allRoot.addElement( fv );
					Collections.sort( allRoot, new VectorSorter<NamedVector<DesktopItem>>() );
				}
			});
		}

		// Put the desktop item in the list if we care about this group.
		v.addElement( it );
		if( mygroups.get(group) != null ) {
			addMineItem( it );
			log.finest("adding ("+group+"): "+it );
//			mine.addItem(it);
		}

		synchronized( v ) {
		// Sort the items in the groups list
		Collections.sort( v, new Comparator<DesktopItem>() {
			public int compare( DesktopItem i1, DesktopItem i2 ) {
				return i1.name.compareToIgnoreCase( i2.name );
			}
		});
		}

		// Check if we have a saved desktop location and place the item on the desk if so
		Point p = (Point)desklocs.get( sr );
		if( p != null ) {
			placeItem( it, p );
		}

		doUpdates();
	}
	
	private static class VectorSorter<T> implements Comparator<T> {
		public int compare( T i1, T i2 ) {
			return i1.toString().compareTo( i2.toString() );
		}
	}
	
	private void removeMineItem( final DesktopItem it ) {
		runInSwing( new Runnable() {
			public void run() {
				minev.removeElement(it);
				mine.setModel( new DefaultComboBoxModel( minev ) );
			}
		});
	}

	private void addMineItem( DesktopItem it ) {
		minev.addElement(it);
		synchronized( minev ) {
			Collections.sort(minev, new Comparator<DesktopItem>() {
				public int compare( DesktopItem i1, DesktopItem i2 ) {
					return i1.toString().compareTo( i2.toString() );
				}
			});
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					mine.setModel( new DefaultComboBoxModel( minev ) );
				}
			});
		}
	}
	
	private Vector<DesktopItem> minev = new Vector<DesktopItem>();

	public Action getAction( String name ) {
		return am.getAction(name);
	}

	protected void buildActions() {
		am.defineAction( "Rescan Services", new LabeledAction("Rescan Services", 
			"Rescan Services" ) {
			public void actionPerformed( ActionEvent ev ) {
				new ComponentUpdateThread( am.getAction("Rescan Services") ) {
					public void setup() {
						flushServices();
						moreList.repaint();
					}

					public Object construct() {
						try {
							log.fine("Starting Lookup");
							startLookup();
						} catch( Exception ex ) {
							reportException(ex);
						}
						return null;
					}
				}.start();
			}
		});

		am.defineAction( "Configure...", new LabeledAction( "Configure..." ) {
			public void actionPerformed( ActionEvent ev ) {
				configureGroups();
			}
		});

		// Example to get an Icon
		am.defineAction( "xxxx", new LabeledAction( loadIcon("xmit.gif"),
			"xxxx", false ) {
			public void actionPerformed( ActionEvent ev ) {
			}
		});
	}

	private class GroupEntry {
		public String grp;

		public GroupEntry( String grp ) {
			this.grp = grp;
		}

		public String toString() {
			return grp+": "+
				(((groups.get(grp) == null) || size() == 0 ) ? 
				"<empty>" : size()+"");
		}

		public int size() {
			return groups.get(grp).size();
		}
	}

	private void configureGroups() {
		JDialog d = new JDialog( frame, "Configure Groups", true );
		Packer pk = new Packer( d.getContentPane() );
		final VectorListModel<GroupEntry> lmod;
		final JList lst = new JList( lmod = new VectorListModel<GroupEntry>() );
		for( String nm: groups.keySet() ) {
			lmod.addElement( new GroupEntry( nm ) );
		}
		JPanel lp = new JPanel();
		Packer lpk = new Packer( lp );
		lp.setBorder( BorderFactory.createTitledBorder( "Known Groups" ) );
		pk.pack( lp ).gridx(0).gridy(0).fillboth();
		lpk.pack(  new JScrollPane( lst ) ).fillboth();
		JPanel bp = new JPanel();
		Packer bpk = new Packer(bp);
		pk.pack( bp ).gridx(1).gridy(0).filly().inset(5,5,5,5);
		int y = -1;
		final JButton del;
		bpk.pack( del = new JButton( "Delete" ) ).gridx(0).gridy(++y).fillx().weightx(0);
		bpk.pack( new JPanel() ).gridx(0).gridy(100).filly();
		ModalComponent mc = new ModalComponent( lst, null );
		mc.add( del );
		mc.configure();

		del.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				GroupEntry ge = lmod.elementAt( lst.getSelectedIndex() );
				NamedVector<DesktopItem> dv = groups.get(ge.grp);
				if( dv != null && dv.size() > 0 ) {
					JOptionPane.showMessageDialog( frame, "Group is not empty",
						"Group delete stopped", JOptionPane.WARNING_MESSAGE );
					return;
				}
				groups.remove( ge.grp );
				mygroups.remove( ge.grp );
				lmod.removeElementAt( lst.getSelectedIndex() );
				lst.clearSelection();
				moreRoot.remove( new NamedVector(ge.grp) );
				new ComponentUpdateThread( del ) {
					public void setup() {
						buildGroups(groups.keySet());
					}
					public Object construct() {
						saveGroups();
						return null;
					}
				}.start();
			}
		});

		d.pack();
		d.setSize(200,200);
		d.setLocationRelativeTo( frame );
		d.setVisible(true);
		d.dispose();
	}

	public static Icon loadIcon( String name ) {
		URL u = JiniDeskTop.class.getClassLoader().getResource( "images/"+name );
		if( u != null )
			return new ToolIcon( u );	
		return null;
	}
	
	private static class ToolIcon extends ImageIcon {
		static int sz = 2;
		Icon icon;
		public ToolIcon( URL u ) {
			super(u);
			if( super.getIconWidth() > sz )
				sz = super.getIconWidth();
			if( super.getIconHeight() > sz )
				sz = super.getIconHeight();
		}
		public int getIconWidth() {
			return sz;
		}
		public int getIconHeight() {
			return sz;
		}
		public void paintIcon( Component c, Graphics g, int x, int y ) {
//			new Throwable().printStackTrace();
			int cx = x;
			int cy = y;
			int hx = super.getIconWidth();
			int hy = super.getIconHeight();
			if( hx < sz )
				cx += (sz - hx)/2;
			if( hy < sz )
				cy += (sz - hy)/2;
			super.paintIcon( c, g, cx, cy );
		}
	}

	private static abstract class LabeledToggleAction extends LabeledAction {
		boolean sel;
		Vector<ActionListener> lis;
		public void addActionListener( ActionListener l ) {
			lis.addElement(l);
		}
		public void removeActionListener( ActionListener l ) {
			lis.removeElement(l);
		}
		private void notifyListeners( ActionEvent ev ) {
			for( int i = 0; i < lis.size(); ++i ) {
				ActionListener al = (ActionListener)lis.elementAt(i);
				al.actionPerformed(ev);
			}
		}
		protected void notifyListeners() {
			notifyListeners( new ActionEvent(this,1,""+new Boolean(isSelected())) );
		}
		public void setSelected( boolean how ) {
			sel = how;
		}
		public boolean isSelected() {
			return sel;
		}
		public LabeledToggleAction( String desc ) {
			super(desc);
			lis = new Vector<ActionListener>();
		}
		public LabeledToggleAction( Icon icon, String desc ) {
			super(icon,desc);
			lis = new Vector<ActionListener>();
		}
		public LabeledToggleAction( String name, String desc ) {
			super(name,desc);
			lis = new Vector<ActionListener>();
		}
		public LabeledToggleAction( String name, String desc, boolean enabled ) {
			super( name, desc, enabled );
			lis = new Vector<ActionListener>();
		}
		public LabeledToggleAction( Icon icon, String desc, boolean enabled ) {
			super( icon, desc, enabled );
			lis = new Vector<ActionListener>();
		}
	}

	private static abstract class LabeledAction extends AbstractAction {
		public LabeledAction( String desc ) {
			if( desc != null ) {
	        		putValue( Action.NAME, desc );
	        		putValue( Action.SHORT_DESCRIPTION, desc );
			}
		}
		public LabeledAction( Icon icon, String desc ) {
	        if( icon != null )
	        	putValue( Action.SMALL_ICON, icon );
			if( desc != null )
	        	putValue( Action.NAME, desc );
	        		putValue( Action.SHORT_DESCRIPTION, desc );
		}
		public LabeledAction( String name, String desc ) {
	        if( name != null )
	        	putValue( Action.NAME, name );
	        		putValue( Action.SHORT_DESCRIPTION, name );
			if( desc != null )
	        		putValue( Action.SHORT_DESCRIPTION, desc );
		}
		public LabeledAction( String name, String desc, boolean enabled ) {
			this( name, desc );
			this.setEnabled( enabled );
		}
		public LabeledAction( Icon icon, String desc, boolean enabled ) {
			this( icon, desc );
			this.setEnabled( enabled );
		}
	}
	
	private String dumpTemplate( ServiceTemplate tmp ) {
		String str = "";
		Entry[]ents = tmp.attributeSetTemplates;
		Class[]cls = tmp.serviceTypes;
		if( ents != null ) {
			str = "Entries {";
			for( int i = 0; i < ents.length; ++i ) {
				str += ents[i].getClass().getName()+": "+ents[i].toString();
				if( i+1 < ents.length )
					str += ",";
			}
			str += "}";
		}
		if( cls != null ) {
			str += " Classes {";
			for( int i = 0; i < cls.length; ++i ) {
				str += cls[i].getName();
				if( i+1 < cls.length )
					str += ",";			
			}
			str += "}";
		}
		if( tmp.serviceID != null ) {
			str += "ID { "+tmp.serviceID + "}";
		}
		return str;
	}
}
