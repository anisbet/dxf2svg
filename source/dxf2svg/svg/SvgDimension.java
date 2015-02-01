
/****************************************************************************
**
**	FileName:	SvgDimension.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates a DIMENSION object from Dxf and translates it into
**				a <use> xml link reference.
**
**	Date:		September 13, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - February 26, 2003
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.DxfConverter;

/**
*	This class encapsulates Dxf Dimension. Any Dimension reference will
*	be translated into a &lt;use&gt; xml link reference in Svg that
*	references a &lt;symbol&gt; in the SVG preamble.
*
*	@version	0.01 - February 26, 2003
*	@author		Andrew Nisbet
*/
public final class SvgDimension extends SvgEntityReference
{
	/**	Set the conversion context and type of object.
	*/
	public SvgDimension(DxfConverter dxfc)
	{	super(dxfc);	}

	// These methods don't apply to this type of object so
	// I block thier functionality here.
	public final void setScaleX(double xscale)
	{	}

	public final void setScaleY(double yscale)
	{	}

	/** Sets the anchor's x value
	*/
	public void setX(double x)
	{	Anchor.setXUU(0.0);	}

	/** Sets the anchor's y value
	*/
	public void setY(double y)
	{	Anchor.setYUU(0.0);	}


}