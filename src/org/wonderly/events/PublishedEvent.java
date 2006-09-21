package org.wonderly.events;

/**
 *  Events that are published with a topic and data are
 *  represented by this class.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class PublishedEvent {
	protected Topic topic;
	protected Object data;

	public PublishedEvent( Object data, Topic topic ) {
		this.topic = topic;
		this.data = data;
	}

	public Topic getTopic() {
		return topic;
	}

	public Object getEvent() {
		return data;
	}
}