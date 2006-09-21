package org.wonderly.jini2;

import java.io.*;

/**
 *  This interface is used by objects that want to provide explicit naming
 *  control over their use of Configuration objects and other names where
 *  the package name and the objectname together make up a hierarchy of
 *  namespace.
 *
 *  If an Objects class is <code>my.domain.package.part.TheObject</code>,
 *  then the object might implement this interface, and then define the
 *  <code>getPackage()</code> to return <code>"my.domain.package.part"</code>,
 *  and <code>getName()</code> would return <code>"TheObject"</code>.
 *
 *  This allows varied control over the namespace within an application
 *  with multiple instances of a particular class, or other places where
 *  the classname is not a great way to determine the name of an object
 *  in order to get the correct information.
 */
public class NameableObjectImpl implements NameableObject {
	public String name;
	public String pkg;

	public String toString() {
		return pkg+"."+name;
	}

	public NameableObjectImpl( Class cls ) {
		init( cls );
	}
	public NameableObjectImpl() {
		init( getClass() );
	}
	
	public NameableObjectImpl( NameableObject no ) throws IOException {
		name = no.getName();
		pkg = no.getPackage();
	}
	
	private void init( Class cls ) {
		name = cls.getName();
		int nmi = name.lastIndexOf('.');
		pkg = name.substring( 0, nmi );
		name = name.substring( nmi+1 );
	}

	/**
	 *  @return name of object
	 */
	public String getName() {
		return name;
	}

	/**
	 *  @return package for object
	 */
	public String getPackage() {
		return pkg;
	}
}
	