
/****************************************************************************
**
**	FileName:	SvgCircle.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	SvgCircle class
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
*	This class encapsulates the SVG circle element.
*
*	@version	1.00 - August 5, 2002
*	@author		Andrew Nisbet
*/


public class SvgCircle extends SvgGraphicElement
{
	protected double Radius;


	// constructor uses parent object constructor to populate ObjType
	/** Sets the fill of the circle to none and calls the super class' constructor.
	*	@see SvgGraphicElement
	*	@see SvgObject
	*	@param dxfc the conversion context
	*/
	public SvgCircle(DxfConverter dxfc)
	{
		super(dxfc);
		setType("circle");
		// We set the fill of this object because it will default fill.
		// If we don't we get filled circles. I used to counter act this
		// by setting the layer to automatically add a fill:none; but
		// have opted to include only layer information that comes from
		// the Dxf. All other information that is required to correctly
		// generate an object has to be explicitly coded.
		setFill("none");
	}

	/** Sets the radius of the arc object.*/
	public void setRadius(double r)
	{
		Radius = r;

		Radius *= svgUtility.Units();
		//Radius = svgUtility.trimDouble(Radius,2);
	}

	/** Performs the unique calculation required to describe this object as an SVG element.*/
	protected String calculateMyUniqueData()
	{
		/*
		*	Syntax for a Circle in Svg is:
		*	<circle id="LTop" cx="0" cy="18" r="113" style="stroke: black"/>
		*/

		StringBuffer CircleOutput = new StringBuffer();


		CircleOutput.append(Anchor.toStringCircle());
		Radius = svgUtility.trimDouble(Radius);
		CircleOutput.append(" r=\""+Radius+"\"");

		return CircleOutput.toString();
	}

	protected Object clone()
	{
		SvgCircle sc= (SvgCircle)super.clone();

		sc.Radius	= this.Radius;

		return sc;
	}

} // end class