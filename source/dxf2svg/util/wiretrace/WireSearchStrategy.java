
/****************************************************************************
**
**	FileName:	WireSearchStrategy.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Strategy interface for different types of SVG content searches.
**
**	Date:		March 14, 2005
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.2_04)
**
**	Version:	0.01 - March 14, 2005
**				0.02 - March 18, 2005 Moved this class to the wire trace 
**				name space and changed its name to reflect its job.
**
**	TODO:
**
**
*****************************************************************************/

package dxf2svg.util.wiretrace;

import java.util.Vector;
import dxf2svg.svg.SvgLayerGroup;
import java.util.regex.Pattern;
import dxf2svg.DxfConverter;
import dxf2svg.DxfPreprocessor;

/** Super class of the search strategies for wires. This class currently is sub-classed to
*	encapsulate tasks of location and modifying objects based on their 
*	relationship to other objects that might be otherwise un-related.
*/
public abstract class WireSearchStrategy
{
	protected boolean DEBUG = false;
	
	public WireSearchStrategy()
	{
		DEBUG = DxfPreprocessor.debugMode();
	}
	
	public abstract void searchMatchModify(
		DxfConverter conversionContext,
		Vector results,
		SvgLayerGroup svgl,
		Vector components,
		Pattern p
	);
	
	
	/** Returns the name of the search strategy currently being used.
	*/
	public String toString()
	{
		return this.getClass().getName();
	}
}