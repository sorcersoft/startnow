package org.wonderly.jini2;

import java.io.IOException;

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
public interface NameableObject {
	public String getPackage() throws IOException;
	public String getName() throws IOException;
}
	