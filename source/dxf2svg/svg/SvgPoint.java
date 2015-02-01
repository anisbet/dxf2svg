
/****************************************************************************
**
**	FileName:	SvgPoint.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates the Svg Point object.
**
**	Date:		November 29, 2001
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	1.00 - August 5, 2002
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;

/**
*	This class encapsulates the SVG point element. It actually takes a one
*	dimensional point from Acad and draws a Acad style of point visually
*	into the SVG. There is no such thing as just a point in SVG.
*
*	@version	1.00 - August 5, 2002
*	@author		Andrew Nisbet
*/


public class SvgPoint extends SvgGraphicElement
{
	/** Calls the super class' constructor.
	*	@see SvgGraphicElement
	*	@see SvgObject
	*	@param dxfc the conversion context
	*/
	public SvgPoint(DxfConverter dxfc)
	{
		super(dxfc);
		setType("path");
		setFill("");
	}

	// this replaces the objects toString() method which is now handled by the super class
	// SvgGraphicElement.
	/** Performs the unique calculation required to describe this object as an SVG element.*/
	protected String calculateMyUniqueData()
	{
		StringBuffer SvgPointOutput = new StringBuffer();

		SvgPointOutput.append("d=\"M");
		SvgPointOutput.append(Anchor.toStringPolyLine());
		SvgPointOutput.append(" m-10,0h20,m-10,0m0,-10v20\"");

		return SvgPointOutput.toString();
	}

}	// End of SvgPoint class