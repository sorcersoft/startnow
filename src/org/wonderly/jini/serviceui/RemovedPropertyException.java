package org.wonderly.jini.serviceui;

/**
 *  This exception can be thrown by an overridden getXXX
 *  method to cause the associated property to disappear
 *	from the list that the user sees.
 *  @version 1.0
 *  @author Gregg Wonderly -  gregg.wonderly@pobox.com
 */
public class RemovedPropertyException extends RuntimeException {
}