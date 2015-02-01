
/****************************************************************************
**
**	FileName:	SvgLayerSubGroup.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates Dxf layer sub grouped objects into a <g></g> tags
**				for the purposes of adding animation.
**
**	Date:		August 30, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - July 20, 2003
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.*;
import dxf2svg.animation.*;

/**
*	This class represents sub groups, or children of the parent layer group.
*	These sub-groups contain related segments that may represent wire runs
*	or other objects that are bound by being on a specific layer <EM>and</EM>
*	share common vertices.
*
*	@version	0.01 - July 20, 2003
*	@author		Andrew Nisbet
*	@deprecated	March 05, 2004 This class is now exactly the same as a {@link SvgLayerGroup} 
*	but without any layer colour or layer name information.
*/
public final class SvgLayerSubGroup extends SvgGroup
{
	//////////// Constructors ////////////////
	// default
	/**
	*	Sets the DxfConverter object reference.
	*	@param dxfc conversion context
	*/
	public SvgLayerSubGroup(DxfConverter dxfc)
	{
		super(dxfc);
	}
}