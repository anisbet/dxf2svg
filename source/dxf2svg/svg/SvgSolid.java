
/****************************************************************************
**
**	FileName:	SvgSolid.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates the Dxf object SOLID.
**
**	Date:		October 7, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - October 7, 2002
**				0.10 - November 17, 2004 Modified how fills are handled in 
**				this class's getFill().
**				0.11 - February 10, 2005 Updated @param tag with parameter name to 
**				correct error with Javadoc 1.4.2-04.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;
import dxf2svg.DxfPreprocessor;

/**
*	Encapsulates the Dxf object SOLID. A solid in SVG, is a polygon 
*	with a fill. The fill is taken from the style sheet description 
*	unless it has been specifically set by the user to be a different
*	colour.
*
*	@version	0.10 - November 17, 2004
*	@author		Andrew Nisbet
*/

public class SvgSolid extends SvgPolyLine
{
	// constructor uses parent object constructor to populate ObjType
	/** Calls the super class' constructor.
	*	@see SvgGraphicElement
	*	@see SvgObject
	*	@param dxfc the conversion context.
	*/
	public SvgSolid(DxfConverter dxfc)
	{
		super(dxfc);
		setType("polygon");
	}

	/** This method over-rides the {@link SvgObject#getFill} to test
	*	for the default polyline fill value of 'none'. If if finds
	*	'fill:none;' it means that the solid takes the layer's
	*	colour attribute. If it is anything else
	*	it means that the illustrator has changed the colour value
	*	of this object to something other from the value of the layer.
	*	@see SvgObject#getFill
	*	@see SvgObject#setFill
	*	@return String <code>fill:&#035;somenum;</code>
	*/
	protected String getFill()
	{
		// The super class SvgPolyline sets the fill to none. SvgSolid must have a fill
		// and if it is not explicitly set, then drop the fill so it will default to the
		// layer's fill in the style sheet. If we explicitly set the fill we cannot manipulate
		// it with JavaScript.
		//
		// If the fill is still the default from the super class then just add stroke none
		// because we don't need the fill set (use the layer's).
		if (Fill.equals("fill:none;") || DxfPreprocessor.isColourCoercedByLayer())
		{
			return "stroke:none;";
		}

		// Solids don't have strokes so add that here or we get a polyline
		// with its stroke the colour of the layer it came from and the fill
		// potentially set to something different.
		return Fill + "stroke:none;";
	}

	/**	This method swaps point three and point four to allow the Dxf's solid
	*	draw order to follow the draw order of a polyline as shown below<br>
	*	Solid draw order:<BR>
	*	<pre>
	*	1------3<br>
	*	|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br>
	*	|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br>
	*	2------4</pre><br><br>
	*	Regular polyline:<br>
	*	<pre>
	*	1------4<br>
	*	|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br>
	*	|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br>
	*	2------3</pre><br>
	*/
	protected void swapPointThreeFour()
	{
		if (PolyLinePoints.size() < 4)
			return;

		VertexPoint p3, p4;

		p4 = (VertexPoint)PolyLinePoints.remove(3);
		p3 = (VertexPoint)PolyLinePoints.remove(2);

		PolyLinePoints.add(p4);
		PolyLinePoints.add(p3);
	}


	/**
	*	Calculates the unique ordering of SOLID's points.
	*/
	protected String calculateMyUniqueData()
	{
		getFill();
		swapPointThreeFour();
		
		StringBuffer SvgPolyLineOut = new StringBuffer();

		SvgPolyLineOut.append(" points=\"");
		SvgPolyLineOut.append(extractPolyLinePoints());
		SvgPolyLineOut.append("\"");

		return SvgPolyLineOut.toString();
	}

}	// end of class SvgSolid.