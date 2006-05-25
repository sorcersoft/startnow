package org.wonderly.util.jini2;

/*

Applet/JApplet are strange critters from the perspective that the
implementation of that class is always provided by a web browser/custom JVM
implementation since there is, otherwise, no way to provide the document base
and other information that the methods on the object return.

I am working on an application, where we will, perhaps embed applications into
a substrate similar to the web page use of applets.  I have started to
recognize that more communications is needed between the applications and the
substrate.  I've been messing around with some APIs, trying to get a feel for
what seems useable to start with.

Some of the interactions that seem to need a container/contained relationship are:

o       The user wants to close the applications: need something like Applet.stop().
o       The user launches the application: need something like Applet.start() t
o
        decouple component creation from actions in the component such as prompting
        for user input using a properly parented dialog.
o       The user launches the application which needs to be granted some dynami
c
        policies:  need a PolicyManagementFactory or some such that lets the
        application specify to the user what policy it wants to assert and then
        another object can use the factory to get that policy (as permissions?),
        and prompt the user for confirmation, and then assert that policy.  There
        are of course object level policy validations that happen dynamically, so
        something more than permissions is probably required.
o       There is the whole notion of titling and such.  In a desktop environment,
        the 'active' application needs to be enumerated in the title bar(s) and
        a JComponentFactory doesn't really provide a way to let the parent and
        the component interact.  It seems that something like the AppletContext
        would be useful in serviceUI.  It seems like the calls into the factories 
        should include a context of some sort.

Considering Gary's comments and the things that I've thought about, it seems
like perhaps a new subclass of JComponentFactory is needed for desktop
applications that embed serviceUIs.  Here's somethings that I just invented.
I balanced the issues between serviceUI control of the container and container
discovery of serviceUI capabilities to try and make it less likely that a
serviceUI would have interfering powers explicitly through the API.  It can
certainly exploit the component hierarchy to discover things that can allow it
to change how it interacts with the user.
*/

import javax.swing.*;
import java.security.*;
import net.jini.id.*;
import net.jini.lookup.ui.*;
import net.jini.lookup.ui.factory.*;

public interface JDesktopComponentFactory
                extends JComponentFactory,JMenuBarFactory {

        // Get the JComponent for this component (if any, might be menu only).
        public JComponent getJDesktopComponent( JDesktopContext ctx, Object item );

        // Get the JMenuBar for this component (if any)
        public JMenuBar getJMenuBar( JDesktopContext ctx );

        // Get the title applicable to this component (if any)
        public String getTitle( JDesktopContext ctx );
//
//        // Get the permissions (if any) needed
//        public Permission[] getRequiredPolicy( JDesktopContext ctx );

        // The Uuid identifying this instance to the container
        public Uuid getInstanceId();
}
