
/****************************************************************************
**
**	FileName:	SvgSymbol.java
**
**	Project:	Dxf2Svg
**
**	Purpose:	Encapsulates all objects from a discreet DXF layer into a
**				<g></g> tag.
**
**	Date:		September 3, 2002
**
**	Author:		Andrew Nisbet
**
**	Environment:Java(TM) 2 Runtime Environment, Standard Edition
**				(build 1.4.0_01-b03)
**
**	Version:	0.01 - September 3, 2002
**				0.02 - April 8, 2004 use setType() in construtor.
**
**	TODO:
**

**
*****************************************************************************/

package dxf2svg.svg;

import dxf2svg.*;	// DxfConverter
/**
*	This class encapsulates a collection of SvgObjects that, in the case
*	of converting Dxf files, represents a block.
*	@deprecated	This class and {@link SvgUseXLink} can be implemented if required but their
*	original purpose has been superceded by {@link SvgEntityReference} and {@link SvgEntityDeclaration}
*
*	@version	0.01 - September 3, 2002
*	@author		Andrew Nisbet
*/
public class SvgSymbol extends SvgCollection
{
	//////////// Constructor ////////////////
	// default
	/**
	*	Sets a reference to this Thread's current DxfConverter Object.
	*/
	public SvgSymbol(DxfConverter dxfc)
	{
		super(dxfc);
		////////////// April 8, 2004 //////////////
		setType("symbol");
	}


	/**
	*	Returns the collection's attributes as a StringBuffer.
	*	@return StringBuffer of formatted attributes.
	*/
	protected StringBuffer getAttributes()
	{
		StringBuffer OutputString = new StringBuffer();

		// if the ObjType is null or unknown and we still want to see it in
		// the SVG for debugging purposes
		OutputString.append(getObjID());

		if (getIncludeClassAttribute() == true)
			OutputString.append(" "+getClassAttribute());
		OutputString.append(getAdditionalAttributes());
		
		return OutputString;
	}

}	// end of SvgSymbol class