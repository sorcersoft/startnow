package org.wonderly.util.jini2;

import java.awt.*;
import net.jini.id.*;

/**
 *

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

o       The user launches the application: need something like Applet.start().

o       Decouple component creation from actions in the component such as prompting
        for user input using a properly parented dialog.

o       The user launches the application which needs to be granted some dynamic
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

public interface JDesktopContext {
        /**
         *  Reuse this method from JComponent to get it for free
         *  if the context is already a JComponent
         */
        public Container getTopLevelAncestor();

        /**
         *  Report errors to the user with an explicit interface
         *  provided by the implementation.
         */
        public void reportException( Uuid inst, String msg, Throwable ex, boolean prompt );

        /**
         *  Allow a component to request that it get the focus.  For a desktop
         *  using JInternalFrames, this would cause the frame to be raised
         *  to the top or otherwise made visible to the user as needing
         *  attention.
         */
        public void requestFocus( Uuid inst );
 
        /**
         *  Ask to be notified when the container shutsdown the UI.
         *  This Runnable is first removed from the context, and then executed.
         *  Its execution is protected by exeception handling and any exceptions
         *  that do occur will be reported to the user.
         */
        public void registerShutdownHandler(  Uuid inst, Runnable r );
        
        /**
         *  This application is closing down, release all desktop resources
         */
        public void closing( Uuid inst );
}
