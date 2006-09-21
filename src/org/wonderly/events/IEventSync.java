package org.wonderly.events;

import org.wonderly.events.PublishedEvent;

/**
 *  An interface for objects that can have events
 *  enqueued to them
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public interface IEventSync {
	public void enqueue( PublishedEvent ev );
}