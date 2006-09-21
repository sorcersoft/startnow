package org.wonderly.events;

import java.security.*;

/**
 *  This permission is used to convey access to a particular
 *  topic.
 *
 *  @author <a href="mailto:gregg.wonderly@pobox.com">Gregg Wonderly</a>.
 */
public class SubscriptionPermission extends BasicPermission {
	Topic topic;

	public SubscriptionPermission( String str ) {
		super(str);
		topic = new Topic( str );
	}

	public boolean implies( Permission perm ) {
		if( perm instanceof SubscriptionPermission == false ) {
			return false;
		}

		topic.matches( ((SubscriptionPermission)perm).topic );
		return true;
	}
}