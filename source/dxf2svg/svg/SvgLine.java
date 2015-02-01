
/****************************************************************************
**
**	FileName:	SvgLine.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SvgLine object definition
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**				1.01 - December 10, 2002 Added getElementAsPath() method.
**				1.02 - June 23, 2003 Made it a subclass of SvgDoubleEndedGraphicElement.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;						// DxfConverter

/**
*	This class encapsulates the SVG line element.
*
*	@version	December 10, 2002
*	@author		Andrew Nisbet
*/

public class SvgLine extends SvgDoubleEndedGraphicElement
{
	//private Point EndPoint;

	// constructor uses parent object constructor to populate ObjType
	/** Calls the super class' constructor.
	*	@see SvgGraphicElement
	*	@see SvgObject
	*	@param dxfc the conversion context
	*/
	public SvgLine(DxfConverter dxfc)
	{
		super(dxfc);
		setType("line");
		endPoint = new Point(DxfConverterRef);
		setFill("");
	}

	/** Sets the x value of the end point.
	*/
	public void setEndPointX(double x)
	{	endPoint.setX(x);	}

	/** Sets the y value of the end Point.
	*/
	public void setEndPointY(double y)
	{	endPoint.setY(y);	}

	/** Returns the description of the line as a path in a String format.
	*	@see SvgGraphicElement#getElementAsPath
	*/
	public String getElementAsPath(boolean fromPreviousPoint)
	{
		StringBuffer renderPath = new StringBuffer();

		if (fromPreviousPoint == false)
			renderPath.append("M"+Anchor.toTransformCoordinate());
		renderPath.append("L"+endPoint.toTransformCoordinate());

		return renderPath.toString();
	}

	/** Performs the unique calculation required to describe this object as an SVG element.*/
	protected String calculateMyUniqueData()
	{

		/*
		*	Syntax for a line in Svg is:
		*	<line id="LTop" x1="0" y1="18" x2="113" y2="18" style="stroke: black"/>
		* 	may have to describe them as paths to avoid the output requirement of
		*	some objects requiring x,y values and some x1,y1 as with lines and not possible
		*	with Point.toString() method as it stands now.
		*/
		StringBuffer LineOutput = new StringBuffer();

		LineOutput.append(Anchor.toStringFirstCoord() );
		LineOutput.append(endPoint.toStringSecondCoord() );

		return LineOutput.toString();
	}
	

	protected Object clone()
	{
		SvgLine sl = (SvgLine)super.clone();

		return sl;
	}
}