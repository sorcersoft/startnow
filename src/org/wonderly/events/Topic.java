package org.wonderly.events;
 
import java.io.*;
import java.util.*;

/**
 *  This class represents the topics that are used throughout
 *  the System for specifying how published data is
 *  tracked and conveyed in the system.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class Topic  implements Serializable {
    private static final String SEPARATOR = ".";
    private static final String MATCH_CURRENT = "*";
    private static final String MATCH_REMAINING = "<";
    private String parts[];
    private String whole;
    static final long serialVersionUID = 1l;
 
    public Topic(String s) {
    	whole = s;
		parts = s.split(".");
    }

    public boolean equals(Object obj) {
        if(!(obj instanceof Topic))
            return false;
        Topic topic = (Topic)obj;
        return topic.whole.equals( whole );
    }

    public String toString()  {
    	return whole;
    }

    public int hashCode() {
    	return whole.hashCode();
    }

    public boolean matches( Topic topic) {
        if(parts.length != topic.parts.length && !last().equals("<") && !topic.last().equals("<"))
            return false;
        for(int i = 0; i < parts.length; i++)
        {
            String s = parts[i];
            String s1 = topic.parts[i];
            if(s.equals("<") || s1.equals("<"))
                return true;
            if(!s.equals(s1) && !s.equals("*") && !s1.equals("*"))
                return false;
        }

        return true;
    }

    private String last() {
        return parts[parts.length - 1];
    }
}