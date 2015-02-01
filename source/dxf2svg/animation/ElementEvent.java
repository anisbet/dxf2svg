
/****************************************************************************
**
**	FileName:	ElementEvent.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates a graphical element's possible events; like 
**				onClick, onFocusIn etc..
**
**	Date:		March 3, 2004
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.1)
**
**	Version:	0.1 - March 3, 2004
**
**	TODO:		
**

**
*****************************************************************************/

package dxf2svg.animation;

import dxf2svg.util.Attribute;

/**	This class is the encapsulation of graphical element events. Graphical elements
*	are things like lines, polylines, polygones, text, circles and the rest. All 
*	these elements can be made to 'listen' for events like onMouseDown and onClick.
*	
*	@version	0.1 - March 3, 2004
*	@author		Andrew Nisbet
*	@deprecated Removed in favour of the more flexible and general {@link dxf2svg.util.Attribute}
*/
public class ElementEvent extends Attribute implements Cloneable
{
	public final static int ONFOCUSIN   = 0;
	public final static int ONFOCUSOUT  = 1;
	public final static int ONACTIVATE  = 2;
	public final static int ONCLICK     = 3;
	public final static int ONMOUSEDOWN = 4;
	public final static int ONMOUSEUP   = 5;
	public final static int ONMOUSEOVER = 6;
	public final static int ONMOUSEMOVE = 7;
	public final static int ONMOUSEOUT  = 8;
	public final static int ONLOAD      = 9;
	
	private final static String[] events = {
		"onfocusin",
		"onfocusout",
		"onactivate",
		"onclick",
		"onmousedown",
		"onmouseup",
		"onmouseover",
		"onmousemove",
		"onmouseout",
		"onload",
	};
	
	//////////////////////
	//   Constructor    //
	//////////////////////
	
	/**	Constructor that takes the name of an event and the event itself as its args.
	*	@throws InvalidGraphicElementEvent if the event type is not defined or illegal.
	*/
	public ElementEvent(String eventName, String eventAction)
	{
		convertEventType(eventName);
		setAttributeValue(eventAction);
	}	
	
	//////////////////////
	//      Methods     //
	//////////////////////
	/** Applies the action required to take place to the event as follows:
	*	<CODE>onclick=&quot;<EM>action</EM>&quot;</CODE>. This method could also be used
	*	to change the events objects target event action.
	*/
	public void setEventAction(String action)
	{
		setAttributeValue(action);
	}
	
	
	/** Converts a String request for an event type into an integer value
	*	based on error checking to see if the event is of a valid type.
	*	@throws InvalidGraphicElementEvent if the type of event is undefined
	*	or an invalid type of event for an SVG object.
	*/
	protected void convertEventType(String type)
	{
		String testType = type.toLowerCase();
		
		for (int i = 0; i < events.length; i++)
		{
			if (testType.compareTo(events[i]) == 0)
			{
				setAttribute(events[i]);
				return;
			}
		}
		
		throw new InvalidGraphicElementEvent(type);
	}
	
	
	//////////////////////////
	//   Internal Classes   //
	//////////////////////////
	/** This Runtime Exception gets thrown if the client of this object
	*	requests an invalid event type; an event that is not supported 
	*	at this time.
	*/
	protected class InvalidGraphicElementEvent extends RuntimeException
	{
		protected InvalidGraphicElementEvent(String type)
		{
			System.err.println("No such event type: '"+type+"'.");
			System.err.println("If you are trying to apply an attribute and you "+
				"are sure that the event name is correct, try using the super class"+
				" Attribute and apply a generic attribute to the element.");
		}
	}
}