
/****************************************************************************
**
**	FileName:	SvgMPath.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Maps the data from a DXF into equivelant SvgObjects
**
**	Date:		September 13, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - May 13, 2003
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.*;

/**
*	An mpath is an object is a wrapper for a path reference that {@link dxf2svg.animation.SvgAnimator}
*	uses to apply an animation effect along.
*
*	This object looks and behaves like a SvgReference except that it does not require
*	any styling attribs or anchor to locate it, rather we rely on the target of this 
*	reference for all of that information.
*
*	@version	0.01 - May 13, 2003
*	@author		Andrew Nisbet
*/

public final class SvgMPath extends SvgReference
{	
	public SvgMPath(DxfConverter dxfc)
	{
		super(dxfc);
	}
	
	// Deactivate the anchor.
	/** Sets the anchor's x value
	*/
	public void setX(double x)
	{	}

	// Deactivate the anchor.
	/** Sets the anchor's y value
	*/
	public void setY(double y)
	{	}
	
	// Everything else is handled by the abstract super class.
}