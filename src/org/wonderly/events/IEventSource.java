package org.wonderly.events;

/**
 *  An interface for Objects that are the source of
 *  events.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public interface IEventSource {
	public void disconnect( IEventSync sync );
}