
/****************************************************************************
**
**	FileName:	LinkSearchStrategy.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Strategy interface for different types of SVG content searches.
**
**	Date:		March 30, 2005
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.2_04)
**
**	Version:	0.01 - March 30, 2005
**
**	TODO:
**
**
**
*****************************************************************************/

package dxf2svg.util.svglink;

import dxf2svg.svg.SvgLayerGroup;
import dxf2svg.DxfConverter;
import java.util.regex.Pattern;

/** Super class of the search strategies for Part Numbers. This class currently is sub-classed to
*	encapsulate tasks of location and modifying objects based on their 
*	relationship to other objects that might be otherwise un-related.
*/
public abstract class LinkSearchStrategy
{
	public LinkSearchStrategy(){}
	
	/** Searches matches and modifies objects.
	*	@param svgl layer group to search for pattern.
	*	@param p Pattern to search for with in the layer.
	*	@param dxfc Dxf conversion context for creating other elements on demand.
	*/
	public abstract void searchMatchModify(	SvgLayerGroup svgl,	Pattern p, DxfConverter dxfc );
	
	/** Returns the name of the search strategy currently being used.
	*/
	public String toString()
	{
		return this.getClass().getName();
	}
}