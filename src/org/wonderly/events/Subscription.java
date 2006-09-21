package org.wonderly.events;

import java.io.Serializable;
import org.wonderly.events.Topic;

/**
 *  Subscriptions are represented by this class.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class Subscription implements Serializable {
    String name;
    boolean listening;
    static final long serialVersionUID = 1l;
    
    public Subscription(String s, boolean flag)
    {
        name = s;
        listening = flag;
    }

    public String toString()
    {
        return (listening ? " " : "-") + name;
    }

    public boolean equals(Object obj)
    {
        return name.equals(((Subscription)obj).name);
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    public Topic getTopic()
    {
        return new Topic(name);
    }

    public String getName()
    {
        return name;
    }

    public boolean isListenedTo()
    {
        return listening;
    }
}