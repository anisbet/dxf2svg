
/****************************************************************************
**
**	FileName:	SvgDoubleEndedGraphicElement.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Provides a convience class to allow querying of first and last
**				points for animation as well as a way of typeId for determining
**				if animation can be applied to any of its neighbours. Neighbours
**				is defined as any other double ended Graphic element that shares
**				either a start or an end point.
**
**	Date:		June 23, 2003
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - June 23, 2003
**				0.02 - March 23, 2005 Added getAllSegmentPoints() method.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.*;
import java.util.Vector;


/**	This abstract class is the super class for all elements that have a start and end point.
*	The methods in this class allow for query of the start and end point and the comparison 
*	of the locality of the points. With this information it is possible to determine if elements
*	share the same start or end point and therefore are really one contiguous group like a wire
*	run.
*
*	@version 0.01 - June 23, 2003
*	@author Andrew Nisbet
*/
public abstract class SvgDoubleEndedGraphicElement extends SvgGraphicElement
{
	protected Point endPoint = null;
	
	/** Takes a reference to the dxf converter context (dxf converter).
	*/
	public SvgDoubleEndedGraphicElement(DxfConverter dfxc)
	{	super(dfxc);	}
	
	
	// In the case of SvgHatchLine this may be null so try and catch a NullPointerException.
	/** Returns the end point of the object. The end point is the opposite
	*	of anchor point.
	*/
	public Point getEndPoint()
	{	return endPoint;	}
	
	
	// This method was originally a final class returning anchor but SvgPolyLine
	// requires VertexPoints to be placed on a Vector so we include this to accomodate
	// that requirement.
	/**
	*	Returns the start point of a double ended graphic object (which 
	*	is the Anchor). It is a trivial implementation that duplicates 
	*	another method but this method will help keep the abstraction clear
	*	to the author's intent.
	*	@return Point The anchor point or point of origin for the object.
	*/
	public Point getStartPoint()
	{	return Anchor;	}
	
	
	/** This method returns the end points of all the segments in this object.
	*	For {@link SvgLine} and the like the method populates the argument 
	*	vector with the start and end points, but for {@link SvgPolyLine}
	*	the entire set of intersection points is returned (as references). 
	*	The method does not alter the contents of the argument vector, but
	*	simply adds the points to the end. Start point is added first, end
	*	point second.
	*	@param points A vector for storing the points. If the vector is 
	*	null the method returns; it is upto the caller to manage this.
	*/
	public void getAllSegmentPoints(Vector points)
	{
		if ( points == null )
		{
			return;
		}
		
		points.add(Anchor);
		points.add(endPoint);
	}
	
	
	
	/**	This method compares two SvgDoubleEndedGraphicElements and determines whether 
	*	either the start OR end point are local to each other. If two points are local
	*	they are said to be on the same point +- the fuzz value.
	*	<P>
	*	If the start point are located on the same spot the method exits with a true 
	*	value and does not check the end point.
	*/
	public boolean shareStartOrEndPoint(SvgDoubleEndedGraphicElement sp)
	{
		Point p1 = this.getStartPoint();
		Point t1 = sp.getStartPoint();
		Point p2 = this.getEndPoint();
		Point t2 = sp.getEndPoint();
		
		if (p1.isSamePlace(t1))
			return true;
		if (p1.isSamePlace(t2))
			return true;
		try
		{
			if (p2.isSamePlace(t1))
				return true;
			
			if (p2.isSamePlace(t2))
				return true;
		}
		catch (NullPointerException e)
		{
			// We could get here if an object like a hatch line or solid ends up on
			// the heap of objects to be tested.
			System.err.println("Warning: attempt to test a null endpoint on: "+this.getClass().getName());
		}
		
		return false;
	}
	
	
	
	protected Object clone()
	{
		SvgDoubleEndedGraphicElement sd = (SvgDoubleEndedGraphicElement)super.clone();
		
		if (this.endPoint != null)
			sd.endPoint	= (Point)this.endPoint.clone();
			
		return sd;
	}
}