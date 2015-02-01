
/***************************************************************************
**
**	FileName:	WireLabelLocation.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	This trival class encapsulates an SvgObject which should 
**				be a wire (or gang) and stores it and the end of the wire
**				Where the label can be found. There is a requirement of
**				other classes to find the location of the wire number so
**				they can query this classes label location. It can be said
**				that all wires have their wire number at the opposite end
**				from the wire label (SHEET 2 of whatever).
**
**	Date:		March 17, 2005
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.2_05-b05)
**
**	Version:	0.01 - March 17, 2005
**				0.02 - August 16, 2005 Modified set ID method.
**				0.03 - August 23, 2005 Modified documentation to fix javadoc
**				reported error of incorrect link in setId() comments.
**				0.04 - September 6, 2005 Modified to get search start Point
**				to resolve two ids matching one wire. Removed redundant getLabelLocation()
**				use getWireLabelLocation() instead. Also added ID as SvgText 
**				element rather than text string. This level of abstraction aids
**				in testing and arbitrating disputes over wire ids and is just 
**				a better OOP design.
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util.wiretrace;

import dxf2svg.svg.SvgDoubleEndedGraphicElement;
import dxf2svg.util.Attribute;
import dxf2svg.svg.Point;
import dxf2svg.svg.SvgText;
import java.util.Vector;

/**	This class encapsulates an SvgObject which should 
*	be a wire (or gang) and stores it and the integer value 
*	or either {@link #START_POINT} or {@link #END_POINT} of 
*	where the label can be found. These values refer to the logical
*	start and end points of the wire. The {@link #START_POINT} is defined as the 
*	end opposite of the 'SHEET [n]' label. The {@link #END_POINT} is defined
*	as the end of the wire beside the 'SHEET [n]' label. This object 
*	contains the wire, the label location and any wire ID number as an {@link dxf2svg.svg.SvgText}
*	element.
*
*	@see #getWireIntersection
*	@see #getId
*	
*	@version	0.04 - September 6, 2005
*	@author		Andrew Nisbet
*/
public class WireLabelLocation
{
	/** This value specifies that the SHEET label was found at the start of the polyline. */
	public final static int START_POINT = -1;
	/** This value specifies that the SHEET label was found at the end of the polyline. */
	public final static int END_POINT   = -2;
	
	private SvgDoubleEndedGraphicElement wire;
	private int labelLocation;
	private boolean hasID;       // Flag for id being set.
	private SvgText id;
	
	
	/** Takes a wire from and stores it and the location of the wire label
	*	either the start or end of the wire; the wire's ID will be at the
	*	other end.
	*	@param element The wire being stored probably a polyline, but could be
	*	any SvgDoubleEndedGraphicElement.
	*	@param whichEnd {@link #START_POINT} is the label was found at the start of 
	*	the wire and {@link #END_POINT} if the label (SHEET 2) was found at the end of 
	*	the wire.
	*/
	public WireLabelLocation(SvgDoubleEndedGraphicElement element, int whichEnd)
	{
		wire              = element;
		if (whichEnd != START_POINT && whichEnd != END_POINT)
		{
			System.err.println("WireLabelLocation.constructor(): expected " +
				"START_POINT (-1) or END_POINT (-2), but received '" + whichEnd + "'");
			labelLocation = -3;
		}
		else
		{
			labelLocation = whichEnd;
		}
		hasID = false;
	}
	
	
	
	
	/** Allows the wire to take a wire id number as a String and applies it as an 
	*	ID attribute to the element. Note: if a new ID object is passed but is null the 
	*	the value is ignored.
	*	@param id id of the wire
	*	@param isDebugMode If true the id is set via {@link dxf2svg.svg.SvgElement#setObjID} method
	*	and if false a new {@link dxf2svg.util.Attribute} is added. This is done so we don't 
	*	get duplicate ID tags if the DEBUG switch is set.
	*/
	public void setId(SvgText id, boolean isDebugMode)
	{
		if ( id == null )
		{
			return;
		}
		
		if ( isDebugMode )
		{
			// Use this method rather than adding a new Attribute object
			// so if we use DEBUG we don't get duplicate ID tags.
			wire.setObjID( id.getString() );
		}
		else // This stops double 'id' attributes. If we debug use object id
		     // if we're not then add NEW attribute called 'id'.
		{
			wire.addAttribute( new Attribute( "id", id.getString() ) );
		}
		
		this.id = id;
		hasID = true;
	}
	
	
	/** Returns true if the ID for this object has been set and false otherwise.
	*/
	public boolean isIdFlagSet()
	{
		return hasID;
	}
	
	
	/** Returns the end of the wire where the wire id number is expected to be found.
	*	If the label was found at the end of the wire, then the wire ID number will
	*	be found at the start point.
	*	@return Location of the 'SHEET [n]' label. Either the wires {@link #START_POINT} or
	*	{@link #END_POINT}
	*/
	public int getWireLabelLocation()
	{
		return labelLocation;
	}
	
	
	/** Returns SvgText object of this wires id and null if the wire doesn't have an ID.
	*/
	public SvgText getId()
	{
		return id;
	}
	
	
	/** Returns the stored element, as an java.lang.Object which will need to
	*	be down cast to the appropriate SVG Object.
	*/
	public SvgDoubleEndedGraphicElement getWire()
	{
		return wire;
	}
	
	
	/** Returns any intersection point of this objects wire.
	*	If the requested intersection is out of range null will be returned.
	*	@param which the Point you require. If you require the logical start point (defined
	*	as the end opposite of the 'SHEET [n]' label then use {@link #START_POINT}.
	*	If you require the logical end Point, use {@link #END_POINT}. Any other 
	*	integer will return the requested point from the wire or null if the 
	*	requested number is out of range of the number of intersections. NOTE:
	*	intersection points are numbered from 0 to n-1.
	*	@return point The requested intersection Point of the wire or null if out of
	*	range.
	*/
	public Point getWireIntersection(int which)
	{
		switch (which)
		{
		case START_POINT:	
			if (labelLocation == START_POINT)
			{
				return wire.getEndPoint();
			}
			return wire.getStartPoint();
		
		case END_POINT:
			if (labelLocation == START_POINT)
			{
				return wire.getStartPoint();
			}
			return wire.getEndPoint();
			
		default:
			if (which >= 0)
			{
				Vector points = new Vector();
				wire.getAllSegmentPoints( points );
				if (which < points.size())
				{
					return (Point)points.get(which);
				}
			} // end if
			return null;
		} // end switch			
	} // end getIntersection()
	
	
	
	/** Returns the string value of the WireLabelLocation.
	*/
	public String toString()
	{
		StringBuffer outBuff = new StringBuffer();

		outBuff.append("starts (at 'SHEET')");		
		// give coordinates.
		if( labelLocation == START_POINT )
		{
			outBuff.append(" @ ");
			outBuff.append(wire.getStartPoint().asDxfPoint());
			outBuff.append(", ends @ ");
			outBuff.append(wire.getEndPoint().asDxfPoint());
		}
		else
		{
			outBuff.append(" @ ");
			outBuff.append(wire.getEndPoint().asDxfPoint());
			outBuff.append(", ends @ ");
			outBuff.append(wire.getStartPoint().asDxfPoint());			
		}
		
		if (isIdFlagSet())
		{
			outBuff.append(" id = '" + id.getString() + "'");
		}
		else
		{
			outBuff.append(" has no wire id.");
		}
		outBuff.append("\n");
		
		return  outBuff.toString();
	}
}